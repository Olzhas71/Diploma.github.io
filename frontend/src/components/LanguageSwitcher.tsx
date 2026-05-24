import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Languages, Check } from 'lucide-react';
import clsx from 'clsx';
import { SUPPORTED_LANGUAGES, type LanguageCode } from '@/i18n';

export function LanguageSwitcher({ compact = false }: { compact?: boolean }) {
  const { i18n } = useTranslation();
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  const current = SUPPORTED_LANGUAGES.find((l) => l.code === i18n.resolvedLanguage)
              ?? SUPPORTED_LANGUAGES[0];

  useEffect(() => {
    const onClick = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', onClick);
    return () => document.removeEventListener('mousedown', onClick);
  }, []);

  const pick = (code: LanguageCode) => {
    i18n.changeLanguage(code);
    setOpen(false);
  };

  return (
    <div className="relative" ref={ref}>
      <button
        onClick={() => setOpen(!open)}
        className="btn-ghost px-2 h-9 flex items-center gap-1.5"
        aria-label="Change language"
        title={current.label}
      >
        <Languages className="w-4 h-4" />
        {compact ? (
          <span className="text-xs font-semibold uppercase">{current.code}</span>
        ) : (
          <>
            <span className="text-base">{current.flag}</span>
            <span className="hidden sm:inline text-xs font-semibold uppercase">{current.code}</span>
          </>
        )}
      </button>

      {open && (
        <div className="absolute right-0 top-full mt-2 w-44 bg-white rounded-xl shadow-lift border border-slate-200 py-1 z-50 animate-fade-in">
          {SUPPORTED_LANGUAGES.map((lang) => {
            const isCurrent = lang.code === current.code;
            return (
              <button
                key={lang.code}
                onClick={() => pick(lang.code)}
                className={clsx(
                  'w-full flex items-center gap-2 px-3 py-2 text-sm transition-colors',
                  isCurrent ? 'bg-brand-50 text-brand-700' : 'text-slate-700 hover:bg-slate-50'
                )}
              >
                <span className="text-base">{lang.flag}</span>
                <span className="flex-1 text-left">{lang.label}</span>
                {isCurrent && <Check className="w-4 h-4" />}
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}
