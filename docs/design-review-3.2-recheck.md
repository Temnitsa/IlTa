# Design recheck — IlTa 3.2 (after fixing the 4 blockers)

**Score: 9/10 — target met.** All four blockers from `docs/design-review-3.2.md` are closed and verifiably fixed in the fresh captures and in code. The remaining items are local polish (one title-fallback defect on four article rules, a stale 203 capture, and optional match/glyph/discoverability/dark-theme nits); none of them changes the visual system, hierarchy or typography. Read-only recheck; only this report was written. No gradle.

## Blocker closure

| Prior blocker | Status | Evidence |
|---|---|---|
| 1 — long-press translation popup off-system, overlays content | **Fixed** | 211 + `RuleDetailScreen.kt`: the popup is gone (`DropdownMenu`/`Popup` no longer used); long-press now expands an inline translation row inside the same example card — `surfaceVariant` 22% fill, 8dp radius, 4dp gap, 14sp `onSurfaceVariant`, tap-to-collapse. No overlay, no shadow, no off-palette menu container; nothing is covered. It matches the flat example rows. |
| 2 — rule-page composition inconsistent (summary inside progress card) | **Fixed** | 210 + code: the progress card is now identical everywhere (level chip + «Прогресс темы» + % + bar). The summary is a separate plain paragraph directly under the card (14sp `onSurfaceVariant`, page margin), and the first section card follows normally. The tenses pages show no summary block because all 12 tenses summaries are formula-only strings (e.g. «(+) V / V-s · (–) do / does…»), i.e. content-driven, not a layout inconsistency. 210 (articles) and 207 (tenses) now share the same page skeleton. |
| 3 — app-bar short titles not robust | **Fixed for the reported failure** | `RuleDetailScreen.kt`: new `shortRuleTitle()` splits on “:”, “ — ” and “ (”; long titles now render short: “Present Simple” (207), “Система артиклей” (210), and the four previously-ellipsized tense titles resolve to “Present Perfect Continuous”, “Future Continuous”, “Future Perfect”, “Future Perfect Continuous” (all ≤26 chars). One residual fallback defect on the four article rules — see minor 1. |
| 4 — wrong practice instruction («Выберите правильный предлог» on tense questions) | **Fixed (code)** | `PracticeScreen.kt` prompt is now section-aware: `articles` → «Выберите правильный артикль», `prepositions` → «Выберите правильный предлог», other choice questions → «Выберите правильный вариант». 203 is a stale capture (12:06, pre-fix) that still shows the old prompt — re-capture it (minor 2). |

## Re-verified screens

- **210 rule page with summary block** — strong: identical clean progress card, summary as a calm grey paragraph, text-only section headings, tinted formula block, hairline + «Примеры» rows. The page reads in one clear vertical rhythm; no icons needed.
- **211 inline translation** — the fix feels native: translation appears exactly under the tapped example inside the card, no overlay, no elevation, no menu chrome; the example list never shifts out of view.
- **212 About logo** — elephant now reads clearly (36dp brand plate, painter scaled 1.35×) and aligns with the «IlTa» / version rows; the cream `#FBF8F2` plate is brand-correct on the white card (optional neutral-tint note only).
- **207 / 200 / 201 / 203 (unchanged captures)** — home hierarchy, match «Значение/Пример» semantics and choice feedback hierarchy remain good; 201’s minor grid/whitespace notes from the main review are unchanged (minor 3), 203 needs a re-capture (minor 2).
- System checks: palette (#F7F8FA / #FFF / #4D6BFE / #E4E7EC), 14dp card/10dp control/8dp chip radii, 1dp hairlines/no shadows (the modal sheet aside), 20/18/16/15/14/13/12/11 type scale with clear roles, 16dp margins, 10dp stacks, single accent, line icons, no emoji/streak/taglines — all consistent across the fresh screens.

## Remaining minor notes (non-blocking)

1. **Article app-bar titles are chopped mid-word** by the `shortRuleTitle` fallback (`full.take(30)`) because their “:”-stems are 3–4 chars and fail the `length >= 6` guard: `art-a-an` → “a/an: первое упоминание, профе”, `art-a-an-special` → “a/an: мера, порядок слов и зву”, `art-the-known` → “The: известное, единственное и”, `art-the-adjective` → “the: прилагательные, националь”. Fix: add explicit short titles for these four rules (e.g. «a/an: первое упоминание», «a/an: особые случаи», «The: известное», «the + прилагательные») or change the fallback to `full` with `maxLines = 1` + ellipsis instead of a hard 30-char cut; consider the same for `prep-fixed` (“Устойчивые выражения и тонкие случаи”, 36 chars) at 360dp. This is the only visible defect left from the title work.
2. **Stale captures in the gallery:** `203_choice_feedback.png` (12:06) still shows the pre-fix «Выберите правильный предлог» on a future-simple question; `200/201/207` are unchanged but valid. Re-capture 203 (tense choice) so the screenshot set matches the shipped build.
3. **Match screen** (unchanged): the two columns are independent `Column`s with `heightIn(min = 52.dp)`, so rows look slightly ragged when texts differ in line count, and the block leaves ~45% empty page on tall screens. Optional polish: equal card height (e.g. 72dp) and vertically centering the two-column block.
4. **Discoverability of inline translation** — no hint that «Примеры» rows are long-pressable; optional small caption «долгое нажатие — перевод» in the «Примеры» label row (11sp `onSurfaceVariant`).
5. **Feedback ✓/✕ text glyphs** while the rest of the app uses line icons; optional `Icons.Filled.Check/Close` for optical consistency.
6. **No dark-theme captures** — components rely on theme roles; a dark set (home, rule page with inline translation, feedback) is optional but worth having for the release gallery.
7. **Home section counters** («9 тем · 630 вопросов») retained while rule/theme rows dropped counters — intentional section totals; no change needed.

**Bottom line:** 9/10 — the four blockers are closed, the system is DSH-grade and consistent across the new screens; fix minor 1 (four explicit short titles) and re-capture 203 and the set is ready.
