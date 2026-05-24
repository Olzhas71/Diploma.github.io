import { Link, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
  Line,
  ComposedChart,
} from 'recharts';
import { mlApi, type ModelInfo } from '@/api/ml';
import { parkingsApi } from '@/api/parkings';
import { useAuthStore } from '@/store/auth';

export function ForecastPage() {
  const { t, i18n } = useTranslation();
  const { id } = useParams<{ id: string }>();
  const parkingId = Number(id);
  const locale = i18n.resolvedLanguage === 'kk' ? 'kk-KZ' : i18n.resolvedLanguage === 'en' ? 'en-US' : 'ru-RU';

  const { data: parking } = useQuery({
    queryKey: ['parking', parkingId],
    queryFn: () => parkingsApi.get(parkingId),
    enabled: !!parkingId,
  });
  const { data: forecast, isLoading } = useQuery({
    queryKey: ['forecast', parkingId],
    queryFn: () => mlApi.forecast(parkingId),
    enabled: !!parkingId,
  });
  const { data: model } = useQuery({
    queryKey: ['ml', 'model'],
    queryFn: mlApi.modelInfo,
  });

  const chartData = forecast?.hourly.map((h, idx) => ({
    name: `+${idx}h`,
    hour: `${String(h.hourOfDay).padStart(2, '0')}:00`,
    occupancyPct: Math.round(h.predictedOccupancyRate * 100),
    free: h.predictedFreeSpots,
  }));

  return (
    <div className="space-y-6">
      <Link to={`/parkings/${parkingId}`} className="text-sm text-brand-600 hover:underline">
        {t('forecast.backToParking')}
      </Link>

      <div className="card">
        <h1 className="text-2xl font-bold">{t('forecast.title')}</h1>
        {parking && <p className="text-slate-600 mt-1">{parking.name} · {parking.address}</p>}
        {forecast && (
          <p className="text-xs text-slate-500 mt-2">
            {t('forecast.generatedAt', { date: new Date(forecast.generatedAt).toLocaleString(locale) })}
          </p>
        )}
      </div>

      {model && <ModelCard model={model} locale={locale} />}

      {isLoading && <div className="card text-slate-500">{t('forecast.loading')}</div>}

      {chartData && (
        <>
          <div className="card">
            <h2 className="text-lg font-semibold mb-4">{t('forecast.chartTitle')}</h2>
            <div style={{ width: '100%', height: 360 }}>
              <ResponsiveContainer>
                <ComposedChart data={chartData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                  <XAxis dataKey="hour" />
                  <YAxis yAxisId="left" />
                  <YAxis yAxisId="right" orientation="right" domain={[0, 100]} unit="%" />
                  <Tooltip />
                  <Legend />
                  <Bar yAxisId="left" dataKey="free" name={t('forecast.free')} fill="#10b981" />
                  <Line yAxisId="right" type="monotone" dataKey="occupancyPct" name={t('forecast.occupancyPct')} stroke="#ef4444" strokeWidth={2} />
                </ComposedChart>
              </ResponsiveContainer>
            </div>
          </div>

          <div className="card">
            <h2 className="text-lg font-semibold mb-3">{t('forecast.byHour')}</h2>
            <div className="grid grid-cols-3 sm:grid-cols-6 lg:grid-cols-12 gap-2">
              {chartData.map((h, i) => {
                const color =
                  h.occupancyPct > 85 ? 'bg-red-100 text-red-700'
                  : h.occupancyPct > 60 ? 'bg-amber-100 text-amber-700'
                  : 'bg-emerald-100 text-emerald-700';
                return (
                  <div key={i} className={`rounded-md p-2 text-center text-xs ${color}`}>
                    <div className="font-mono">{h.hour}</div>
                    <div className="font-bold text-base">{h.occupancyPct}%</div>
                    <div className="text-[10px] opacity-70">{t('forecast.hourFree', { count: h.free })}</div>
                  </div>
                );
              })}
            </div>
          </div>
        </>
      )}
    </div>
  );
}

function ModelCard({ model, locale }: { model: ModelInfo; locale: string }) {
  const { t } = useTranslation();
  const role = useAuthStore((s) => s.role);
  const canRetrain = role === 'ADMIN' || role === 'OPERATOR';
  const qc = useQueryClient();
  const retrain = useMutation({
    mutationFn: mlApi.retrain,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['ml', 'model'] });
      qc.invalidateQueries({ queryKey: ['forecast'] });
    },
  });

  if (!model.trained) {
    return (
      <div className="card bg-amber-50 border-amber-200">
        <div className="flex justify-between items-center">
          <div>
            <h3 className="font-semibold">{t('forecast.model.notTrainedTitle')}</h3>
            <p className="text-sm text-slate-600 mt-1">{t('forecast.model.notTrainedText')}</p>
          </div>
          {canRetrain && (
            <button className="btn-primary" onClick={() => retrain.mutate()} disabled={retrain.isPending}>
              {retrain.isPending ? '...' : t('forecast.model.trainNow')}
            </button>
          )}
        </div>
      </div>
    );
  }

  return (
    <div className="card">
      <div className="flex items-start justify-between gap-4">
        <div className="flex-1">
          <h3 className="font-semibold text-sm text-slate-700 uppercase tracking-wide">
            {t('forecast.model.title')}
          </h3>
          <p className="text-base mt-1">{model.algorithm}</p>
          <p className="text-xs text-slate-500 mt-1">
            {t('forecast.model.trainedAt', { date: model.trainedAt ? new Date(model.trainedAt).toLocaleString(locale) : '—' })}
            {' · '}
            {t('forecast.model.features', { features: model.features.join(', ') })}
          </p>
        </div>
        {canRetrain && (
          <button className="btn-secondary" onClick={() => retrain.mutate()} disabled={retrain.isPending}>
            {retrain.isPending ? '...' : t('forecast.model.retrain')}
          </button>
        )}
      </div>
      <div className="grid grid-cols-2 sm:grid-cols-5 gap-3 mt-4">
        <Metric label={t('forecast.model.train')} value={model.trainSamples.toString()} hint={t('forecast.model.trainHint')} />
        <Metric label={t('forecast.model.test')}  value={model.testSamples.toString()}  hint={t('forecast.model.trainHint')} />
        <Metric label="RMSE" value={model.rmse?.toFixed(4) ?? '—'} hint={t('forecast.model.rmseHint')} tone="violet" />
        <Metric label="MAE"  value={model.mae?.toFixed(4)  ?? '—'} hint={t('forecast.model.maeHint')}  tone="violet" />
        <Metric label="R²"   value={model.r2?.toFixed(4)   ?? '—'} hint={t('forecast.model.r2Hint')}   tone="emerald" />
      </div>
    </div>
  );
}

function Metric({ label, value, hint, tone = 'slate' }: { label: string; value: string; hint: string; tone?: 'slate' | 'violet' | 'emerald' }) {
  const bg = tone === 'violet' ? 'bg-violet-50' : tone === 'emerald' ? 'bg-emerald-50' : 'bg-slate-50';
  const fg = tone === 'violet' ? 'text-violet-700' : tone === 'emerald' ? 'text-emerald-700' : 'text-slate-700';
  return (
    <div className={`${bg} rounded-md p-2`}>
      <div className="text-xs text-slate-500">{label}</div>
      <div className={`text-lg font-bold tabular-nums ${fg}`}>{value}</div>
      <div className="text-[10px] text-slate-400">{hint}</div>
    </div>
  );
}
