#!/usr/bin/env python3
"""One-off migration to content schema v2.

- Adds `sectionId: "articles"` to every existing rule.
- Converts old `fill` questions to `choice` with the standard article options
  (IlTa keeps only multiple-choice and matching exercises).
"""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CONTENT = ROOT / "app/src/main/assets/data/content"
ARTICLES = ["a", "an", "the", "\u2014"]


def main() -> None:
    changed_files = 0
    converted = 0
    for path in sorted(CONTENT.glob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        changed = False
        for rule in data.get("rules") or []:
            if rule.get("sectionId") != "articles":
                rule["sectionId"] = "articles"
                changed = True
        for question in data.get("questions") or []:
            if question.get("type") == "fill":
                question["type"] = "choice"
                question["options"] = list(ARTICLES)
                answer = question.get("answer")
                if answer not in ARTICLES:
                    raise SystemExit(
                        f"{path.name}: {question.get('id')} has non-article fill answer {answer!r}"
                    )
                question.pop("accepted", None)
                converted += 1
                changed = True
        if changed:
            path.write_text(
                json.dumps(data, ensure_ascii=False, indent=2) + "\n",
                encoding="utf-8",
            )
            changed_files += 1
            print(f"migrated {path.name}")
    print(f"done: {changed_files} files changed, {converted} fill -> choice")


if __name__ == "__main__":
    main()
