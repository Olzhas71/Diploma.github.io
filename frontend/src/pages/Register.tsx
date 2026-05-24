import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Mail, Lock, User, Phone, ParkingSquare, Loader2, AlertCircle, ShieldCheck, Zap, Layers } from 'lucide-react';
import { authApi } from '@/api/auth';
import { useAuthStore } from '@/store/auth';
import { useToast } from '@/components/Toast';

export function RegisterPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const setSession = useAuthStore((s) => s.setSession);
  const toast = useToast();

  const [form, setForm] = useState({ email: '', password: '', fullName: '', phone: '' });
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const change = (key: keyof typeof form) => (v: string) => setForm({ ...form, [key]: v });

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const data = await authApi.register(form.email, form.password, form.fullName, form.phone || undefined);
      setSession(data);
      toast.success(t('auth.register.created'));
      navigate('/', { replace: true });
    } catch (err: any) {
      const msg = err?.response?.data?.message
        ?? Object.values(err?.response?.data?.fieldErrors ?? {}).join(', ')
        ?? t('auth.register.failed');
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-[calc(100vh-9rem)] grid lg:grid-cols-2 gap-8 -m-4 md:-m-8">
      <div className="hidden lg:flex relative overflow-hidden bg-gradient-to-br from-accent-700 via-emerald-800 to-brand-900 text-white p-12 flex-col justify-between">
        <div className="absolute inset-0 opacity-30">
          <div className="absolute -top-32 -left-12 w-96 h-96 rounded-full bg-accent-500 blur-3xl" />
          <div className="absolute -bottom-24 -right-16 w-96 h-96 rounded-full bg-brand-500 blur-3xl" />
        </div>
        <div className="relative">
          <div className="flex items-center gap-2">
            <div className="w-10 h-10 rounded-xl bg-white/15 backdrop-blur grid place-items-center">
              <ParkingSquare className="w-6 h-6" />
            </div>
            <div>
              <div className="font-bold text-lg">{t('common.appName')}</div>
              <div className="text-xs uppercase tracking-widest text-white/60">{t('common.tagline')}</div>
            </div>
          </div>
        </div>

        <div className="relative space-y-6 max-w-md">
          <h1 className="text-4xl xl:text-5xl font-bold leading-tight">
            {t('auth.register.heroTitle1')} <span className="text-accent-300">{t('auth.register.heroTitle2')}</span>
          </h1>
          <p className="text-white/80 text-lg">{t('auth.register.heroText')}</p>
          <div className="grid grid-cols-1 gap-3">
            <Feature icon={<Zap className="w-5 h-5" />} text={t('auth.register.heroFeatures.quick')} />
            <Feature icon={<Layers className="w-5 h-5" />} text={t('auth.register.heroFeatures.discount')} />
            <Feature icon={<ShieldCheck className="w-5 h-5" />} text={t('auth.register.heroFeatures.secure')} />
          </div>
        </div>

        <div className="relative text-xs text-white/50">© 2026 {t('common.appName')}</div>
      </div>

      <div className="flex items-center justify-center p-6 lg:p-12">
        <div className="w-full max-w-sm space-y-6 animate-fade-in-up">
          <div>
            <h2 className="text-3xl font-bold text-slate-900">{t('auth.register.title')}</h2>
            <p className="text-sm text-slate-500 mt-1">{t('auth.register.subtitle')}</p>
          </div>

          <form onSubmit={submit} className="space-y-4">
            <FieldWithIcon icon={<User className="w-4 h-4" />}  label={t('auth.register.fullName')} required value={form.fullName} onChange={change('fullName')} />
            <FieldWithIcon icon={<Mail className="w-4 h-4" />}  label={t('auth.register.email')} type="email" required value={form.email} onChange={change('email')} />
            <FieldWithIcon icon={<Phone className="w-4 h-4" />} label={t('auth.register.phone')} placeholder="+7..." value={form.phone} onChange={change('phone')} />
            <FieldWithIcon icon={<Lock className="w-4 h-4" />}  label={t('auth.register.password')} type="password" required minLength={8} value={form.password} onChange={change('password')} hint={t('auth.register.passwordHint')} />

            {error && (
              <div className="flex items-start gap-2 text-sm text-red-700 bg-red-50 border border-red-200 rounded-lg px-3 py-2 animate-fade-in">
                <AlertCircle className="w-4 h-4 mt-0.5 shrink-0" />
                <span>{error}</span>
              </div>
            )}

            <button type="submit" className="btn-primary w-full h-11" disabled={loading}>
              {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : null}
              {loading ? t('auth.register.submitting') : t('auth.register.submit')}
            </button>
          </form>

          <div className="text-sm text-center text-slate-600 border-t border-slate-200 pt-4">
            {t('auth.register.hasAccount')}{' '}
            <Link to="/login" className="text-brand-600 hover:text-brand-700 font-semibold">
              {t('auth.register.loginLink')}
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}

function Feature({ icon, text }: { icon: React.ReactNode; text: string }) {
  return (
    <div className="flex items-start gap-3 bg-white/5 backdrop-blur rounded-xl px-4 py-3 border border-white/10">
      <div className="w-9 h-9 rounded-lg bg-white/10 grid place-items-center shrink-0">{icon}</div>
      <span className="text-sm text-white/90 leading-relaxed pt-1">{text}</span>
    </div>
  );
}

function FieldWithIcon({
  icon, label, type = 'text', value, onChange, required, placeholder, minLength, hint,
}: {
  icon: React.ReactNode;
  label: string;
  type?: string;
  value: string;
  onChange: (v: string) => void;
  required?: boolean;
  placeholder?: string;
  minLength?: number;
  hint?: string;
}) {
  return (
    <label className="block">
      <span className="label">{label}</span>
      <div className="relative">
        <span className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400">{icon}</span>
        <input
          type={type}
          required={required}
          minLength={minLength}
          placeholder={placeholder}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          className="input pl-10"
        />
      </div>
      {hint && <div className="text-xs text-slate-400 mt-1">{hint}</div>}
    </label>
  );
}
