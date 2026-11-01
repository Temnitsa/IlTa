# UI contract v7 — tenses section and sentence building

## New question type

`Question` gains:
```kotlin
val words: List<String> = emptyList()
val answers: List<List<String>> = emptyList()   // accepted token orders
val isBuild: Boolean get() = type == "build"
```

`AnswerChecker.isBuildCorrect(question, selectedTokens: List<String>)` and
`AnswerChecker.correctText(question)` are available.

## Required changes

### TopicsScreen
- New parameter `onStartBuildPractice: () -> Unit`.
- New full-width card/button **«Сборка предложений» / “Sentence building”** (🧩), placed after
  «Тренировка» and before/after «Работа над ошибками».
- Show number of build questions (`questions.count { it.isBuild }`): “N заданий” / “N tasks”;
  disabled if 0.
- Everything else unchanged.

### PracticeScreen
- Handle `question.isBuild` in addition to choice/match.
- Instruction: «Соберите предложение из слов» / “Build the sentence from the words”.
- `BuildBody`:
  - word bank: shuffled tiles (use `FlowRow`, no new dependencies; remember shuffle per question id);
  - answer area: selected tiles in tap order;
  - tap bank tile → appends to answer; tap selected tile → returns to bank;
  - all words must be used; **Проверить / Check** button enabled only when every word is placed;
  - on check, compare selected tokens with `question.answers` via `AnswerChecker.isBuildCorrect`;
  - call `onSubmit(correct, selectedTokens.joinToString(" "))` exactly once;
  - after answer, feedback must show the correct sentence (`AnswerChecker.correctText(question)`)
    and the standard explanation/translation. For a wrong answer, show the student's attempt too.
- Keep existing choice/match rendering untouched.

### ResultScreen
- `MistakeCard` for build: show the correct assembled sentence and «Ваш ответ: <tokens>» (no
  itemLabel conversion); theme/level as usual; explanation below.
- `userAnswerText` must not treat build input as an article/preposition answer.

### App wiring (Lead does it)
- `PracticeMode.BUILD`, title «Сборка предложений» / “Sentence building”, size = min(session size,
  build question count); `TopicsScreen` gets `onStartBuildPractice`.

## Constraints
Only TopicsScreen.kt, PracticeScreen.kt, ResultScreen.kt. No gradle. Material3, RU/EN, dark/light.
