# Rule catalog — 23 topics in 3 sections

Levels: A1, A2, B1, B2. `order` values are assigned in increments of 10.
Every rule belongs to a section via `sectionId`.

Article themes are intentionally consolidated: each theme covers a coherent area of article
usage, its rule text is based on *English Grammar in Use*, and its pool
contains **55–60 questions**. A topic training session shows **30** of them (all `choice`).

## Section `articles` — Артикли / Articles (9 themes, 530 questions)

| id | order | level | title (ru) | scope |
|---|---|---|---|---|
| art-system | 10 | A1 | Система артиклей: исчисляемое, общее и конкретное | countable/uncountable, general meaning without article, new singular with a/an, known/specific with the, how to choose |
| art-a-an | 20 | A1 | a/an: первое упоминание, профессии и единственная роль | new information and one of many, professions and roles, nationalities and religions, a/an vs the for a unique role |
| art-the-known | 30 | A2 | The: известное, единственное и конкретизация | second mention, unique in the world or situation, of-phrases and relative clauses, superlatives/ordinals, same/only/last/next |
| art-a-an-special | 40 | A2 | a/an: мера, порядок слов и звуковые исключения | measure/frequency/price, a/an vs one, half/such/quite/what word order, silent h and 'yu' sound, abbreviations |
| art-zero | 50 | A2 | Нулевой артикль: общее значение, места и устойчивые выражения | plural/uncountable general, institutions and habitual places (bed/home/school/work/hospital/prison), transport, food/time/calendar, fixed expressions |
| art-names-geo | 60 | B1 | Имена, названия и география | people's names, titles, family plurals and decades; languages/days/months/holidays; cities/countries/continents incl. the-exceptions; rivers/seas/oceans/straits/bays/mountain ranges/deserts vs individual lakes/peaks; newspapers/hotels/museums/organisations; streets/squares/stations/airports vs universities/churches/landmarks; unique roles and fixed pairs (incl. out of the question) |
| art-the-adjective | 70 | B2 | the: прилагательные, национальности, учреждения | the + adjective groups; nationality names in -sh/-ch/-ese/-ss vs plural nationality nouns; generic a/an and class alternatives; institutions and leisure places (activity vs building); superlatives/ordinals/only/same/next/last/possessive and time expressions |
| art-contrasts | 80 | B2 | Устойчивые выражения и контрасты | fixed phrases with the (instruments, parts of day, weekend, seaside, sides, linkers); fixed a/an phrases (illness, hurry, decisions, quantity, rule); a few/a little vs few/little; take place vs take the place of; in front of vs in the front of; in the end vs at the end of; out of the question; names, organisations and unique roles |
| art-academic | 90 | B2 | Артикли в связном тексте: общее, конкретное, связность | general vs specific meaning, abstract nouns (life/experience), specification by of-phrases/relative clauses/descriptions, superlatives and ordinals, cohesion and repeated mention |

## Section `prepositions` — Предлоги / Prepositions (2 themes, 90 questions)

The two preposition themes are **visual dictionaries**: every preposition is a picture tile
(blue isometric cube + red sphere, matching the reference art), tapping it opens a dialog with
translation, explanation and English examples with Russian translations. Each theme has a pool
of **exactly 45 `choice` questions**; a training session shows 30 of them.

| id | order | level | title (ru) | prepositions |
|---|---|---|---|---|
| prep-position | 20 | A1 | Позиционные предлоги | above, across, against, among, around, behind, below, beneath, beside, between, by, in, in front of, inside, near, next to, on, outside, over, through, under, underneath (22) |
| prep-direction | 30 | A1 | Предлоги направления | across, against, along, around, away (from), down, from, in(to), off (of), on(to), out (of), over, through, to, toward(s), under, up (17) |

The dictionary data lives in `app/src/main/assets/data/preposition_reference.json`, the picture
assets in `app/src/main/res/drawable-nodpi/prep_<key>.png`.

Question ids are globally unique; article questions are tagged with their original subtopic and
optional pattern tags; preposition questions are tagged with the theme.

## Section `tenses` — Времена / Tenses (12 themes, 960 questions)

Each tense theme has a pool of **80 questions**: 20 `choice` + 60 `build`; a training session
draws 15 `choice` + 15 `build`.
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
