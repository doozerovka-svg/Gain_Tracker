# Android Workout Tracker — Master Orchestration Plan

## Objective
Deliver a production-ready, 100% offline, local-first Android Workout Tracker app in Russian, meeting all requirements (R1: active workout logging, +X buttons, RIR slider, rest timer; R2: calendar, session cloning, auto-population; R3: deterministic progression algorithm; R4: analytics graphs & Excel/PDF export), thoroughly tested with unit & E2E tests, verified builds, and audited for complete integrity.

## Architecture
- **Tech Stack**: Android, Kotlin, Jetpack Compose, Room (SQLite), Coroutines/Flow, Material 3, MPAndroidChart / custom Compose canvas charts, Apache POI / light spreadsheet generator & Android PdfDocument.
- **Language & UI**: 100% Russian language for all UI strings, date formats, labels, and export headers.
- **Offline / Local-First**: Room database with pre-populated exercise library, DAO architecture, no network requests.

## Phase Breakdown

### Phase 0: Survey & Scope Exploration
- Spawn 3 parallel Explorers / Spec Miners:
  1. `teamwork_preview_spec_miner` 1: Deep analysis of R1 & R3 (Workout set logging UI UX constraints, +X buttons, RIR slider, touch targets >=48dp, rest timer, deterministic progression formulas and edge cases).
  2. `teamwork_preview_spec_miner` 2: Deep analysis of R2 & R4 (Calendar view, session cloning semantics, auto-population, 1RM Epley/Brzycki formulas, Excel .xlsx and PDF offline document generation).
  3. `teamwork_preview_explorer` 3: Workspace inspection, Android project configuration, Gradle build setup, dependencies, target SDK, and test harness structure.

### Phase 1: PROJECT.md & Test Suite Architecture (TEST_INFRA.md)
- Synthesize explorer reports into authoritative `PROJECT.md` and `TEST_INFRA.md`.
- Spawn E2E Testing Orchestrator / Test Writer to prepare comprehensive test cases (Tiers 1-4).

### Phase 2: Milestone 1 — Domain Models, Room DB, Progression Algorithm
- Entities: `Exercise`, `WorkoutSession`, `SetEntry`, `ProgressConfig`, `ExerciseCategory`.
- Room Database, DAOs, TypeConverters, Pre-populated exercises (Russian names).
- Progression Algorithm with unit tests (all 3 delta branches, inventory rounding, 0 kg / 0 reps edge cases).
- Verification via Reviewer, Challenger, and Forensic Auditor.

### Phase 3: Milestone 2 — Active Workout Screen, Logging & Rest Timer
- Jetpack Compose UI: +1, +2.5, +5, +10, +20 kg buttons, direct numeric input, clear/backspace.
- Integer reps, 0-5 discrete RIR slider.
- Flow: <=4 clicks / 3 screens per set, touch targets >=48dp.
- Rest Timer service/component (90s / 180s defaults, customizable, background notification / vibration).
- Verification via Reviewers, Challengers, and Forensic Auditor.

### Phase 4: Milestone 3 — Calendar, History, Cloning & Auto-population
- Monthly / Weekly calendar with color-coded workout indicators.
- Workout history screen with session details.
- Session cloning logic (copy exercises, order, sets into target date).
- Target weight/reps auto-population from last completed set of that exercise (graceful fallback if empty history).
- Verification via Reviewers, Challengers, and Forensic Auditor.

### Phase 5: Milestone 4 — Analytics Charts & Excel / PDF Export
- Analytics screen: Exercise picker, date-range picker, Chart (X = Date, Y1 = 1RM Epley/Brzycki, Y2 = Working Weight).
- Offline Excel (.xlsx) exporter: All sessions, exercises, sets.
- Offline PDF report generator: Summary statistics, period volume, workout log table.
- Verification via Reviewers, Challengers, and Forensic Auditor.

### Phase 6: Milestone 5 — Full Test Suite, Agent-as-Judge & Final Hardening
- Run all automated unit and integration tests.
- Execute Agent-as-Judge reviews (UI click count, touch target size >=48dp, export integrity).
- Gradle build verification (APK build output).
- Final Forensic Audit verification.
