import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { CheckCircle2, XCircle, Info, X } from 'lucide-react';
import clsx from 'clsx';

type ToastType = 'success' | 'error' | 'info';
interface Toast {
  id: number;
  type: ToastType;
  message: string;
}

interface ToastContextValue {
  show: (message: string, type?: ToastType) => void;
  success: (message: string) => void;
  error: (message: string) => void;
  info: (message: string) => void;
}

const ToastContext = createContext<ToastContextValue | null>(null);

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const show = useCallback((message: string, type: ToastType = 'info') => {
    const id = Date.now() + Math.random();
    setToasts((prev) => [...prev, { id, type, message }]);
    setTimeout(() => setToasts((prev) => prev.filter((t) => t.id !== id)), 4000);
  }, []);

  const value: ToastContextValue = {
    show,
    success: (m) => show(m, 'success'),
    error: (m) => show(m, 'error'),
    info: (m) => show(m, 'info'),
  };

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="fixed bottom-4 right-4 z-50 space-y-2 pointer-events-none">
        {toasts.map((t) => (
          <ToastItem key={t.id} toast={t} onDismiss={() => setToasts((p) => p.filter((x) => x.id !== t.id))} />
        ))}
      </div>
    </ToastContext.Provider>
  );
}

function ToastItem({ toast, onDismiss }: { toast: Toast; onDismiss: () => void }) {
  const [closing, setClosing] = useState(false);

  useEffect(() => {
    const t = setTimeout(() => setClosing(true), 3700);
    return () => clearTimeout(t);
  }, []);

  const Icon = toast.type === 'success' ? CheckCircle2 : toast.type === 'error' ? XCircle : Info;
  const tone = toast.type === 'success'
    ? 'bg-emerald-50 border-emerald-200 text-emerald-800'
    : toast.type === 'error'
    ? 'bg-red-50 border-red-200 text-red-800'
    : 'bg-brand-50 border-brand-200 text-brand-800';

  return (
    <div
      className={clsx(
        'pointer-events-auto flex items-start gap-3 min-w-[280px] max-w-md rounded-xl border px-4 py-3 shadow-lift',
        tone,
        closing ? 'opacity-0 translate-x-4 transition-all duration-200' : 'animate-slide-in-right'
      )}
    >
      <Icon className="w-5 h-5 mt-0.5 shrink-0" />
      <span className="flex-1 text-sm font-medium">{toast.message}</span>
      <button onClick={onDismiss} className="text-current opacity-50 hover:opacity-100">
        <X className="w-4 h-4" />
      </button>
    </div>
  );
}

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used inside <ToastProvider>');
  return ctx;
}
