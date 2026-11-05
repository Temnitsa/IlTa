# Design recheck #2 — IlTa 3.2 rule page (bullet style)

**Score: 9/10 — target met.** Evaluated the rule page/style after the user criticism, using `art/screenshots/222_rule_movement_new.png` (movement rule) plus the current `RuleDetailScreen.kt` renderer and the rule content. The three criticised points are fixed: the summary block under the progress card is gone, the “English Grammar in Use” source string is gone from the app content, and the rule body now renders as a compact bullet list (each body line becomes one bullet; oversized lines are sentence-split). The page reads cleanly, the hierarchy holds without icons or summary prose, and the bullet rhythm is a real improvement over the paragraph version. No blockers. Three optional polish notes below; none of them affects the 9/10.

Inspected read-only: `222_rule_movement_new.png`, `RuleDetailScreen.kt` (current renderer, no summary rendering), `prepositions_03_movement.json` bodies/examples/translations, and a content-wide grep for “Grammar in Use”. Only this report was written; no gradle.

## Verified fixes

| User criticism | Status | Evidence |
|---|---|---|
| Summary block under the progress card | **Fixed** | 222 starts with the progress card (A2 chip + «Прогресс темы» + % + bar) and then goes straight to the first rule card; `RuleDetailScreen.kt` no longer renders `rule.summary` at all. |
| “English Grammar in Use” source | **Fixed** | No occurrence of the string in `app/src/main` (content, strings, screens); nothing on the page/credit line. |
| Long body paragraphs | **Fixed** | Body is now a bullet list: every non-empty body line renders as its own bullet with a hanging indent (8 dp marker gap, 8 dp inter-bullet spacing, 16 sp/22 body), and lines longer than ~110 chars are sentence-split. 222 shows 4–5 short bullets per card, which is exactly the intended scannable rhythm. |

Other things that hold up on 222: app-bar title, progress card, 14 dp cards with 1 dp hairlines, 16 sp bullets / 18 sp semibold headings / 11 sp «Примеры» label, hairline before the examples, primary-colour “→” arrows, consistent 16 dp page margins and 10 dp stack gaps. The page has no icons/summary/source noise and the examples block (including the inline long-press translation) is untouched.

## Three concrete polish notes (optional, not blocking)

1. **Bullet “voice” is mixed: some bullets are English-led, some Russian-led** — on 222: bullet 1 «To after go, come, travel, return: …» (EN), bullet 2 «Сравните: …» (RU), bullet 3 «Get to a place; …» (EN), bullet 4 «Home без предлога: …» (EN+RU), bullet 5 «(I've) been to = бывал где-то.» (EN=RU). The list reads like assembled notes rather than one authored pattern. Fix: standardise one bullet template for the whole rule page, e.g. every bullet = short Russian cue + bold English pattern («направление: **go to** Italy, **come to** the party»), or all bullets = English pattern + consistent Russian gloss in parentheses; the same template on every card.
2. **Bullets + «Примеры» duplicate the same patterns** — in 222 every one of the five example rows repeats a pattern already quoted in the bullets above (`go to Italy`, `arrive in/at`, `get home`, `been to`), so each card says the same thing twice and grows tall. Fix: keep bullets at pattern level (preposition + short collocation, no full mini-sentences) and let «Примеры» carry the full sentences, or drop the inline example fragments from bullets where the same sentence appears in the examples block.
3. **Section headings are punctuation-heavy** — «To, at, in, into: движение и положение», «Into / in, out of; on / off: вход и транспорт», «By + транспорт; in/on + транспорт; on foot»: slashes, plus signs and semicolons inside an 18 sp semibold heading look noisy next to the calm card style. Fix: simplify headings to one Russian topic phrase («Движение и положение», «Вход и транспорт», «Способы передвижения») and keep the preposition lists in the bullets.

## Context notes (outside this page)

- The renderer now bulletises every body line for all rules, but only the movement/time/place bodies have been reflowed into short lines; the remaining rule bodies (articles 1–9, prep-adjectives, prep-verbs, prep-fixed) are still one paragraph per section and will render as longer 1–2-sentence bullets. If the short-bullet style is now the standard, those bodies need the same line split (content task) for a consistent rule look.
- 222’s status bar is half-rendered in the capture (the clock glyphs are clipped) — a screenshot artifact, not an app issue; re-capture before using it in the gallery.

**Bottom line:** 9/10 — the user-criticised page now meets the bar; the three notes above are optional polish that would take the rule style from 9 to 9.5 without touching the design system.
