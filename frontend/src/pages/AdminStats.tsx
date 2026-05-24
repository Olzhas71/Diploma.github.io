import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { statsApi, type HourlyOccupancyPoint, type PopularParking, type RevenuePoint, type StatsOverview } from '@/api/stats';

export function AdminStatsPage() {
  const { t } = useTranslation();
  const overview = useQuery({ queryKey: ['stats', 'overview'], queryFn: statsApi.overview, refetchInterval: 60_000 });
  const revenue  = useQuery({ queryKey: ['stats', 'revenue', 30], queryFn: () => statsApi.revenue(30) });
  const popular  = useQuery({ queryKey: ['stats', 'popular', 30], queryFn: () => statsApi.popular(30, 5) });
  const occupancy = useQuery({ queryKey: ['stats', 'occupancy', 7], queryFn: () => statsApi.occupancyByHour(7) });

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">{t('adminStats.title')}</h1>
        <p className="text-sm text-slate-500 mt-1">{t('adminStats.subtitle')}</p>
      </div>

      {overview.data && <KpiRow data={overview.data} />}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <Card title={t('adminStats.revenue30')}>
          {revenue.isLoading && <Skeleton />}
          {revenue.data && revenue.data.length === 0 && <Empty text={t('adminStats.revenueEmpty')} />}
          {revenue.data && revenue.data.length > 0 && <RevenueChart data={revenue.data} />}
        </Card>

        <Card title={t('adminStats.occupancyByHour')}>
          {occupancy.isLoading && <Skeleton />}
          {occupancy.data && occupancy.data.length === 0 && <Empty text={t('adminStats.occupancyEmpty')} />}
          {occupancy.data && occupancy.data.length > 0 && <OccupancyChart data={occupancy.data} />}
        </Card>
      </div>

      <Card title={t('adminStats.topParkings')}>
        {popular.isLoading && <Skeleton />}
        {popular.data && <PopularList data={popular.data} />}
      </Card>
    </div>
  );
}

function KpiRow({ data }: { data: StatsOverview }) {
  const { t } = useTranslation();
  const occupancyPct = Math.round(data.averageOccupancyRate * 100);
  return (
    <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
      <Kpi label={t('adminStats.kpi.parkings')} value={data.totalParkings.toString()} accent="bg-brand-50 text-brand-700" />
      <Kpi
        label={t('adminStats.kpi.free')}
        value={`${data.freeSpots} / ${data.totalSpots}`}
        accent="bg-emerald-50 text-emerald-700"
        sub={t('adminStats.kpi.occupiedNow', { pct: occupancyPct })}
      />
      <Kpi
        label={t('adminStats.kpi.activeBookings')}
        value={data.activeBookings.toString()}
        accent="bg-amber-50 text-amber-700"
        sub={t('adminStats.kpi.totalBookings', { count: data.totalBookings })}
      />
      <Kpi
        label={t('adminStats.kpi.revenue')}
        value={`${formatMoney(data.totalRevenue)} ${data.currency}`}
        accent="bg-violet-50 text-violet-700"
      />
    </div>
  );
}

function Kpi({ label, value, accent, sub }: { label: string; value: string; accent: string; sub?: string }) {
  return (
    <div className={`card ${accent}`}>
      <div className="text-xs uppercase tracking-wide opacity-75">{label}</div>
      <div className="text-2xl font-bold mt-1 tabular-nums">{value}</div>
      {sub && <div className="text-xs opacity-70 mt-1">{sub}</div>}
    </div>
  );
}

function Card({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="card">
      <h2 className="text-lg font-semibold mb-4">{title}</h2>
      {children}
    </div>
  );
}

function Skeleton() {
  return <div className="h-48 rounded bg-slate-100 animate-pulse" />;
}

function Empty({ text }: { text: string }) {
  return <div className="h-48 flex items-center justify-center text-slate-400 text-sm">{text}</div>;
}

function RevenueChart({ data }: { data: RevenuePoint[] }) {
  const { t, i18n } = useTranslation();
  const locale = i18n.resolvedLanguage === 'kk' ? 'kk-KZ' : i18n.resolvedLanguage === 'en' ? 'en-US' : 'ru-RU';
  const chartData = data.map((d) => ({
    ...d,
    label: new Date(d.day).toLocaleDateString(locale, { day: '2-digit', month: 'short' }),
  }));
  return (
    <div style={{ width: '100%', height: 280 }}>
      <ResponsiveContainer>
        <LineChart data={chartData}>
          <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
          <XAxis dataKey="label" tick={{ fontSize: 11 }} />
          <YAxis tick={{ fontSize: 11 }} />
          <Tooltip />
          <Legend />
          <Line type="monotone" dataKey="revenue"  name={t('adminStats.revenueLabel')}  stroke="#7c3aed" strokeWidth={2} dot={false} />
          <Line type="monotone" dataKey="bookings" name={t('adminStats.bookingsLabel')} stroke="#10b981" strokeWidth={2} dot={false} />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}

function OccupancyChart({ data }: { data: HourlyOccupancyPoint[] }) {
  const { t } = useTranslation();
  const chartData = data.map((d) => ({
    hour: `${String(d.hourOfDay).padStart(2, '0')}h`,
    pct: Math.round(d.averageOccupancyRate * 100),
  }));
  return (
    <div style={{ width: '100%', height: 280 }}>
      <ResponsiveContainer>
        <BarChart data={chartData}>
          <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
          <XAxis dataKey="hour" tick={{ fontSize: 10 }} />
          <YAxis domain={[0, 100]} unit="%" tick={{ fontSize: 11 }} />
          <Tooltip formatter={(v: number) => `${v}%`} />
          <Bar dataKey="pct" name={t('adminStats.occupancyLabel')}>
            {chartData.map((entry, idx) => (
              <Cell key={idx} fill={entry.pct > 80 ? '#ef4444' : entry.pct > 50 ? '#f59e0b' : '#10b981'} />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}

function PopularList({ data }: { data: PopularParking[] }) {
  const { t } = useTranslation();
  if (data.length === 0) return <Empty text={t('adminStats.noData')} />;
  const max = Math.max(1, ...data.map((d) => d.bookings));
  return (
    <div className="space-y-3">
      {data.map((p, idx) => (
        <div key={p.parkingId} className="flex items-center gap-3">
          <div className="w-6 text-slate-400 text-sm">{idx + 1}.</div>
          <div className="flex-1">
            <div className="flex items-center justify-between mb-1">
              <span className="font-medium">{p.name}</span>
              <span className="text-sm text-slate-600 tabular-nums">
                {p.bookings} {t('adminStats.bookingsShort')} · {formatMoney(p.revenue)} USD
              </span>
            </div>
            <div className="h-2 rounded bg-slate-100 overflow-hidden">
              <div className="h-full bg-brand-500" style={{ width: `${(p.bookings / max) * 100}%` }} />
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}

function formatMoney(n: number) {
  return new Intl.NumberFormat('ru-RU', { maximumFractionDigits: 2 }).format(n);
}
