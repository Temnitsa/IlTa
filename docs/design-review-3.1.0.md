# Design review — IlTa 3.1.0 (line icons · radar · rule page · launcher)

Version reviewed: **IlTa 3.1.0** (working tree `versionName = "3.1.0"`, uncommitted 3.1.0 changes).
Artifacts inspected via image reading: `art/screenshots/165_home_line_icons.png`, `162_stats_radar.png`, `168_section_clean.png`, `169_rule_final.png`, `170_launcher_final.png`. Code/asset inspection (read-only, no gradle): `ui/theme/Theme.kt`, `ui/components/Common.kt`, `ui/App.kt`, `ui/screens/TopicsScreen.kt`, `SectionScreen.kt`, `RuleDetailScreen.kt`, `StatsScreen.kt`, `domain/StatsCalculator.kt` (radar metric semantics), `res/drawable/ic_section_*.xml`, `ic_rule_*.xml`, `res/mipmap-*/ic_launcher*.png`, `mipmap-anydpi-v26/ic_launcher*.xml`, `values/colors.xml`. No files besides this report were written.

**Verdict: good (8/10).** The 3.1.0 visual system is coherent and clearly DSH-flavoured: neutral light canvas, flat white cards with 1 dp hairline borders and no elevation, one blue accent, compact type scale, line icons everywhere instead of letter tiles, a genuinely minimal-but-not-bare rule page, and no filler copy. Two items should be closed before release (radar readability/empty state; launcher asset finalisation) and four are polish. Nothing blocks the layout.

## Checklist requested by the task

| Ask | Result | Evidence |
|---|---|---|
| Elephant fully inside the circle | ✅ (assets) / ⚠️ screenshot↔asset mismatch | In 170 the elephant sits fully inside the cream disc with visible margins. In the shipped res the adaptive foreground elephant bbox is 250×206 of a 432 canvas → 57.9 % width, below the 61.1 % adaptive safe zone (3.0.0 assets were 72 % and would be clipped by a circular mask). Legacy square/round PNGs: elephant 246×192 with ≥92 px margins. See finding 1 for the ring problem. |
| No question marks on the stat tiles | ✅ | Screenshot 162: tiles show values only (“4 / 25 % / 0”), no “?”. Code grep: `?` appears only in legitimate dialog titles («Сбросить прогресс?», «Выйти из тренировки?»). The previous hint glyphs are gone. |
| Minimal line icons instead of А/П/В | ✅ | `SectionIcon()` uses 6 new 24 dp/1.7-stroke vector drawables (`ic_section_articles/prepositions/tenses`, `ic_rule_form/markers/when`), tinted `onSurfaceVariant`, in a 34–36 dp rounded `surfaceVariant` chip. No letter-monogram section tiles anywhere; the only remaining letter mark is the About card (`MonogramBadge("IlTa")` → “I”, see finding 2). |
| Rule page not bare, but minimalist | ✅ | 169: progress card (level chip + “Прогресс темы” + % + thin bar + “30 вопросов в тренировке · 90 вопросов в теме”), then section cards with heading icon chips, tinted monospace formula block, bullet lists, a hairline + “Примеры” label + example rows with a primary “→”, bottom CTA. Two-plus screens of content, no dead white space, no decorative noise. |
| Radar readable (labels/lines/dots, empty data) | ⚠️ labels/geometry ok, scale + sparse data weak | 162: 6 axis labels readable and not clipped, 4-level grid light but visible, primary 2 dp data line + 3 dp dots legible. But there is **no numeric scale/legend**, the 6 axes mix section accuracy with hidden metrics, and with only one non-zero axis the polygon collapses into a line plus a dot stack at the centre (see finding 2). Zero-data case is handled by a separate “Пока нет данных” card — good. |
| DSH-style consistency | ✅ | Tokens: `background #F7F8FA`, `surface #FFFFFF`, `primary #4D6BFE`, `onSurface #111318`, `onSurfaceVariant #5F6673`, `outlineVariant #E4E7EC`; cards 14 dp / buttons 46 dp / chips 8–10 dp / inputs 6 dp radii; hairline borders, no shadows; one accent; no emoji, mascots, streak or taglines; Russian-only; flat `NavigationBar` with a top hairline divider. Dark scheme exists and the system/light/dark switch is in Settings. |
| Clipping / overlays | ✅ provided screens; ⚠️ predicted app-bar ellipsis | 165/162/168/169: no visible clipping or overlap; 2-line rule titles fit inside section cards; radar labels stay inside the card; bars/percent/labels are aligned; content scrolls inside Scaffold `innerPadding` (nothing hides under the nav bar). Predicted: the one-line app bar title (`maxLines = 1`) will ellipsise for the longest tense titles (tenses_04/08/12, 55–64 chars, e.g. “Future Perfect Continuous — будущее совершённое длительное время”) — see finding 5. |

## DSH resemblance checklist

| Criterion | Result | Notes |
|---|---|---|
| Light, flat, clean | ✅ | Flat white cards, 1 dp `outlineVariant` hairline, no elevation anywhere; `NavigationBar` `tonalElevation = 0.dp`; app bar container = background. |
| Compactness | ✅ | 16 dp screen margins, 10 dp stack gap, 14 dp card padding, 14–15 sp body, 11–13 sp labels, 46 dp buttons. |
| Single accent | ✅ | Only `primary #4D6BFE` + `primaryContainer #E8EDFF`; per-level colours removed (LevelChip is neutral), percentages are `onSurfaceVariant` (previous contrast problem solved). |
| Hierarchy | ✅ | Home: title → progress card → primary CTA → mistakes → “Разделы”; Section: summary → CTA → “Темы” list; Rule: progress → sections → CTA. One H1 per screen (the duplicated section/rule title is gone). |
| Typography | ✅ | One scale (20/18/18/15/13/15/14/12/13/12/11 sp) with `LineBreak.Heading` for prose; monospace only in the formula block — meaningful exception. |
| Iconography | ✅ / ⚠️ | Consistent 1.7-stroke line icons at 24 dp viewport, tinted; remaining glyph “›” in home/section rows and a 20 dp custom stats nav icon (finding 6). |
| Empty / zero states | ✅ | “Пока нет данных — ответьте на несколько вопросов…” on Stats; factual “Пока нет ошибок”; no marketing filler. |
| Dark theme | ✅ code / ⚠️ no capture | `DarkColors` (#0F1115 bg / #171A21 surface / #7C93FF primary / #9AA3B2 secondary text) with correct status/nav-bar icon appearance; all colours come from `MaterialTheme` (no hardcoded colours outside Theme.kt), so charts/rows/icons adapt. A dark screenshot set is still missing — please add one before release. |

## Screen notes

- **Home (165)** — clean and fast; one progress card (duplicate section rows removed, 3.0.0 finding 2 closed), primary button with session size on the right, mistake row, three section cards with line icons, percent + chevron, thin bar, meta line. No visual noise.
- **Stats / radar (162)** — three metric tiles, progress ring card, radar card, levels list. Radar is the only soft spot: attractive and readable as a shape, but without a scale the numbers behind the shape are unknowable and the sparse-data rendering looks like an artifact.
- **Section (168)** — app bar now sits directly under the status bar (2.9.3 double-inset bug fixed), summary card has icon + counts + %, CTA, and rule cards with 2-line titles and a neutral level chip; no chevrons on clickable cards (see finding 6).
- **Rule (169)** — strongest screen: progress card, icon-labelled section cards, tinted monospace formulas, “Примеры” rows with the primary arrow, bottom CTA. Nothing bare, nothing loud.
- **Launcher (170)** — elephant reads well, is inside the circle and safe-zone-correct; the pale-teal ring is not reproducible from the current res (finding 1).

## Findings (max 6)

| # | Severity | Issue | Exact suggested fix |
|---|---|---|---|
| 1 | **medium** | **Launcher screenshot does not match the shipped assets.** 170 shows a pale-teal ring (`#8EE2E5`, ~16 px at 158 px icon) around a cream disc (`#FBF8F2`) with the elephant; but the current `res` cannot produce that ring: adaptive-icon background is the solid colour `#FBF8F2` (`values/colors.xml`), the legacy/round PNGs are flat cream plates and the monochrome layer is just the elephant. So either the screenshot is from an intermediate build or the ring layer is missing from the final assets. Related brand inconsistency: the in-app About card still shows `MonogramBadge("IlTa")` = “I” instead of the elephant mark. | Decide the final mark and make assets match: if the ring is final, add it to the adaptive background as a layer-list (`fillColor #8EE2E5` + inset oval `#FBF8F2` covering ~80 % of the 108 dp canvas), export all densities incl. `_round` and regenerate `_monochrome` from the same silhouette, then re-capture 170. If cream-only is final, re-capture 170 from the current build. Replace the About `MonogramBadge` with the 36 dp elephant mark (same `SectionIcon`/drawable treatment) so the launcher, splash and About share one mark. |
| 2 | **medium** | **Radar semantics + sparse data.** Labels/lines/dots are legible, but: (a) there is no numeric scale or legend, so 25 % on “Артикли” vs a hidden “Скорость/Освоение/Покрытие” score cannot be read — the six axes mix section *accuracy* with behavioural metrics of different meaning; (b) with only one non-zero axis (the screenshot state) the polygon degenerates to a line with 5 dots stacked at the centre, which reads as a rendering bug; (c) the Canvas chart has no accessibility semantics. | Add per-axis value captions at the ring tips (e.g. “25 %”) or a one-line legend under the title (“точность по разделам · скорость/освоение/покрытие — скрытые метрики”); draw dots only for `value > 0.02` (draw hollow/dim dots at 0) and, when fewer than 3 metrics are non-zero, show only dots/lines plus a hint “Мало данных — ответьте ещё на несколько вопросов”; add `Modifier.semantics { contentDescription = "Артикли 25 %, Предлоги 0 %, …" }` for screen readers. If the mix is still unclear, split: radar = 3 sections’ accuracy, three compact bars = Скорость/Освоение/Покрытие. |
| 3 | **minor** | **Stat tiles are tappable but have no affordance.** The “?” hint glyphs were removed as planned (good), but `StatTile(onClick = …)` still opens the explanation dialogs, so the useful hints are now undiscoverable; labels also read awkwardly under a number (“4 / Ответов”, mixed “Точность” / “Освоено”). | Choose one: (a) make tiles non-interactive (`onClick = null`) and drop the hint dialogs, or (b) keep them interactive and add a subtle cue — 12 dp `info` glyph (neutral tint) in the tile's top-right corner or the label underlined with a dotted 1 dp rule — plus `Modifier.semantics { role = Role.Button }` and contentDescription. Relabel to “Ответы” / “Точность” / “Освоено” (or “Всего ответов”) for consistent noun forms. |
| 4 | **minor** | **Section CTA still shows the pool size, not the session size.** “Тренировать раздел / 1080 вопросов” on 168, while the action starts a 10-question session (`App.kt` takes `settings.sessionSize`); Home says “10 вопросов”, Rule says “30 вопросов”. This is the leftover of the 2.9.3 consistency finding. | Change the section button right label to `questionsLabel(settings.sessionSize)` (“10 вопросов”) — matching Home — or “10 из 1080”. Pool count stays in the summary card only. |
| 5 | **minor** | **Rule screen: long titles ellipsise in the app bar and the primary action is only at the page bottom.** `BackTopBar` title is `maxLines = 1`; tenses_04/08/12 titles are 55–64 chars and will show “…”; and “Тренировать тему” sits after all sections (2–3 screens down on long rules). | Title: show the short English name in the app bar (substring before “:” / “ — ”, e.g. “Future Perfect Continuous”) and keep the full title inside the content, or allow 2 lines in `CenterAlignedTopAppBar`. Action: add a compact “Тренировать” text/icon action to `BackTopBar(actions = …)` (slot already exists) or a sticky bottom CTA, keeping the existing bottom button. |
| 6 | **minor** | **Affordance/icon leftovers.** Home and section rows still use the text glyph `Text("›", titleLarge)` as a chevron (baseline/thickness differ from icons, cannot be tinted/rotated consistently), while the section theme cards are clickable with **no** chevron at all — two different patterns for the same interaction; the custom `StatsBarIcon` is drawn at 20 dp next to 24 dp Material icons in the same nav bar. | Use one affordance everywhere: either `Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(18.dp), tint = onSurfaceVariant)` on all clickable cards (home sections, mistakes, section rules) or no chevrons anywhere and rely on card ripple; render `StatsBarIcon` at 24 dp and match its bar weight (2–2.5 dp) to the Material icons for optical balance. |

## Delta vs the 3.0.0 review

| 3.0.0 finding | 3.1.0 status |
|---|---|
| 1. Launcher icon quality / brand consistency | ⚠️ largely fixed (elephant redrawn, rescaled into the safe zone, monochrome layer added) — ring/asset + About monogram open (finding 1) |
| 2. Duplicated progress on home | ✅ fixed — one progress card; section percentages live only in section cards |
| 3. Stats hint affordance | ⚠️ “?” removed as requested, but tiles are still tappable without a cue (finding 3) |
| 4. Dialog radius | ✅ fixed — `AlertDialog(shape = RoundedCornerShape(14.dp))` in Stats/Settings/Practice |
| 5. Rule-screen accent overuse / examples labelling | ✅ fixed — neutral headings with icon chips, grey bullets, explicit “Примеры” divider, tinted formula block |
| 6. Chevron / nav icon consistency | ⚠️ partly — no letter tiles and consistent line icons, but “›” glyph and 20 dp stats icon remain (finding 6) |
| 7. Disabled-card contrast | ✅ fixed — disabled mistake card is a plain `AppCard` without `onClick`/alpha; caption readable |
| 8. Microcopy + zero-state | ✅ mostly — duplicate headings and marketing phrases removed; Stats zero state is factual (its only gap is the missing next action, now covered by the global CTA/nav) |

## Summary — priorities

1. Radar scale/legend + sparse-data rendering + accessibility semantics (finding 2) — the new headline element.
2. Launcher/adaptive icon finalisation (ring or cream, matching 170) and the About brand mark (finding 1) — visible outside the app.
3. Stat-tile affordance/labels (3), section CTA count (4), long rule titles + reachable train action (5), chevron/nav icon consistency (6) — one polish pass.
4. Add a dark-theme screenshot set; the code-level dark scheme and bar-icon handling are already in place.

Everything else (palette, radius scale, type scale, spacing, line-icon set, rule-page composition, no-“?” tiles, no emoji/streak/taglines) meets the DSH-style target.
