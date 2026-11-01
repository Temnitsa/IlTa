# UI contract v2 — Topics entry screen and Section screen (`dev-screens`)

Project: **IlTa** (renamed from ArticleWise) — offline English grammar trainer.
Package root: `com.iltatrainer.app`. Source root: `app/src/main/java/com/iltatrainer/app/`.
Kotlin + Jetpack Compose, Material 3, offline, RU/EN toggle in settings.

The app now has **3 tabs**: **Темы** (Topics), **Статистика** (Stats), **Настройки** (Settings).
Entry screen = list of grammar sections (Articles, Prepositions). Tapping a section opens its
list of themes; tapping a theme opens theory + a practice button (RuleDetailScreen already exists).

You own exactly these two **new** files (the Lead deletes HomeScreen.kt/RulesScreen.kt and wires
navigation):

1. `ui/screens/TopicsScreen.kt`
2. `ui/screens/SectionScreen.kt`

## Mandatory composable signatures (the Lead calls exactly these)

```kotlin
@Composable
fun TopicsScreen(
    sections: List<Section>,
    rules: List<Rule>,
    questions: List<Question>,
    progress: ProgressState,
    contentErrors: List<String>,
    onQuickStart: () -> Unit,
    onStartMistakes: () -> Unit,
    onStartFavorites: () -> Unit,
    onOpenSection: (sectionId: String) -> Unit,
    modifier: Modifier = Modifier,
)

@Composable
fun SectionScreen(
    section: Section?,
    rules: List<Rule>,
    questionsByRule: Map<String, Int>,
    progress: ProgressState,
    onBack: () -> Unit,
    onOpenRule: (ruleId: String) -> Unit,
    onPracticeSection: () -> Unit,
)
```

Apply `modifier` to the root of `TopicsScreen` (the app Scaffold passes inner padding).
Screens must NOT create their own Scaffold/bottom bar/navigation. Use bottom padding 24.dp.
`SectionScreen` is a full-screen detail with `BackTopBar`, so it has no `modifier` parameter.

## Foundation to read first

- `data/Models.kt` — `Section` (id, order, emoji, title/short/description: Bilingual),
  `Rule` (now has `sectionId`), `Question` (only `choice`/`match`; no `accepted`/`isFill`),
  `ProgressState`.
- `ui/Localization.kt` — `tr(ru, en)`, `Bilingual.localized()`, `formatPercent`,
  `questionsLabel`, `itemLabel(value, sectionId)` for “—” options, `pluralRu`.
- `ui/components/Common.kt` — `AppCard`, `SectionHeader`, `LevelChip`, `StatTile`,
  `AccuracyBar`, `ProgressRing`, `EmptyState`, `BackTopBar`, `levelColor`, `StatsBarIcon`.
- `domain/StatsCalculator.kt` — `accuracy`, `overallAccuracy`, `ruleAccuracy`, `streak`,
  `bestStreak`, `answeredOn`, `todayProgress`, `dailyGoalMet`, `mistakeQuestions`,
  `favoritesQuestions`, `levelStats`, `masteredCount`.
- `domain/SessionBuilder.kt` — `hasMistakes`, `mistakeQuestions`.
- `data/ProgressStore.kt` — `store.state`, settings.
- `data/AppContainer.kt`, `ui/theme/Theme.kt`.

## TopicsScreen requirements (entry screen)

Header:
- Title "IlTa" (`headlineMedium`), subtitle "Тренажёр английской грамматики" / "English grammar trainer" (`bodyMedium`, onSurfaceVariant).
- Right: streak badge (surface secondaryContainer, rounded, "🔥 N") like old Home.

Below header:
- Compact daily-goal card: `ProgressRing` (progress = today / goal, label "X/Y", caption "сегодня"/"today") + column: "Цель на сегодня"/"Today's goal", remaining questions or "Цель выполнена!"/"Daily goal reached!", streak line, best streak.
- Primary full-width button "Быстрая тренировка"/"Quick practice" with subtitle `${questionsLabel(settings.sessionSize)} · все темы` / `${questionsLabel(...)} · all topics`.
- Row of two mode cards (`AppCard`, weight 1f): "Ошибки"/"Mistakes" (emoji 🎯) with count or "Пока пусто"/"Nothing yet"; "Избранное"/"Favorites" (emoji ❤) with count or "Отметьте сердечком"/"Tap the heart". Disabled (alpha 0.55) when count == 0.
- If `contentErrors` is non-empty: a small error-container surface "Часть контента не загрузилась"/"Some content failed to load" with the first error.
- `SectionHeader("Разделы"/"Sections")`.
- For each section (they arrive already sorted): `AppCard(onClick = { onOpenSection(section.id) })`:
  - Row: big emoji (28.sp), column with localized `title` (`titleMedium`) + localized `short` (`bodySmall`, onSurfaceVariant); trailing "›".
  - localized `description` (`bodySmall`, maxLines 2).
  - Row: accuracy bar for the section + percent, and "N тем · M вопросов"/"N topics · M questions".
  - Section stats: sum `ProgressState.ruleStats` attempts/correct over the section's rules
    (`questions`/`rules` params let you compute which rules belong to a section).
- If `sections.isEmpty()` → `EmptyState`.

Footer note: "Всё работает офлайн. Прогресс хранится только на этом устройстве." /
"Fully offline. Progress is stored on this device only."

## SectionScreen requirements

- `BackTopBar(title = section.title.localized(), onBack = onBack)`.
- If `section == null` → `EmptyState("🤔", "Раздел не найден"/"Section not found", ...)` and stop.
- Summary `AppCard`: emoji + localized description; row "N тем · M вопросов"; if the section has
  attempts → accuracy + `AccuracyBar` with percent.
- Full-width `Button("Тренировать раздел · N вопросов"/"Practise this section · N questions")`,
  enabled when there are questions, height 52.dp → `onPracticeSection`.
- `SectionHeader("Темы"/"Topics")` + list of `AppCard(onClick = { onOpenRule(rule.id) })`:
  localized title + `LevelChip(rule.level)` on the right, accuracy percent + `AccuracyBar`
  (only if attempts > 0), "N вопросов"/"N questions" (`questionsByRule[rule.id] ?: 0`), trailing "›".
- Empty rules list → `EmptyState`.
- Scrollable (`LazyColumn` or `Column` + `verticalScroll`), horizontal padding 20.dp.

## Design rules

- Material 3 only, `MaterialTheme.colorScheme`, brand colors come from theme.
- Everything bilingual via `tr(ru, en)` / `localized()`; no Android string resources.
- Dark + light themes, RU + EN, offline.
- No new dependencies, no `material-icons-extended`; emoji/text symbols are fine.
- No gradle runs, no changes outside your two files, do not touch App.kt/navigation or other screens.
- The Lead compiles and integrates; report deviations precisely.
