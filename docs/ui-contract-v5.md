# UI contract v5 — 30-question themes, mistake closure, favourites removed

Project: **IlTa 2.3.0** (package `com.iltatrainer.app`). You own these four screens:
`TopicsScreen.kt`, `StatsScreen.kt`, `SectionScreen.kt`, `RuleDetailScreen.kt`.

## Foundation changes (already in the repo)

- Themes now have **30 questions** (25 choice + 5 match); topic training plays all 30.
- **Favourites are removed completely** from the app (no heart, no card, no mode, no stats row).
  The data field is kept only for old-config compatibility.
- New mistake model in `data/ProgressStore.kt`:

```kotlin
@Serializable
data class MistakeEntry(
    val ruleId: String = "",
    // the original question has been answered correctly at least once after the last error
    val originalCorrect: Boolean = false,
    // how many similar questions of the same theme were answered correctly (0..3)
    val similarCorrect: Int = 0,
    val openedAt: Long = 0L,
)
// ProgressState gains:
val mistakes: Map<String, MistakeEntry> = emptyMap()   // open mistakes, key = question id
val closedMistakes: Int = 0
```

- New helpers in `domain/SessionBuilder.kt`:

```kotlin
fun openMistakes(state: ProgressState): List<Pair<String, MistakeEntry>>  // oldest first
fun openMistakeCount(state: ProgressState): Int
```

- `StatsCalculator.ruleProgress` is now additive: each correct answer adds `100/30 ≈ 3.33%`,
  each mistake costs two steps; repeated correct answers and replaying a theme raise progress.
  `sectionProgress` / `learningProgress` signatures are unchanged.

## Changes per screen

### TopicsScreen
- Remove the “Избранное / Favorites” card and the `onStartFavorites` parameter entirely.
- Keep a single full-width **“Работа над ошибками” / “Mistake review”** card (emoji 🎯):
  show the number of open mistakes (`progress.mistakes.size` / `SessionBuilder.openMistakeCount`),
  e.g. “3 ошибки в работе” / “3 mistakes to review”, or “Пока нет ошибок” / “No open mistakes”
  when 0; disabled (alpha 0.55, no click) when there are none. Click → `onStartMistakes`.
- Everything else (header, streak, learning card, quick-practice picker, sections list, footer)
  stays as is. Section cards already show `sectionProgress`, keep it.

### StatsScreen
- Remove `onStartFavorites` parameter and the “Избранное” button; keep a single
  “Работа над ошибками” button (enabled when `progress.mistakes.isNotEmpty()`).
- Replace the “Сложные вопросы” block with an **open-mistakes block**:
  - header “Ошибки в работе” / “Mistakes to review”; if none — a short positive
    “Ошибок нет — отлично!” / “No open mistakes — great!”;
  - up to 5 open mistakes from `SessionBuilder.openMistakes(progress)` (oldest first),
    for each: sentence preview (blank replaced with “…”), theme title, and closure status line:
    RU «Исходный: ✓/— · Похожих: X/3», EN “Original: ✓/— · Similar: X/3”
    (✓ if `originalCorrect`, otherwise “—”; X = `similarCorrect`);
  - a footnote “Ошибка закрывается после верного ответа на исходный вопрос + 3 похожих.” /
    “A mistake closes after the original + 3 similar questions are answered correctly.”
  - If a mistake question id is no longer in the content, skip it in the list.
- Keep the 2×2 tiles, learning card, levels and action buttons otherwise unchanged.

### SectionScreen and RuleDetailScreen — button overflow fix (photo #1)
- “Тренировать раздел · N вопросов” / “Тренировать тему · N вопросов” buttons currently
  wrap and get clipped vertically. Replace the single long label with a two-line button:
  `Column` inside the `Button`:
  - line 1: “Тренировать раздел” / “Тренировать тему” (`titleMedium`, `maxLines = 1`),
  - line 2: “N вопросов” / “N questions” (`labelSmall`, `maxLines = 1`).
  Button height **64.dp**, full width, 8.dp vertical padding inside. Same fix for both screens.
- Update counts: a theme now has **30 вопросов** (content-driven, no hardcoding).
- Everything else in SectionScreen/RuleDetailScreen stays as is (they already use the new
  progress metrics and typography).

## Constraints

- Only the four files above; do not touch App.kt (the Lead wires the new signatures),
  Settings, Practice/Result, domain, data or content.
- Material 3, bilingual RU/EN via `tr()`, `localized()`, `pluralRu`; light + dark; no new deps;
  no gradle runs (the Lead compiles and verifies).
- Report the final `TopicsScreen` / `StatsScreen` signatures and any deviations, then complete
  the shared task.
