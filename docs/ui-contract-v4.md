# UI contract v4 — penalized theme progress, full theme sessions, clean rule typography

Project: **IlTa 2.2.0** (package `com.iltatrainer.app`). You own these four screens:
`TopicsScreen.kt`, `SectionScreen.kt`, `RuleDetailScreen.kt`, `StatsScreen.kt`.
`SettingsScreen.kt` and the quick-practice bottom sheet stay as they are.

## Foundation changes (already in the repo)

`StatsCalculator` now has (old `learnedCount` / `learningProgress(state, questions)` are gone):

```kotlin
fun ruleProgress(state: ProgressState, ruleQuestions: List<Question>): Int      // 0..100
fun sectionProgress(state: ProgressState, sectionRules: List<Rule>, questions: List<Question>): Int
fun learningProgress(state: ProgressState, rules: List<Rule>, questions: List<Question>): Int
```

Rules:
- a theme has exactly **20 questions** (15 choice + 5 match) and a theme session always plays all 20;
- one correct answer on a 20-question theme gives 5%, one wrong answer costs 10%;
- `ruleProgress` is the penalized progress of that theme (clamped 0..100);
- `sectionProgress` / `learningProgress` are averages of `ruleProgress` over the section/all rules.

## Changes per screen

### TopicsScreen
- Learning card: use `StatsCalculator.learningProgress(progress, rules, questions)` for the big
  percentage instead of the old metric. Same wording “Вы изучили X% грамматики” / “You've learned
  X% of the grammar”, “N из M вопросов”, bar, per-section rows and footer.
- Section cards in the list: replace the accuracy numbers/bar with `sectionProgress(progress,
  sectionRules, questions)` — show “X%” and a bar (`X / 100f`), plus “N тем · M вопросов”.
- Quick-practice picker, mistakes/favourites cards, header/streak — keep unchanged.

### SectionScreen
- Each theme row: replace the old accuracy % with **`ruleProgress(progress, questionsOfTheme)`**
  (theme questions = `questions` filtered by `ruleId`, or use the passed `questionsByRule` for the
  count). Show “X%” and an `AccuracyBar(progress = X / 100f)`. Keep the theme question count
  (“20 вопросов”) and the chevron. No accuracy calculations on this screen anymore.
- Section summary card: replace accuracy with `sectionProgress(...)` (“X%”, bar). Keep the
  “Тренировать раздел · N вопросов” button.

### RuleDetailScreen
- Replace the accuracy card with a **theme progress card**: “Прогресс темы” / “Theme progress”,
  big “X%” from `ruleProgress(progress, questionsOfTheme)`, `AccuracyBar(X/100f)`, and a line
  “N вопросов в теме” + a small hint “Ошибки снижают прогресс” / “Mistakes lower the progress”.
- Pass the theme's questions into the screen. Current signature
  `RuleDetailScreen(rule, questionCount, progress, onBack, onTrain)` is called by the Lead; you may
  add a parameter `questions: List<Question>` (the Lead will pass the theme's questions) — state
  the final signature in your report.
- Typography (fixes the “single word on a last line” problem, photo #4): for section bodies use
  `MaterialTheme.typography.bodyLarge.copy(lineBreak = LineBreak.Heading, lineHeight = 26.sp)`,
  for headings `titleMedium` with `LineBreak.Heading`, examples in a relaxed bulleted list with
  comfortable line height. Add `@OptIn(ExperimentalTextApi::class)` only if the compiler requires it.
  Keep the existing card structure/padding.

### StatsScreen
- Learning card: `StatsCalculator.learningProgress(progress, rules, questions)` for the ring/label.
  Keep “N из M вопросов”, “Тем начато: N”, streak lines, 2×2 tiles, levels list, tricky questions,
  action buttons.
- No daily-goal wording anywhere.

## Constraints

- Only the four files listed above; no domain/App/navigation/content/Settings changes.
- Material 3, bilingual RU/EN via `tr()`, `localized()`, `pluralRu`; light + dark; no new
  dependencies; no gradle runs (the Lead compiles and verifies).
- Report the final composable signatures and any deviations, then complete the shared task.
