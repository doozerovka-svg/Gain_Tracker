# 🏋️‍♂️ ТРЕКЕР ТРЕНИРОВОК (Workout Tracker)

> **Автономное мобильное приложение для силовых тренировок и интерактивная веб-версия (Local-First, No-AI, 100% русский интерфейс).**

[![Android Build](https://img.shields.io/badge/Android-SDK_36_%7C_Kotlin_2.3-green.svg)](https://developer.android.com/)
[![Web App](https://img.shields.io/badge/Web_Version-React_19_+_Vite_+_Tailwind-blue.svg)](https://doozerovka-svg.github.io/Gain_Tracker/)
[![Local-First](https://img.shields.io/badge/Architecture-Local--First_Offline-orange.svg)](#)
[![Tests](https://img.shields.io/badge/Tests-100%25_Passing-brightgreen.svg)](#)

---

## 🚀 Как скачать APK и открыть веб-версию

### 1. 📱 Скачать установочный APK на Android-смартфон
- 📥 **[Скачать workout-tracker.apk из Releases (Последний релиз)](https://github.com/doozerovka-svg/Gain_Tracker/releases/latest)**
- 📥 **[Прямая загрузка workout-tracker.apk (19.9 МБ)](https://raw.githubusercontent.com/doozerovka-svg/Gain_Tracker/main/docs/workout-tracker.apk)**
- 📦 **[Скачать из GitHub Actions Artifacts](https://github.com/doozerovka-svg/Gain_Tracker/actions)**

### 2. 🌐 Веб-версия в браузере (PWA)
- 🔗 **[Открыть трекер в браузере](https://doozerovka-svg.github.io/Gain_Tracker/)**
*(Для первой активации в репозитории: Settings → Pages → Deploy from branch `gh-pages` → Save)*.

---

## ⚡ Ключевой функционал

### 1. ⏱ Компактный интерфейс «Zero-Scroll» и таймер отдыха
- **Все ключевые элементы на одном экране без вертикальной прокрутки**:
  - Двухколоночный ввод: вес (степпер, быстрые чипы `+1`, `+2.5`, `+5`, `+10` кг) слева и повторения (`[-] [+]`, `6`, `8`, `10`, `12`) справа.
  - Сегментированная горизонтальная шкала RIR: `0: Отказ`, `1: Предел`, `2: Рабочий`, `3: Запас`, `4: Легко`, `5+: Разминка`.
  - Зона нажатия главной кнопки $\ge 48\times 48\text{ dp/px}$.
  - Таймер отдыха (обратный отсчет, звуковые сигналы Web Audio и вибрация).

### 2. 🧠 Авторегуляция и двойная прогрессия (No-AI)
Детерминированная математическая модель подбора нагрузки без нейросетей и внешних API:
- **Перевыполнение с запасом ($RIR \ge 4$, $\Delta_{reps} \ge +2$)**: прирост веса $+7.5\% .. +10\%$ (или минимум $+2$ шага инвентаря, $+5$ кг).
- **Выполнение с уверенным запасом ($RIR \ge 3$)**: стандартный прирост $+5\%$ (минимум $+1$ шаг, $+2.5$ кг).
- **Точное попадание в цель ($RIR \in [2, 3]$)**: **Двойная прогрессия** — удержание веса и рекомендация добавить $+1$ повторение.
- **Предел на плане ($RIR = 1$)**: шаг веса $+2.5$ кг.
- **Отказ на плане ($RIR = 0$) / Недобор повторений**: удержание веса для закрепления техники.
- **Глубокий срыв ($<70\%$ плана при $RIR = 0$)**: автоматический Deload ($-10\%$).

### 3. 📅 Календарь, клонирование и автоподстановка
- Месячный и недельный вид с цветовой индикацией статуса тренировок.
- Клонирование структуры упражнений и подходов в любой день.
- Автозаполнение веса и повторений из истории.

### 4. 📊 Аналитика и экспорт данных
- Двухосевые графики 1RM (Эпли и Бжицки) и рабочего тоннажа.
- Экспорт в **Excel (.xlsx)** и **PDF отчет формата A4**.

---

## 🛠 Технологический стек

### Android (`app/`)
- **Язык**: Kotlin 2.3.20, Java 17
- **UI**: Jetpack Compose, Material 3
- **БД**: Room 2.7 (SQLite, Local-First, `room.generateKotlin = true`)
- **Архитектура**: Clean Architecture + MVVM + MVI Flow

### Web / PWA (`web/`)
- **Стек**: React 19, TypeScript, Vite 8, Tailwind CSS v4, Lucide Icons
- **Хранилище**: LocalStorage Local-First DB
- **Экспорт**: SheetJS (XLSX), jsPDF + html2canvas (PDF)
