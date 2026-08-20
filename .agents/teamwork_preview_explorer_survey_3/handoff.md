# Project Architecture & Environment Survey Report

**Agent**: `teamwork_preview_explorer_survey_3`  
**Mission**: Phase 0 Architecture & Environment Survey for Android Workout Tracker («Трекер Тренировок»)  
**Target File**: `.agents/teamwork_preview_explorer_survey_3/handoff.md`  

---

## 1. Observation

### 1.1 Local Workspace State
- **Root Directory**: `c:\Users\DenCrut\Documents\antigravity\wonderful-pythagoras`
- **Existing Content**: Contains `.git`, `.agents/` metadata directory, and `ORIGINAL_REQUEST.md`. No pre-existing Android project files exist in the root yet, providing a clean slate for project generation.

### 1.2 Environment Tooling & SDK Detection
Direct terminal probes executed via PowerShell / CMD confirmed the following environment configuration:

1. **Java Development Kit (JDK)**:
   - Location: `C:\Program Files\Android\Android Studio\jbr`
   - Binary: `C:\Program Files\Android\Android Studio\jbr\bin\java.exe`
   - Version output:
     ```text
     openjdk version "21.0.10" 2026-01-20
     OpenJDK Runtime Environment (build 21.0.10+-14961533-b1163.108)
     OpenJDK 64-Bit Server VM (build 21.0.10+-14961533-b1163.108, mixed mode)
     ```
   - *Requirement for build scripts*: Environment variable `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr` must be set or passed to Gradle wrapper.

2. **Android SDK & Build Tools**:
   - SDK Root: `C:\Users\DenCrut\AppData\Local\Android\Sdk` (identified via `android info`)
   - Installed Platforms:
     - `platforms/android-36` (Android 16 / API 36)
     - `platforms/android-36.1`
     - `platforms/android-35` (Android 15 / API 35)
     - `platforms/android-34` (Android 14 / API 34)
     - `platforms/android-33` (Android 13 / API 33)
     - `platforms/android-30` (Android 11 / API 30)
   - Installed Build-Tools: `34.0.0`, `35.0.0`, `36.0.0`, `36.1.0`, `37.0.0`
   - Platform-Tools: `37.0.0`
   - *Configuration Requirement*: `local.properties` file must specify:
     ```properties
     sdk.dir=C\:\\Users\\DenCrut\\AppData\\Local\\Android\\Sdk
     ```

3. **Android CLI Tool**:
   - Version: `1.0.15498356`
   - Available Templates: `empty-activity` (tags: `compose, activity, agp-9`)

4. **Gradle & Build Capabilities Verification**:
   - Template project tested with Gradle 9.1.0, AGP 9.0.1, Kotlin 2.3.20, and Java 21.
   - Verification command executed:
     ```cmd
     set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr&& gradlew.bat testDebugUnitTest
     ```
   - Result:
     ```text
     BUILD SUCCESSFUL in 50s
     24 actionable tasks: 24 executed
     ```

---

## 2. Logic Chain

1. **Toolchain Alignment**:
   - Android SDK 36 (`platforms/android-36`) + OpenJDK 21 (`C:\Program Files\Android\Android Studio\jbr`) + Gradle 9.1.0 + AGP 9.0.1 are present and proven to build and run test suites cleanly.
   - Setting `compileSdk = 36`, `targetSdk = 36`, `minSdk = 24` guarantees compatibility with modern Android features (e.g., Notification channels, Material 3, modern date/time APIs, Canvas drawing) while supporting ~98% of active Android devices.

2. **Clean Architecture Decomposition (Strict Separation of Concerns)**:
   - **Presentation Layer (Jetpack Compose + Material 3 + ViewModels)**:
     - 100% Russian UI strings via standard Android string resources (`res/values/strings.xml`).
     - Fast entry set logging screen with large interactive buttons (`+1`, `+2.5`, `+5`, `+10`, `+20` kg), direct numeric pad, clear/backspace, discrete RIR slider [0..5], touch targets strictly $\ge 48 \times 48\text{ dp}$.
     - Calendar screen (month/week toggle, colored workout markers, 1-click clone action).
     - Analytics screen featuring dual-axis progress charts (X = Date, Y1 = 1RM via Epley/Brzycki, Y2 = Working Weight).
     - Rest Timer HUD with background notification and vibration support.
   - **Domain Layer (Kotlin-only, Framework-independent)**:
     - Pure domain models (`Exercise`, `WorkoutSession`, `SetEntry`, `ProgressConfig`, `ExerciseCategory`).
     - `CalculateProgressionUseCase`: Implements $W_{next} = W_{prev} \times (1 + \Delta)$ with deterministic branches:
       - RIR $\in [0, 1]$ & target reps met $\rightarrow \Delta = +0.05$
       - RIR $\ge 2 \rightarrow \Delta = +0.02$ (or hold weight + recommend reps)
       - Plan reps missed $\rightarrow \Delta = 0.0$ (hold/deload)
       - Granular inventory step rounding (e.g. nearest 1.25 or 2.5 kg).
     - `CalculateOneRepMaxUseCase`: Epley ($W \times (1 + \frac{R}{30})$) & Brzycki ($W \times \frac{36}{37 - R}$) formulas.
     - `CloneWorkoutSessionUseCase`: Clones session structure, exercises, and set count into target date.
     - `GetAutoPopulateValuesUseCase`: Extracts last completed weight and reps for exercise (graceful empty fallback).
   - **Data Layer (Room SQLite + Local-First DAOs)**:
     - Room 2.7.x / 2.6.x SQLite database with pre-populated standard exercise library in Russian (Грудь, Спина, Ноги, Плечи, Руки, Кор).
     - DAOs: `ExerciseDao`, `WorkoutSessionDao`, `SetEntryDao`, `ProgressConfigDao`.
     - 100% offline, local persistence with zero remote API / network dependencies.
   - **Export Services Layer**:
     - **Excel Export (`.xlsx`)**: Pure Kotlin streaming OpenXML `.xlsx` zip writer (or lightweight POI stream), writing worksheets for Sessions, Exercises, and Set History. Fast, local, zero-leak.
     - **PDF Report Generation**: Native Android `android.graphics.pdf.PdfDocument` + `android.graphics.Canvas`. Produces high-resolution, vector-rendered summary tables and graphs with embedded Cyrillic typography without bulky 3rd-party PDF engines.

3. **Dependency Optimization (Zero Bloat, High Performance)**:
   - Relying on native `PdfDocument` and custom Jetpack Compose Canvas charting avoids heavy external jars (e.g. iText or bloated 3rd party chart libraries), keeping APK size minimal (<8MB) and build times rapid.

---

## 3. Recommended Project Structure

```text
wonderful-pythagoras/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/workouttracker/
│       │   │   ├── WorkoutApplication.kt
│       │   │   ├── MainActivity.kt
│       │   │   ├── data/
│       │   │   │   ├── local/
│       │   │   │   │   ├── AppDatabase.kt
│       │   │   │   │   ├── PrepopulateData.kt
│       │   │   │   │   ├── entity/
│       │   │   │   │   │   ├── ExerciseEntity.kt
│       │   │   │   │   │   ├── WorkoutSessionEntity.kt
│       │   │   │   │   │   ├── SetEntryEntity.kt
│       │   │   │   │   │   ├── ProgressConfigEntity.kt
│       │   │   │   │   │   └── CategoryEntity.kt
│       │   │   │   │   └── dao/
│       │   │   │   │       ├── ExerciseDao.kt
│       │   │   │   │       ├── WorkoutSessionDao.kt
│       │   │   │   │       ├── SetEntryDao.kt
│       │   │   │   │       └── ProgressConfigDao.kt
│       │   │   │   └── repository/
│       │   │   │       ├── WorkoutRepositoryImpl.kt
│       │   │   │       └── ExerciseRepositoryImpl.kt
│       │   │   ├── domain/
│       │   │   │   ├── model/
│       │   │   │   │   ├── Exercise.kt
│       │   │   │   │   ├── WorkoutSession.kt
│       │   │   │   │   ├── SetEntry.kt
│       │   │   │   │   ├── ProgressConfig.kt
│       │   │   │   │   ├── Category.kt
│       │   │   │   │   └── WorkoutSessionWithSets.kt
│       │   │   │   ├── repository/
│       │   │   │   │   ├── WorkoutRepository.kt
│       │   │   │   │   └── ExerciseRepository.kt
│       │   │   │   └── usecase/
│       │   │   │       ├── CalculateProgressionUseCase.kt
│       │   │   │       ├── CalculateOneRepMaxUseCase.kt
│       │   │   │       ├── CloneWorkoutSessionUseCase.kt
│       │   │   │       └── GetAutoPopulatedValuesUseCase.kt
│       │   │   ├── export/
│       │   │   │   ├── ExcelExporter.kt
│       │   │   │   └── PdfReportExporter.kt
│       │   │   ├── timer/
│       │   │   │   ├── RestTimerManager.kt
│       │   │   │   └── RestTimerNotificationService.kt
│       │   │   └── presentation/
│       │   │       ├── navigation/
│       │   │       │   ├── Screen.kt
│       │   │       │   └── AppNavHost.kt
│       │   │       ├── theme/
│       │   │       │   ├── Color.kt
│       │   │       │   ├── Theme.kt
│       │   │       │   └── Type.kt
│       │   │       ├── components/
│       │   │       │   ├── DualAxisProgressChart.kt
│       │   │       │   ├── NumericWeightKeypad.kt
│       │   │       │   ├── DiscreteRirSlider.kt
│       │   │       │   ├── RestTimerOverlay.kt
│       │   │       │   └── CommonButtons.kt
│       │   │       └── screens/
│       │   │           ├── active_workout/
│       │   │           │   ├── ActiveWorkoutScreen.kt
│       │   │           │   └── ActiveWorkoutViewModel.kt
│       │   │           ├── calendar/
│       │   │           │   ├── CalendarScreen.kt
│       │   │           │   └── CalendarViewModel.kt
│       │   │           ├── history/
│       │   │           │   ├── HistoryScreen.kt
│       │   │           │   └── HistoryViewModel.kt
│       │   │           ├── analytics/
│       │   │           │   ├── AnalyticsScreen.kt
│       │   │           │   └── AnalyticsViewModel.kt
│       │   │           └── export/
│       │   │               ├── ExportScreen.kt
│       │   │               └── ExportViewModel.kt
│       │   └── res/
│       │       ├── values/
│       │       │   ├── strings.xml (100% Russian strings)
│       │       │   └── colors.xml
│       │       └── drawable/
│       └── test/java/com/example/workouttracker/
│           ├── domain/
│           │   ├── CalculateProgressionUseCaseTest.kt
│           │   ├── CalculateOneRepMaxUseCaseTest.kt
│           │   ├── CloneWorkoutSessionUseCaseTest.kt
│           │   └── AutoPopulateUseCaseTest.kt
│           ├── data/
│           │   └── local/RoomDatabaseDAOTest.kt
│           └── export/
│               ├── ExcelExporterTest.kt
│               └── PdfReportExporterTest.kt
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── local.properties
```

---

## 4. Dependencies & Version Catalog (`libs.versions.toml`)

```toml
[versions]
androidGradlePlugin = "9.0.1"
kotlin = "2.3.20"
ksp = "2.3.20-1.0.31"
room = "2.7.0-alpha13" # Or 2.6.1 stable with SQLite driver
androidxCore = "1.18.0"
androidxLifecycle = "2.10.0"
androidxActivity = "1.13.0"
androidxComposeBom = "2026.03.01"
navCompose = "2.8.8"
coroutines = "1.10.2"
junit = "4.13.2"
truth = "1.4.4"
mockk = "1.13.16"
androidxTest = "1.7.0"
androidxTestExt = "1.3.0"
androidxTestRunner = "1.7.0"
androidxTestEspresso = "3.7.0"

[libraries]
# Core & Lifecycle
androidx-core-ktx = { module = "androidx.core:core-ktx", version.ref = "androidxCore" }
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "androidxActivity" }
androidx-lifecycle-runtime-ktx = { module = "androidx.lifecycle:lifecycle-runtime-ktx", version.ref = "androidxLifecycle" }
androidx-lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "androidxLifecycle" }
androidx-lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "androidxLifecycle" }

# Compose BOM & UI
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "androidxComposeBom" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }

# Navigation
androidx-navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navCompose" }

# Coroutines
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }

# Room SQLite
androidx-room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
androidx-room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
androidx-room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }

# Testing
junit = { module = "junit:junit", version.ref = "junit" }
truth = { module = "com.google.truth:truth", version.ref = "truth" }
mockk = { module = "io.mockk:mockk", version.ref = "mockk" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
androidx-test-core = { module = "androidx.test:core", version.ref = "androidxTest" }
androidx-test-ext-junit = { module = "androidx.test.ext:junit", version.ref = "androidxTestExt" }
androidx-test-runner = { module = "androidx.test:runner", version.ref = "androidxTestRunner" }
androidx-test-espresso-core = { module = "androidx.test.espresso:espresso-core", version.ref = "androidxTestEspresso" }
androidx-compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }

[plugins]
android-application = { id = "com.android.application", version.ref = "androidGradlePlugin" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

---

## 5. Caveats
1. **JAVA_HOME in CLI Sessions**: `JAVA_HOME` is not globally configured in Windows user PATH by default; running Gradle commands requires setting `JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"` or configuring Gradle Daemon JDK in `gradle.properties` (`org.gradle.java.home=C:\\Program Files\\Android\\Android Studio\\jbr`).
2. **Android CLI vs Gradle Wrapper**: The `android` CLI uses Android Studio Quail/Ladybug templates. The standalone Gradle wrapper `gradlew.bat` operates reliably once Java 21 is passed.
3. **Emulator & Virtual Devices**: The Android Emulator is installed (`emulator 36.5.10`), but headless unit and Robolectric tests are recommended for continuous fast validation during development cycles.

---

## 6. Conclusion
The environment has been verified and meets all technical requirements for building the production-ready Android Workout Tracker:
- **Build System**: AGP 9.0.1, Gradle 9.1.0, Kotlin 2.3.20, Java 21.
- **SDK Target**: CompileSdk 36, MinSdk 24.
- **Architecture**: 4-tier Clean Architecture (Presentation Compose $\rightarrow$ Domain UseCases $\rightarrow$ Data Room $\rightarrow$ Export/Timer Services).
- **Offline / Local-First**: Room SQLite local persistence with pre-loaded Russian exercises and 100% offline math/export engines.
- **Verification Harness Ready**: Unit test execution passed in 50s.

---

## 7. Verification Method
Any subsequent agent or tester can independently verify the environment and build capability using:

1. **Verify Environment Tools**:
   ```cmd
   "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" -version
   android info
   ```
2. **Execute Unit Tests via Gradle**:
   ```cmd
   set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
   gradlew.bat testDebugUnitTest
   ```
3. **Invalidation Conditions**:
   - Failure of `testDebugUnitTest` or `assembleDebug`.
   - Any dependency attempting remote network calls in production.
   - Non-Russian UI strings or missing Cyrillic glyph rendering in PDF export.
