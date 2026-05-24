import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { accessEventsApi, type AccessEventType } from '@/api/accessEvents';
import { parkingsApi } from '@/api/parkings';

const TYPE_BADGE: Record<AccessEventType, string> = {
  ENTRY: 'bg-emerald-100 text-emerald-700',
  EXIT: 'bg-amber-100 text-amber-700',
};

export function AdminAccessEventsPage() {
  const { t, i18n } = useTranslation();
  const qc = useQueryClient();
  const [filterParking, setFilterParking] = useState<number | undefined>(undefined);
  const parkings = useQuery({ queryKey: ['parkings'], queryFn: parkingsApi.list });
  const events = useQuery({
    queryKey: ['access-events', filterParking],
    queryFn: () => accessEventsApi.list({ parkingId: filterParking, page: 0, size: 50 }),
    refetchInterval: 30_000,
  });
  const locale = i18n.resolvedLanguage === 'kk' ? 'kk-KZ' : i18n.resolvedLanguage === 'en' ? 'en-US' : 'ru-RU';

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold">{t('adminAccess.title')}</h1>
          <p className="text-sm text-slate-500 mt-1">{t('adminAccess.subtitle')}</p>
        </div>
        <select
          className="input w-64"
          value={filterParking ?? ''}
          onChange={(e) => setFilterParking(e.target.value ? Number(e.target.value) : undefined)}
        >
          <option value="">{t('adminAccess.allParkings')}</option>
          {parkings.data?.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
        </select>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <div className="lg:col-span-2 card overflow-x-auto">
          <table className="min-w-full text-sm">
            <thead>
              <tr className="text-left text-slate-500 border-b border-slate-200">
                <th className="py-2 pr-4">{t('adminAccess.time')}</th>
                <th className="py-2 pr-4">{t('adminAccess.parking')}</th>
                <th className="py-2 pr-4">{t('adminAccess.plate')}</th>
                <th className="py-2 pr-4">{t('adminAccess.type')}</th>
                <th className="py-2 pr-4">{t('adminAccess.inDb')}</th>
              </tr>
            </thead>
            <tbody>
              {events.data?.content.length === 0 && (
                <tr><td colSpan={5} className="py-6 text-center text-slate-400">{t('adminAccess.empty')}</td></tr>
              )}
              {events.data?.content.map((e) => (
                <tr key={e.id} className="border-b border-slate-100 last:border-0">
                  <td className="py-2 pr-4 text-slate-600 font-mono text-xs">
                    {new Date(e.timestamp).toLocaleString(locale)}
                  </td>
                  <td className="py-2 pr-4">{e.parkingName}</td>
                  <td className="py-2 pr-4 font-mono font-semibold">{e.licensePlateRecognized}</td>
                  <td className="py-2 pr-4">
                    <span className={`badge ${TYPE_BADGE[e.eventType]}`}>
                      {e.eventType === 'ENTRY' ? t('adminAccess.entry') : t('adminAccess.exit')}
                    </span>
                  </td>
                  <td className="py-2 pr-4">
                    {e.vehicleId
                      ? <span className="text-emerald-600">{t('adminAccess.registered')}</span>
                      : <span className="text-slate-400">{t('adminAccess.notFound')}</span>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <SimulatorPanel onRecorded={() => qc.invalidateQueries({ queryKey: ['access-events'] })} />
      </div>
    </div>
  );
}

function SimulatorPanel({ onRecorded }: { onRecorded: () => void }) {
  const { t } = useTranslation();
  const parkings = useQuery({ queryKey: ['parkings'], queryFn: parkingsApi.list });
  const [parkingId, setParkingId] = useState<number | undefined>(undefined);
  const [plate, setPlate] = useState('');
  const [type, setType] = useState<AccessEventType>('ENTRY');
  const [error, setError] = useState<string | null>(null);

  const record = useMutation({
    mutationFn: () => accessEventsApi.record({
      parkingId: parkingId!,
      licensePlate: plate,
      eventType: type,
    }),
    onSuccess: () => {
      setPlate('');
      setError(null);
      onRecorded();
    },
    onError: (err: any) => setError(err?.response?.data?.message ?? t('common.error')),
  });

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!parkingId || !plate.trim()) return;
    record.mutate();
  };

  return (
    <div className="card">
      <h2 className="text-lg font-semibold mb-3">{t('adminAccess.simulator')}</h2>
      <form onSubmit={submit} className="space-y-3">
        <div>
          <label className="label">{t('adminAccess.parking')}</label>
          <select className="input" value={parkingId ?? ''} onChange={(e) => setParkingId(Number(e.target.value) || undefined)} required>
            <option value="">{t('adminAccess.chooseParking')}</option>
            {parkings.data?.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
          </select>
        </div>
        <div>
          <label className="label">{t('adminAccess.licensePlate')}</label>
          <input
            className="input font-mono uppercase"
            placeholder="A123BC77"
            value={plate}
            onChange={(e) => setPlate(e.target.value.toUpperCase())}
            required
          />
        </div>
        <div>
          <label className="label">{t('adminAccess.event')}</label>
          <div className="flex gap-2">
            <button type="button" onClick={() => setType('ENTRY')}
              className={`btn flex-1 ${type === 'ENTRY' ? 'bg-emerald-600 text-white' : 'btn-secondary'}`}>
              {t('adminAccess.entry')}
            </button>
            <button type="button" onClick={() => setType('EXIT')}
              className={`btn flex-1 ${type === 'EXIT' ? 'bg-amber-600 text-white' : 'btn-secondary'}`}>
              {t('adminAccess.exit')}
            </button>
          </div>
        </div>
        {error && <div className="text-sm text-red-600 bg-red-50 border border-red-200 rounded p-2">{error}</div>}
        <button type="submit" className="btn-primary w-full" disabled={record.isPending}>
          {record.isPending ? t('adminAccess.recording') : t('adminAccess.record')}
        </button>
      </form>
    </div>
  );
}
