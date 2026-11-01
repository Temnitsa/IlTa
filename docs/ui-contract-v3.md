# UI contract v3 — quick-practice topic picker + learning progress card

Project: **IlTa 2.1.0** (package `com.iltatrainer.app`). You own the same UI area as before:
`TopicsScreen`, `StatsScreen`, `SettingsScreen` (and keep `SectionScreen` as is).

The Lead already changed the foundation:

- `SessionConfig` now has `ruleIds: List<String> = emptyList()` — a mixed quick practice can be
  limited to any subset of topics.
- `StatsCalculator.learnedCount(state, questions)` and
  `StatsCalculator.learningProgress(state, questions)` — a question counts as learned once the
  learner has answered it correctly at least once; `learningProgress` is the share of the whole
  question bank (0..1).
- `TopicsScreen` entry point changed: instead of `onQuickStart: () -> Unit` it must now be

```kotlin
onStartQuickPractice: (List<String>) -> Unit
```

and the screen itself owns the topic-picker bottom sheet.

## 1. TopicsScreen — quick practice topic picker (bottom sheet)

- Keep everything else from the current screen (header with streak badge, sections list, mistakes /
  favorites cards, offline footer), but:
  - **Remove the daily-goal card** (“Цель на сегодня”, “Осталось N вопросов”, “Рекорд”) entirely.
  - Replace it with a **learning-progress card** (see §2).
  - The primary button “Быстрая тренировка” / “Quick practice” keeps its subtitle but now hints at
    the picker: `${questionsLabel(settings.sessionSize)} · выбрать темы` /
    `${questionsLabel(...)} · choose topics`.
- Tapping “Быстрая тренировка” opens a **Material 3 `ModalBottomSheet`**
  (`@OptIn(ExperimentalMaterial3Api::class)`, `rememberModalBottomSheetState(skipPartiallyExpanded = true)`):
  - Title: “Темы для тренировки” / “Topics for practice”.
  - Subtitle: “Выберите одну или несколько тем — можно собрать микс” /
    “Pick one or more topics — mix them freely”.
  - Scrollable list grouped by grammar section: a small section header with emoji + localized
    section title, then each rule of that section as a row:
    `Checkbox(checked = rule.id in selected, onCheckedChange = { toggle(rule.id) })`
    + localized rule title. Make the whole row clickable (toggle on row tap) and keep it compact.
  - Toolbar row above the list: `TextButton("Выбрать все"/"Select all")` and
    `TextButton("Снять все"/"Clear")`.
  - Default selection when the sheet opens: **all rules selected** (so the default behaviour is
    “all topics” as before). Keep the selection in `rememberSaveable` state while the app lives.
  - Footer: “Выбрано: N тем · M вопросов” / “Selected: N topics · M questions”
    (M = sum of question counts of selected rules; `TopicsScreen` receives `questions`, so compute
    per-rule counts with `questions.groupingBy { it.ruleId }.eachCount()`), plus a note with the
    session size: “В одной тренировке: ${questionsLabel(sessionSize)}” / “Session size: ...”.
  - Primary button “Начать тренировку” / “Start practice”: enabled only if the selection is not
    empty; on click dismiss the sheet and call `onStartQuickPractice(selectedRuleIds.toList())`.
  - Dismiss by swipe/back/scrim as usual.
- Keep the existing “Ошибки” and “Избранное” cards unchanged.

## 2. Learning-progress card (TopicsScreen and StatsScreen)

Purpose: replace daily-goal motivation with “how much grammar you already know”.

- Metric: `learned = StatsCalculator.learnedCount(progress, questions)`,
  `total = questions.size`, `percent = learned * 100 / total` (round).
- TopicsScreen card (`AppCard`):
  - Title: “Вы изучили X% грамматики” / “You've learned X% of the grammar”
    (`titleMedium`; X bold).
  - Line: “N из M вопросов” / “N of M questions”.
  - Full-width progress bar (`AccuracyBar` or `LinearProgressIndicator`) with `X / 100f`.
  - Per-section mini rows (both sections from `sections` param): emoji + localized section title
    + “Y%” on the right + thin `AccuracyBar` under each; Y = learned questions of that section’s
    rules / total questions of that section (compute via `rules` + `questions`).
  - Small footer line “Чем больше отвечаете — тем выше процент” /
    “The more you practise, the higher it goes”.
- StatsScreen:
  - Replace the daily-goal `ProgressRing` card with a learning-progress card:
    `ProgressRing(progress = percent/100f, label = "X%", caption = "грамматики"/"grammar")`
    + column: “Изучено грамматики” / “Grammar learned”, “N из M вопросов”,
    “N тем начато” / “N topics started” (topics with attempts > 0), then the existing
    streak lines (“🔥 N дней подряд”, “Лучшая серия: N” / “Best streak”).
  - Remove “Цель выполнена / Осталось ответов / today / daily goal” texts.
  - Keep the 2×2 stat tiles, levels, tricky questions and action buttons unchanged.

## 3. SettingsScreen

- Remove the entire **“Цель на день” / “Daily goal”** section (title, caption, 10/20/30/50 chips).
  Keep language, theme, dynamic colors, session length, data export/import/reset and About.
- Update the About version line to “Версия 2.1.0” / “Version 2.1.0”.

## Constraints

- Only the three files: `ui/screens/TopicsScreen.kt`, `ui/screens/StatsScreen.kt`,
  `ui/screens/SettingsScreen.kt`. Do not touch `SectionScreen.kt`, App.kt, navigation, models,
  domain or content.
- Material 3 only, no new dependencies, no `material-icons-extended`; bilingual RU/EN via `tr()`,
  `localized()`, `pluralRu`; light + dark theme; offline.
- No gradle runs — the Lead compiles, integrates and takes screenshots.
- Report deviations precisely and mark the shared task complete.
