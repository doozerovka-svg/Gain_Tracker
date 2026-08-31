# Project: Android Workout Tracker («Трекер Тренировок»)

## Architecture
- **Paradigm**: Clean Architecture + MVVM + MVI unidirectional state flow.
- **Tech Stack**: Kotlin 2.3.20, Android SDK 36 (minSdk 24, compileSdk 36), Jetpack Compose, Material 3, Room 2.7.x / 2.6.x (SQLite), Kotlin Coroutines & Flow, Android PdfDocument, streaming XLSX writer.
- **Localization**: 100% Russian language for all UI elements, date pickers, dialogs, charts, Excel sheets, and PDF reports.
- **Persistence & Connectivity**: 100% offline, local-first architecture without external APIs or remote servers.

## Code Layout
```text
app/
├── src/
│   ├── main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/example/workouttracker/
│   │   │   ├── WorkoutApplication.kt
│   │   │   ├── MainActivity.kt
│   │   │   ├── domain/
│   │   │   │   ├── model/ (Exercise, WorkoutSession, SetEntry, ProgressConfig, Category, WorkoutWithSets)
│   │   │   │   ├── repository/ (WorkoutRepository, ExerciseRepository)
│   │   │   │   └── usecase/ (CalculateProgressionUseCase, CalculateOneRepMaxUseCase, CloneWorkoutSessionUseCase, GetAutoPopulatedValuesUseCase)
│   │   │   ├── data/
│   │   │   │   ├── local/
│   │   │   │   │   ├── AppDatabase.kt
│   │   │   │   │   ├── PrepopulateData.kt
│   │   │   │   │   ├── entity/ (ExerciseEntity, WorkoutSessionEntity, SetEntryEntity, ProgressConfigEntity, CategoryEntity)
│   │   │   │   │   └── dao/ (ExerciseDao, WorkoutSessionDao, SetEntryDao, ProgressConfigDao)
│   │   │   │   └── repository/ (WorkoutRepositoryImpl, ExerciseRepositoryImpl)
│   │   │   ├── export/
│   │   │   │   ├── ExcelExporter.kt
│   │   │   │   └── PdfReportExporter.kt
│   │   │   ├── timer/
│   │   │   │   ├── RestTimerManager.kt
│   │   │   │   └── RestTimerNotificationService.kt
│   │   │   └── presentation/
│   │   │       ├── navigation/ (Screen.kt, AppNavHost.kt)
│   │   │       ├── theme/ (Color.kt, Theme.kt, Type.kt)
│   │   │       ├── components/ (DualAxisProgressChart.kt, NumericWeightKeypad.kt, DiscreteRirSlider.kt, RestTimerOverlay.kt, CommonButtons.kt)
│   │   │       └── screens/
│   │   │           ├── active_workout/ (ActiveWorkoutScreen.kt, ActiveWorkoutViewModel.kt)
│   │   │           ├── calendar/ (CalendarScreen.kt, CalendarViewModel.kt)
│   │   │           ├── history/ (HistoryScreen.kt, HistoryViewModel.kt)
│   │   │           ├── analytics/ (AnalyticsScreen.kt, AnalyticsViewModel.kt)
│   │   │           └── export/ (ExportScreen.kt, ExportViewModel.kt)
│   │   └── res/
│   │       ├── values/strings.xml (100% Russian strings)
│   │       └── values/colors.xml
│   └── test/java/com/example/workouttracker/
│       ├── domain/ (Progression, 1RM, Cloning, AutoPopulate, Adversarial Stress tests)
│       ├── data/ (Room Database & DAO tests, Room Database Stress tests)
│       └── export/ (Excel & PDF export tests)
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── local.properties
```

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Quick Increment Buttons | Buttons (+1, +2.5, +5, +10, +20 kg) to rapidly adjust weight | M2 | R1 (line 15) |
| 2 | Direct Numeric Keypad | Keypad (0-9, ., ⌫, C) for manual weight entry | M2 | R1 (line 15) |
| 3 | Integer Reps Input | Stepper & numeric input for positive integer reps | M2 | R1 (line 15) |
| 4 | Discrete RIR Slider | 0 to 5 slider with step 1 representing Reps In Reserve | M2 | R1 (line 15) |
| 5 | Touch Target Accessibility | Interactive touch areas strictly >= 48x48 dp | M2 | R1 (line 15) |
| 6 | Fast Click Budget | Set logging completed in <= 3 screens and <= 4 clicks | M2 | R1 (line 15) |
| 7 | Auto Rest Timer | 90s (set) / 180s (exercise) auto countdown timer with notification/vibration | M2 | R1 (line 15) |
| 8 | Calendar Month / Week View | Monthly grid & weekly strip with Monday start and Russian headers | M3 | R2 (line 18) |
| 9 | Workout Visual Indicators | Color indicators (green completed, amber draft, ring today) | M3 | R2 (line 18) |
| 10 | Session Cloning Engine | Clones historical session with all exercises, order, and sets to target date | M3 | R2 (line 18, 48) |
| 11 | Auto-Population Engine | Pre-fills weight and reps from last completed set of that exercise in history | M3 | R2 (line 18, 49) |
| 12 | Empty History Fallback | Graceful handling when exercise has no prior history (empty fields, no crash) | M3 | R2 (line 50) |
| 13 | Deterministic Progression Engine | W_next = W_prev * (1 + delta) with branches (+5%, +2%, hold/deload) | M1 | R3 (line 21-26) |
| 14 | Inventory Step Rounding | Quantizes weight to nearest plate step (e.g. 1.25 or 2.5 kg) | M1 | R3 (line 26) |
| 15 | ProgressConfig & Override | Per-exercise config, placeholder display, user manual override | M1 | R3 (line 26) |
| 16 | Epley & Brzycki 1RM Calculators | Mathematical 1RM calculation with 0-rep / zero-division guards | M1 | R4 (line 29) |
| 17 | Dual-Axis Progress Chart | X=Date, Y1=1RM curve, Y2=Max Working Weight curve | M4 | R4 (line 29) |
| 18 | Offline Excel Exporter (.xlsx) | Offline local generation of multi-sheet workbook with sessions & sets | M4 | R4 (line 29, 59) |
| 19 | Offline PDF Report Exporter | Offline vector PDF summary report with Russian typography and volume tables | M4 | R4 (line 29, 60) |
| 20 | 100% Russian Localization | Russian UI strings, dialogs, buttons, dates, table headers | M1-M4 | Original Request (line 7) |
| 21 | Room SQLite Local-First DB | Pre-populated library of Russian exercises, categories, DAOs, transactions | M1 | Model (line 31-39) |
| 22 | Full E2E & Unit Test Harness | Complete test suite across Tiers 1-4 + Tier 5 coverage hardening | M5 | Acceptance Criteria |
| 23 | Set Tags (Warmup, Drop, Failure) | Explicit tags for sets (W, N, D, F) to exclude warmups from volume | M2 | GymKeeper Audit |
| 24 | Superset & Circuit Grouping | Group exercises visually with a shared rest timer | M2 | GymKeeper Audit |
| 25 | Quick Swap Exercise | Replace active exercise mid-workout preserving logged sets | M2 | GymKeeper Audit |
| 26 | Plate Calculator | Visual scheme for loading plates for calculated target weights | M3 | GymKeeper Audit |
| 27 | Body Tracker & Photos | Track bodyweight, body fat, measurements, and progress photos | M4 | GymKeeper Audit |
| 28 | Auto-Periodization Engine | Suggest deload weeks automatically on persistent RIR=0 or plateaus | M5 | GymKeeper Audit (Radical) |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Project Setup, Room DB, Domain Models & Progression Engine | Gradle project scaffolding, Room Database, Prepopulated Russian exercises, DAOs, Progression Engine (all 3 branches + inventory rounding), 1RM Calculators, and Unit Tests. *Includes Set Tags and Superset data model support.* | none | DONE |
| M2 | Active Workout Screen, Logging & Rest Timer | Active workout Compose UI, +X buttons (+1..+20kg), numeric pad, RIR slider (0..5), touch targets >=48dp, click budget <=4, Rest Timer manager (PiP/Foreground), Set Tags UI, Superset UI, Quick Swap, background notification & vibration | M1 | PLANNED |
| M3 | Calendar, Workout History, Session Cloning & Auto-Population | Monthly / Weekly Calendar UI, colored workout badges, Session Cloning use case & dialog, Last Completed Set auto-population engine with empty history fallback, Plate Calculator | M1, M2 | PLANNED |
| M4 | Analytics Progress Charts & Offline Excel/PDF Export | Dual-axis progress chart (Date vs 1RM & Working weight), pure offline XLSX streaming export, vector PDF report generator, Share intent integration, Body Tracker & Photos | M1, M2, M3 | PLANNED |
| M5 | E2E Testing & Advanced AI/Progression Features | Full execution of Tiers 1-4 E2E tests, Tier 5 adversarial coverage, Auto-Periodization Engine, On-Device ML/RPG Gamification exploration | M1, M2, M3, M4 | PLANNED |

## Interface Contracts

### Domain ↔ Data Layer
- `WorkoutRepository`:
  - `fun getActiveSession(): Flow<WorkoutSessionWithSets?>`
  - `suspend fun startNewSession(date: Long = System.currentTimeMillis(), notes: String = ""): Long`
  - `suspend fun completeSession(sessionId: Long)`
  - `suspend fun deleteSession(sessionId: Long)`
  - `fun getSessionsByDateRange(startDate: Long, endDate: Long): Flow<List<WorkoutSessionWithSets>>`
  - `suspend fun getLastCompletedSetForExercise(exerciseId: Long, beforeDate: Long): SetEntry?`
  - `suspend fun insertSet(set: SetEntry): Long`
  - `suspend fun updateSet(set: SetEntry)`
  - `suspend fun deleteSet(setId: Long)`
  - `suspend fun cloneSession(sourceSessionId: Long, targetDate: Long): Long`

- `ExerciseRepository`:
  - `fun getAllExercises(): Flow<List<Exercise>>`
  - `fun getExercisesByCategory(categoryId: Long): Flow<List<Exercise>>`
  - `fun getAllCategories(): Flow<List<Category>>`
  - `suspend fun getProgressConfig(exerciseId: Long): ProgressConfig`
  - `suspend fun updateProgressConfig(config: ProgressConfig)`

### Progression Module
- `ProgressionEngine.calculateNextWorkout(previousWeightKg: Double, actualReps: Int, actualRir: Int, config: ProgressConfig): ProgressionResult`
- `ProgressionEngine.roundToStep(weight: Double, minStepKg: Double): Double`
- `OneRepMaxCalculator.calculateEpley(weight: Double, reps: Int): Double`
- `OneRepMaxCalculator.calculateBrzycki(weight: Double, reps: Int): Double`

### Export Services
- `ExcelExporter.exportToStream(sessions: List<WorkoutSessionWithSets>, exercises: Map<Long, Exercise>, outputStream: OutputStream)`
- `PdfReportExporter.generateReportToStream(context: Context, sessions: List<WorkoutSessionWithSets>, exercises: Map<Long, Exercise>, startDate: Long, endDate: Long, outputStream: OutputStream)`
