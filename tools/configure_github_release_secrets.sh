#!/bin/zsh
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
repository="katoaapps/openminilaunch"
properties="$repo_root/.signing/release.properties"
keystore="$repo_root/.signing/openminilaunch-release.jks"

if [[ ! -f "$properties" || ! -f "$keystore" ]]; then
  print -u2 "Missing local release signing material in $repo_root/.signing."
  exit 1
fi

login="$(gh api user --jq .login)"
if [[ "${login:l}" != "katoaapps" ]]; then
  print -u2 "Refusing to configure secrets while GitHub CLI is logged in as $login."
  print -u2 "Authenticate the Katoa Apps account first with: gh auth login --web"
  exit 1
fi

permission="$(gh repo view "$repository" --json viewerPermission --jq .viewerPermission)"
if [[ "$permission" != "ADMIN" ]]; then
  print -u2 "$login does not have admin access to $repository."
  exit 1
fi

property_value() {
  sed -n "s/^$1=//p" "$properties"
}

base64 < "$keystore" | tr -d '\n' | gh secret set ANDROID_SIGNING_KEY_BASE64 --repo "$repository"
property_value storePassword | gh secret set ANDROID_KEYSTORE_PASSWORD --repo "$repository"
property_value keyAlias | gh secret set ANDROID_KEY_ALIAS --repo "$repository"
property_value keyPassword | gh secret set ANDROID_KEY_PASSWORD --repo "$repository"

print "Configured GitHub Actions release secrets for $repository as $login."
