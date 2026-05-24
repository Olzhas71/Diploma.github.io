import { useState } from 'react';
import { useNavigate, Link, useLocation } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Mail, Lock, ParkingSquare, Loader2, AlertCircle, Sparkles, BarChart3, Wifi, ShieldCheck, Wrench, Car } from 'lucide-react';
import { authApi } from '@/api/auth';
import { useAuthStore } from '@/store/auth';
import { useToast } from '@/components/Toast';

export function LoginPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const setSession = useAuthStore((s) => s.setSession);
  const toast = useToast();

  const [email, setEmail] = useState('admin@parking.local');
  const [password, setPassword] = useState('admin123');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const from = (location.state as { from?: { pathname: string } } | null)?.from?.pathname ?? '/';

  const doLogin = async (loginEmail: string, loginPassword: string) => {
    setError(null);
    setLoading(true);
    try {
      const data = await authApi.login(loginEmail, loginPassword);
      setSession(data);
      toast.success(t('auth.login.welcome', { email: data.email }));
      navigate(from, { replace: true });
    } catch (err: any) {
      const msg = err?.response?.status === 429
        ? t('auth.login.tooMany')
        : err?.response?.data?.message ?? t('auth.login.failed');
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    doLogin(email, password);
  };

  const demoLogin = (demoEmail: string) => {
    setEmail(demoEmail);
    setPassword('admin123');
    doLogin(demoEmail, 'admin123');
  };

  return (
    <div className="min-h-[calc(100vh-9rem)] grid lg:grid-cols-2 gap-8 -m-4 md:-m-8">
      <div className="hidden lg:flex relative overflow-hidden bg-gradient-to-br from-brand-700 via-brand-800 to-brand-900 text-white p-12 flex-col justify-between">
        <div className="absolute inset-0 opacity-30">
          <div className="absolute -top-24 -right-24 w-96 h-96 rounded-full bg-brand-500 blur-3xl" />
          <div className="absolute -bottom-32 -left-12 w-96 h-96 rounded-full bg-accent-500 blur-3xl" />
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
            {t('auth.login.heroTitle1')} <span className="text-accent-300">{t('auth.login.heroTitle2')}</span>
          </h1>
          <p className="text-white/80 text-lg leading-relaxed">{t('auth.login.heroText')}</p>
          <div className="grid grid-cols-1 gap-3">
            <Feature icon={<Sparkles className="w-5 h-5" />} text={t('auth.login.heroFeatures.ml')} />
            <Feature icon={<Wifi className="w-5 h-5" />} text={t('auth.login.heroFeatures.ws')} />
            <Feature icon={<BarChart3 className="w-5 h-5" />} text={t('auth.login.heroFeatures.analytics')} />
          </div>
        </div>

        <div className="relative text-xs text-white/50">
          © 2026 {t('common.appName')} · CART regression tree (Tribuo) · 27 tests passing
        </div>
      </div>

      <div className="flex items-center justify-center p-6 lg:p-12">
        <div className="w-full max-w-sm space-y-6 animate-fade-in-up">
          <div>
            <h2 className="text-3xl font-bold text-slate-900">{t('auth.login.title')}</h2>
            <p className="text-sm text-slate-500 mt-1">{t('auth.login.subtitle')}</p>
          </div>

          <form onSubmit={submit} className="space-y-4">
            <FieldWithIcon icon={<Mail className="w-4 h-4" />} label="Email" type="email" value={email} onChange={setEmail} required />
            <FieldWithIcon icon={<Lock className="w-4 h-4" />} label={t('auth.register.password')} type="password" value={password} onChange={setPassword} required />

            {error && (
              <div className="flex items-start gap-2 text-sm text-red-700 bg-red-50 border border-red-200 rounded-lg px-3 py-2 animate-fade-in">
                <AlertCircle className="w-4 h-4 mt-0.5 shrink-0" />
                <span>{error}</span>
              </div>
            )}

            <button type="submit" className="btn-primary w-full h-11" disabled={loading}>
              {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : null}
              {loading ? t('auth.login.submitting') : t('auth.login.submit')}
            </button>
          </form>

          <div className="space-y-2">
            <div className="text-xs text-center text-slate-500 uppercase tracking-wider">
              {t('auth.login.demoLogin')}
            </div>
            <div className="grid grid-cols-3 gap-2">
              <DemoButton
                icon={<ShieldCheck className="w-4 h-4" />}
                label={t('auth.login.roleAdmin')}
                onClick={() => demoLogin('admin@parking.local')}
                disabled={loading}
                tone="brand"
              />
              <DemoButton
                icon={<Wrench className="w-4 h-4" />}
                label={t('auth.login.roleOperator')}
                onClick={() => demoLogin('operator@parking.local')}
                disabled={loading}
                tone="amber"
              />
              <DemoButton
                icon={<Car className="w-4 h-4" />}
                label={t('auth.login.roleDriver')}
                onClick={() => demoLogin('driver@parking.local')}
                disabled={loading}
                tone="emerald"
              />
            </div>
            <div className="text-[10px] text-center text-slate-400">
              {t('auth.login.demoPasswordHint')}
            </div>
          </div>

          <div className="text-sm text-center text-slate-600 border-t border-slate-200 pt-4">
            {t('auth.login.noAccount')}{' '}
            <Link to="/register" className="text-brand-600 hover:text-brand-700 font-semibold">
              {t('auth.login.registerLink')}
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

function DemoButton({
  icon, label, onClick, disabled, tone,
}: {
  icon: React.ReactNode;
  label: string;
  onClick: () => void;
  disabled?: boolean;
  tone: 'brand' | 'amber' | 'emerald';
}) {
  const tones: Record<typeof tone, string> = {
    brand:   'bg-brand-50 text-brand-700 hover:bg-brand-100 border-brand-200',
    amber:   'bg-amber-50 text-amber-700 hover:bg-amber-100 border-amber-200',
    emerald: 'bg-emerald-50 text-emerald-700 hover:bg-emerald-100 border-emerald-200',
  };
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      className={`flex flex-col items-center justify-center gap-1 border rounded-lg py-2 text-xs font-medium transition disabled:opacity-50 disabled:cursor-not-allowed ${tones[tone]}`}
    >
      {icon}
      {label}
    </button>
  );
}

function FieldWithIcon({
  icon, label, type, value, onChange, required,
}: {
  icon: React.ReactNode;
  label: string;
  type: string;
  value: string;
  onChange: (v: string) => void;
  required?: boolean;
}) {
  return (
    <label className="block">
      <span className="label">{label}</span>
      <div className="relative">
        <span className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400">{icon}</span>
        <input
          type={type}
          required={required}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          className="input pl-10"
        />
      </div>
    </label>
  );
}
