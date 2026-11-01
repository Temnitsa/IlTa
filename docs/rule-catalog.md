# Rule catalog — 27 topics in 2 sections

Levels: A1, A2, B1, B2. `order` values are assigned in increments of 10.
Every rule belongs to a section via `sectionId`.

## Section `articles` — Артикли / Articles (20 rules, ~300 questions)

### Pack 01 — 01_indefinite_article.json (indefinite article a/an, 65 questions)

| id | order | level | title (ru) | scope |
|---|---|---|---|---|
| a-an-basics | 10 | A1 | A / An: форма и звук | a before consonant sound, an before vowel sound; a/an only with singular countable nouns |
| a-an-first-mention | 20 | A1 | A / An: первое упоминание | new information, "one of many", introducing a thing for the first time |
| a-an-professions | 30 | A1 | A / An: профессии и характеристики | professions, nationalities/religions used as nouns, "He is a doctor" |
| a-an-frequency | 40 | A2 | A / An: «за / в» (мера) | once a week, twice a day, 60 km an hour, 3 times a year |
| a-an-exceptions | 50 | A2 | A / An: исключения по звуку | an hour, an honest man, a university, a European, a one-way street, an MBA, an SMS |

### Pack 02 — 02_definite_article.json (definite article the, 76 questions)

| id | order | level | title (ru) | scope |
|---|---|---|---|---|
| the-unique | 110 | A1 | The: единственный в контексте | the sun, the moon, the door, the teacher (in class), shared knowledge |
| the-second-mention | 120 | A1 | The: повторное упоминание | first mention a/an → second mention the |
| the-superlatives | 130 | A2 | The: превосходная степень и порядковые | the best, the first, the only, the last, the same, the next |
| the-specific | 140 | A2 | The: конкретизация | of-phrases, relative clauses, "the man in the hat", defining context |
| the-geography | 150 | B1 | The: география | the Thames, the Alps, the USA, the Netherlands, the Pacific; zero with lakes/most countries |
| the-institutions | 160 | B1 | The: учреждения и места | the school (building) vs school (process); the hospital, the bank, the cinema, the theatre |

### Pack 03 — 03_zero_article.json (zero article, 65 questions)

| id | order | level | title (ru) | scope |
|---|---|---|---|---|
| zero-basics | 210 | A1 | Нулевой артикль: общий смысл | plural and uncountable nouns in general meaning: books, water, money |
| zero-names | 220 | A1 | Нулевой артикль: имена и названия | people, cities, most countries, languages, days, months, holidays |
| zero-meals-transport | 230 | A2 | Нулевой артикль: еда, транспорт, время | have breakfast, by bus, at night, at noon, on Monday, in summer |
| zero-fixed | 240 | A2 | Нулевой артикль: устойчивые выражения | at home, in bed, on foot, go to school, watch TV, play football, in fact |

### Pack 04 — 04_idioms_advanced.json (idioms + advanced, 65 questions)

| id | order | level | title (ru) | scope |
|---|---|---|---|---|
| idioms-the | 310 | B1 | Устойчивые выражения с the | play the piano, in the morning, at the weekend, at the seaside, in the end |
| idioms-a-an | 320 | B1 | Устойчивые выражения с a/an | have a cold, in a hurry, a few, a little, make a decision, as a rule |
| advanced-abstract | 330 | B2 | Абстрактные существительные: the или — | life/the life, society, space, history, power, nature, experience |
| advanced-generic | 340 | B2 | Обобщение: the + прилагательное/нация | the rich, the young, the British, the French; a/an in generic statements |
| advanced-exceptions | 350 | B2 | Тонкие случаи и исключения | the + names of newspapers/hotels/museums; a/an vs the with unique roles |

### Pack 05 — 05_articles_expansion.json (NEW, questions only, ~30 questions)

Adds new questions to the 20 article rules above (no new rules; `"rules": []`).
Ids `art2-0001`…`art2-0030`. Mix: 25 choice + 5 match. Distribute across all 20 rules
(at least 1 new question per rule), spread levels A1–B2 according to the rules.
Every choice uses options `["a", "an", "the", "—"]`.

## Section `prepositions` — Предлоги / Prepositions (7 rules, ~300 questions)

### Pack 06 — prepositions_01_time_place.json (~85 questions: 77 choice + 8 match)

| id | order | level | title (ru) | scope |
|---|---|---|---|---|
| prep-time | 410 | A1 | Предлоги времени: at, on, in | at 7 o'clock, at noon, at night, at the weekend; on Monday, on 5 May; in July, in 2026, in the morning; in two hours |
| prep-place | 420 | A1 | Предлоги места: at, on, in и другие | at the bus stop, at home, at work; in the box, in the room; on the wall, on the table; next to, between, among, in front of, behind, under, opposite |

### Pack 07 — prepositions_02_movement.json (~45 questions: 41 choice + 4 match)

| id | order | level | title (ru) | scope |
|---|---|---|---|---|
| prep-movement | 430 | A2 | Предлоги движения | to, into, onto, out of, through, across, along, over, under, towards, from…to, off |

### Pack 08 — prepositions_03_adjectives_verbs.json (~90 questions: 82 choice + 8 match)

| id | order | level | title (ru) | scope |
|---|---|---|---|---|
| prep-adjectives | 440 | A2 | Предлог после прилагательного | afraid of, proud of, interested in, good/bad at, famous for, ready for, different from, similar to, worried about, excited about, kind to, full of, responsible for (and 3–4 more high-frequency ones) |
| prep-verbs | 450 | A2 | Предлог после глагола | depend on, wait for, listen to, look at, arrive at/in, belong to, apologise for, thank for, think about, explain to + **no preposition** cases: discuss, marry, enter, reach |

### Pack 09 — prepositions_04_fixed_advanced.json (~80 questions: 72 choice + 8 match)

| id | order | level | title (ru) | scope |
|---|---|---|---|---|
| prep-fixed | 460 | B1 | Устойчивые выражения с предлогами | in the end, on time, in time, by mistake, for example, at least, in fact, on the other hand, by chance, in advance, on purpose, under control, in charge of |
| prep-advanced | 470 | B2 | Тонкие случаи: by / with / in, during / for, between / among | by + means of transport, with + instrument, in + material; during vs for vs while; between vs among; except vs besides; by the time; prepositions in questions |

## Fixed quotas (v3)

Every theme has **exactly 30 questions: 25 `choice` + 5 `match` (4 pairs each)**.
The total bank is 27 × 30 = **810 questions**. “Длина сессии” affects only quick practice,
mistake review and section sessions; training a theme always contains all 30 questions.

Levels inside a theme may vary (the rule level plus neighbouring levels), but keep the mix
realistic (A1–B2) and the majority at the rule’s own level.

## Rule text requirements

- `body` explains the rule and **does not repeat or list the examples** — examples go below,
  in `examples` only.
- Bodies are concise, well structured and easy to scan (2–4 short paragraphs or sentences).
- `examples`: 3–5 English phrases/sentences per section, each illustrating one point.
- Avoid walls of text, duplicated examples inside the body, and one-word orphan lines.

## Answer quality reminders

- One canonical answer per blank. When in doubt, make the sentence context force the answer.
- Explanations must teach the collocation or rule, not just say “this is correct”.
- For match questions, categories must be disjoint so each right example has exactly one left.
- Russian explanations/translations: natural language, «ё», no machine-translation artefacts.
- Article questions always choose from `a / an / the / —` (4 options).
