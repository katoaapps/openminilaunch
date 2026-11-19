#!/usr/bin/env bash
set -euo pipefail

openmink_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
properties_file="$openmink_root/minkboard-source.properties"
source_root="${MINKBOARD_SOURCE_DIR:-$(cd "$openmink_root/.." && pwd)/minkboardpro}"

property() {
  local key="$1"
  local value
  value="$(sed -n "s/^${key}=//p" "$properties_file")"
  if [[ -z "$value" ]]; then
    echo "Missing ${key} in ${properties_file}" >&2
    exit 1
  fi
  printf '%s' "$value"
}

source_commit="$(property sourceCommit)"
source_branch="$(property sourceBranch)"

if [[ ! -d "$source_root/.git" ]]; then
  echo "Canonical MinkBoard checkout not found at $source_root" >&2
  echo "Set MINKBOARD_SOURCE_DIR to the canonical checkout." >&2
  exit 1
fi

if ! git -C "$source_root" cat-file -e "${source_commit}^{commit}" 2>/dev/null; then
  echo "Pinned MinkBoard commit does not exist in $source_root: $source_commit" >&2
  exit 1
fi

declare -a modules=(
  "minkboard-model:minkboardModelTree"
  "minkboard-core:minkboardCoreTree"
  "minkboard-data-android:minkboardDataAndroidTree"
  "minkboard-compose:minkboardComposeTree"
)

failed=0
for entry in "${modules[@]}"; do
  module="${entry%%:*}"
  tree_property="${entry##*:}"
  expected_tree="$(property "$tree_property")"
  pinned_tree="$(git -C "$source_root" rev-parse "${source_commit}:${module}")"
  if [[ "$pinned_tree" != "$expected_tree" ]]; then
    echo "$module: recorded tree $expected_tree does not match pinned tree $pinned_tree" >&2
    failed=1
    continue
  fi
  if [[ -n "$(git -C "$source_root" status --porcelain -- "$module")" ]]; then
    echo "$module: canonical checkout has changes not represented by $source_commit" >&2
    failed=1
  fi
  if ! diff -qr \
    --exclude build \
    --exclude .gradle \
    "$source_root/$module" \
    "$openmink_root/$module" >/dev/null; then
    echo "$module: OpenMink copy differs from canonical checkout" >&2
    failed=1
  fi
done

if [[ "$failed" -ne 0 ]]; then
  echo "MinkBoard provenance verification failed." >&2
  echo "Land shared changes in $source_root on $source_branch, refresh complete module trees," >&2
  echo "then update sourceCommit and tree hashes together." >&2
  exit 1
fi

echo "MinkBoard provenance verified at $source_commit ($source_branch)."
