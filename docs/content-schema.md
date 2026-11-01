# Content schema — IlTa trainer (v2)

App: **IlTa** — offline Android trainer for English grammar.
Two sections are supported: **Articles** (`articles`) and **Prepositions** (`prepositions`),
and new sections can be added later through `sections.json`.

All learning content lives in `app/src/main/assets/data/`:

```
data/
├── sections.json                 ← section catalogue (headings, descriptions, order)
└── content/
    ├── 01_indefinite_article.json
    ├── ...
    └── prepositions_03_fixed_advanced.json
```

The app loads **every** `*.json` file in `data/content`, so different authors work on
separate files in parallel.

## 1. Sections file

`data/sections.json` is the entry screen of the app:

```json
{
  "sections": [
    {
      "id": "articles",
      "order": 10,
      "emoji": "📘",
      "title": { "ru": "Артикли", "en": "Articles" },
      "short": { "ru": "a / an / the / нулевой", "en": "a / an / the / zero" },
      "description": {
        "ru": "Неопределённый, определённый и нулевой артикль — от базовых правил до устойчивых выражений.",
        "en": "Indefinite, definite and zero article — from the basics to fixed expressions."
      }
    },
    {
      "id": "prepositions",
      "order": 20,
      "emoji": "🧭",
      "title": { "ru": "Предлоги", "en": "Prepositions" },
      "short": { "ru": "at / on / in / to / …", "en": "at / on / in / to / …" },
      "description": {
        "ru": "Предлоги времени, места, движения, зависимости после прилагательных и глаголов, устойчивые сочетания.",
        "en": "Prepositions of time, place, movement, dependent prepositions after adjectives and verbs, fixed phrases."
      }
    }
  ]
}
```

Required for a section: `id` (non-empty, unique), `order` (number), `emoji` (string),
`title.ru/en`, `short.ru/en`, `description.ru/en`.

## 2. Content file

Each content file:

```json
{
  "meta": { "package": "name", "version": 1 },
  "rules": [ ... ],
  "questions": [ ... ]
}
```

`rules` may be **empty** if the file only adds questions to rules defined in another file
(this is how the article-expansion pack works). Question `ruleId` values are resolved
across the whole `data/content` folder.

### Rule object

```json
{
  "id": "prep-time",
  "sectionId": "prepositions",
  "order": 410,
  "level": "A1",
  "title": { "ru": "Предлоги времени: at, on, in", "en": "Prepositions of time: at, on, in" },
  "summary": {
    "ru": "At — для точного времени, on — для дней и дат, in — для месяцев, лет и длительных периодов.",
    "en": "At is for exact time, on for days and dates, in for months, years and longer periods."
  },
  "sections": [
    {
      "heading": { "ru": "At — точное время", "en": "At — exact time" },
      "body": {
        "ru": "At ставим перед точным временем и в устойчивых сочетаниях: at 7 o'clock, at noon, at night, at the weekend.",
        "en": "Use at before exact times and in fixed phrases: at 7 o'clock, at noon, at night, at the weekend."
      },
      "examples": ["at 7 o'clock", "at night", "at the weekend"]
    }
  ]
}
```

Required: `id`, `sectionId`, `order`, `level` (A1|A2|B1|B2), `title.ru/en`,
`summary.ru/en`, 2–5 bilingual `sections` with `heading`, `body` and 2–6 English examples
(no `___` in rule examples). Rule `id`s are globally unique.

**Rule text rules (global, enforced by the validator):**
- `body` explains the rule in prose and **must not repeat any example from `examples`** and must not
  embed example sentences of its own. Examples are shown only in the list below the body.
- `body.ru` / `body.en` are 60–450 characters each, natural, structured (short paragraphs or
  sentences), no walls of text, no duplicated examples.
- `examples` — 3–5 short English examples per section, each illustrating exactly one point.
- Avoid one-word orphan lines in the UI: prefer slightly shorter, well-structured sentences.

## 3. Question quotas (fixed per theme)

**Article themes** (section `articles`) have a larger pool of **exactly 70 questions**:
- at least **25 `choice`** questions;
- at least **5 `match`** questions (each with 4 pairs).

A topic training session draws **30 questions** from the pool: all five matching questions plus
25 choice questions (weak/unseen questions are preferred). The pool stays 70, so different
sessions show different examples.

**Preposition themes** (section `prepositions`) have a pool of **50–60 questions**
(at least 25 `choice` and 5 `match`). A topic session draws **30** questions: all five matching
questions plus 25 choice questions.

This is enforced by `scripts/validate_content.py`. The full set is
9 article themes × 70 + 6 preposition themes × 50/60 = **940 questions**.
The “Длина сессии” setting does not affect topic training.

## 4. Question objects — only **choice** and **match**

There are **no typing exercises** in IlTa anymore. Every question is multiple choice or matching.

### 4.1 `choice` — pick the missing item

Article example:

```json
{
  "id": "art2-0001",
  "type": "choice",
  "ruleId": "a-an-basics",
  "level": "A1",
  "sentence": "I saw ___ dog in the park.",
  "translation": { "ru": "Я увидел собаку в парке." },
  "options": ["a", "an", "the", "—"],
  "answer": "a",
  "explanation": {
    "ru": "Dog начинается с согласного звука, а собака упоминается впервые — нужен a.",
    "en": "Dog begins with a consonant sound and is mentioned for the first time, so we use a."
  }
}
```

Preposition example:

```json
{
  "id": "prep2-0001",
  "type": "choice",
  "ruleId": "prep-adjectives",
  "level": "A2",
  "sentence": "My little sister is afraid ___ the dark.",
  "translation": { "ru": "Моя младшая сестра боится темноты." },
  "options": ["of", "from", "at", "with"],
  "answer": "of",
  "explanation": {
    "ru": "Прилагательное afraid всегда требует предлог of: afraid of something.",
    "en": "The adjective afraid always takes the preposition of: afraid of something."
  }
}
```

Requirements:
- `sentence` contains exactly one blank marker `___`.
- `translation.ru` — natural Russian translation of the whole sentence.
- `options` — exactly **4** distinct values for article questions (always `a`, `an`, `the`, `—`)
  and **3–4** distinct plausible values for preposition questions (real prepositions;
  the em dash `—` may be one of the options only when the correct answer is “no preposition”).
- `answer` must be one of `options`. Exactly one option is correct in the given context.
- `explanation.ru` and `explanation.en` (1–3 sentences) explain *why* this item is correct.
- Levels: A1–B2; the level must match the sentence difficulty, not the rule name.

For preposition questions the em dash `—` means “no preposition is needed”
(e.g. *We discussed ___ the problem.* → answer `—`). The app shows it as
“— (без предлога)” / “— (no preposition)”.

### 4.2 `match` — rule ↔ example

```json
{
  "id": "prep2-0020",
  "type": "match",
  "ruleId": "prep-adjectives",
  "level": "B1",
  "prompt": { "ru": "Сопоставьте прилагательное и подходящий предлог.", "en": "Match each adjective with its preposition." },
  "pairs": [
    { "left": { "ru": "interested ___", "en": "interested ___" }, "right": "She is interested in modern art." },
    { "left": { "ru": "good ___", "en": "good ___" }, "right": "He is good at maths." },
    { "left": { "ru": "afraid ___", "en": "afraid ___" }, "right": "My sister is afraid of the dark." },
    { "left": { "ru": "famous ___", "en": "famous ___" }, "right": "The city is famous for its bridges." }
  ],
  "explanation": {
    "ru": "Каждое прилагательное закреплено за своим предлогом: interested in, good at, afraid of, famous for.",
    "en": "Each adjective is fixed with its preposition: interested in, good at, afraid of, famous for."
  }
}
```

Requirements: `prompt.ru/en`; `pairs` — 4 (3–5 allowed); each `left.ru/en`;
`right` — a full English sentence **without** `___`; all left values distinct,
all right values distinct; every left unambiguously corresponds to exactly one right.

## Quality bar

- 100% correct English, British or American spelling consistent **within a file**.
- Exactly one defensible answer for every blank. For prepositions especially: check that no
  second preposition sounds natural in the sentence. If it does, rewrite the sentence.
- Distractors are real, plausible words of the same type (other articles / other prepositions),
  never random or obviously wrong by part of speech.
- No duplicate sentences, ids or match pairs anywhere in the content set.
- `translation.ru` is a natural Russian translation, not a word-for-word gloss.
- Explanations must teach the pattern, not just restate the answer.
- Russian text: correct punctuation, «ё» where appropriate.
- Sentences: 5–20 words for choice questions.
- Article themes have 70 pool questions; preposition themes have 50–60; a topic session always
  draws 30 (5 match + 25 choice). `tags` are optional pattern labels used to pick truly similar
  questions during mistake review.

## Current content files and ownership

| File | Rule | Questions |
|---|---|---|
| articles_01_system.json | art-system | 70 |
| articles_02_a_an.json | art-a-an | 70 |
| articles_03_the_known.json | art-the-known | 70 |
| articles_04_a_an_special.json | art-a-an-special | 70 |
| articles_05_zero.json | art-zero | 70 |
| articles_06_names_geo.json | art-names-geo | 70 |
| articles_07_the_adjective.json | art-the-adjective | 70 |
| articles_08_contrasts.json | art-contrasts | 70 |
| articles_09_academic.json | art-academic | 70 |
| prepositions_01_time.json | prep-time | 50 |
| prepositions_02_place.json | prep-place | 50 |
| prepositions_03_movement.json | prep-movement | 50 |
| prepositions_04_adjectives.json | prep-adjectives | 50 |
| prepositions_05_verbs.json | prep-verbs | 50 |
| prepositions_06_fixed_advanced.json | prep-fixed | 60 |

Target: **940 questions total** (9×70 articles + 5×50 + 60 prepositions), 15 rules, 2 sections.

## Validation

Run before reporting (each author validates their own file):

```bash
python3 scripts/validate_content.py app/src/main/assets/data/content/<your-file>.json
```

Whole set (Lead / final check):

```bash
python3 scripts/validate_content.py app/src/main/assets/data/content
```

The script checks JSON schema, unique ids, section references, rule references, blank markers,
option count and answer presence, bilingual explanations, translation hints, duplicates,
the fixed 15+5 quota per rule, and that rule bodies do not repeat their examples.
Use `--no-quotas` when validating a syntax sample instead of a real content pack.
