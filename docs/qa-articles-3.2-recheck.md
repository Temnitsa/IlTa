# Recheck — IlTa 3.2 articles (after fixes)

**Verdict: pass** — all critical and major findings from `docs/qa-articles-3.2.md` are fixed, no blocking defects remain; five minor residual notes and a few optional observations are listed at the end. Read-only recheck; only this report was written. No gradle.

## Verified fixes

| # (from QA report) | Finding | Status |
|---|---|---|
| 1 | **critical** `articles_01` `ind-0079` broken match pair | **Fixed** — pair 4 is now «новое исчисляемое существительное с прилагательным» ↔ “We met a friendly guide at the museum.”; all four cues now match their rights uniquely (article directly before noun → fox, plural general → vegetables, uncountable general → butter, a/an + adjective → friendly guide). |
| 2 | `ind-0018` cue «неисчисляемое в общем значении» | **Fixed** — cue now «неисчисляемое, неопределённое количество» ↔ “We need some water.” |
| 3 | `def-0103` pair 4 invalid definite example | **Fixed** — now «предмет, известный по описанию места» ↔ “The keys on the table are mine.” |
| 4 | `articles_05` `art-zero` coverage (building/work, radio/TV, months/holidays) | **Fixed** — §1 adds “My father is at work.” and “We walked past the school.” (activity vs building; body’s list trimmed, no orphan “сон” claim); §2 adds “I listen to the radio in the car.” and “We watch television in the evening.”; §3 adds “We met in July.” and “Christmas is a family holiday.” |
| 5 | `articles_06` `art-names-geo` coverage | **Fixed** — §0 adds Mr Smith / The Smiths / The 1990s; §1 adds the Netherlands (historical exception); §2 covers rivers, oceans, seas, mountain ranges, deserts, bays, lakes, peaks, straits; §3 adds streets/squares/stations/airports and universities/churches; §4 adds “A priest visited the school.” and “out of the question”. |
| 6 | `articles_07` `art-the-adjective` coverage | **Fixed** — §1 adds Dutch/Swiss/Germans (-ch/-ss and plural nationality nouns); §2 adds the/plural class alternatives (“The good mechanic… / Good mechanics…”); §3 replaces the concert example with “Shall we go to the cinema tonight?”; §4 adds next/last/possessive and time expressions (“Next week…”, “My best friend and I chose the same route.”). |
| 7 | `articles_08` `art-contrasts` coverage | **Fixed** — §0 off-topic run example replaced with parts-of-day and sides examples (“I always read in the morning.”, “The bank is on the left and the post office on the right.”); §3 now states the in-the-end vs at-the-end-of distinction and adds “A holiday on the Moon is out of the question.”; §4 adds “He works for the BBC.” |
| 8 | **major, docs** `docs/rule-catalog.md` IELTS framing | **Fixed** — exam wording removed (“IELTS”, “typical IELTS Writing mistakes”, “Grammar for IELTS”); theme 9 row is now «Артикли в связном тексте: общее, конкретное, связность» with connected-text scope; the scope column for all article themes is updated to the 3.2 structure. Only the internal id/file name `art-academic` / `articles_09_academic.json` remains (acceptable; documented in the catalog). |
| 9 | `articles_01` §0/§3 example gaps | **Fixed** — §0 adds “The water in the glass is cold.” (the + uncountable); §3 adds “I read a book. The book was interesting.” (second mention). |
| 10 | `articles_02` §0/§3 example gaps | **Fixed** — §0 adds “I met a woman. The woman was wearing blue.”; §3 adds “I spoke to a nurse at the reception.”; musician translation corrected. |
| 11 | `articles_03` §3 same/next | **Fixed for same/next** — body now lists only/same/next, examples add “They chose the same route.” and “The next train leaves at six.”; the possessive-pronoun claim remains without an example (minor, see below). |
| 12 | `articles_04` §0 consumption | **Fixed** — “The car uses six litres a kilometre.” added. |
| 13 | `articles_09` §1–§3 gaps | **Fixed** — life/the life pair, a/an + of-phrase (“He is a colleague of my father.”), next/possessive examples added; “last” time expression still unillustrated (minor, see below). |
| 14 | match cue wording (`def-0013`, `def-0093`, `def-0105`, `ind-0030`) | **Fixed** — “единственный объект, известный всем: мир в целом”, “событие, уточнённое придаточным предложением”, reworded new-item cue for “I’ve got some news…” |
| 15 | translation nits (`музыкантша`, `сидеть в тюрьме`) | **Fixed** — «талантливый музыкант», «находиться в тюрьме». |

## Structural checks (all green)

- `python3 scripts/validate_content.py app/src/main/assets/data/content` → **OK: content is valid** (single-file warnings about other sections are mode artefacts).
- Question inventory unchanged vs git HEAD (3.1.0): 70 questions per theme, 630 total (530 choice + 100 match; themes 6 and 9 keep 55 choice + 15 match), ids identical and in the same order, choice questions untouched.
- 43 sections / 181 examples; every section has 3–5 examples with a 1:1 aligned `exampleTranslations`; translations accurate, no missing «ё», no misalignment (new examples checked individually).
- No body/example duplication (no example string appears in its section body); no duplicate examples within a theme.
- No IELTS/TOEFL/exam strings in article JSON, `docs/rule-catalog.md`, `docs/content-schema.md` or `sections.json`.
- All 100 match questions / 400 pairs reviewed: lefts are semantic Russian cues without target article/answer, no broken pairs found; the only Latin characters in cues are phonetic hints (немая h, [w], [i:], [b], [d]).

## Remaining minor notes (non-blocking)

1. `articles_03` §3 — body still says “После притяжательного местоимения артикль не нужен”, but the five examples (best/first/only/same/next) contain no possessive-pronoun example; add e.g. “She is my best friend.” or trim the sentence.
2. `articles_06` §3 — body lists magazines, galleries and theatres, but examples only cover newspaper/museum/hotel/streets/universities/churches/organisation; no magazine/gallery/theatre example.
3. `articles_07` §4 — body says “В выражениях времени next и last идут без артикля”, but only “Next week…” is illustrated; no “last week / last year” example.
4. `articles_09` §3 — same: “last” time expression is promised in the body but not illustrated (only “Next month…”).
5. `articles_08` §4 — body mentions museums and landmarks, but examples show newspaper/hotel/unique role/profession/organisation only; no museum-name or landmark-name example.

## Optional observations (not blockers)

- `articles_06` §2 last example “Lake Victoria, Mont Blanc and the Strait of Gibraltar” mixes zero-article lake/peak with the-strait in one line (schema prefers one point per example).
- Cross-theme reused examples (pre-existing, not introduced by the fixes): “Water is essential for life.” (01/05), “I saw a dog.” / “There is a message for you.” / “She is a doctor.” (01/02), “the Pope” (02/06).
- `docs/rule-catalog.md` art-the-known scope still lists “last” while the rule body now says only/same/next (tiny doc mismatch).
- `articles_07` §4 and `articles_09` §3 use near-identical next/last example sentences (next week/month, next train/last one) — variety note only.

**Conclusion:** the 3.2 article content and `docs/rule-catalog.md` can be accepted; remaining notes are optional polish.
