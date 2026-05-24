import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  Map as MapIcon,
  Search,
  Brain,
  Zap,
  CreditCard,
  ShieldCheck,
  ArrowRight,
  Settings,
} from 'lucide-react';
import { useAuthStore } from '@/store/auth';

export function HomePage() {
  const { t } = useTranslation();
  const { accessToken, role } = useAuthStore();

  return (
    <div className="space-y-12 animate-fade-in-up">
      <section className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-brand-700 via-brand-800 to-brand-900 text-white p-10 md:p-16">
        <div className="absolute inset-0 opacity-30 pointer-events-none">
          <div className="absolute -top-24 -right-24 w-[28rem] h-[28rem] rounded-full bg-brand-400 blur-3xl" />
          <div className="absolute -bottom-32 -left-12 w-[28rem] h-[28rem] rounded-full bg-accent-500 blur-3xl" />
        </div>
        <div className="relative max-w-3xl">
          <span className="inline-flex items-center gap-2 bg-white/10 backdrop-blur border border-white/20 rounded-full px-3 py-1 text-xs font-semibold uppercase tracking-wider mb-6">
            <Brain className="w-3.5 h-3.5" /> {t('home.badge')}
          </span>
          <h1 className="text-4xl md:text-6xl font-bold leading-[1.1] tracking-tight">
            {t('home.titleLine1')}<br />
            <span className="bg-gradient-to-r from-accent-300 to-brand-200 bg-clip-text text-transparent">
              {t('home.titleLine2')}
            </span>
          </h1>
          <p className="mt-6 text-lg md:text-xl text-white/80 max-w-2xl leading-relaxed">
            {t('home.description')}
          </p>
          <div className="mt-8 flex flex-wrap gap-3">
            {accessToken ? (
              <>
                <Link to="/parkings" className="btn bg-white text-brand-700 hover:bg-slate-50 shadow-lift h-12 px-6">
                  <Search className="w-4 h-4" /> {t('home.findParking')}
                </Link>
                <Link to="/map" className="btn bg-white/10 backdrop-blur border border-white/30 text-white hover:bg-white/20 h-12 px-6">
                  <MapIcon className="w-4 h-4" /> {t('home.openMap')}
                </Link>
              </>
            ) : (
              <>
                <Link to="/register" className="btn bg-white text-brand-700 hover:bg-slate-50 shadow-lift h-12 px-6">
                  {t('home.createAccount')} <ArrowRight className="w-4 h-4" />
                </Link>
                <Link to="/login" className="btn bg-white/10 backdrop-blur border border-white/30 text-white hover:bg-white/20 h-12 px-6">
                  {t('home.login')}
                </Link>
              </>
            )}
          </div>
        </div>

        <div className="relative mt-10 grid grid-cols-2 md:grid-cols-4 gap-3 max-w-3xl">
          <Stat label={t('home.stats.forecast')} value={t('home.stats.forecastValue')} sub="ML window" />
          <Stat label={t('home.stats.algorithm')} value="CART" sub="regression" />
          <Stat label={t('home.stats.realtime')} value="WS" sub="WebSocket" />
          <Stat label={t('home.stats.discount')} value="−35%" sub="subscription" />
        </div>
      </section>

      <section>
        <h2 className="text-2xl font-bold mb-6 text-slate-900">{t('home.features.title')}</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          <FeatureCard icon={Brain}        tone="brand"  title={t('home.featuresList.ml.title')}        text={t('home.featuresList.ml.text')} />
          <FeatureCard icon={Zap}          tone="accent" title={t('home.featuresList.realtime.title')}  text={t('home.featuresList.realtime.text')} />
          <FeatureCard icon={CreditCard}   tone="violet" title={t('home.featuresList.tariffs.title')}   text={t('home.featuresList.tariffs.text')} />
          <FeatureCard icon={MapIcon}      tone="amber"  title={t('home.featuresList.geo.title')}       text={t('home.featuresList.geo.text')} />
          <FeatureCard icon={ShieldCheck}  tone="rose"   title={t('home.featuresList.security.title')}  text={t('home.featuresList.security.text')} />
          <FeatureCard icon={Settings}     tone="slate"  title={t('home.featuresList.analytics.title')} text={t('home.featuresList.analytics.text')} />
        </div>
      </section>

      {accessToken && (role === 'ADMIN' || role === 'OPERATOR') && (
        <section className="card flex flex-wrap items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-brand-500 to-brand-700 grid place-items-center text-white">
              <Settings className="w-6 h-6" />
            </div>
            <div>
              <h3 className="font-semibold text-slate-900">{t('home.adminBlock.title')}</h3>
              <p className="text-sm text-slate-600">{t('home.adminBlock.text')}</p>
            </div>
          </div>
          <div className="flex gap-2">
            <Link to="/admin/stats" className="btn-secondary">{t('nav.stats')}</Link>
            <Link to="/admin/parkings" className="btn-primary">{t('nav.admin')}</Link>
          </div>
        </section>
      )}
    </div>
  );
}

function Stat({ label, value, sub }: { label: string; value: string; sub: string }) {
  return (
    <div className="bg-white/10 backdrop-blur border border-white/20 rounded-xl px-4 py-3">
      <div className="text-[10px] uppercase tracking-widest text-white/60">{label}</div>
      <div className="text-2xl font-bold mt-1">{value}</div>
      <div className="text-xs text-white/50">{sub}</div>
    </div>
  );
}

const TONES = {
  brand:  { bg: 'bg-brand-50',  fg: 'text-brand-700',  ring: 'ring-brand-200' },
  accent: { bg: 'bg-accent-50', fg: 'text-accent-700', ring: 'ring-accent-200' },
  violet: { bg: 'bg-violet-50', fg: 'text-violet-700', ring: 'ring-violet-200' },
  amber:  { bg: 'bg-amber-50',  fg: 'text-amber-700',  ring: 'ring-amber-200' },
  rose:   { bg: 'bg-rose-50',   fg: 'text-rose-700',   ring: 'ring-rose-200' },
  slate:  { bg: 'bg-slate-100', fg: 'text-slate-700',  ring: 'ring-slate-200' },
};

function FeatureCard({
  icon: Icon, tone, title, text,
}: {
  icon: React.ComponentType<{ className?: string }>;
  tone: keyof typeof TONES;
  title: string;
  text: string;
}) {
  const tones = TONES[tone];
  return (
    <div className="card-hover group">
      <div className={`w-10 h-10 rounded-xl ${tones.bg} ${tones.fg} grid place-items-center group-hover:scale-110 transition-transform`}>
        <Icon className="w-5 h-5" />
      </div>
      <h3 className="mt-4 font-semibold text-slate-900">{title}</h3>
      <p className="mt-1 text-sm text-slate-600 leading-relaxed">{text}</p>
    </div>
  );
}
