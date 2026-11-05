# Recheck — IlTa 3.2 prepositions (after fixes)

**Verdict: pass** — all six major findings and the actionable minor findings from `docs/qa-prepositions-3.2.md` are fixed; the two bank-frozen minors (option counts, absolutist explanations) remain accepted as-is; no critical or blocking defects remain. Structural checks are green, the question bank is untouched, and the remaining 9 notes are local polish (two of them new wording/coverage nits introduced by the fixes). Read-only recheck; only this report was written. No gradle.

## Verified fixes

| Prior finding | Status | Evidence |
|---|---|---|
| 1 — place theme lost spatial relations | **Fixed** | `prep-place` has a fifth section «Положение относительно объектов» (6 examples + translations): next to, beside, by, behind, in front of, opposite, above/below, over/under, inside/outside, in the middle of, between/among; summary updated. All 16 choice items and the 5 match pairs (prep1-0083/0084/0085) are now backed by the rule. |
| 2 — `-ing` after prepositions missing | **Fixed** | `prep-adjectives` §3 and `prep-verbs` §1 state «После предлога глагол стоит в форме -ing»; examples added (“She is good at solving difficult puzzles.”, “He apologised for being late.”). |
| 3 — no-prep verbs missing | **Fixed** | `prep-verbs` §1 lists «discuss, enter, reach, marry, join»; match `prep3-0090` now pairs *вступление в клуб* ↔ “She joined the club last month.” and §2 adds “We discussed the plan for hours.” |
| 4 — place/other EGU 121–136 coverage | **Fixed** | Added bullets + examples: on a river/coast, on a menu/list/page/website, on the front/back of an envelope, at/on the corner, in the middle of, in a row/newspaper/sky, in the north of; at somebody's house, at the doctor's, at the station/airport; in the sun/shade/dark/bad weather, in a mood, in love, in a currency, on a trip/tour/cruise, for a holiday; at the minute/at this time; in the past. |
| 5 — verb EGU 132–136 coverage | **Fixed** | consist of, live on, happen to, compliment on, blame on, cut/split into, supply with, bump/run into, aim/shoot at, throw at/to, shout to, apply to a company, complain (to somebody) about/of, care what/where, depend when/where, smile at — all added to bodies (with new examples for most). |
| 6 — adjectives rule/bank mismatch | **Fixed (one residual)** | The 9 tested collocations now in `prep-adjectives` §5 (serious about, careful with, popular with, suitable for, curious about, grateful for, allergic to, successful in, patient with); ready/late for and close to/fond of added to §3/§4; honest/stupid/silly of, good/generous/friendly/cruel to, «sorry I…» added to §2. Residual: *generous with* — see below. |
| 7 — since / complain / smile at | **Fixed** | *Since* in time §5 with example (“…worked here since 2018.”); *complain (to somebody) about/of* in verbs §3; *smile at* in verbs §1 with example. |
| 8 — match cue / explanation wording | **Fixed, one new residual** | `prep2-0045` cue is now «вход в воду (движение внутрь)» (accurate for *dived into*); `prep4-0081` explanation now reads «Пары противопоставляют: at the end of / in the end, on time / in time, by chance / on purpose, in charge of / under control.» New residual: `prep2-0045`’s own explanation still says «out of — наружу» though its pair 4 is now *past*. |
| 9 — match duplication | **Partly fixed** | `prep3-0090` replaced *belong to* with *join*; `prep4-0081` pair 1 became an *at the end of* contrast; `prep4-0082` rights refreshed (by boat / with scissors / during the speech / except Wednesday). Residual duplication noted below. |
| 10 — match tags/prompts | **Fixed** | All 35 matches carry `[theme, subtopic]` tags (e.g. `['prep-place','spatial-relations']`) and use the unified prompt «Сопоставьте смысл и пример.» |
| 11 — examples not covering subrules | **Largely fixed** | Examples 124 → **151**; like/as now has an example (“He behaves like a child, but he works as a manager.”); added examples for while, onto/along, by boat, by post/in cash, at least/by mistake/on purpose, in the shade, thought of/reminds, discussed, consisted, supplied, bumped, dependent on, good at solving. |
| 12 — translation nits | **Fixed (one new nit)** | “in the night” → «посреди ночи», “by Friday” → «не позднее пятницы», “next summer” → «Следующим летом», “excited about the trip” → «в восторге от поездки». New: «Сад в задней части дома.» — see below. |
| 13–14 — option counts / absolutist explanations | Accepted | Choice bank untouched by instruction (0 items changed vs HEAD), so these non-blocking pre-existing items remain. |

## Structural checks (all green)

- `python3 scripts/validate_content.py` — all six preposition files **OK: content is valid**; whole content set OK (only unrelated `tenses_*` warnings).
- Question inventory unchanged vs git HEAD (3.1.0): **50/50/50/50/50/60** (275 choice + 35 match), ids/types/levels/order identical, **choice bank 0 items changed**; rule ids/levels/orders unchanged; 5 sections per rule.
- **151 examples / 151 `exampleTranslations`** — 0 misaligned, 0 empty, Russian present everywhere, no markup leaks.
- Matches: 35 × 4 pairs, no duplicate rights, all left cues are semantic Russian hints with no target-preposition leakage, prompt/tag metadata consistent; explanations match the pair sets except the one noted below.
- No body/example duplication (validator-clean).

## Remaining defects (minor, non-blocking)

1. Match explanations — `prep2-0045` is stale after the pair edit: it says «out of — наружу» but pair 4 is “The dog ran past the gate…”; update to «past — мимо» and drop *out of*. `prep3-0090` still ends with «Предлог to нужен только в belong to», although *belong to* is not among its pairs; drop/reword that note.
2. `prep-place` §2 body — «**At** — at the top/bottom/end of the street; **in the north of** a country» groups an *in*-phrase under the *At* bullet; move «in the north/south of a country» to the *In* line (or split it as its own sentence).
3. `prep-place` §4 body — «**in** a mood» should be «in a good/bad mood»; the in-map «in the sun, in the shade, in the dark, in the rain — на солнце, в тени, под дождём» omits «в темноте» for *in the dark*; the *speed* point (at 90 miles an hour, EGU 127D) was dropped — restore if the space is enough.
4. `prep-adjectives` — *generous with* is tested by `prep3-0216` (“generous ___ his time”, answer *with*) but still not named in any body (only generous of/to); add «generous **with** something (time/money)». Book residuals: *at the hairdresser's* (125B), *fire at* (132C), *translate from … into* (136) are still missing from bodies.
5. Tested collocations still absent from bodies: *belong to* (2 items, though taught via match `prep3-0087`), *prepared for* (`prep3-0024`), *worry about* (`prep3-0120`; the rule teaches *worried about*), *on the right* (`prep1-0217`; the body lists only *on the left*) — add one bullet each or accept item-level explanations.
6. Example coverage residual: *like/as* still has no practice item (bank frozen); `prep-place` §5 examples do not show beside/below/inside/outside/between/among; `prep-movement` §3 examples do not show off/past/under/around; `prep-fixed` §2 does not illustrate in advance/in fact/in general/in love/currency/trip (several are covered by matches).
7. Translation nit: `prep-place` §2 “The garden is at the back of the house.” → «Сад в задней части дома.» — better «Сад за домом (с задней стороны дома)».
8. Match duplication residual: *on time* is tested by both `prep4-0038` and `prep4-0081`; the *to/into* arrival sets of `prep2-0045`/`0046` remain similar; `prep4-0081` still re-uses on time/by chance/in charge of from `prep4-0037`–0040.
9. Wording/grouping nits in `prep-verbs` §5: *happen to* sits under «To — предпочтение», *spend on* under «On — настойчивость» — regroup («To — адресат/конструкция», «On — настойчивость и трата»).

**Bottom line:** the six major findings are fixed and the minor set is closed except the local notes above; no question-pool or translation alignment regressions. Pass.
