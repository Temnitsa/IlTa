# UI contract v8 — clean quick picker, build prompts, compact rules

## 1. Remove «Сборка предложений» from the main screen
- Remove the 🧩 card and the `onStartBuildPractice` parameter from `TopicsScreen`.
- Remove the corresponding callback from `App.kt` (Lead does that).
- The mode remains available through the quick-practice type filter (see §2).

## 2. Quick-practice picker: collapsible sections + exercise-type filter
`onStartQuickPractice` signature becomes `(selectedRuleIds: List<String>, selectedTypes: Set<String>) -> Unit`.
Inside `QuickPracticePickerSheet`:
- On open **only section rows are visible**; each section starts collapsed.
- Tapping a section row expands/collapses its themes (chevron ▸/▾). The section checkbox still selects/deselects all themes of that section; when collapsed show a compact counter like `3/12` if partially selected.
- Add a row of 3 type checkboxes/chips before the section list:
  - «Выбор» / “Choice” — `choice`
  - «Соответствие» / “Match” — `match`
  - «Сборка» / “Build” — `build`
  All selected by default; start disabled when no type or no theme is selected.
- Session summary should mention types if not all selected.
- No other screen changes.

## 3. Build exercise layout
In `PracticeScreen` for `question.isBuild`:
1. instruction «Переведите предложение на английский» / “Translate the sentence into English”;
2. the **Russian sentence** = `question.translation?.ru` shown prominently — no (+)/(–)/(?) markers and no other labels;
3. `buildKind` is internal content metadata only, never displayed;
4. empty answer area with selected tiles (tap removes);
5. word bank of English tiles below;
6. «Проверить» enabled when all tiles are placed; after checking show the correct English sentence and explanation (do not repeat the Russian translation in the feedback block).

## 4. Compact rule formatting
`RuleDetailScreen` section bodies must render:
- line breaks exactly as in JSON;
- simple emphasis: `**bold**` and `*italic*` (strip markup, style spans);
- bullets (`• `) as separate lines.
No other text formatting is required. Content authors will write formulas/line bullets using this markup.

## Constraints
Only TopicsScreen.kt, PracticeScreen.kt, RuleDetailScreen.kt. App wiring is Lead's. No gradle. Report new signatures.
