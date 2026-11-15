# Content schema — IlTa trainer (v2)

App: **IlTa** — offline Android trainer for English grammar.
Three sections are supported: **Articles** (`articles`), **Prepositions** (`prepositions`)
and **Tenses** (`tenses`); new sections can be added later through `sections.json`.

All learning content lives in `app/src/main/assets/data/`:

```
data/
├── sections.json                 ← section catalogue (headings, descriptions, order)
└── content/
    ├── 01_indefinite_article.json
    ├── ...
    └── tenses_12_future_perfect_continuous.json
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
        "ru": "Визуальный словарь: позиционные предлоги и предлоги направления — картинки, перевод, объяснения и примеры.",
        "en": "A visual dictionary: positional prepositions and prepositions of direction — pictures, translation, explanations and examples."
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
      "examples": ["at 7 o'clock", "at night", "at the weekend"],
      "exampleTranslations": ["в 7 часов", "ночью", "на выходных"]
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
- `exampleTranslations` — required Russian translations, exactly the same length/order as
  `examples`; shown in a small popup on long-press in the app.
- Avoid one-word orphan lines in the UI: prefer slightly shorter, well-structured sentences.

## 3. Question quotas (fixed per theme)

**Article themes** (section `articles`) have a pool of **55–60 `choice` questions**.

**Tense themes** (section `tenses`) have a pool of **exactly 80 questions**:
20 `choice` + 60 `build`. A tense theme session draws **15 choice + 15 build** = 30 questions.

**Preposition themes** (section `prepositions`) are the two visual-dictionary topics
(`prep-position`, `prep-direction`) with **exactly 45 `choice` questions** each.
A topic session draws **30** questions from that pool.

This is enforced by `scripts/validate_content.py`. The full set is
9 article themes (530) + 2 preposition themes (90) + 12 tense themes (960) = **1580 questions**.
The “Длина сессии” setting does not affect topic training.

The visual dictionary itself is a separate file, `data/preposition_reference.json`
(`PrepositionCatalog`): `topics[]` with `ruleId`, `title`, `subtitle` and `prepositions[]`;
each entry has `key` (snake_case, matches the drawable `prep_<key>`), `label`, `translation`,
`short`, `explanation` and 2–4 `examples` with `en`/`ru`.

## 4. Question objects — **choice** and **build**

Every question is multiple choice (also used by the typing mode «Письмо») or sentence
building (tap word tiles to assemble the sentence). The old `match` type was removed in 3.6.0.

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
- The «Письмо» mode renders the same items as a text field: the learner types the missing
  word, and `AnswerChecker` compares it case-insensitively after whitespace normalization.
- `options` — exactly **4** distinct values for article questions (always `a`, `an`, `the`, `—`)
  and **3–4** distinct plausible values for preposition questions (real prepositions;
  the em dash `—` may be one of the options only when the correct answer is “no preposition”).
- `answer` must be one of `options`. Exactly one option is correct in the given context.
- `explanation.ru` and `explanation.en` (1–3 sentences) explain *why* this item is correct.
- Levels: A1–B2; the level must match the sentence difficulty, not the rule name.

For preposition questions the em dash `—` means “no preposition is needed”
(e.g. *We discussed ___ the problem.* → answer `—`). The app shows it as
“— (без предлога)” / “— (no preposition)”.

### 4.2 `build` — assemble the sentence from word tiles

```json
{
  "id": "tense-present-perfect-0001",
  "type": "build",
  "ruleId": "tense-present-perfect",
  "level": "A2",
  "buildKind": "affirmative",
  "words": ["She", "has", "lived", "in", "London", "for", "five", "years"],
  "answers": [
    ["She", "has", "lived", "in", "London", "for", "five", "years"],
    ["She", "has", "lived", "in", "London", "five", "years", "for"]
  ],
  "translation": { "ru": "Она живёт в Лондоне пять лет." },
  "explanation": {
    "ru": "Present Perfect: has + past participle; for + длительность.",
    "en": "Present Perfect: has + past participle; for + duration."
  },
  "tags": ["tense-present-perfect", "build", "for-since"]
}
```

Rules:
- `buildKind` — `affirmative`, `negative` or `question` (shown as (+), (–), (?));
- `words` — 3–20 tokens (each a non-empty string; keep contractions as one token, e.g. `"don't"`;
  no separate final punctuation token);
- `answers` — 1–4 **accepted token sequences**; each must be a permutation of `words`
  (same multiset); duplicates are rejected;
- accepted orders are for genuinely possible word orders (e.g. adverb before/after the verb);
- `sentence` is not allowed for `build`; translation and bilingual explanation are required;
- the UI shuffles the word bank, so content keeps a natural canonical order in `words`.

## Quality bar

- 100% correct English, British or American spelling consistent **within a file**.
- Exactly one defensible answer for every blank. For prepositions especially: check that no
  second preposition sounds natural in the sentence. If it does, rewrite the sentence.
- Distractors are real, plausible words of the same type (other articles / other prepositions),
  never random or obviously wrong by part of speech.
- No duplicate sentences or ids anywhere in the content set.
- `translation.ru` is a natural Russian translation, not a word-for-word gloss.
- Explanations must teach the pattern, not just restate the answer.
- Russian text: correct punctuation, «ё» where appropriate.
- Sentences: 5–20 words for choice questions.
- Article themes have 55–60 pool questions, preposition themes 45, tense themes 80; a topic
  session always draws 30 (15 choice + 15 build for tenses). `tags` are optional pattern labels
  used to pick truly similar questions during mistake review.

## Current content files and ownership

| File | Rule | Questions |
|---|---|---|
| articles_01_system.json | art-system | 60 |
| articles_02_a_an.json | art-a-an | 60 |
| articles_03_the_known.json | art-the-known | 60 |
| articles_04_a_an_special.json | art-a-an-special | 60 |
| articles_05_zero.json | art-zero | 60 |
| articles_06_names_geo.json | art-names-geo | 55 |
| articles_07_the_adjective.json | art-the-adjective | 60 |
| articles_08_contrasts.json | art-contrasts | 60 |
| articles_09_academic.json | art-academic | 55 |
| prepositions_01_position.json | prep-position | 45 |
| prepositions_02_direction.json | prep-direction | 45 |
| tenses_01_present_simple.json | tense-present-simple | 80 |
| tenses_02_present_continuous.json | tense-present-continuous | 80 |
| tenses_03_present_perfect.json | tense-present-perfect | 80 |
| tenses_04_present_perfect_continuous.json | tense-present-perfect-continuous | 80 |
| tenses_05_past_simple.json | tense-past-simple | 80 |
| tenses_06_past_continuous.json | tense-past-continuous | 80 |
| tenses_07_past_perfect.json | tense-past-perfect | 80 |
| tenses_08_past_perfect_continuous.json | tense-past-perfect-continuous | 80 |
| tenses_09_future_simple.json | tense-future-simple | 80 |
| tenses_10_future_continuous.json | tense-future-continuous | 80 |
| tenses_11_future_perfect.json | tense-future-perfect | 80 |
| tenses_12_future_perfect_continuous.json | tense-future-perfect-continuous | 80 |

Target: **1580 questions total** (9 article themes, 530; 2 preposition themes, 90; 12 tense themes, 960), 23 rules, 3 sections.

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
