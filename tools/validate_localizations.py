#!/usr/bin/env python3
"""Validate OpenMink locale resources against the US English source catalog."""

from __future__ import annotations

import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
RESOURCE_ROOT = PROJECT_ROOT / "app/src/main/res"
SOURCE_DIRECTORY = RESOURCE_ROOT / "values"
SOURCE_FILES = (
    "strings.xml",
    "strings_calendar.xml",
    "strings_demo.xml",
    "strings_localization.xml",
)
LOCALE_DIRECTORIES = (
    "values-en-rGB",
    "values-b+zh+Hans",
    "values-b+yue+Hant+HK",
    "values-ja",
    "values-ko",
    "values-hi",
    "values-es",
    "values-fr",
    "values-bn",
    "values-pt-rBR",
    "values-ru",
    "values-ur",
    "values-b+id",
    "values-ar",
)
PROTECTED_TERMS = (
    "MinkLauncher OpenSource",
    "MinkLauncher",
    "Mink Launcher",
    "OpenMink",
    "Mink’s Day",
    "Mink's Day",
    "Mink",
    "Magic Box",
    "Katoa Apps",
)
COMMAND_SYMBOLS = ("@", "#", "+")
COMMAND_RESOURCE_NAMES = {
    "magic_box_hotkey_hint",
    "magic_box_collapsed_hint",
    "everything_reachable_description",
    "recent_activity_description",
    "magic_box_find_other_apps",
    "add_to_search_and_all_apps_description",
}
ALL_COMMAND_SYMBOLS = ("@", "#", "-", "/", "+", "?")
PRINTF_PATTERN = re.compile(r"%(?:\d+\$)?[a-zA-Z]|%%")
GENERATION_TOKEN_PATTERN = re.compile(r"[ZX]{2,}Q(?:PH|SEP)|QXZ")
UNESCAPED_ATTRIBUTE_REFERENCE_PATTERN = re.compile(r"^\s*\?")


def resource_entries(directory: Path) -> dict[str, str]:
    entries: dict[str, str] = {}
    for file_name in SOURCE_FILES:
        path = directory / file_name
        if not path.is_file():
            raise ValueError(f"Missing {path.relative_to(PROJECT_ROOT)}")
        for element in ET.parse(path).getroot():
            if element.tag == "string":
                if directory == SOURCE_DIRECTORY and element.attrib.get("translatable") == "false":
                    continue
                entries[f"{file_name}|{element.attrib['name']}|string"] = element.text or ""
            elif element.tag == "plurals":
                for item in element.findall("item"):
                    entries[
                        f"{file_name}|{element.attrib['name']}|{item.attrib['quantity']}"
                    ] = item.text or ""
    return entries


def validate_locale(
    directory_name: str,
    source: dict[str, str],
) -> list[str]:
    localized = resource_entries(RESOURCE_ROOT / directory_name)
    errors: list[str] = []

    missing = sorted(source.keys() - localized.keys())
    unexpected = sorted(localized.keys() - source.keys())
    if missing:
        errors.append(f"missing {len(missing)} entries: {', '.join(missing[:5])}")
    if unexpected:
        errors.append(f"has {len(unexpected)} unexpected entries: {', '.join(unexpected[:5])}")

    for key in source.keys() & localized.keys():
        original = source[key]
        translation = localized[key]
        if not translation.strip():
            errors.append(f"{key}: blank translation")
            continue
        expected_placeholders = sorted(PRINTF_PATTERN.findall(original))
        actual_placeholders = sorted(PRINTF_PATTERN.findall(translation))
        if actual_placeholders != expected_placeholders:
            errors.append(
                f"{key}: placeholders {actual_placeholders}, expected {expected_placeholders}",
            )
        if GENERATION_TOKEN_PATTERN.search(translation):
            errors.append(f"{key}: contains an internal generation token")
        if UNESCAPED_ATTRIBUTE_REFERENCE_PATTERN.search(translation):
            errors.append(f"{key}: begins with an unescaped Android attribute marker")
        for term in PROTECTED_TERMS:
            if original.count(term) != translation.count(term):
                errors.append(f"{key}: changed protected term {term!r}")
        resource_name = key.split("|")[1]
        symbols_to_check = (
            ALL_COMMAND_SYMBOLS
            if resource_name in COMMAND_RESOURCE_NAMES
            else COMMAND_SYMBOLS
        )
        for symbol in symbols_to_check:
            if original.count(symbol) > 0 and original.count(symbol) != translation.count(symbol):
                errors.append(f"{key}: changed command symbol {symbol!r}")
    return errors


def main() -> int:
    source = resource_entries(SOURCE_DIRECTORY)
    all_errors: list[str] = []
    for directory_name in LOCALE_DIRECTORIES:
        errors = validate_locale(directory_name, source)
        if errors:
            all_errors.extend(f"{directory_name}: {error}" for error in errors)
        else:
            print(f"{directory_name}: {len(source)} entries valid")

    if all_errors:
        print("\nLocalization validation failed:", file=sys.stderr)
        for error in all_errors:
            print(f"- {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
