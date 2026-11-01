# Rule catalog — 16 topics in 2 sections

Levels: A1, A2, B1, B2. `order` values are assigned in increments of 10.
Every rule belongs to a section via `sectionId`.

Article themes are intentionally consolidated: each theme covers a coherent area of article
usage, its rule text is based on *English Grammar in Use* / *Grammar for IELTS*, and its pool
contains **70 questions**. A topic training session shows **30** of them (5 match + 25 choice).

## Section `articles` — Артикли / Articles (9 themes, 630 questions)

| id | order | level | title (ru) | scope |
|---|---|---|---|---|
| art-system | 10 | A1 | Система артиклей: исчисляемое, общее и конкретное | countable/uncountable, general vs specific, overview of a/an/the/zero |
| art-a-an | 20 | A1 | a/an: первое упоминание, профессии и характеристики | new information, one of many, jobs/descriptions, unique roles (the vs a/an) |
| art-the-known | 30 | A2 | the: известное, единственное, превосходная степень | second mention, known things, context/unique, of-phrases, superlatives/ordinals, same/only/last/next |
| art-a-an-special | 40 | A2 | a/an: мера, частота и звуковые исключения | half an hour, twice a week, a/an vs one, an hour / a university, abbreviations |
| art-zero | 50 | A2 | Нулевой артикль: общее значение и устойчивые выражения | plural/uncountable general, bed/school/work/home/hospital/prison, by car/on foot, meals/time, fixed expressions |
| art-names-geo | 60 | B1 | Имена, названия и география | cities, countries and exceptions, rivers/seas/oceans/mountains, newspapers/hotels/museums/organisations, languages |
| art-the-adjective | 70 | B2 | the: прилагательные, национальности, учреждения | the rich/young/the British, generic a/an, institutions and places (activity vs building) |
| art-contrasts | 80 | B2 | Устойчивые выражения и контрасты | fixed phrases with the/a/an, a few/a little vs few/little, take place / take the place of, in front of / in the front of, out of the question |
| art-academic | 90 | B2 | Артикли в академическом тексте / IELTS | abstract nouns, generalisations, of-phrases/relative clauses, typical IELTS Writing mistakes |

## Section `prepositions` — Предлоги / Prepositions (6 themes, 310 questions)

Each preposition theme has a pool of 50–60 questions; a training session shows 30 of them
(5 match + 25 choice).

| id | order | level | title (ru) | scope |
|---|---|---|---|---|
| prep-time | 10 | A1 | Предлоги времени: at, on, in, by, until | at 7 o'clock, at night, at the weekend; on Monday, on 5 May; in July, in 2026, in the morning; by/until, for/since/during; last/next/this without a preposition |
| prep-place | 20 | A1 | Предлоги места и положения | at/on/in; above/below, over/under; between/among, by/beside, opposite; in front of/behind, next to; inside/outside, on the left/right, at the top/bottom |
| prep-movement | 30 | A2 | Предлоги движения и направления | to/into/onto/out of/off; through/across/along; over/under, towards, past; around/round, from…to |
| prep-adjectives | 40 | A2 | Предлог после прилагательного | afraid of, proud of, interested in, good at, famous for, different from, similar to, worried about, full of, keen on, patient with and more |
| prep-verbs | 50 | A2 | Предлог после глагола | depend on, wait for, listen to, look at, arrive at/in, belong to, apologise for; no preposition: discuss, enter, reach, marry |
| prep-fixed | 60 | B1 | Устойчивые выражения и тонкие случаи | fixed phrases, on time/in time, during/for, by/with/in, between/among, except, at the end/in the end, by the time, questions ending with a preposition |

Question ids are globally unique; article questions are tagged with their original subtopic and
optional pattern tags; preposition questions are tagged with the theme and a pattern label.

## Section `tenses` — Времена / Tenses (12 themes, 360 questions)

Each tense theme has exactly 30 questions: 20 choice + 5 build + 5 match.
Build questions are assembled from word tiles; accepted word orders are stored in `answers`.

| id | order | level | title (ru) | scope |
|---|---|---|---|---|
| tense-present-simple | 10 | A1 | Present Simple | form (-s/-es, do/does), habits/facts/schedules, markers, errors |
| tense-present-continuous | 20 | A1 | Present Continuous | am/is/are + V-ing, action now/temporary/plans, markers, state verbs |
| tense-present-perfect | 30 | A2 | Present Perfect | have/has + V3, result/experience/unfinished time, for/since, been/gone |
| tense-present-perfect-continuous | 40 | B1 | Present Perfect Continuous | have/has been + V-ing, duration with result, for/since/how long, state verbs |
| tense-past-simple | 50 | A1 | Past Simple | regular/irregular verbs, did, finished time, markers, errors |
| tense-past-continuous | 60 | A2 | Past Continuous | was/were + V-ing, interrupted/background actions, when/while, contrast with Past Simple |
| tense-past-perfect | 70 | B1 | Past Perfect | had + V3, action before another past action, by the time, reported speech |
| tense-past-perfect-continuous | 80 | B2 | Past Perfect Continuous | had been + V-ing, duration up to a past moment, cause/result, contrast with Past Perfect |
| tense-future-simple | 90 | A1 | Future Simple | will + base form, predictions/decisions/promises/offers, when/if + Present Simple |
| tense-future-continuous | 100 | B1 | Future Continuous | will be + V-ing, action in progress at a future moment, this time tomorrow |
| tense-future-perfect | 110 | B2 | Future Perfect | will have + V3, completed before a future moment, by/by the time |
| tense-future-perfect-continuous | 120 | B2 | Future Perfect Continuous | will have been + V-ing, duration up to a future moment, by … for |

The dedicated «Сборка предложений» mode draws questions of type `build` from these themes.
