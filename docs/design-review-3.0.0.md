# Design review — IlTa 3.0.0 (DSH-style UI)

**Verdict: borderline** — the direction is right and already close to DeepSeek Harness: neutral light canvas, flat white cards with 1dp hairline borders, no shadows/elevation, one blue accent, compact type and generous but disciplined spacing, Russian-only, no fire/streak, no tagline, no language switch, no filler phrases. It is not yet "good" because of three must-fix items (launcher icon quality/brand consistency, duplicated progress on the home screen, and invisible stats hints) plus five polish items (dialog radius, rule-screen accent overuse/examples labelling, chevron/icon consistency, disabled-card contrast, microcopy + zero-state action).

Inspected: `art/screenshots/143_home_dsh_loaded.png`, `141_stats_dsh.png`, `142_stats_hint.png`, `147_settings_dsh.png`, `150_rule_dsh_final.png`, `151_launcher_elephant.png` (all read via read_image), plus `Theme.kt`, `Common.kt`, `App.kt`, `TopicsScreen.kt`, `StatsScreen.kt`, `SettingsScreen.kt`, `RuleDetailScreen.kt` and the launcher assets (`ic_launcher*.png`, adaptive-icon XML, `values/colors.xml`). No code or content was modified; no gradle.

## DSH resemblance checklist

| Criterion | Result | Evidence |
|---|---|---|
| Light, flat, clean | ✅ good | `background #F7F8FA`, `surface #FFF`, `outlineVariant #E4E7EC`; `AppCard` = 14dp radius, 1dp border, no shadow; `NavigationBar` `tonalElevation = 0.dp`; centered top bar with background container, no elevation |
| Compactness | ✅ good | 16dp screen margins, 10dp stack gap, 14dp card padding, body 14–15sp / 22sp line height, labels 11–13sp, buttons 46dp |
| Single accent | ✅ mostly | one blue `#4D6BFE` / primaryContainer `#E8EDFF`; used for selected nav, progress, section headings, bullets and formula tint — slightly too much on the rule screen (see finding 5) |
| Hierarchy | ⚠️ one duplication | every screen has one 20sp page title and labelled groups; home summary card repeats the per-section percentages that the section cards show again (finding 2) |
| Contrast | ✅ body / ⚠️ disabled | `#111318` on `#FFFFFF` and `#5F6673` on white ≈ 5.5:1; secondary text on `#F7F8FA` still ≥ 4.5:1. Disabled mistake card uses `alpha(0.55)` on the whole card → effective ≈ 2.4:1 (finding 7) |
| No fire / streak | ✅ | no streak/fire UI or strings in screens; `grep` over `ui/` is clean |
| No tagline | ✅ | home is just the "IlTa" title + content; no slogan anywhere |
| No language switcher (Russian-only) | ✅ | `CompositionLocalProvider(LocalLanguage provides AppLanguage.RU)`; `Localization.kt` pins RU; settings has no language row |
| No empty marketing phrases | ✅ | empty states are factual and useful ("Пока нет ошибок", "Пока нет данных — Ответьте на несколько вопросов — здесь появится прогресс."); about text is factual |
| Stats hints | ⚠️ exist but hidden | tapping the three stat tiles opens useful dialogs (142); nothing on the tiles suggests they are tappable (finding 3) |
| Icon quality | ⚠️ | flat teal elephant on cream `#FBF8F2`; no eye, pale-mint highlight and thin trunk read as a blob at launcher size; teal/cream does not match the blue/neutral UI (finding 1) |

## Screen notes

- **Home (143)** — clean, fast, DSH-like; header "IlTa", summary card, primary "Тренировка" button with session size on the right, muted "Работа над ошибками" card, "Разделы" group with three flat section cards (monogram + title + subtitle + % + chevron + progress + meta). The only hierarchy problem is that the summary card's three section rows duplicate the section cards below.
- **Stats (141)** — three compact metric tiles and an informational empty state; with zero data the screen is ~60% empty and offers no next action; the tiles give no hint that they are tappable.
- **Stats hint (142)** — dialog content is good and honest ("Всего ответов за всё время. Повторные попытки тоже учитываются."), single "Понятно" action, dimmed background; the dialog is over-rounded compared with the rest of the system (28dp default vs 14dp cards).
- **Settings (147)** — strongest screen: labelled groups (Оформление / Тренировка / Данные / Обновления / О приложении), segmented chips, two outlined data actions + one destructive outlined action, factual status, compact about card. Only nits: the "I" monogram instead of the elephant brand icon, and the same over-rounded dialogs for destructive confirmations.
- **Rule detail (150)** — clear structure: back bar, level chip + progress card, then section cards with a 3dp primary bar + blue heading, formula box, bullets; formula block is monospaced and boxed. Too many blue elements per card (heading, bar, bullet dots, formula tint) and the English example bullets follow the Russian bullets with only a dot-colour difference and no "Примеры" cue.
- **Launcher (151)** — in the dock the elephant reads as a pale/white shape on a light circle; the asset itself is a layered teal elephant with no eye. It is a decent flat illustration but not a crisp, brand-consistent DSH-style mark.

## Findings (8, prioritised)

| # | Area | Severity | Problem | Exact UI fix |
|---|---|---|---|---|
| 1 | Launcher icon / brand | major | The elephant icon is soft and muddy at launcher size: the large pale-mint highlight on the back/ear, the gradient shading, the missing eye and the thin trunk all disappear at 48dp, so the dock shows a pale blob; the warm cream background `#FBF8F2` and teal body do not match the app's neutral `#F7F8FA` + blue `#4D6BFE` identity; the adaptive icon has no monochrome layer, so themed launchers render a different-looking white-on-mint mark; the About card shows a letter "I" monogram instead of the brand icon. | Redraw the launcher as a flat 2-colour mark: white elephant (with a small dark eye dot and a thicker trunk) on `#4D6BFE`, or dark `#111318` elephant on `#F7F8FA`; remove gradients/highlights, keep the silhouette readable at 24–48dp; export `ic_launcher*` (legacy/round) and adaptive layers with background `#4D6BFE`; add `<monochrome android:drawable="@drawable/ic_launcher_monochrome"/>` to `mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml`; reuse the same elephant (or at least the same mark) in the About card instead of `MonogramBadge("IlTa")`. |
| 2 | Home progress card | major | Double progress reporting: `LearningProgressCard` lists all sections with their percentages, and every `SectionCard` below repeats the same percentage — the primary screen shows the same 3 numbers twice and the top card becomes heavy for a DSH-style compact home. | Remove the per-section rows from `LearningProgressCard` (keep the "Изучено грамматики" label, the overall %, the progress bar and "0 из 2020 вопросов"); let the section cards own per-section progress. Optionally move the "0 из 2020 вопросов" line next to the percent to save a row. |
| 3 | Stats hints discoverability | major | The stats hint feature is invisible: `StatTile` has `onClick`, but the tiles look like static cards — no "?", "i" or "Подробнее" affordance — so users will never open the well-written hints (142); screen readers also get no hint that the tiles are actionable. | Add a small info affordance to every `StatTile`: a 14dp `Icons.Outlined.Info` (or 16sp "?") at the tile's top-right in `onSurfaceVariant`, and set `contentDescription = "Пояснение: <label>"` on the tappable surface; keep the existing `AlertDialog`. |
| 4 | Dialog radius | minor | Radius scale is 8dp (chips) / 10dp (buttons) / 14dp (cards) / **28dp** (AlertDialog default) — the stats hint and the reset-confirmation dialogs look like a different design system on a flat DSH-style screen. | Pass `shape = RoundedCornerShape(14.dp)` to both `AlertDialog`s (StatsScreen hint, SettingsScreen reset) or introduce one shared `AppDialog` composable with the 14dp shape; keep the dim/scrim and the single "Понятно"/"Отмена" actions. |
| 5 | Rule screen accents / examples | minor | Accent overload: per section card the 3dp bar, heading, all body bullet dots and the formula-box tint are blue; DSH keeps the accent for actions/selection and progress, so explanations lose hierarchy. English examples are appended directly after Russian bullets with only a dot-colour difference — visually they read as one mixed list. | Keep the 3dp primary bar + primary heading (or make the heading `onSurface` semibold), set body bullet dots to `onSurfaceVariant`, formula box to `surfaceVariant` with `onSurface` monospace text; before `section.examples` add a `labelSmall` caption "Примеры" (or a 1dp `outlineVariant` divider) and keep example bullets neutral. |
| 6 | Chevron / nav icon consistency | minor | Section and mistake rows use a text glyph `Text("›")` instead of an icon — baseline/thickness differ from the real icons and it cannot be tinted/rotated consistently; the custom `StatsBarIcon` is drawn at 20dp while `List`/`Settings` render at 24dp, so the three bottom-nav icons have different optical weight. | Replace row `Text("›")` with `Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(18.dp), tint = onSurfaceVariant)`; draw `StatsBarIcon` at 24dp (`Modifier.size(24.dp)`) inside the nav item and match its bar weight to the other icons (2dp bars or use a material `BarChart`-style path). |
| 7 | Disabled card contrast | minor | `MistakeReviewCard` applies `.alpha(0.55f)` to the whole card when there are no mistakes, so title, caption, border and background all fade to ≈ 2.4:1 — below WCAG 3:1 for large/disabled text and it reads as "broken" rather than "empty". | Do not alpha the whole card: keep the border and `surface` at full strength; render the title in `onSurface` and the caption "Пока нет ошибок" in `onSurfaceVariant`; if a disabled look is required, use `alpha(0.75f)` on the text only (never on the border) and keep the card non-clickable. |
| 8 | Microcopy + zero-state | minor | Microcopy is inconsistent/awkward: home and the stats learning card say "Изучено грамматики" (odd case; and "Изучено" vs the "Освоено" tile are two different metrics with similar wording); the stats zero-state is an actionless card in a mostly empty screen. | Rename the card to «Прогресс грамматики» (or «Освоено: грамматика») and keep "Освоено" only for the mastered-questions tile; add a primary text/outlined action inside the stats empty card, e.g. «Тренировка» → opens the same quick-practice flow as home, so the zero state is actionable. |

## What is already good (keep as is)

- Flat card system: 14dp radius, 1dp `outlineVariant` border, no shadow, 14dp inner padding — very close to DSH.
- Typography scale: 20sp page title / 18sp value / 15sp row title / 14–15sp body / 12–13sp captions — compact and consistent; line heights are sensible.
- Bottom navigation: standard M3 bar, primary-container pill indicator, primary selected icon+label, `tonalElevation = 0`, hairline divider — clean; update dot on Settings is subtle.
- Settings structure and segmented chips (8dp radius, `primaryContainer` for selected) are clear and DSH-like.
- No emoji, no mascots, no gradients in the UI, no "welcome" filler, no fire/streak, no tagline, no language switch; Russian-only is enforced in code, not just hidden in UI.
- Stats hints content itself is good and honest; the only problem is discoverability (finding 3).

## Suggested fix order

1. Launcher/adaptive icon (finding 1) — brand-level, visible outside the app.
2. Home duplication (2) — biggest in-app compactness win.
3. Stats hint affordance (3) — makes an existing feature usable.
4. Dialog radius (4), accent/examples in rules (5), chevrons/nav icon (6), disabled contrast (7), microcopy/zero-state (8) — one polish pass.
