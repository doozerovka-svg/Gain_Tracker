# Handoff Report: Specification Mining for R2 & R4

## 1. Observation

Direct observations extracted from `ORIGINAL_REQUEST.md`, domain fitness literature, Android Platform SDK specifications (`android.graphics.pdf.PdfDocument`, SQLite/Room, Jetpack Compose, OpenXML/XLSX), and mathematical formulas:

1. **Requirement R2 (Calendar, Session Cloning & Auto-population)**:
   - Calendar UI requires Month and Week views with color indicators for completed workouts (`ORIGINAL_REQUEST.md`, line 18).
   - Session cloning must copy any historical workout session to a target date with all exercises, exact order of exercises, and exact number of sets (`ORIGINAL_REQUEST.md`, lines 18, 48).
   - Auto-population must automatically pre-fill target weight and target reps from the last completed set of that exercise in history when an exercise is added to a workout (`ORIGINAL_REQUEST.md`, lines 18, 49).
   - If no history exists for an exercise, fields must remain empty with zero crashes and no errors (`ORIGINAL_REQUEST.md`, line 50).
2. **Requirement R4 (Analytics, Export & Localization)**:
   - Progress charts require X-axis = Date, Y1-axis = Estimated 1RM (1 Rep Max) using Epley and Brzycki formulas, Y2-axis = Working Weight (`ORIGINAL_REQUEST.md`, line 29).
   - Epley formula: $\text{1RM} = W \times (1 + R / 30)$
   - Brzycki formula: $\text{1RM} = W \times (36 / (37 - R))$
   - Excel export (.xlsx): Complete export of all sessions, exercises, sets. Must be generated 100% offline and locally without external network APIs (`ORIGINAL_REQUEST.md`, lines 29, 59).
   - PDF export: Formatted summary report for a user-selected date period. Generated 100% offline and locally (`ORIGINAL_REQUEST.md`, lines 29, 60).
   - 100% Russian localization for all UI, chart labels, dates, table headers, and PDF text (`ORIGINAL_REQUEST.md`, lines 7, 72).
3. **Data Schema Entities**:
   - `Exercise`: `id` (UUID/Long), `name` (String), `category_id` (Long), `default_rest_time` (Int seconds).
   - `WorkoutSession`: `id` (UUID/Long), `date` (Long/String timestamp), `status` (`draft` | `completed`), `notes` (String?).
   - `SetEntry`: `id` (UUID/Long), `workout_session_id` (Long), `exercise_id` (Long), `set_number` (Int), `weight_kg` (Double), `reps` (Int), `rir` (Int 0..5), `timestamp` (Long).
   - `ProgressConfig`: `exercise_id` (Long), `min_step_kg` (Double), `progression_percent` (Double).

---

## 2. Logic Chain

### 2.1 Formal Specification: Requirement R2 (Calendar, Cloning & Auto-population)

```
+-----------------------------------------------------------------------------------+
|                                  R2 ARCHITECTURE                                  |
+-----------------------------------------------------------------------------------+
|                                                                                   |
|  +------------------------+      +---------------------+      +----------------+  |
|  |     Calendar View      |      |   Session Cloning   |      | Auto-populate  |  |
|  |  (Month / Week Toggle) |      |       Engine        |      |     Engine     |  |
|  +-----------+------------+      +----------+----------+      +--------+-------+  |
|              |                              |                          |          |
|              v                              v                          v          |
|     Room Database: Query           Room Database: Copy        Room Database: Last |
|   Workouts by Date Range          Session & Sets to Date       Completed Set Query|
+-----------------------------------------------------------------------------------+
```

#### A. Calendar Interface & State Machine
1. **View Modes**:
   - `CalendarViewMode.MONTH`: 7x5 or 7x6 day grid (Пн, Вт, Ср, Чт, Пт, Сб, Вс), starting on Monday (`FirstDayOfWeek.MONDAY` for Russian locale).
   - `CalendarViewMode.WEEK`: 7-day horizontal scroll/paging bar or list showing current week days.
2. **Visual Badges & Color Indicators**:
   - `Status.COMPLETED`: Solid colored dot / chip badge (Accent Green / Primary, e.g. `#10B981` / `#3B82F6`).
   - `Status.DRAFT`: Outline or warning dot (Amber, e.g. `#F59E0B`).
   - `Current Day (Сегодня)`: Circular border highlight / distinctive ring.
   - `Selected Day`: Full highlight background.
   - `Multiple Sessions on Day`: Number badge or double dots indicating $N$ workouts.
3. **Selected Day Details Panel**:
   - When a user taps a calendar day $D$:
     - If sessions exist: Displays session cards showing Session Name/Time, Completed badge, Total Volume (кг), Total Sets count, Exercise pills. Actions: "Открыть" (Open), "Клонировать" (Clone), "Удалить" (Delete).
     - If no sessions exist: Display "Нет тренировок на этот день" with a prominent action "Создать тренировку" or "Клонировать прошлую".

#### B. Session Cloning Engine
1. **Algorithm (`cloneWorkoutSession(sourceSessionId: Long, targetDate: Long)`):**
   - **Step 1**: Load `sourceSession = workoutSessionDao.getById(sourceSessionId)` and `sourceSets = setEntryDao.getBySessionId(sourceSessionId)`.
   - **Step 2**: Verify source exists; if not, throw `SessionNotFoundException`.
   - **Step 3**: Create new `WorkoutSession`:
     - `id = 0` (or new UUID)
     - `date = targetDate`
     - `status = WorkoutStatus.DRAFT` (always created as draft, never completed)
     - `notes = sourceSession.notes` (or empty / prefixed "Копия от ДД.ММ.ГГГГ")
   - **Step 4**: Insert new session into DB: `newSessionId = workoutSessionDao.insert(newSession)`.
   - **Step 5**: Group `sourceSets` by `exercise_id` preserving original `orderIndex` and `set_number`.
   - **Step 6**: For each set in `sourceSets`:
     - Create new `SetEntry`:
       - `id = 0` (new ID)
       - `workout_session_id = newSessionId`
       - `exercise_id = set.exercise_id`
       - `set_number = set.set_number`
       - `weight_kg = set.weight_kg` (pre-filled from source)
       - `reps = set.reps` (pre-filled from source)
       - `rir = set.rir` (or default 2 for draft)
       - `timestamp = targetDate`
   - **Step 7**: Batch insert sets: `setEntryDao.insertAll(newSets)`.
   - **Step 8**: Return `newSessionId` and navigate to active workout screen or calendar.

#### C. Auto-Population Engine
1. **Algorithm (`getLastCompletedSet(exerciseId: Long, beforeTimestamp: Long)`):**
   - **SQL Query**:
     ```sql
     SELECT s.* FROM SetEntry s
     INNER JOIN WorkoutSession w ON s.workout_session_id = w.id
     WHERE s.exercise_id = :exerciseId
       AND w.status = 'COMPLETED'
       AND w.date <= :beforeTimestamp
     ORDER BY w.date DESC, s.set_number DESC
     LIMIT 1;
     ```
   - **Auto-population logic on Adding Exercise / Set**:
     - Call `getLastCompletedSet(exerciseId, currentSession.date)`.
     - If result $S_{last}$ is found:
       - Default Target Weight = $S_{last}\text{.weight\_kg}$
       - Default Target Reps = $S_{last}\text{.reps}$
       - UI displays placeholder / pre-fill with hint "Прошлый раз: {weight} кг × {reps}".
     - If result is `null` (no previous completed sets found):
       - Target Weight = `null` (empty input field)
       - Target Reps = `null` (empty input field)
       - UI displays placeholder "Введите вес" / "Повторения".
       - Graceful execution: Zero exceptions, no error banners.

---

### 2.2 Formal Specification: Requirement R4 (Analytics, Export & Russian Localization)

#### A. Analytics & 1RM Progression Curves
1. **Mathematical Definitions**:
   - **Epley 1RM Formula**:
     $$\text{1RM}_{\text{Epley}}(W, R) = \begin{cases} 0, & \text{if } W = 0 \text{ or } R = 0 \\ W, & \text{if } R = 1 \\ W \times \left(1 + \frac{R}{30}\right), & \text{if } R > 1 \end{cases}$$
   - **Brzycki 1RM Formula**:
     $$\text{1RM}_{\text{Brzycki}}(W, R) = \begin{cases} 0, & \text{if } W = 0 \text{ or } R = 0 \\ W, & \text{if } R = 1 \\ W \times \left(\frac{36}{37 - \min(R, 36)}\right), & \text{if } 1 < R \le 36 \\ W \times 36, & \text{if } R \ge 37 \text{ (guard constraint)} \end{cases}$$
2. **Chart Series Aggregation per Exercise over Time**:
   - For a given `exerciseId` and date range $[T_{start}, T_{end}]$:
   - Query all completed sets:
     ```sql
     SELECT w.date as session_date, s.weight_kg, s.reps, s.rir
     FROM SetEntry s
     JOIN WorkoutSession w ON s.workout_session_id = w.id
     WHERE s.exercise_id = :exerciseId AND w.status = 'COMPLETED'
       AND w.date BETWEEN :startDate AND :endDate
     ORDER BY w.date ASC;
     ```
   - For each distinct date $D_k$:
     - $\text{MaxWorkingWeight}(D_k) = \max_{j \in \text{sets}(D_k)} (W_j)$
     - $\text{Best1RM}_{\text{Epley}}(D_k) = \max_{j \in \text{sets}(D_k)} (\text{1RM}_{\text{Epley}}(W_j, R_j))$
     - $\text{Best1RM}_{\text{Brzycki}}(D_k) = \max_{j \in \text{sets}(D_k)} (\text{1RM}_{\text{Brzycki}}(W_j, R_j))$
   - Render Dual-Axis Chart:
     - X-Axis: Date ($D_k$) formatted as "дд.ММ" (e.g. `12.08`)
     - Left Y-Axis (Y1): Расчётный 1ПМ (кг) [Epley/Brzycki curve]
     - Right Y-Axis (Y2): Максимальный рабочий вес (кг) [Working weight curve]

#### B. Local Offline Excel Export (.xlsx)
1. **Architecture & Zero-Network Implementation**:
   - Export is executed on a background coroutine (`Dispatchers.IO`).
   - Uses a pure local OpenXML / Apache POI / fast XLSX writer to write directly to a `FileOutputStream` / SAF URI.
   - File Name: `Дневник_тренировок_ГГГГ-ММ-ДД.xlsx`.
2. **Workbook Structure**:
   - **Sheet 1: "Журнал подходов" (Detailed Set Log)**
     - Column A: `ID Сессии` (Integer)
     - Column B: `Дата` (`ДД.ММ.ГГГГ`)
     - Column C: `Упражнение` (String)
     - Column D: `Категория` (String)
     - Column E: `Подход №` (Integer)
     - Column F: `Вес (кг)` (Decimal 0.00)
     - Column G: `Повторения` (Integer)
     - Column H: `RIR (в запасе)` (Integer)
     - Column I: `1ПМ Эпли (кг)` (Decimal 0.00)
     - Column J: `1ПМ Бжицки (кг)` (Decimal 0.00)
     - Column K: `Тоннаж (кг)` (Formula: `=F{row}*G{row}`)
     - Column L: `Заметки` (String)
   - **Sheet 2: "Сводка упражнений" (Exercise Summary)**
     - Column A: `Упражнение`
     - Column B: `Всего подходов`
     - Column C: `Макс. рабочий вес (кг)`
     - Column D: `Рекорд 1ПМ (кг)`
     - Column E: `Общий тоннаж (кг)`
     - Column F: `Дата рекорда`

#### C. Local Offline PDF Report Generator
1. **Architecture & Rendering Pipeline**:
   - Uses Android Native `android.graphics.pdf.PdfDocument` + `Canvas` / `TextPaint`.
   - Page dimensions: Standard A4 ($595 \times 842$ pt at 72 dpi).
   - Multi-page pagination engine with dynamic height calculation to avoid clipping rows.
   - 100% offline generation, saving to `MediaStore.Downloads` or App storage.
2. **Visual Layout Specification**:
   ```
   +---------------------------------------------------------------+
   |                      ДНЕВНИК ТРЕНИРОВОК                       |
   |              Отчёт за период: 01.08.2026 — 19.08.2026         |
   |              Сформирован: 19.08.2026 21:30                    |
   +---------------------------------------------------------------+
   |  [ Всего тренировок: 12 ]    [ Общий тоннаж: 42 500 кг ]     |
   |  [ Всего подходов: 148 ]     [ Средняя част.: 3.2 / нед ]    |
   +---------------------------------------------------------------+
   | ЛИЧНЫЕ РЕКОРДЫ В УПРАЖНЕНИЯХ                                 |
   | Упражнение        | Макс. вес | Рекорд 1ПМ (Эпли) | Дата      |
   | Жим лёжа          | 100.0 кг  | 116.7 кг          | 15.08.2026|
   | Приседания        | 140.0 кг  | 163.3 кг          | 12.08.2026|
   | Становая тяга     | 170.0 кг  | 192.6 кг          | 08.08.2026|
   +---------------------------------------------------------------+
   | ДЕТАЛЬНЫЙ ЖУРНАЛ ТРЕНИРОВОК                                   |
   | ------------------------------------------------------------- |
   | Тренировка: 19.08.2026 (Среда) — Тяга / Спина                 |
   | Упражнение        | Сет | Вес (кг) | Повт. | RIR | 1ПМ (кг)  |
   | Подтягивания      | 1   | 0.0      | 12    | 2   | 0.0       |
   | Тяга штанги в накл| 1   | 70.0     | 10    | 1   | 93.3      |
   | Тяга штанги в накл| 2   | 75.0     | 8     | 0   | 95.0      |
   +---------------------------------------------------------------+
   | Стр. 1 из 3                            Workout Tracker Android|
   +---------------------------------------------------------------+
   ```

#### D. Russian Localization Dictionary (100% Coverage)
1. **Core UI Strings**:
   - `app_name` = "Трекер Тренировок"
   - `tab_workout` = "Тренировка"
   - `tab_calendar` = "Календарь"
   - `tab_analytics` = "Аналитика"
   - `tab_settings` = "Настройки"
2. **Calendar Strings**:
   - `calendar_view_month` = "Месяц"
   - `calendar_view_week` = "Неделя"
   - `calendar_no_workouts` = "Нет тренировок"
   - `calendar_start_workout` = "Начать тренировку"
   - `calendar_clone_session` = "Клонировать тренировку"
   - `calendar_delete_session` = "Удалить тренировку"
   - `calendar_dialog_clone_title` = "Клонирование тренировки"
   - `calendar_dialog_clone_msg` = "Выберите дату для переноса упражнений и подходов:"
   - `calendar_clone_success` = "Тренировка успешно скопирована"
3. **Analytics Strings**:
   - `analytics_title` = "Аналитика прогресса"
   - `analytics_select_exercise` = "Выберите упражнение"
   - `analytics_period_1m` = "1 мес"
   - `analytics_period_3m` = "3 мес"
   - `analytics_period_6m` = "6 мес"
   - `analytics_period_1y` = "1 год"
   - `analytics_period_all` = "Всё время"
   - `analytics_axis_date` = "Дата"
   - `analytics_axis_1rm` = "Расчётный 1ПМ (кг)"
   - `analytics_axis_weight` = "Рабочий вес (кг)"
   - `analytics_formula_epley` = "Формула Эпли"
   - `analytics_formula_brzycki` = "Формула Бжицки"
   - `analytics_empty_chart` = "Недостаточно данных для графика. Завершите хотя бы одну тренировку с этим упражнением."
4. **Export Strings**:
   - `export_title` = "Экспорт данных"
   - `export_excel_btn` = "Экспорт в Excel (.xlsx)"
   - `export_pdf_btn` = "Сформировать PDF-отчёт"
   - `export_period_select` = "Период отчёта"
   - `export_generating` = "Формирование файла..."
   - `export_success_excel` = "Файл Excel успешно сохранён"
   - `export_success_pdf` = "PDF-отчёт успешно сформирован"
   - `export_share_prompt` = "Открыть или отправить файл"
   - `export_error` = "Ошибка при создании файла"

---

## 3. Discovered Features & Full Specification Matrix

### Features Discovered
| # | Category | Feature | Description | Inputs | Outputs | Error Behavior | Discovered Via |
|---|----------|---------|-------------|--------|---------|----------------|----------------|
| 1 | R2 Calendar | Calendar Month View Grid | 7-column monthly grid with Monday start and Russian weekday headers | Month, Year, WorkoutSession list | Rendered Compose Grid with badge dots | Fallback to current month on invalid date | ORIGINAL_REQUEST.md line 18 |
| 2 | R2 Calendar | Calendar Week Strip View | Horizontal 7-day strip for quick day-to-day switching | Active Week Date, WorkoutSession list | Rendered week strip | Scroll to current day | ORIGINAL_REQUEST.md line 18 |
| 3 | R2 Calendar | Workout Status Visual Badges | Green badge for completed sessions, amber for draft sessions, ring for today | Session status (`DRAFT`, `COMPLETED`) | Visual indicator on calendar day cell | Default to neutral style if session unknown | ORIGINAL_REQUEST.md line 18 |
| 4 | R2 Calendar | Day Workout Summary Card | Detailed list of sessions for selected day with total volume & sets | Selected date, sessions query | UI Card with summary statistics | Shows "Нет тренировок" when empty | ORIGINAL_REQUEST.md line 18 |
| 5 | R2 Cloning | Historical Session Cloning | Clones past workout session with all exercises, exact order, and set counts to target date | `sourceSessionId: Long`, `targetDate: Long` | New `WorkoutSession` (status `DRAFT`) with duplicate `SetEntry` list | Throws `SessionNotFoundException` if source invalid | ORIGINAL_REQUEST.md line 18, 48 |
| 6 | R2 Cloning | Target Date Picker Dialog | Native date picker to select destination date for cloned session | Default target date (today) | Selected `targetDate` timestamp | User cancellation aborts cloning safely | ORIGINAL_REQUEST.md line 18, 48 |
| 7 | R2 Auto-population | Last Set History Query | Queries DB for last completed set of an exercise prior to current date | `exerciseId: Long`, `beforeDate: Long` | Last `SetEntry` (`weight_kg`, `reps`) or `null` | Returns `null` safely without throwing | ORIGINAL_REQUEST.md line 18, 49 |
| 8 | R2 Auto-population | Target Weight & Reps Pre-fill | Auto-fills input fields with last completed weight and reps when exercise/set is added | `lastSet: SetEntry?` | Input fields populated with previous values | Leaves fields empty if `lastSet == null` | ORIGINAL_REQUEST.md line 18, 50 |
| 9 | R4 Analytics | Epley 1RM Calculation | Computes estimated 1 Rep Max via Epley formula: $W \times (1 + R / 30)$ | `weight_kg: Double`, `reps: Int` | `est1RM: Double` | Returns `0.0` if $W \le 0$ or $R \le 0$; $W$ if $R = 1$ | ORIGINAL_REQUEST.md line 29 |
| 10 | R4 Analytics | Brzycki 1RM Calculation | Computes estimated 1 Rep Max via Brzycki formula: $W \times (36 / (37 - R))$ | `weight_kg: Double`, `reps: Int` | `est1RM: Double` | Caps $R$ at 36 to prevent division by zero / negative 1RM | ORIGINAL_REQUEST.md line 29 |
| 11 | R4 Analytics | Dual-Axis Progress Chart | Interactive chart: X = Date, Y1 = 1RM curve, Y2 = Max Working Weight curve | `exerciseId`, `timeRange`, formula type | Compose Canvas / MPAndroidChart dual series | Displays "Недостаточно данных" if 0 entries | ORIGINAL_REQUEST.md line 29 |
| 12 | R4 Analytics | Time Range Filter | Filter chart data by 1 month, 3 months, 6 months, 1 year, or All time | Range enum selection | Filtered dataset | Defaults to All time if range has no data | ORIGINAL_REQUEST.md line 29 |
| 13 | R4 Export | Offline Excel (.xlsx) Generation | Generates valid .xlsx file containing full sessions, sets, and exercise summary tables | All DB sessions and sets | `.xlsx` file written to local storage/SAF | Emits error toast and logs if disk full | ORIGINAL_REQUEST.md line 29, 59 |
| 14 | R4 Export | Offline PDF Report Generation | Generates multi-page formatted A4 PDF summary report with metrics and session logs | Date range $[T_1, T_2]$, DB records | `.pdf` file written to local storage/SAF | Safe canvas pagination, no text clipping | ORIGINAL_REQUEST.md line 29, 60 |
| 15 | R4 Localization | 100% Russian Localization | Comprehensive Russian resource strings for all UI, dates, charts, headers, and exports | Locale `ru_RU` | Localized UI and exported document text | Default fallback to standard Russian strings | ORIGINAL_REQUEST.md line 7, 72 |
| 16 | R4 Export | Local File Sharing Intent | Android `Intent.ACTION_SEND` with `FileProvider` URI for sharing exported .xlsx/.pdf | Generated `File` path | System Share Sheet for Telegram, Email, Files | Traps `ActivityNotFoundException` | Android Platform Best Practice |

---

## 4. Edge Cases & Boundary Handling

### Edge Cases
| # | Feature | Input / Scenario | Observed / Specified Behavior |
|---|---------|------------------|-------------------------------|
| 1 | R2 Auto-population | Exercise added with zero historical completed workouts | Auto-population query returns `null`. Input fields remain empty. No crash, no red error banner. User enters values manually. |
| 2 | R2 Auto-population | Previous workout exists but had 0 kg weight (bodyweight exercise) | Pre-fills `weight_kg = 0.0` and reps from previous set. Displays placeholder "0.0 кг". |
| 3 | R2 Auto-population | Workout history was deleted by user | Query returns `null`. Graceful fallback to empty fields. |
| 4 | R2 Session Cloning | Cloning a session that contains 0 sets / 0 exercises | Creates an empty draft session on target date without error. |
| 5 | R2 Session Cloning | Cloning a session to the same day as the original | Creates a new distinct session with status `DRAFT` and new UUIDs. Original session is untouched. |
| 6 | R2 Session Cloning | Cloning a draft (uncompleted) session | Allowed. Clones all draft sets into the new draft session on target date. |
| 7 | R2 Calendar | User navigates to year 2099 or 1970 | Calendar calculates day grid deterministically using `java.time.YearMonth`. No overflow. |
| 8 | R2 Calendar | Multiple workouts on a single calendar day | Day cell displays count badge (e.g. "2"). Details card lists both sessions with start times. |
| 9 | R4 Analytics | Brzycki formula with $R \ge 37$ (division by zero / negative) | Formula contains guard: if $R \ge 37$, returns $W \times 36$ or caps $R=36$. No arithmetic exceptions. |
| 10 | R4 Analytics | Reps = 0 or Weight = 0 | 1RM calculation returns `0.0`. Working weight returns `0.0`. Chart plots `0.0` or skips zero-rep sets. |
| 11 | R4 Analytics | Single data point recorded for exercise | Chart displays a single distinct point with value tooltip. Does not crash on zero line segment. |
| 12 | R4 Analytics | Multiple sessions with the same exercise on the same date | Aggregator selects the highest 1RM and highest working weight for that date point. |
| 13 | R4 Excel Export | Huge dataset (10,000+ sets over several years) | Export runs in background IO thread with streaming rows. Displays progress bar and prevents UI freeze. |
| 14 | R4 PDF Export | Multi-page overflow (50+ workouts in period) | Pagination engine calculates row heights, breaks cleanly across page boundaries, and updates page numbers ("Стр. 1 из 4"). |
| 15 | R4 PDF Export | Russian Cyrillic characters rendering | Uses standard Android `Typeface` (Roboto / Sans-serif) which natively renders Cyrillic glyphs without character corruption (`???`). |
| 16 | R4 Export | Offline mode / Airplane mode | 100% offline generation using local file system. Zero network calls; operations succeed completely in airplane mode. |

---

## 5. Caveats

1. **Storage Permissions & Scoped Storage on Android 10+ (API 29+)**:
   - Modern Android versions require saving exports to public directories via `MediaStore.Downloads` or using the Storage Access Framework (`Intent.ACTION_CREATE_DOCUMENT`) / `FileProvider` cache to avoid requiring legacy `WRITE_EXTERNAL_STORAGE` permission.
2. **Chart Library Choice**:
   - Native Jetpack Compose Canvas charting avoids heavy external dependencies like MPAndroidChart, but if MPAndroidChart is used, Compose AndroidView interop is required. Both approaches are fully compatible with the specified data models.
3. **1RM Accuracy for High Reps**:
   - In exercise science, 1RM formulas (both Epley and Brzycki) are most accurate in the 1–10 rep range. While our formulas calculate deterministically for any integer $R$, the UI may include an optional informative hint for $R > 15$.

---

## 6. Conclusion

Requirements R2 and R4 have been thoroughly mined, analyzed, and specified down to exact mathematical formulas, Room SQL queries, domain logic flows, UI states, export schemas (both .xlsx and .pdf), and 100% Russian localization strings. All edge cases (zero history, division by zero in Brzycki, multiple sessions per day, scoped storage) have been addressed with deterministic guard conditions.

The engineering team can immediately use this specification to implement:
- Room DAOs and database migrations for R2 & R4 queries.
- Jetpack Compose Calendar and Day Details views.
- Session cloning and auto-population domain use cases.
- 1RM calculation utilities and interactive dual-axis chart components.
- Offline Excel (.xlsx) and PDF report generators with Russian typography.

---

## 7. Verification Method

To independently verify these specifications:
1. **Mathematical Verification**:
   - Epley: $W = 100\text{ kg}, R = 10 \implies 100 \times (1 + 10/30) = 133.33\text{ kg}$.
   - Epley: $W = 100\text{ kg}, R = 1 \implies 100.0\text{ kg}$.
   - Brzycki: $W = 100\text{ kg}, R = 10 \implies 100 \times (36 / 27) = 133.33\text{ kg}$.
   - Brzycki: $W = 100\text{ kg}, R = 37 \implies$ Guard triggers, returns $3600.0\text{ kg}$ (no division by zero).
2. **SQL Query Verification**:
   - Verify `getLastCompletedSet` returns the single latest row from completed sessions before the specified date.
3. **Export Verification**:
   - Generated `.xlsx` files can be validated with Apache POI / Microsoft Excel / LibreOffice Calc.
   - Generated `.pdf` files can be validated with Android `PdfRenderer` and standard PDF viewers.
4. **Localization Verification**:
   - Check `res/values/strings.xml` and verify all string keys contain valid Russian Cyrillic text with correct grammatical pluralization.
