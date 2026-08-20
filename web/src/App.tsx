import React, { useState } from 'react';
import type { TabType } from './types';
import { ActiveWorkoutTab } from './components/ActiveWorkoutTab';
import { CalendarTab } from './components/CalendarTab';
import { HistoryTab } from './components/HistoryTab';
import { AnalyticsTab } from './components/AnalyticsTab';
import { ExportTab } from './components/ExportTab';
import { Dumbbell, Calendar, History, TrendingUp, Download, Smartphone } from 'lucide-react';

export const App: React.FC = () => {
  const [activeTab, setActiveTab] = useState<TabType>('active');
  const [refreshKey, setRefreshKey] = useState(0);

  const handleRefresh = () => {
    setRefreshKey((k) => k + 1);
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col justify-between">
      {/* Top App Bar */}
      <header className="sticky top-0 z-40 bg-slate-900/90 backdrop-blur border-b border-slate-800 px-4 py-3 shadow-md">
        <div className="max-w-xl mx-auto flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-blue-600 to-indigo-500 flex items-center justify-center text-white shadow-md shadow-blue-500/20">
              <Dumbbell size={20} />
            </div>
            <div>
              <h1 className="text-sm font-extrabold tracking-tight text-white uppercase">
                Трекер Тренировок
              </h1>
              <span className="text-[10px] font-medium text-blue-400 block -mt-0.5">
                Local-First • Без ИИ • Силовой прогресс
              </span>
            </div>
          </div>

          <a
            href="./workout-tracker.apk"
            download="workout-tracker.apk"
            className="touch-target px-3 py-1.5 bg-blue-600/20 hover:bg-blue-600/30 text-blue-400 hover:text-blue-300 border border-blue-500/30 rounded-xl text-xs font-semibold flex items-center gap-1.5 transition"
            title="Скачать APK для Android"
          >
            <Smartphone size={15} />
            <span>APK</span>
          </a>
        </div>
      </header>

      {/* Main Content View */}
      <main className="flex-1 px-4 py-4 max-w-xl mx-auto w-full">
        {activeTab === 'active' && <ActiveWorkoutTab key={refreshKey} onRefresh={handleRefresh} />}
        {activeTab === 'calendar' && (
          <CalendarTab
            key={refreshKey}
            onRefresh={handleRefresh}
            onOpenActiveTab={() => setActiveTab('active')}
          />
        )}
        {activeTab === 'history' && <HistoryTab key={refreshKey} onRefresh={handleRefresh} />}
        {activeTab === 'analytics' && <AnalyticsTab key={refreshKey} />}
        {activeTab === 'export' && <ExportTab key={refreshKey} />}
      </main>

      {/* Bottom Navigation Bar (Fixed) */}
      <nav className="fixed bottom-0 left-0 right-0 z-40 bg-slate-900/95 backdrop-blur border-t border-slate-800 px-2 py-1 shadow-2xl">
        <div className="max-w-xl mx-auto grid grid-cols-5 gap-1">
          <button
            onClick={() => setActiveTab('active')}
            className={`touch-target flex flex-col items-center justify-center py-1.5 rounded-xl transition ${
              activeTab === 'active' ? 'text-blue-400 font-bold bg-blue-950/40' : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Dumbbell size={20} />
            <span className="text-[10px] mt-1">Тренировка</span>
          </button>

          <button
            onClick={() => setActiveTab('calendar')}
            className={`touch-target flex flex-col items-center justify-center py-1.5 rounded-xl transition ${
              activeTab === 'calendar' ? 'text-blue-400 font-bold bg-blue-950/40' : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Calendar size={20} />
            <span className="text-[10px] mt-1">Календарь</span>
          </button>

          <button
            onClick={() => setActiveTab('history')}
            className={`touch-target flex flex-col items-center justify-center py-1.5 rounded-xl transition ${
              activeTab === 'history' ? 'text-blue-400 font-bold bg-blue-950/40' : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <History size={20} />
            <span className="text-[10px] mt-1">История</span>
          </button>

          <button
            onClick={() => setActiveTab('analytics')}
            className={`touch-target flex flex-col items-center justify-center py-1.5 rounded-xl transition ${
              activeTab === 'analytics' ? 'text-blue-400 font-bold bg-blue-950/40' : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <TrendingUp size={20} />
            <span className="text-[10px] mt-1">Аналитика</span>
          </button>

          <button
            onClick={() => setActiveTab('export')}
            className={`touch-target flex flex-col items-center justify-center py-1.5 rounded-xl transition ${
              activeTab === 'export' ? 'text-blue-400 font-bold bg-blue-950/40' : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Download size={20} />
            <span className="text-[10px] mt-1">Экспорт</span>
          </button>
        </div>
      </nav>
    </div>
  );
};

export default App;
