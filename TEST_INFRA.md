# E2E Test Infra: Android Workout Tracker («Трекер Тренировок»)

## Test Philosophy
- **Requirement-Driven & Opaque-Box**: Test cases derive directly from `ORIGINAL_REQUEST.md` and user specifications, independent of internal implementation details.
- **Methodology**: Category-Partition + Boundary Value Analysis (BVA) + Pairwise Interaction Testing + Real-World Workload Simulation.
- **Offline Integrity**: All tests must verify zero remote network requests and 100% deterministic local calculation.

## Feature Inventory & Test Mapping
| # | Feature | Requirement Source | Tier 1 (Isolated) | Tier 2 (Boundary) | Tier 3 (Pairwise) | Tier 4 (Real-World) |
|---|---------|-------------------|:-----------------:|:-----------------:|:-----------------:|:-------------------:|
| 1 | Quick Increment Buttons (+1..+20kg) | R1 line 15 | 5 | 5 | ✓ | ✓ |
| 2 | Direct Numeric Keypad & Clear/Back | R1 line 15 | 5 | 5 | ✓ | ✓ |
| 3 | Integer Reps Input | R1 line 15 | 5 | 5 | ✓ | ✓ |
| 4 | Discrete RIR Slider (0..5) | R1 line 15 | 5 | 5 | ✓ | ✓ |
| 5 | Touch Target >=48dp & Click Budget <=4 | R1 line 15, 44 | 5 | 5 | ✓ | ✓ |
| 6 | Rest Timer (90s / 180s + Notification) | R1 line 15, 64 | 5 | 5 | ✓ | ✓ |
| 7 | Calendar Month/Week Views & Indicators | R2 line 18 | 5 | 5 | ✓ | ✓ |
| 8 | Session Cloning Engine | R2 line 18, 48 | 5 | 5 | ✓ | ✓ |
| 9 | Last Set Auto-Population & Empty Fallback | R2 line 18, 49-50 | 5 | 5 | ✓ | ✓ |
| 10 | Progression Engine (+5%, +2%, Hold/Deload) | R3 line 21-26, 53 | 5 | 5 | ✓ | ✓ |
| 11 | Inventory Step Rounding (1.25/2.5kg) | R3 line 26, 54 | 5 | 5 | ✓ | ✓ |
| 12 | ProgressConfig & Override | R3 line 26, 56 | 5 | 5 | ✓ | ✓ |
| 13 | 1RM Calculators (Epley & Brzycki) | R4 line 29 | 5 | 5 | ✓ | ✓ |
| 14 | Dual-Axis Progress Chart | R4 line 29 | 5 | 5 | ✓ | ✓ |
| 15 | Offline Excel (.xlsx) Export | R4 line 29, 59 | 5 | 5 | ✓ | ✓ |
| 16 | Offline PDF Report Generator | R4 line 29, 60 | 5 | 5 | ✓ | ✓ |
| 17 | 100% Russian Localization | Line 7, 72 | 5 | 5 | ✓ | ✓ |

## Test Architecture & Execution
- **Unit & Logic Tests**: JUnit 4/5 + Google Truth + MockK running on JVM (`testDebugUnitTest`).
- **Database & DAO Integration Tests**: Room In-Memory Database Tests (`Room.inMemoryDatabaseBuilder`).
- **Export Validation**:
  - Apache POI / OpenXML reader parsing generated `.xlsx` bytes to assert sheet names, column headers in Russian, cell formulas, and row data.
  - Android `PdfRenderer` / PDF text parser asserting page count, dimensions, text elements, and Russian typography.
- **Invocation Command**:
  ```cmd
  set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
  gradlew.bat testDebugUnitTest
  ```

## Real-World Workload Scenarios (Tier 4)
1. **Full Workout Lifecycle Scenario**:
   - User creates session, performs 3 exercises (Bench Press, Incline DB Press, Dips).
   - Uses +X buttons, adjusts RIR slider, confirms 3 sets per exercise with auto rest timers.
   - Completes workout session, verifies persistence in Room DB and appearance on Calendar.
2. **Progression Cycle Across 3 Consecutive Weeks**:
   - Week 1: 100 kg × 8 reps, RIR 1 $\to$ Engine suggests 105 kg (+5%).
   - Week 2: 105 kg × 8 reps, RIR 3 $\to$ Engine suggests 107.5 kg (+2% rounded).
   - Week 3: 107.5 kg × 6 reps (plan 8 missed) $\to$ Engine holds 107.5 kg.
3. **Historical Session Cloning & Progression Continuity**:
   - User navigates to Calendar, clones last Wednesday's Leg Day to today.
   - All 4 exercises, exact set sequence, and previous target loads are preserved.
   - User adds a 5th exercise; auto-population pre-fills values from the last time that exercise was performed 2 weeks prior.
4. **End-of-Month Analytics & Multi-Format Data Export**:
   - User selects 30-day period in Analytics, inspects 1RM curve for Squat and Bench Press.
   - Triggers Excel `.xlsx` export $\to$ validates 30-day records across multiple worksheets.
   - Triggers PDF summary report $\to$ validates multi-page A4 document with total tonnage, personal records, and Russian text.
5. **Zero-History & Bodyweight Edge Case Handling**:
   - First-time user logs Pull-ups (0.0 kg) and Push-ups (0.0 kg).
   - Verifies auto-population falls back cleanly without crash; progression transitions to plate loading or reps progression.

## Coverage Goals
- **Total Test Cases**: $\ge 150$ unit & integration test cases.
- **Pass Semantics**: 100% pass rate with exit code 0.
