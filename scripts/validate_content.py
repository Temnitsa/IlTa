#!/usr/bin/env python3
"""Validate IlTa learning content (sections + articles/prepositions packs).

Usage:
    python3 scripts/validate_content.py app/src/main/assets/data/content
    python3 scripts/validate_content.py app/src/main/assets/data/content/01_indefinite_article.json
    python3 scripts/validate_content.py <file-or-dir> --sections path/to/sections.json

Exit code 0 = no errors. Warnings do not fail unless --strict is passed.
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ARTICLES = {"a", "an", "the", "\u2014"}
LEVELS = {"A1", "A2", "B1", "B2"}
TYPES = {"choice", "match", "build"}
ARTICLE_SECTION = "articles"
RU_LETTERS = re.compile(r"[А-Яа-яЁё]")

errors: list[str] = []
warnings: list[str] = []


def err(where: str, message: str) -> None:
    errors.append(f"{where}: {message}")


def warn(where: str, message: str) -> None:
    warnings.append(f"{where}: {message}")


def nonempty_str(value, where: str, field: str, min_len: int = 1) -> bool:
    if not isinstance(value, str) or len(value.strip()) < min_len:
        err(where, f"`{field}` must be a non-empty string (min {min_len} chars)")
        return False
    return True


def check_bilingual(obj, where: str, field: str, min_len: int = 1, require_en: bool = True,
                    warn_cyrillic: bool = True) -> None:
    if not isinstance(obj, dict):
        err(where, f"`{field}` must be an object with ru/en")
        return
    if not nonempty_str(obj.get("ru"), where, f"{field}.ru", min_len):
        pass
    else:
        if warn_cyrillic and not RU_LETTERS.search(obj.get("ru", "")):
            warn(where, f"`{field}.ru` contains no Cyrillic text")
    if require_en:
        nonempty_str(obj.get("en"), where, f"{field}.en", min_len)


def check_blank(sentence: str, where: str) -> None:
    count = sentence.count("___")
    if count != 1:
        err(where, f"sentence must contain exactly one `___` blank, found {count}")


def validate_sections_file(data, where: str) -> set[str]:
    sections = data.get("sections") if isinstance(data, dict) else None
    if not isinstance(sections, list) or not sections:
        err(where, "`sections` must be a non-empty list")
        return set()
    ids: set[str] = set()
    for index, section in enumerate(sections):
        sw = f"{where} sections[{index}]"
        if not isinstance(section, dict):
            err(sw, "section must be an object")
            continue
        section_id = section.get("id")
        if nonempty_str(section_id, sw, "id"):
            if section_id in ids:
                err(sw, f"duplicate section id `{section_id}`")
            ids.add(section_id)
        if not isinstance(section.get("order"), (int, float)):
            err(sw, "`order` must be a number")
        if not isinstance(section.get("emoji"), str) or not section.get("emoji"):
            warn(sw, "`emoji` is missing or empty")
        check_bilingual(section.get("title"), sw, "title", min_len=2)
        check_bilingual(section.get("short"), sw, "short", min_len=2, warn_cyrillic=False)
        check_bilingual(section.get("description"), sw, "description", min_len=10)
    return ids


def validate_rule(rule, where: str, known_sections: set[str] | None,
                  file_rules: set[str], counts: dict[str, int]) -> None:
    if not isinstance(rule, dict):
        err(where, "rule must be an object")
        return
    rule_id = rule.get("id")
    if not nonempty_str(rule_id, where, "id"):
        return
    if rule_id in file_rules:
        err(where, f"duplicate rule id `{rule_id}`")
    file_rules.add(rule_id)
    counts[rule_id] = counts.get(rule_id, 0)
    section_id = rule.get("sectionId")
    if nonempty_str(section_id, where, "sectionId"):
        if known_sections is not None and section_id not in known_sections:
            err(where, f"unknown sectionId `{section_id}`")
    if not isinstance(rule.get("order"), (int, float)):
        err(where, "`order` must be a number")
    if rule.get("level") not in LEVELS:
        err(where, f"`level` must be one of {sorted(LEVELS)}, got {rule.get('level')!r}")
    check_bilingual(rule.get("title"), where, "title", min_len=3)
    check_bilingual(rule.get("summary"), where, "summary", min_len=10)
    sections = rule.get("sections")
    if not isinstance(sections, list) or not (2 <= len(sections) <= 6):
        err(where, "`sections` must be a list of 2..6 sections")
        return
    for index, section in enumerate(sections):
        sw = f"{where} section[{index}]"
        if not isinstance(section, dict):
            err(sw, "section must be an object")
            continue
        check_bilingual(section.get("heading"), sw, "heading", min_len=2)
        check_bilingual(section.get("body"), sw, "body", min_len=10)
        examples = section.get("examples")
        if not isinstance(examples, list) or not (2 <= len(examples) <= 6):
            err(sw, "`examples` must be a list of 2..6 strings")
            continue
        body_ru = (section.get("body", {}) or {}).get("ru", "") if isinstance(section.get("body"), dict) else ""
        body_en = (section.get("body", {}) or {}).get("en", "") if isinstance(section.get("body"), dict) else ""
        body_ru_l = body_ru.lower()
        body_en_l = body_en.lower()
        for example in examples:
            if not isinstance(example, str) or len(example.strip()) < 2:
                err(sw, "example must be a non-empty string")
            elif "___" in example:
                err(sw, "rule examples must not contain a `___` blank")
            elif not re.search(r"[A-Za-z]", example):
                err(sw, f"rule example has no English text: {example!r}")
            else:
                probe = example.strip().lower()
                if len(probe) >= 4 and (probe in body_ru_l or probe in body_en_l):
                    err(sw, f"body repeats the example {example!r}; examples belong only to `examples`")


def validate_choice(question, where: str, section_id: str | None) -> None:
    sentence = question.get("sentence")
    if nonempty_str(sentence, where, "sentence", min_len=5):
        check_blank(sentence, where)
    check_bilingual(question.get("translation"), where, "translation", min_len=3, require_en=False)
    options = question.get("options")
    answer = question.get("answer")
    if not isinstance(options, list) or not (3 <= len(options) <= 4):
        err(where, "`options` must contain 3 or 4 values")
        return
    if any(not isinstance(option, str) or not option.strip() for option in options):
        err(where, "options must be non-empty strings")
        return
    if any(option != option.strip() for option in options):
        err(where, "options must not have leading/trailing spaces")
    if len(set(options)) != len(options):
        err(where, "`options` contain duplicates")
    if answer not in options:
        err(where, f"`answer` {answer!r} is not present in options")
        return
    is_article_question = section_id == ARTICLE_SECTION or (
        section_id is None and set(options) == ARTICLES
    )
    if is_article_question:
        if set(options) != ARTICLES:
            err(where, "article questions must have exactly the options a, an, the, —")
        if answer not in ARTICLES:
            err(where, f"article answer must be one of a, an, the, —; got {answer!r}")
    else:
        for option in options:
            if not re.fullmatch(r"[A-Za-z][A-Za-z ]*|\u2014", option):
                err(where, f"preposition option looks invalid: {option!r}")


def validate_match(question, where: str, seen_match_rights: dict[str, str] | None = None) -> None:
    check_bilingual(question.get("prompt"), where, "prompt", min_len=3)
    pairs = question.get("pairs")
    if not isinstance(pairs, list) or not (3 <= len(pairs) <= 5):
        err(where, "`pairs` must contain 3..5 pairs")
        return
    lefts: set[str] = set()
    rights: set[str] = set()
    for index, pair in enumerate(pairs):
        pw = f"{where} pair[{index}]"
        if not isinstance(pair, dict):
            err(pw, "pair must be an object")
            continue
        check_bilingual(pair.get("left"), pw, "left", min_len=2)
        right = pair.get("right")
        if nonempty_str(right, pw, "right", min_len=5):
            if "___" in right:
                err(pw, "`right` must be a full example without `___`")
            if not re.search(r"[A-Za-z]", right):
                err(pw, "`right` must contain English text")
            left_ru = pair.get("left", {}).get("ru") if isinstance(pair.get("left"), dict) else None
            if left_ru in lefts:
                err(pw, f"duplicate left value {left_ru!r}")
            if right in rights:
                err(pw, f"duplicate right value {right!r}")
            if seen_match_rights is not None and isinstance(right, str):
                key = re.sub(r"\s+", " ", right.strip().lower())
                existing = seen_match_rights.get(key)
                if existing is not None:
                    err(pw, f"duplicate match example (also used by {existing})")
                else:
                    seen_match_rights[key] = str(question.get("id"))
            lefts.add(left_ru)
            rights.add(right)


def validate_build(question, where: str) -> None:
    words = question.get("words")
    answers = question.get("answers")
    if not isinstance(words, list) or not (3 <= len(words) <= 20):
        err(where, "`words` must be a list of 3..20 tokens")
        return
    if any(not isinstance(w, str) or not w.strip() for w in words):
        err(where, "every word token must be a non-empty string")
        return
    if not isinstance(answers, list) or not (1 <= len(answers) <= 4):
        err(where, "`answers` must contain 1..4 accepted token sequences")
        return
    seen = set()
    for index, answer in enumerate(answers):
        if not isinstance(answer, list) or any(not isinstance(t, str) for t in answer):
            err(where, f"answers[{index}] must be a list of strings")
            continue
        if sorted(answer) != sorted(words):
            err(where, f"answers[{index}] must be a permutation of `words`")
        key = tuple(answer)
        if key in seen:
            err(where, f"duplicate accepted answer {answer}")
        seen.add(key)
        if "___" in " ".join(answer):
            err(where, "build answers must not contain `___`")
    if question.get("sentence") is not None:
        err(where, "build questions must not include a `sentence` field")


def validate_question(question, where: str, known_rules: dict[str, str] | None,
                      seen_question_ids: set[str], seen_sentences: dict[str, str],
                      seen_match_rights: dict[str, str] | None = None) -> None:
    if not isinstance(question, dict):
        err(where, "question must be an object")
        return
    question_id = question.get("id")
    if not nonempty_str(question_id, where, "id"):
        return
    if question_id in seen_question_ids:
        err(where, f"duplicate question id `{question_id}`")
    seen_question_ids.add(question_id)
    question_type = question.get("type")
    if question_type not in TYPES:
        err(where, f"`type` must be one of {sorted(TYPES)}, got {question_type!r}")
        return
    if question.get("level") not in LEVELS:
        err(where, f"`level` must be one of {sorted(LEVELS)}, got {question.get('level')!r}")
    rule_id = question.get("ruleId")
    section_id: str | None = None
    if nonempty_str(rule_id, where, "ruleId"):
        if known_rules is not None:
            if rule_id not in known_rules:
                err(where, f"unknown ruleId `{rule_id}`")
            else:
                section_id = known_rules[rule_id]
    check_bilingual(question.get("explanation"), where, "explanation", min_len=15)
    if question_type == "choice":
        validate_choice(question, where, section_id)
        sentence = question.get("sentence")
        if isinstance(sentence, str) and sentence.strip():
            key = re.sub(r"\s+", " ", sentence.strip().lower())
            if key in seen_sentences:
                err(where, f"duplicate sentence (also used by {seen_sentences[key]})")
            else:
                seen_sentences[key] = str(question_id)
    elif question_type == "build":
        validate_build(question, where)
        answers = question.get("answers") or []
        if answers:
            sentence = " ".join(answers[0])
            key = re.sub(r"\s+", " ", sentence.strip().lower())
            if key in seen_sentences:
                err(where, f"duplicate build sentence (also used by {seen_sentences[key]})")
            else:
                seen_sentences[key] = str(question_id)
    else:
        validate_match(question, where, seen_match_rights)


def find_sections_path(target: Path) -> Path | None:
    candidates = []
    if target.is_dir():
        candidates.append(target.parent / "sections.json")
    else:
        candidates.append(target.parent.parent / "sections.json")
        candidates.append(target.parent / "sections.json")
    for candidate in candidates:
        if candidate.is_file():
            return candidate
    return None


def main(argv: list[str]) -> int:
    args = [arg for arg in argv[1:] if not arg.startswith("--")]
    strict = "--strict" in argv
    skip_quotas = "--no-quotas" in argv

    sections_arg = None
    for index, arg in enumerate(argv[1:]):
        if arg == "--sections" and index + 2 <= len(argv[1:]):
            sections_arg = argv[1:][index + 1]
    if sections_arg in args:
        args.remove(sections_arg)

    if len(args) != 1:
        print(__doc__)
        return 2
    target = Path(args[0])
    if target.is_dir():
        files = sorted(target.glob("*.json"))
        directory_mode = True
    else:
        files = [target]
        directory_mode = False
    if not files:
        print(f"ERROR: no JSON files found in {target}")
        return 2

    sections_path = Path(sections_arg) if sections_arg else find_sections_path(target)
    known_sections: set[str] | None = None
    if sections_path is not None and sections_path.is_file():
        try:
            sections_data = json.loads(sections_path.read_text(encoding="utf-8"))
            known_sections = validate_sections_file(sections_data, sections_path.name)
            print(f"checked {sections_path.name}: {len(known_sections)} section(s)")
        except json.JSONDecodeError as exc:
            errors.append(f"{sections_path.name}: invalid JSON: {exc}")
            known_sections = set()
    elif directory_mode:
        err(str(sections_path or target.parent / "sections.json"), "sections.json not found")
    else:
        warn(target.name, "sections.json not found; sectionId checks skipped")

    packages: list[tuple[Path, dict]] = []
    for path in files:
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except json.JSONDecodeError as exc:
            errors.append(f"{path.name}: invalid JSON: {exc}")
            continue
        if not isinstance(data, dict):
            err(path.name, "top-level JSON must be an object")
            continue
        packages.append((path, data))

    # pass 1: rules (collect globally, so questions-only files can reference them)
    known_rule_sections: dict[str, str] = {}
    rule_question_counts: dict[str, int] = {}
    rule_type_counts: dict[str, dict[str, int]] = {}
    for path, data in packages:
        rules = data.get("rules", [])
        if rules is None:
            rules = []
        if not isinstance(rules, list):
            err(path.name, "`rules` must be a list")
            rules = []
        file_rules: set[str] = set()
        for index, rule in enumerate(rules):
            validate_rule(rule, f"{path.name} rules[{index}]", known_sections,
                          file_rules, rule_question_counts)
        for rule_id in file_rules:
            if rule_id in known_rule_sections:
                err(path.name, f"rule id `{rule_id}` already defined in another file")
            rule = next((r for r in rules if isinstance(r, dict) and r.get("id") == rule_id), {})
            known_rule_sections[rule_id] = str(rule.get("sectionId", ""))

    # pass 2: questions
    seen_question_ids: set[str] = set()
    seen_sentences: dict[str, str] = {}
    seen_match_rights: dict[str, str] = {}
    total_questions = 0
    type_counts = {"choice": 0, "match": 0, "build": 0}
    level_counts = {level: 0 for level in LEVELS}
    section_counts: dict[str, int] = {}

    for path, data in packages:
        questions = data.get("questions", [])
        if questions is None:
            questions = []
        if not isinstance(questions, list) or not questions:
            err(path.name, "`questions` must be a non-empty list")
            questions = []
        for index, question in enumerate(questions):
            validate_question(
                question,
                f"{path.name} questions[{index}]",
                known_rule_sections if directory_mode else None,
                seen_question_ids,
                seen_sentences,
                seen_match_rights if directory_mode else None,
            )
            total_questions += 1
            if isinstance(question, dict):
                tags = question.get("tags")
                if tags is not None and (not isinstance(tags, list) or any(not isinstance(tag, str) for tag in tags)):
                    err("question", f"{where}: `tags` must be a list of strings")
                question_type = question.get("type")
                if question_type in type_counts:
                    type_counts[question_type] += 1
                if question.get("level") in level_counts:
                    level_counts[question["level"]] += 1
                rule_id = question.get("ruleId")
                if rule_id:
                    rule_question_counts[rule_id] = rule_question_counts.get(rule_id, 0) + 1
                    type_counter = rule_type_counts.setdefault(rule_id, {"choice": 0, "match": 0, "build": 0})
                    if question_type in type_counter:
                        type_counter[question_type] += 1
                    section_id = known_rule_sections.get(rule_id)
                    if section_id:
                        section_counts[section_id] = section_counts.get(section_id, 0) + 1
        print(f"checked {path.name}: {len(data.get('rules', []) or [])} rules, "
              f"{len(questions)} questions")

    # cross checks
    if known_sections is not None:
        for section_id in sorted(known_sections):
            count = sum(1 for value in known_rule_sections.values() if value == section_id)
            if count == 0:
                warn("sections", f"section `{section_id}` has no rules yet")
    if not skip_quotas:
        for rule_id in sorted(known_rule_sections):
            total = rule_question_counts.get(rule_id, 0)
            types = rule_type_counts.get(rule_id, {"choice": 0, "match": 0, "build": 0})
            rule_section = known_rule_sections.get(rule_id)
            is_article_theme = rule_section == "articles"
            is_tense_theme = rule_section == "tenses"
            if is_tense_theme:
                if total != 30:
                    err("quotas", f"tense theme `{rule_id}` must have exactly 30 questions, found {total}")
                if types.get("choice", 0) != 20:
                    err("quotas", f"tense theme `{rule_id}` must have 20 choice questions, found {types.get('choice', 0)}")
                if types.get("build", 0) != 5:
                    err("quotas", f"tense theme `{rule_id}` must have 5 build questions, found {types.get('build', 0)}")
                if types.get("match", 0) != 5:
                    err("quotas", f"tense theme `{rule_id}` must have 5 match questions, found {types.get('match', 0)}")
            elif is_article_theme:
                if total != 70:
                    err("quotas", f"article theme `{rule_id}` must have exactly 70 questions, found {total}")
                if types.get("choice", 0) < 25:
                    err("quotas", f"article theme `{rule_id}` needs at least 25 choice questions, found {types.get('choice', 0)}")
                if types.get("match", 0) < 5:
                    err("quotas", f"article theme `{rule_id}` needs at least 5 match questions, found {types.get('match', 0)}")
            else:
                if not (50 <= total <= 60):
                    err("quotas", f"preposition theme `{rule_id}` must have 50..60 questions, found {total}")
                if types.get("choice", 0) < 25:
                    err("quotas", f"preposition theme `{rule_id}` needs at least 25 choice questions, found {types.get('choice', 0)}")
                if types.get("match", 0) < 5:
                    err("quotas", f"preposition theme `{rule_id}` needs at least 5 match questions, found {types.get('match', 0)}")
    for rule_id, count in sorted(rule_question_counts.items()):
        if count == 0:
            warn("rules", f"rule `{rule_id}` has no questions")

    if directory_mode:
        if total_questions < 1300:
            warn("content", f"expected 1300 questions, found {total_questions}")
        for section_id, count in sorted(section_counts.items()):
            print(f"section `{section_id}`: {count} questions")

    print()
    print(f"total questions: {total_questions} | "
          + " ".join(f"{key}={value}" for key, value in type_counts.items()))
    print("levels: " + " ".join(f"{key}={value}" for key, value in level_counts.items()))
    if warnings:
        print(f"\n{len(warnings)} warning(s):")
        for item in warnings:
            print("  WARN", item)
    if errors:
        print(f"\n{len(errors)} error(s):")
        for item in errors:
            print("  ERROR", item)
        return 1
    if strict and warnings:
        print("\nstrict mode: warnings treated as errors")
        return 1
    print("\nOK: content is valid")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
