import React, { useState, useMemo } from 'react';
import { AppDatabase } from '../db';
import type { Exercise } from '../types';
import { FileSpreadsheet, FileText, Download, Calendar, Smartphone, CheckCircle, AlertCircle } from 'lucide-react';
import * as XLSX from 'xlsx';
import { jsPDF } from 'jspdf';
import autoTable from 'jspdf-autotable';

export const ExportTab: React.FC = () => {
  const [startDateStr, setStartDateStr] = useState<string>(() => {
    const d = new Date();
    d.setDate(d.getDate() - 30);
    return d.toISOString().split('T')[0];
  });
  const [endDateStr, setEndDateStr] = useState<string>(() => {
    return new Date().toISOString().split('T')[0];
  });

  const [exercises] = useState<Exercise[]>(() => AppDatabase.getExercises());
  const [statusMessage, setStatusMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const exerciseMap = useMemo(() => new Map(exercises.map((e) => [e.id, e.name])), [exercises]);

  const allSessions = useMemo(
    () => AppDatabase.getAllSessionsWithSets().filter((s) => s.session.status === 'COMPLETED'),
    []
  );

  const filteredSessions = useMemo(() => {
    const startMs = new Date(startDateStr).setHours(0, 0, 0, 0);
    const endMs = new Date(endDateStr).setHours(23, 59, 59, 999);

    return allSessions.filter((sw) => sw.session.date >= startMs && sw.session.date <= endMs);
  }, [allSessions, startDateStr, endDateStr]);

  const showStatus = (type: 'success' | 'error', text: string) => {
    setStatusMessage({ type, text });
    setTimeout(() => setStatusMessage(null), 4000);
  };

  // Export to Excel (.xlsx)
  const handleExportExcel = () => {
    if (filteredSessions.length === 0) {
      showStatus('error', 'Нет тренировок за выбранный период для экспорта');
      return;
    }

    try {
      const workbook = XLSX.utils.book_new();

      // Sheet 1: Тренировки
      const sheet1Data = [
        ['Дата', 'Статус', 'Заметки', 'Кол-во подходов', 'Общий объём (кг)'],
        ...filteredSessions.map((sw) => {
          const volume = sw.sets.reduce((sum, s) => sum + s.weightKg * s.reps, 0);
          return [
            new Date(sw.session.date).toLocaleDateString('ru-RU'),
            sw.session.status === 'COMPLETED' ? 'Завершена' : 'Черновик',
            sw.session.notes || '',
            sw.sets.length,
            Math.round(volume * 10) / 10,
          ];
        }),
      ];
      const sheet1 = XLSX.utils.aoa_to_sheet(sheet1Data);
      XLSX.utils.book_append_sheet(workbook, sheet1, 'Тренировки');

      // Sheet 2: Подходы
      const sheet2Data = [
        ['Дата тренировки', 'Упражнение', 'Подход №', 'Вес (кг)', 'Повторения', 'RIR'],
        ...filteredSessions.flatMap((sw) =>
          sw.sets.map((set) => [
            new Date(sw.session.date).toLocaleDateString('ru-RU'),
            exerciseMap.get(set.exerciseId) || `Упражнение #${set.exerciseId}`,
            set.setNumber,
            set.weightKg,
            set.reps,
            set.rir,
          ])
        ),
      ];
      const sheet2 = XLSX.utils.aoa_to_sheet(sheet2Data);
      XLSX.utils.book_append_sheet(workbook, sheet2, 'Подходы');

      const fileName = `workout_tracker_${startDateStr}_${endDateStr}.xlsx`;
      XLSX.writeFile(workbook, fileName);
      showStatus('success', `Файл ${fileName} успешно сохранён!`);
    } catch (e) {
      showStatus('error', `Ошибка экспорта Excel: ${(e as Error).message}`);
    }
  };

  // Export to PDF
  const handleExportPDF = () => {
    if (filteredSessions.length === 0) {
      showStatus('error', 'Нет тренировок за выбранный период для экспорта');
      return;
    }

    try {
      const doc = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' });

      // Title & Header
      doc.setFontSize(18);
      doc.text('Отчёт по тренировкам', 14, 20);

      doc.setFontSize(11);
      doc.setTextColor(100);
      doc.text(`Период: ${new Date(startDateStr).toLocaleDateString('ru-RU')} — ${new Date(endDateStr).toLocaleDateString('ru-RU')}`, 14, 28);

      // Summary Stats
      const totalSessions = filteredSessions.length;
      const totalSets = filteredSessions.reduce((sum, s) => sum + s.sets.length, 0);
      const totalVolume = filteredSessions.reduce(
        (sum, sw) => sum + sw.sets.reduce((sSum, s) => sSum + s.weightKg * s.reps, 0),
        0
      );

      doc.setFontSize(12);
      doc.setTextColor(0);
      doc.text(`Всего тренировок: ${totalSessions}  |  Всего подходов: ${totalSets}  |  Суммарный объём: ${totalVolume.toFixed(1)} кг`, 14, 36);

      // Detailed Table
      const tableData = filteredSessions.flatMap((sw) =>
        sw.sets.map((set) => [
          new Date(sw.session.date).toLocaleDateString('ru-RU'),
          exerciseMap.get(set.exerciseId) || `Упражнение #${set.exerciseId}`,
          `№${set.setNumber}`,
          `${set.weightKg} кг`,
          `${set.reps}`,
          `RIR ${set.rir}`,
        ])
      );

      autoTable(doc, {
        startY: 42,
        head: [['Дата', 'Упражнение', 'Сет', 'Вес', 'Повт.', 'RIR']],
        body: tableData,
        theme: 'striped',
        headStyles: { fillColor: [59, 130, 246] },
        styles: { fontSize: 9 },
      });

      const fileName = `workout_report_${startDateStr}_${endDateStr}.pdf`;
      doc.save(fileName);
      showStatus('success', `Файл ${fileName} успешно сохранён!`);
    } catch (e) {
      showStatus('error', `Ошибка экспорта PDF: ${(e as Error).message}`);
    }
  };

  return (
    <div className="space-y-4 pb-20 max-w-xl mx-auto">
      {/* Toast Alert */}
      {statusMessage && (
        <div
          className={`fixed top-16 left-1/2 -translate-x-1/2 z-50 px-4 py-2.5 rounded-2xl shadow-xl text-sm font-medium flex items-center gap-2 ${
            statusMessage.type === 'success' ? 'bg-emerald-600 text-white' : 'bg-red-600 text-white'
          }`}
        >
          {statusMessage.type === 'success' ? <CheckCircle size={18} /> : <AlertCircle size={18} />}
          {statusMessage.text}
        </div>
      )}

      <div className="flex items-center justify-between">
        <h2 className="text-lg font-bold text-white flex items-center gap-2">
          <Download className="text-blue-400" size={20} />
          Экспорт и Загрузка
        </h2>
      </div>

      {/* Android APK Download Card */}
      <div className="bg-gradient-to-br from-blue-900/60 via-indigo-900/40 to-slate-800/80 border border-blue-500/40 rounded-2xl p-5 shadow-lg space-y-3">
        <div className="flex items-center gap-3">
          <div className="w-12 h-12 rounded-2xl bg-blue-600/30 border border-blue-400/40 flex items-center justify-center text-blue-400 shrink-0">
            <Smartphone size={26} />
          </div>
          <div>
            <div className="text-base font-extrabold text-white">Установочный APK для Android</div>
            <div className="text-xs text-blue-200">Готовый файл приложения для установки на телефон</div>
          </div>
        </div>

        <p className="text-xs text-slate-300 leading-relaxed">
          Автономное мобильное приложение со 100% офлайн работой, Room базой данных, вибро-таймером и аппаратной поддержкой.
        </p>

        <a
          href="./workout-tracker.apk"
          download="workout-tracker.apk"
          className="touch-target w-full bg-blue-600 hover:bg-blue-500 text-white font-bold py-3 px-4 rounded-xl flex items-center justify-center gap-2 shadow-lg shadow-blue-600/30 transition active:scale-[0.99]"
        >
          <Download size={20} />
          Скачать APK на телефон (19.9 МБ)
        </a>
      </div>

      {/* Data Export Form */}
      <div className="bg-slate-800/80 border border-slate-700/60 rounded-2xl p-5 space-y-4 shadow-sm">
        <div className="text-sm font-bold text-white flex items-center gap-2">
          <Calendar size={18} className="text-blue-400" />
          Экспорт журнала тренировок
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="text-xs font-semibold text-slate-400 block mb-1">С даты:</label>
            <input
              type="date"
              value={startDateStr}
              onChange={(e) => setStartDateStr(e.target.value)}
              className="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2.5 text-white text-sm focus:outline-none focus:border-blue-500"
            />
          </div>
          <div>
            <label className="text-xs font-semibold text-slate-400 block mb-1">По дату:</label>
            <input
              type="date"
              value={endDateStr}
              onChange={(e) => setEndDateStr(e.target.value)}
              className="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2.5 text-white text-sm focus:outline-none focus:border-blue-500"
            />
          </div>
        </div>

        <div className="text-xs text-slate-400">
          Найдено завершённых тренировок за период: <b className="text-white">{filteredSessions.length}</b>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5 pt-1">
          <button
            onClick={handleExportExcel}
            className="touch-target bg-emerald-700 hover:bg-emerald-600 text-white font-bold py-3 px-4 rounded-xl flex items-center justify-center gap-2 shadow-md transition"
          >
            <FileSpreadsheet size={20} />
            Excel (.xlsx)
          </button>

          <button
            onClick={handleExportPDF}
            className="touch-target bg-indigo-700 hover:bg-indigo-600 text-white font-bold py-3 px-4 rounded-xl flex items-center justify-center gap-2 shadow-md transition"
          >
            <FileText size={20} />
            PDF Отчёт (.pdf)
          </button>
        </div>
      </div>

      {/* Full Offline JSON Backup & Restore Card */}
      <div className="bg-slate-800/80 border border-slate-700/60 rounded-2xl p-5 space-y-3 shadow-sm">
        <div className="text-sm font-bold text-white flex items-center gap-2">
          <Download size={18} className="text-blue-400" />
          Полное резервное копирование (JSON)
        </div>
        <p className="text-xs text-slate-300 leading-relaxed">
          Экспортируйте или импортируйте всю локальную базу (упражнения, историю, замеры тела и настройки) в единый JSON файл.
        </p>

        <div className="grid grid-cols-2 gap-2.5 pt-1">
          <button
            onClick={() => {
              try {
                const json = AppDatabase.exportFullBackupJson();
                const blob = new Blob([json], { type: 'application/json' });
                const url = URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = `gain_tracker_backup_${new Date().toISOString().split('T')[0]}.json`;
                a.click();
                URL.revokeObjectURL(url);
                showStatus('success', 'Резервная копия JSON успешно скачана!');
              } catch (e) {
                showStatus('error', `Ошибка экспорта: ${(e as Error).message}`);
              }
            }}
            className="touch-target bg-blue-600 hover:bg-blue-500 text-white font-bold py-2.5 px-3 rounded-xl text-xs flex items-center justify-center gap-1.5 shadow transition"
          >
            Экспорт JSON
          </button>

          <label className="touch-target bg-slate-700 hover:bg-slate-600 text-white font-bold py-2.5 px-3 rounded-xl text-xs flex items-center justify-center gap-1.5 shadow transition cursor-pointer text-center">
            <span>Импорт JSON</span>
            <input
              type="file"
              accept=".json,application/json"
              className="hidden"
              onChange={(e) => {
                const file = e.target.files?.[0];
                if (!file) return;
                const reader = new FileReader();
                reader.onload = (evt) => {
                  try {
                    const text = evt.target?.result as string;
                    const res = AppDatabase.importFullBackupJson(text);
                    if (res.success) {
                      showStatus('success', `База успешно восстановлена (${res.count} записей)! Перезагрузка...`);
                      setTimeout(() => window.location.reload(), 1200);
                    } else {
                      showStatus('error', `Ошибка импорта: ${res.error}`);
                    }
                  } catch (err) {
                    showStatus('error', `Ошибка чтения файла: ${(err as Error).message}`);
                  }
                };
                reader.readAsText(file);
              }}
            />
          </label>
        </div>
      </div>
    </div>
  );
};
