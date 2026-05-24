import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  Sparkles,
  ChevronRight,
  CheckCircle2,
  Clock,
  RefreshCw,
  X,
  CreditCard,
  AlertTriangle,
  Building2,
  Loader2,
  Tag,
  Layers,
} from 'lucide-react';
import clsx from 'clsx';
import { subscriptionsApi, type SubscriptionResponse } from '@/api/subscriptions';
import { parkingsApi } from '@/api/parkings';
import { useToast } from '@/components/Toast';
import { SkeletonRows } from '@/components/Skeleton';

type Tab = 'active' | 'history' | 'buy';

export function SubscriptionsPage() {
  const { t } = useTranslation();
  const [tab, setTab] = useState<Tab>('active');
  const subs = useQuery({ queryKey: ['subscriptions'], queryFn: () => subscriptionsApi.list() });

  const active = useMemo(() => subs.data?.filter((s) => s.status === 'ACTIVE') ?? [], [subs.data]);
  const history = useMemo(() => subs.data?.filter((s) => s.status !== 'ACTIVE') ?? [], [subs.data]);

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-3xl font-bold text-slate-900 flex items-center gap-3">
            <Sparkles className="w-7 h-7 text-brand-600" /> {t('subscriptions.title')}
          </h1>
          <p className="text-sm text-slate-500 mt-1 max-w-xl">{t('subscriptions.subtitle')}</p>
        </div>
      </div>

      <div className="flex gap-1 bg-slate-100 p-1 rounded-xl w-full max-w-md">
        <TabButton active={tab === 'active'}  onClick={() => setTab('active')}  count={active.length}>
          {t('subscriptions.tabs.active')}
        </TabButton>
        <TabButton active={tab === 'history'} onClick={() => setTab('history')} count={history.length}>
          {t('subscriptions.tabs.history')}
        </TabButton>
        <TabButton active={tab === 'buy'}     onClick={() => setTab('buy')}>
          {t('subscriptions.tabs.buy')}
        </TabButton>
      </div>

      {subs.isLoading && <SkeletonRows count={2} />}

      {tab === 'active'  && <ActiveList subs={active} loading={subs.isLoading} />}
      {tab === 'history' && <HistoryList subs={history} loading={subs.isLoading} />}
      {tab === 'buy'     && <BuyWizard activeSubs={active} />}
    </div>
  );
}

function TabButton({ active, count, onClick, children }: { active: boolean; count?: number; onClick: () => void; children: React.ReactNode }) {
  return (
    <button
      onClick={onClick}
      className={clsx(
        'flex-1 px-4 py-2 rounded-lg text-sm font-medium transition-all flex items-center justify-center gap-2',
        active ? 'bg-white shadow-soft text-slate-900' : 'text-slate-600 hover:text-slate-900'
      )}
    >
      {children}
      {count !== undefined && count > 0 && (
        <span className={clsx('min-w-[20px] h-5 px-1.5 rounded-full text-[11px] font-semibold grid place-items-center',
          active ? 'bg-brand-100 text-brand-700' : 'bg-slate-200 text-slate-600'
        )}>{count}</span>
      )}
    </button>
  );
}

function useLocale() {
  const { i18n } = useTranslation();
  return i18n.resolvedLanguage === 'kk' ? 'kk-KZ' : i18n.resolvedLanguage === 'en' ? 'en-US' : 'ru-RU';
}

function fmtDate(iso: string, locale: string) {
  return new Date(iso).toLocaleDateString(locale, { day: '2-digit', month: 'short', year: 'numeric' });
}

function daysBetween(a: string, b: string) {
  return Math.max(0, Math.ceil((new Date(b).getTime() - new Date(a).getTime()) / 86_400_000));
}

function ActiveList({ subs, loading }: { subs: SubscriptionResponse[]; loading: boolean }) {
  const { t } = useTranslation();
  if (loading) return null;
  if (subs.length === 0) {
    return (
      <div className="card text-center py-16">
        <Sparkles className="w-12 h-12 text-slate-300 mx-auto mb-3" />
        <p className="text-slate-500 mb-4">{t('subscriptions.noActive')}</p>
        <p className="text-xs text-slate-400">{t('subscriptions.buyHint')}</p>
      </div>
    );
  }
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
      {subs.map((s) => <ActiveCard key={s.id} sub={s} />)}
    </div>
  );
}

function ActiveCard({ sub }: { sub: SubscriptionResponse }) {
  const { t } = useTranslation();
  const locale = useLocale();
  const qc = useQueryClient();
  const toast = useToast();
  const totalDays = daysBetween(sub.validFrom, sub.validTo);
  const daysLeft = daysBetween(new Date().toISOString(), sub.validTo);
  const pct = totalDays === 0 ? 0 : Math.max(0, Math.min(100, ((totalDays - daysLeft) / totalDays) * 100));
  const expiringSoon = daysLeft <= 7;

  const cancel = useMutation({
    mutationFn: () => subscriptionsApi.cancel(sub.id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['subscriptions'] }); toast.success(t('subscriptions.cancelled1')); },
    onError: (e: any) => toast.error(e?.response?.data?.message ?? t('common.error')),
  });

  const renew = useMutation({
    mutationFn: () => subscriptionsApi.renew(sub.id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['subscriptions'] }); toast.success(t('subscriptions.renewed')); },
    onError: (e: any) => toast.error(e?.response?.data?.message ?? t('common.error')),
  });

  return (
    <div className={clsx('card relative overflow-hidden', expiringSoon ? 'ring-2 ring-amber-300' : '')}>
      <div className="absolute top-0 left-0 right-0 h-1 bg-slate-100">
        <div className={clsx('h-full transition-all', expiringSoon ? 'bg-amber-500' : 'bg-gradient-to-r from-brand-500 to-accent-500')}
             style={{ width: `${pct}%` }} />
      </div>

      <div className="flex items-start gap-3">
        <div className={clsx('w-11 h-11 rounded-xl grid place-items-center shrink-0',
          expiringSoon ? 'bg-amber-100 text-amber-700' : 'bg-brand-50 text-brand-700')}>
          <Sparkles className="w-5 h-5" />
        </div>
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap">
            <h3 className="font-semibold text-slate-900 truncate">{sub.parkingName}</h3>
            <span className="badge bg-accent-100 text-accent-700 flex items-center gap-1">
              <CheckCircle2 className="w-3 h-3" /> {t('subscriptions.active')}
            </span>
          </div>
          <p className="text-xs text-slate-500 mt-1">
            {fmtDate(sub.validTo, locale)}{daysLeft > 0 && <> · {t('subscriptions.daysLeft', { count: daysLeft })}</>}
          </p>
        </div>
        <Link to={`/parkings/${sub.parkingId}`} className="btn-ghost text-xs">
          {t('subscriptions.toParking')} <ChevronRight className="w-3 h-3" />
        </Link>
      </div>

      {expiringSoon && (
        <div className="mt-3 flex items-start gap-2 text-xs text-amber-800 bg-amber-50 border border-amber-200 rounded-lg px-3 py-2">
          <AlertTriangle className="w-4 h-4 mt-0.5 shrink-0" />
          <span>{t('subscriptions.expiringSoon')}</span>
        </div>
      )}

      <div className="mt-4 grid grid-cols-3 gap-3 text-xs">
        <Stat label={t('subscriptions.period')} value={`${totalDays} ${t('subscriptions.daysShort')}`} />
        <Stat label={t('subscriptions.daysLeftShort')} value={`${daysLeft} ${t('subscriptions.daysShort')}`} tone={expiringSoon ? 'amber' : 'brand'} />
        <Stat label={t('subscriptions.cost')} value={`${sub.price} ${sub.currency}`} />
      </div>

      <div className="mt-4 flex gap-2">
        <button className="btn-secondary flex-1" onClick={() => renew.mutate()} disabled={renew.isPending}>
          {renew.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <RefreshCw className="w-4 h-4" />}
          {t('subscriptions.renew')}
        </button>
        <button className="btn-ghost text-red-600" onClick={() => {
          if (confirm(t('subscriptions.cancelConfirm'))) cancel.mutate();
        }}>
          <X className="w-4 h-4" /> {t('subscriptions.cancel')}
        </button>
      </div>
    </div>
  );
}

function Stat({ label, value, tone = 'slate' }: { label: string; value: string; tone?: 'slate' | 'brand' | 'amber' }) {
  const colors = { slate: 'bg-slate-50 text-slate-700', brand: 'bg-brand-50 text-brand-700', amber: 'bg-amber-50 text-amber-700' };
  return (
    <div className={clsx('rounded-lg p-2', colors[tone])}>
      <div className="text-[10px] uppercase tracking-wider opacity-70">{label}</div>
      <div className="font-bold mt-0.5 tabular-nums">{value}</div>
    </div>
  );
}

function HistoryList({ subs, loading }: { subs: SubscriptionResponse[]; loading: boolean }) {
  const { t } = useTranslation();
  const locale = useLocale();
  const qc = useQueryClient();
  const toast = useToast();

  const renew = useMutation({
    mutationFn: (id: number) => subscriptionsApi.renew(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['subscriptions'] }); toast.success(t('subscriptions.resumed')); },
    onError: (e: any) => toast.error(e?.response?.data?.message ?? t('common.error')),
  });

  if (loading) return null;
  if (subs.length === 0) {
    return (
      <div className="card text-center py-16">
        <Clock className="w-12 h-12 text-slate-300 mx-auto mb-3" />
        <p className="text-slate-500">{t('subscriptions.noHistory')}</p>
      </div>
    );
  }
  return (
    <div className="card overflow-x-auto">
      <table className="min-w-full text-sm">
        <thead>
          <tr className="text-left text-slate-500 border-b border-slate-200">
            <th className="py-2 pr-4">{t('subscriptions.historyParking')}</th>
            <th className="py-2 pr-4">{t('subscriptions.historyPeriod')}</th>
            <th className="py-2 pr-4">{t('subscriptions.historyStatus')}</th>
            <th className="py-2 pr-4 text-right">{t('subscriptions.historyPrice')}</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {subs.map((s) => (
            <tr key={s.id} className="border-b border-slate-100 last:border-0">
              <td className="py-3 pr-4 font-medium">{s.parkingName}</td>
              <td className="py-3 pr-4 text-slate-600">
                {fmtDate(s.validFrom, locale)} — {fmtDate(s.validTo, locale)}
              </td>
              <td className="py-3 pr-4">
                <span className={clsx('badge',
                  s.status === 'EXPIRED' ? 'bg-slate-100 text-slate-500' : 'bg-red-100 text-red-700'
                )}>
                  {s.status === 'EXPIRED' ? t('subscriptions.expired') : t('subscriptions.cancelled')}
                </span>
              </td>
              <td className="py-3 pr-4 text-right tabular-nums">{s.price} {s.currency}</td>
              <td className="py-3 pl-4 text-right">
                <button className="btn-secondary text-xs" onClick={() => renew.mutate(s.id)} disabled={renew.isPending}>
                  <RefreshCw className="w-3 h-3" /> {t('subscriptions.historyResume')}
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function BuyWizard({ activeSubs }: { activeSubs: SubscriptionResponse[] }) {
  const { t } = useTranslation();
  const parkings = useQuery({ queryKey: ['parkings'], queryFn: parkingsApi.list });
  const [parkingId, setParkingId] = useState<number | null>(null);
  const [days, setDays] = useState(30);

  const DURATIONS = [
    { days: 7,   label: t('subscriptions.durations.week'),    sub: t('subscriptions.durations.weekSub') },
    { days: 30,  label: t('subscriptions.durations.month'),   sub: t('subscriptions.durations.monthSub'), popular: true },
    { days: 90,  label: t('subscriptions.durations.quarter'), sub: t('subscriptions.durations.quarterSub') },
    { days: 365, label: t('subscriptions.durations.year'),    sub: t('subscriptions.durations.yearSub') },
  ];

  const activeParkingIds = new Set(activeSubs.map((s) => s.parkingId));

  return (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
      <div className="card">
        <Step n={1} title={t('subscriptions.step1')} />
        {parkings.isLoading && <SkeletonRows count={3} />}
        <div className="mt-3 space-y-2 max-h-96 overflow-y-auto">
          {parkings.data?.map((p) => {
            const already = activeParkingIds.has(p.id);
            return (
              <button
                key={p.id}
                onClick={() => !already && setParkingId(p.id)}
                disabled={already}
                className={clsx('w-full text-left p-3 rounded-lg border transition-all',
                  already ? 'bg-accent-50 border-accent-200 cursor-not-allowed opacity-60'
                  : parkingId === p.id ? 'bg-brand-50 border-brand-300 ring-2 ring-brand-200'
                  : 'bg-white border-slate-200 hover:border-brand-300 hover:bg-slate-50'
                )}
              >
                <div className="flex items-center gap-2 mb-1">
                  <Building2 className="w-4 h-4 text-slate-400" />
                  <span className="font-medium text-sm">{p.name}</span>
                  {already && <span className="badge bg-accent-100 text-accent-700 text-[10px] ml-auto">{t('subscriptions.alreadyActive')}</span>}
                </div>
                <p className="text-xs text-slate-500 truncate">{p.address}</p>
              </button>
            );
          })}
        </div>
      </div>

      <div className={clsx('card', !parkingId && 'opacity-60 pointer-events-none')}>
        <Step n={2} title={t('subscriptions.step2')} />
        <div className="mt-3 space-y-2">
          {DURATIONS.map((d) => (
            <button
              key={d.days}
              onClick={() => setDays(d.days)}
              className={clsx('w-full text-left p-3 rounded-lg border transition-all flex items-center gap-3',
                days === d.days ? 'bg-brand-50 border-brand-300 ring-2 ring-brand-200' : 'bg-white border-slate-200 hover:border-brand-300'
              )}
            >
              <Layers className="w-4 h-4 text-slate-400" />
              <div className="flex-1">
                <div className="font-medium text-sm flex items-center gap-2">
                  {d.label}
                  {d.popular && <span className="badge bg-brand-100 text-brand-700 text-[10px]">{t('subscriptions.durations.monthBadge')}</span>}
                </div>
                <p className="text-xs text-slate-500">{d.sub}</p>
              </div>
            </button>
          ))}
        </div>
      </div>

      <div className={clsx('card', !parkingId && 'opacity-60 pointer-events-none')}>
        <Step n={3} title={t('subscriptions.step3')} />
        {parkingId ? (
          <ConfirmBlock parkingId={parkingId} days={days} />
        ) : (
          <p className="text-sm text-slate-400 mt-3">{t('subscriptions.chooseFirst')}</p>
        )}
      </div>
    </div>
  );
}

function Step({ n, title }: { n: number; title: string }) {
  return (
    <div className="flex items-center gap-2">
      <div className="w-7 h-7 rounded-full bg-brand-100 text-brand-700 grid place-items-center text-sm font-bold">{n}</div>
      <h3 className="font-semibold text-slate-900">{title}</h3>
    </div>
  );
}

function ConfirmBlock({ parkingId, days }: { parkingId: number; days: number }) {
  const { t } = useTranslation();
  const qc = useQueryClient();
  const toast = useToast();
  const quote = useQuery({
    queryKey: ['sub-quote', parkingId, days],
    queryFn: () => subscriptionsApi.quote(parkingId, days),
  });
  const buy = useMutation({
    mutationFn: () => subscriptionsApi.buy(parkingId, days),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['subscriptions'] }); toast.success(t('subscriptions.bought')); },
    onError: (e: any) => toast.error(e?.response?.data?.message ?? t('common.error')),
  });

  if (quote.isLoading) {
    return <p className="text-sm text-slate-400 mt-3 flex items-center gap-2">
      <Loader2 className="w-4 h-4 animate-spin" /> {t('subscriptions.calculating')}
    </p>;
  }
  if (!quote.data) return null;

  const perDay = quote.data.price / Math.max(1, quote.data.durationDays);
  return (
    <div className="mt-3 space-y-3">
      <div className="bg-gradient-to-br from-brand-50 to-accent-50 border border-brand-200 rounded-xl p-4 text-center">
        <div className="text-xs uppercase tracking-wider text-slate-500">{t('subscriptions.toPay')}</div>
        <div className="text-3xl font-bold tabular-nums text-slate-900 mt-1">
          {quote.data.price} <span className="text-lg text-slate-500">{quote.data.currency}</span>
        </div>
        <div className="text-xs text-slate-500 mt-1">
          {t('subscriptions.perDay', { amount: perDay.toFixed(2), currency: quote.data.currency })}
        </div>
      </div>

      <ul className="space-y-1.5 text-sm text-slate-700">
        <li className="flex items-center gap-2"><CheckCircle2 className="w-4 h-4 text-accent-600" />{t('subscriptions.benefitUnlimited', { count: quote.data.durationDays })}</li>
        <li className="flex items-center gap-2"><CheckCircle2 className="w-4 h-4 text-accent-600" />{t('subscriptions.benefitFreeBookings')}</li>
        <li className="flex items-center gap-2"><CheckCircle2 className="w-4 h-4 text-accent-600" />{t('subscriptions.benefitDiscount')}</li>
        <li className="flex items-center gap-2"><Tag className="w-4 h-4 text-brand-600" />{t('subscriptions.benefitCancel')}</li>
      </ul>

      <button className="btn-primary w-full h-11" onClick={() => buy.mutate()} disabled={buy.isPending}>
        {buy.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <CreditCard className="w-4 h-4" />}
        {buy.isPending ? t('subscriptions.buying') : t('subscriptions.buy')}
      </button>
    </div>
  );
}
