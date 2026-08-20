# Specification & Analysis Report: Requirements R1 & R3

## 1. Observation

Direct requirements extracted from `ORIGINAL_REQUEST.md`:

### Requirement R1: Active Workout & Logging
- **Weight Input**: Increment buttons (+1, +2.5, +5, +10, +20 kg) + direct numeric entry with clear/backspace buttons.
- **Reps Input**: Positive integer ($R \in \mathbb{N}^+$).
- **RIR Input**: Discrete slider from 0 to 5 with step 1 ($RIR \in \{0, 1, 2, 3, 4, 5\}$).
- **Rest Timer**: Automatically started upon set completion. Default durations: 90s between sets of the same exercise, 180s between different exercises. Customizable. Background notification + vibration on completion.
- **UX Constraints**: Interactive touch targets $\ge 48 \times 48$ dp. Set logging completed in $\le 3$ screens / $\le 4$ clicks.
- **Language**: 100% Russian user interface strings.

### Requirement R3: Deterministic Progression Algorithm
- **Core Formula**: $W_{next} = W_{prev} \times (1 + \Delta)$
- **Branching Logic**:
  - $RIR \in [0, 1]$ and target reps met $\implies \Delta = 0.05$ (+5%).
  - $RIR \ge 2$ and target reps met $\implies \Delta = 0.02$ (+2% or hold weight + recommend reps).
  - Target reps not met $\implies W_{next} = W_{prev}$ (Hold weight) or deload step.
- **Inventory Rounding**: Quantized to nearest configurable step $S_{min}$ (e.g., 1.25 kg or 2.5 kg).
- **ProgressConfig**: Configurable per exercise (`exercise_id`, `min_step_kg`, `progression_percent`, `target_reps`, `target_sets`).
- **Placeholder Behavior**: $W_{next}$ displayed as a hint placeholder; user can override. Edge cases: 0 kg (bodyweight), 0 reps (failed set), floating-point precision.

---

## 2. Logic Chain

### 2.1 Domain Data Model & Schema (Room / Kotlin)

```kotlin
// Room Entities for R1 & R3

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, // Russian name, e.g., "Жим штанги лежа"
    val categoryId: Long,
    val defaultRestTimeSeconds: Int = 90, // default 90s between sets
    val defaultExerciseRestTimeSeconds: Int = 180, // default 180s between exercises
    val isBodyweight: Boolean = false
)

@Entity(
    tableName = "progress_configs",
    foreignKeys = [
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["exerciseId"], unique = true)]
)
data class ProgressConfig(
    @PrimaryKey val exerciseId: Long,
    val minStepKg: Double = 2.5, // 0.5, 1.0, 1.25, 2.0, 2.5, 5.0
    val progressionPercentHeavy: Double = 0.05, // 5% for RIR 0-1
    val progressionPercentModerate: Double = 0.02, // 2% for RIR >= 2
    val targetReps: Int = 8,
    val targetSets: Int = 3,
    val deloadPercent: Double = 0.10 // 10% deload
)

@Entity(
    tableName = "set_entries",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSession::class,
            parentColumns = ["id"],
            childColumns = ["workoutSessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workoutSessionId"), Index("exerciseId")]
)
data class SetEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutSessionId: Long,
    val exerciseId: Long,
    val setNumber: Int, // 1-indexed
    val weightKg: Double, // in kg (>= 0.0)
    val reps: Int, // positive integer (>= 0)
    val rir: Int, // 0..5
    val timestamp: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = true
)
```

---

### 2.2 R1: Active Workout & Logging Specification

#### A. Weight Input Component
- **Increment Buttons**: Array of 5 dedicated quick-add buttons: `[+1]`, `[+2.5]`, `[+5]`, `[+10]`, `[+20]` kg.
  - Formula: $W_{new} = \text{clamp}(W_{current} + \text{increment}, 0.0, 999.5)$.
  - Multi-tap support: e.g., tapping `+10` twice adds 20 kg.
- **Direct Numeric Entry & Keypad**:
  - Digits `0-9`, decimal separator `.` or `,`.
  - Backspace `⌫`: Removes trailing digit/symbol.
  - Clear `C`: Resets input to empty.
  - Max fractional precision: 2 decimal digits.
- **Placeholder Behavior**:
  - If field is empty, displays recommended weight $W_{rec}$ (or previous set weight) in `MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)`.
  - Tapping "Save" with empty field commits the placeholder weight.

#### B. Reps Input Component
- Positive integer $R \in [0, 999]$.
- Controls: Stepper buttons `[-]` and `[+]`, plus direct numeric entry.
- Default: Prefilled with previous set reps or target reps (e.g., 8).

#### C. RIR Discrete Slider
- Domain: $RIR \in \{0, 1, 2, 3, 4, 5\}$.
- Step: 1.0 (strict integer values).
- Visual Semantic Scale (Russian):
  - `0`: "0 — До отказа (0 в запасе)"
  - `1`: "1 — Предельно тяжело (1 в запасе)"
  - `2`: "2 — Тяжело (2 в запасе)"
  - `3`: "3 — Умеренно (3 в запасе)"
  - `4`: "4 — Легко (4 в запасе)"
  - `5`: "5 — Разминка / Запас ≥ 5"
- Default Value: 2 (or last logged RIR).

#### D. UX Constraints & Click Budget Verification
- **Touch Target Requirement**: All buttons, slider thumbs, text fields, and icons have minimum layout size `48.dp × 48.dp` (via `Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)`).
- **Click Budget**:
  - **Screen 1 (Single Active Screen Flow)**:
    1. Click 1: Tap weight increment button (e.g. `+2.5` kg) or accept prefilled placeholder.
    2. Click 2: Tap reps stepper or keep prefilled target.
    3. Click 3: Tap RIR discrete slider / chip (0–5).
    4. Click 4: Tap "✓ Сохранить подход" (Save Set).
  - **Total Screens**: 1 screen (Constraint: $\le 3$ screens $\to$ **PASS**).
  - **Total Clicks**: 1 to 4 clicks (Constraint: $\le 4$ clicks $\to$ **PASS**).

#### E. Rest Timer Engine Specification
- **State Machine**:
  - States: `IDLE`, `RUNNING`, `PAUSED`, `FINISHED`.
  - Events: `START(seconds)`, `PAUSE`, `RESUME`, `ADD_30S`, `SUB_30S`, `STOP`.
- **Durations**:
  - Between sets of same exercise: `90 seconds` (configurable).
  - Between exercises: `180 seconds` (configurable).
- **Background Execution & Notification**:
  - Android `ForegroundService` or `WorkManager` with `NotificationCompat.Builder`.
  - Channel: `workout_timer_channel` (Importance: `IMPORTANCE_HIGH`).
  - Notification displays live countdown `ММ:СС` and action buttons: `+30 сек`, `Пропустить`.
- **Completion Alert**:
  - Vibration pattern: `VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500), -1)`.
  - Heads-up notification popup + optional system beep.

---

### 2.3 R3: Deterministic Progression Algorithm Specification

#### A. Mathematical Formulation
Let:
- $W_{prev} \in \mathbb{R}_{\ge 0}$: Weight of previous completed session/set (kg).
- $R_{target} \in \mathbb{N}^+$: Target reps from `ProgressConfig`.
- $R_{actual} \in \mathbb{N}_{\ge 0}$: Reps completed in previous session.
- $RIR \in \{0, 1, 2, 3, 4, 5\}$: Logged Reps In Reserve.
- $S_{min} \in \mathbb{R}^+$: Minimum inventory step (default 2.5 kg).
- $p_{heavy} \in \mathbb{R}$: Progression percentage for high effort (default 0.05 = +5%).
- $p_{mod} \in \mathbb{R}$: Progression percentage for moderate effort (default 0.02 = +2%).

1. **Step 1: Calculate Delta ($\Delta$)**:
   $$\Delta = \begin{cases} 
   p_{heavy} = +0.05, & \text{if } R_{actual} \ge R_{target} \text{ and } RIR \in \{0, 1\} \\
   p_{mod} = +0.02, & \text{if } R_{actual} \ge R_{target} \text{ and } RIR \ge 2 \\
   0.0, & \text{if } R_{actual} < R_{target} \text{ (Hold weight)}
   \end{cases}$$

2. **Step 2: Unrounded Raw Weight Calculation**:
   $$W_{raw} = W_{prev} \times (1.0 + \Delta)$$

3. **Step 3: Quantization & Rounding to Inventory Step ($S_{min}$)**:
   $$W_{next} = \text{round}\left( \frac{W_{raw}}{S_{min}} \right) \times S_{min}$$
   Using half-up rounding: $\text{round}(x) = \lfloor x + 0.5 \rfloor$.

4. **Step 4: Quantization Deadband Handling & Rep Progression Alternative**:
   - For low working weights (e.g. $W_{prev} = 20.0$ kg with $S_{min} = 2.5$ kg):
     - At $\Delta = +0.02$: $W_{raw} = 20.4$ kg $\implies \text{round}(20.4 / 2.5) \times 2.5 = \text{round}(8.16) \times 2.5 = 20.0$ kg.
     - Deadband detected: $W_{next} == W_{prev}$ despite $\Delta > 0$.
   - **Resolution Invariant**:
     When $RIR \ge 2$ and $W_{next} == W_{prev}$, the progression module produces:
     - `recommendedWeight = W_prev` (20.0 kg)
     - `recommendedReps = R_target + 1` (or $+2$)
     - Message: "Вес сохранен (20.0 кг). Рекомендация: увеличьте повторения до 9-10".

#### B. Progression Function Implementation (Pure Kotlin)

```kotlin
data class ProgressionResult(
    val recommendedWeightKg: Double,
    val recommendedReps: Int,
    val deltaApplied: Double,
    val explanationRu: String
)

object ProgressionEngine {
    fun calculateNextWorkout(
        previousWeightKg: Double,
        actualReps: Int,
        actualRir: Int,
        config: ProgressConfig
    ): ProgressionResult {
        // Edge Case: 0 kg (Bodyweight)
        if (previousWeightKg <= 0.0) {
            return if (actualReps >= config.targetReps) {
                ProgressionResult(
                    recommendedWeightKg = config.minStepKg, // start adding weight plate
                    recommendedReps = config.targetReps,
                    deltaApplied = 0.0,
                    explanationRu = "План выполнен с собственным весом. Рекомендуется добавить отягощение ${config.minStepKg} кг."
                )
            } else {
                ProgressionResult(
                    recommendedWeightKg = 0.0,
                    recommendedReps = config.targetReps,
                    deltaApplied = 0.0,
                    explanationRu = "План повторений не выполнен. Продолжайте тренировки с собственным весом."
                )
            }
        }

        // Branch 1: High Effort (RIR 0..1) and Plan Completed
        if (actualReps >= config.targetReps && actualRir in 0..1) {
            val delta = config.progressionPercentHeavy // 0.05
            val rawNext = previousWeightKg * (1.0 + delta)
            val roundedNext = roundToStep(rawNext, config.minStepKg)
            val finalWeight = if (roundedNext <= previousWeightKg) {
                // Ensure at least 1 inventory step increase if rounded down
                roundToStep(previousWeightKg + config.minStepKg, config.minStepKg)
            } else {
                roundedNext
            }
            return ProgressionResult(
                recommendedWeightKg = finalWeight,
                recommendedReps = config.targetReps,
                deltaApplied = delta,
                explanationRu = "Отличная работа (RIR $actualRir). Нагрузка увеличена на 5% (+$finalWeight кг)."
            )
        }

        // Branch 2: Moderate Effort (RIR >= 2) and Plan Completed
        if (actualReps >= config.targetReps && actualRir >= 2) {
            val delta = config.progressionPercentModerate // 0.02
            val rawNext = previousWeightKg * (1.0 + delta)
            val roundedNext = roundToStep(rawNext, config.minStepKg)
            
            return if (roundedNext > previousWeightKg) {
                ProgressionResult(
                    recommendedWeightKg = roundedNext,
                    recommendedReps = config.targetReps,
                    deltaApplied = delta,
                    explanationRu = "План выполнен с запасом (RIR $actualRir). Вес увеличен до $roundedNext кг (+2%)."
                )
            } else {
                // Deadband -> Recommend Reps progression
                ProgressionResult(
                    recommendedWeightKg = previousWeightKg,
                    recommendedReps = actualReps + 1,
                    deltaApplied = delta,
                    explanationRu = "План выполнен с запасом (RIR $actualRir). Рекомендуется увеличить повторения до ${actualReps + 1}."
                )
            }
        }

        // Branch 3: Plan Not Met (actualReps < targetReps)
        return ProgressionResult(
            recommendedWeightKg = previousWeightKg,
            recommendedReps = config.targetReps,
            deltaApplied = 0.0,
            explanationRu = "План повторений не выполнен ($actualReps / ${config.targetReps}). Вес удерживается: $previousWeightKg кг."
        )
    }

    fun roundToStep(weight: Double, minStepKg: Double): Double {
        if (minStepKg <= 0.0) return BigDecimal(weight.toString()).setScale(2, RoundingMode.HALF_UP).toDouble()
        val steps = Math.round(weight / minStepKg)
        val result = steps * minStepKg
        return BigDecimal(result.toString()).setScale(2, RoundingMode.HALF_UP).toDouble()
    }
}
```

---

## 3. Features Discovered

| # | Category | Feature | Description | Inputs | Outputs | Error Behavior | Discovered Via |
|---|----------|---------|-------------|--------|---------|----------------|----------------|
| 1 | R1: UI/UX | Quick Increment Buttons | Buttons (+1, +2.5, +5, +10, +20 kg) to rapidly adjust weight | Current weight, increment button click | New weight sum | Clamped to max 999.5 kg; non-negative | R1 in ORIGINAL_REQUEST.md |
| 2 | R1: UI/UX | Direct Numeric Keypad | Keypad (0-9, ., ⌫, C) for manual entry | Key presses | String weight buffer -> Double | Disallow multiple decimal points; invalid chars ignored | R1 in ORIGINAL_REQUEST.md |
| 3 | R1: UI/UX | Integer Reps Input | Stepper (+ / -) & numeric entry for reps | Rep count | Positive integer ($R \ge 0$) | Clamped to $[0, 999]$; reject negative | R1 in ORIGINAL_REQUEST.md |
| 4 | R1: UI/UX | Discrete RIR Slider | 0 to 5 slider with step 1 representing Reps In Reserve | User drag/tap on slider | Integer RIR $\in [0, 5]$ | Default to 2; strictly discrete steps | R1 in ORIGINAL_REQUEST.md |
| 5 | R1: UI/UX | Touch Target Accessibility | Touch areas $\ge 48 \times 48$ dp for all interactive elements | Bounding boxes | UI layout metrics | Enforce via Compose Modifier.sizeIn | Acceptance Criteria in ORIGINAL_REQUEST.md |
| 6 | R1: UI/UX | Ultra-Fast Click Budget | Set logging completed in $\le 3$ screens and $\le 4$ clicks | Set data entry | Saved set record | Inline active set card ensures 1 screen / 4 clicks | Acceptance Criteria in ORIGINAL_REQUEST.md |
| 7 | R1: Timer | Set Rest Timer | Auto-running countdown timer after saving a set | Trigger event, set completion | 90s countdown, UI ticker | Pausable, editable (+30s / -30s) | R1 in ORIGINAL_REQUEST.md |
| 8 | R1: Timer | Exercise Rest Timer | Rest timer between switching exercises | Exercise completion event | 180s countdown, UI ticker | Fallback to Exercise.defaultRestTime | R1 in ORIGINAL_REQUEST.md |
| 9 | R1: Timer | Background Notification & Vibration | Notification and vibration pattern upon timer reaching 0 | Timer expire event | System vibration + Heads-up notification | Gracefully check VIBRATE permission | R1 in ORIGINAL_REQUEST.md |
| 10 | R3: Algorithm | High Effort Progression ($\Delta = 0.05$) | 5% weight bump when $R_{actual} \ge R_{target}$ & $RIR \in [0, 1]$ | $W_{prev}, R_{actual}, RIR$ | $W_{next} = \text{round}(W_{prev} \times 1.05)$ | Quantize to inventory step $S_{min}$ | R3 in ORIGINAL_REQUEST.md |
| 11 | R3: Algorithm | Moderate Effort Progression ($\Delta = 0.02$) | 2% bump or rep increase recommendation when $RIR \ge 2$ | $W_{prev}, R_{actual}, RIR$ | $W_{next} = \text{round}(W_{prev} \times 1.02)$ or $R_{rec} = R + 1$ | If $\Delta$ swallowed by step, recommend $+1$ rep | R3 in ORIGINAL_REQUEST.md |
| 12 | R3: Algorithm | Hold / Deload Logic | Hold weight $W_{next} = W_{prev}$ when plan not met | $R_{actual} < R_{target}$ | $W_{next} = W_{prev}, \Delta = 0.0$ | Optional deload if consecutive failures | R3 in ORIGINAL_REQUEST.md |
| 13 | R3: Algorithm | Inventory Step Rounding | Quantize raw weight to nearest $S_{min}$ (e.g. 1.25, 2.5 kg) | $W_{raw}, S_{min}$ | $W_{rounded} = \text{round}(W_{raw}/S_{min}) \times S_{min}$ | Guard against $S_{min} \le 0$ | R3 in ORIGINAL_REQUEST.md |
| 14 | R3: Algorithm | Per-Exercise ProgressConfig | Custom $S_{min}$, $\Delta_{heavy}$, $\Delta_{mod}$, $R_{target}$ | Exercise configuration | Tailored calculation | Defaults: $S_{min}=2.5$, $\Delta_H=0.05$, $\Delta_M=0.02$ | R3 & Data Model in ORIGINAL_REQUEST.md |
| 15 | R3: Algorithm | Placeholder & Manual Override | Recommended weight shown as placeholder; editable by user | $W_{next}$ calculation | UI text placeholder | User input takes precedence | R3 in ORIGINAL_REQUEST.md |

---

## 4. Edge Cases

| # | Feature | Input | Observed / Required Behavior |
|---|---------|-------|------------------------------|
| 1 | Progression Engine | $W_{prev} = 0.0$ kg (Bodyweight), $R_{actual} \ge R_{target}$ | $0 \times 1.05 = 0.0$. Progression transitions to adding first inventory plate (e.g. $+2.5$ kg on belt) or recommending reps increase ($R_{target} + 1$). |
| 2 | Progression Engine | $R_{actual} = 0$ (Complete failure / 0 reps) | Plan not met $\implies \Delta = 0.0$, hold weight $W_{next} = W_{prev}$. No crash, no division by zero. |
| 3 | Progression Engine | $S_{min} \le 0.0$ (Invalid configuration) | Guard clause clamps $S_{min}$ to default $2.5$ kg or performs standard 2-decimal rounding. |
| 4 | Progression Engine | $W_{prev} = 52.6$ kg, $S_{min} = 2.5$ kg, $\Delta = 0.0$ | $52.6 / 2.5 = 21.04 \to \text{round}(21.04) = 21 \to 21 \times 2.5 = 52.5$ kg. Correctly rounded to nearest plate step. |
| 5 | Progression Engine | Small Delta with Large Step: $W_{prev} = 20.0$ kg, $\Delta = 0.02$, $S_{min} = 2.5$ kg | $20.0 \times 1.02 = 20.4 \implies \text{round}(20.4/2.5) \times 2.5 = 20.0$ kg. System detects deadband and recommends $+1$ repetition. |
| 6 | Progression Engine | Floating-point IEEE 754 precision: $W = 52.5$ kg | Avoids `52.50000000000001` by using `BigDecimal.setScale(2, HALF_UP)` or `String.format(Locale.US, "%.1f", weight)`. |
| 7 | Weight Entry | Multiple decimal points typed: `5.2.5` | Keypad sanitizes input: rejects subsequent decimal points after the first. |
| 8 | Weight Entry | User clears input field and saves immediately | Field is empty $\to$ System uses placeholder recommended weight $W_{rec}$ without failing. |
| 9 | Reps Entry | Negative or non-numeric input pasted | Sanitizer clamps to integer $\ge 0$. |
| 10 | RIR Slider | Fractional or out-of-bounds RIR value | Slider is discrete with 6 discrete values $\{0, 1, 2, 3, 4, 5\}$, impossible to select out-of-bounds. |
| 11 | Rest Timer | App sent to background while timer is running | Foreground service continues countdown; sends push notification & vibration when timer reaches 0. |
| 12 | Rest Timer | User starts next set before timer finishes | Timer is automatically stopped/reset upon starting next set without duplicate notifications. |

---

## 5. Caveats
1. **Bodyweight Scaling**: Pure multiplicative progression ($W \times (1+\Delta)$) produces zero for bodyweight exercises ($0.0$ kg). The system must detect `isBodyweight == true` or `weightKg == 0.0` and transition to rep progression or additive step progression.
2. **Quantization Deadband**: For light weights (e.g. dumbbells $<25$ kg), a $2\%$ increase is smaller than half a plate ($1.25$ kg). The algorithm must pair weight progression with rep progression recommendations.
3. **Android Vibration Permissions**: On Android 13+ (API 33+), `POST_NOTIFICATIONS` runtime permission is required for timer notifications. The app must request this permission gracefully and fallback to in-app audio/vibration if denied.

---

## 6. Conclusion
Requirements R1 and R3 have been fully specified down to mathematical equations, Room database schema, UI click budget, state machines, and edge case handlers.
- **R1**: Satisfies 1 screen / 4 clicks maximum, touch targets $\ge 48$ dp, 90s/180s background timer with vibration.
- **R3**: Satisfies all 3 delta branches ($\Delta=0.05$, $\Delta=0.02$, $\Delta=0.0$), inventory quantization rounding, and bodyweight/zero-rep edge cases.

---

## 7. Verification Method
1. **Progression Unit Tests**:
   - Test Branch 1: $W=100.0, R=8, R_{target}=8, RIR=1, S_{min}=2.5 \implies W_{next}=105.0$ kg ($\Delta = +0.05$).
   - Test Branch 2: $W=100.0, R=8, R_{target}=8, RIR=3, S_{min}=2.5 \implies W_{next}=102.5$ kg ($\Delta = +0.02$, rounded $102.0 \to 102.5$).
   - Test Branch 3: $W=100.0, R=6, R_{target}=8, RIR=0, S_{min}=2.5 \implies W_{next}=100.0$ kg ($\Delta = 0.0$, hold).
   - Test Inventory Rounding: $W=52.6, S_{min}=2.5 \implies 52.5$ kg.
   - Test Deadband Rep Recommendation: $W=20.0, R=8, RIR=2, S_{min}=2.5 \implies W_{next}=20.0$ kg, $R_{rec}=9$.
   - Test Bodyweight Zero: $W=0.0, R=8, R_{target}=8 \implies W_{next}=2.5$ kg or $R_{rec}=9$.
2. **UI & UX Verification**:
   - Jetpack Compose preview tests measuring touch target bounding boxes ($\ge 48$ dp).
   - UI interaction flow test logging a complete set in $\le 4$ clicks.
