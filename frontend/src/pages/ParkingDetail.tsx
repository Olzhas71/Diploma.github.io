import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Sparkles, AlertTriangle, Radio, WifiOff } from 'lucide-react';
import { parkingsApi } from '@/api/parkings';
import { vehiclesApi } from '@/api/vehicles';
import { bookingsApi } from '@/api/bookings';
import { subscriptionsApi } from '@/api/subscriptions';
import { useOccupancyWebSocket } from '@/hooks/useOccupancyWebSocket';
import { useToast } from '@/components/Toast';
import type { SpotResponse, SpotStatus } from '@/types';

const STATUS_COLOR: Record<SpotStatus, string> = {
  FREE: 'bg-emerald-100 text-emerald-700 border-emerald-200',
  OCCUPIED: 'bg-red-100 text-red-700 border-red-200',
  RESERVED: 'bg-amber-100 text-amber-700 border-amber-200',
  MAINTENANCE: 'bg-slate-100 text-slate-500 border-slate-200',
};

export function ParkingDetailPage() {
  const { t, i18n } = useTranslation();
  const { id } = useParams<{ id: string }>();
  const parkingId = Number(id);
  const qc = useQueryClient();

  const { data: parking } = useQuery({
    queryKey: ['parking', parkingId],
    queryFn: () => parkingsApi.get(parkingId),
    enabled: !!parkingId,
  });

  const { data: spots } = useQuery({
    queryKey: ['parking', parkingId, 'spots'],
    queryFn: () => parkingsApi.spots(parkingId),
    enabled: !!parkingId,
  });

  const { data: tariffs } = useQuery({
    queryKey: ['parking', parkingId, 'tariffs'],
    queryFn: () => parkingsApi.tariffs(parkingId),
    enabled: !!parkingId,
  });

  const { data: coverage } = useQuery({
    queryKey: ['subscription', 'coverage', parkingId],
    queryFn: () => subscriptionsApi.coverage(parkingId),
    enabled: !!parkingId,
  });

  const [flashing, setFlashing] = useState<Set<number>>(new Set());

  const wsStatus = useOccupancyWebSocket((event) => {
    if (event.parkingId !== parkingId) return;
    qc.setQueryData<SpotResponse[]>(['parking', parkingId, 'spots'], (prev) =>
      prev?.map((s) => (s.id === event.spotId ? { ...s, status: event.status } : s)) ?? prev
    );
    setFlashing((prev) => {
      const next = new Set(prev);
      next.add(event.spotId);
      return next;
    });
    window.setTimeout(() => {
      setFlashing((prev) => {
        if (!prev.has(event.spotId)) return prev;
        const next = new Set(prev);
        next.delete(event.spotId);
        return next;
      });
    }, 1800);
  });

  // tick every second so relative time in the sensor badge stays fresh
  const [, setNowTick] = useState(0);
  useEffect(() => {
    const id = window.setInterval(() => setNowTick((x) => x + 1), 1000);
    return () => window.clearInterval(id);
  }, []);

  const [selected, setSelected] = useState<SpotResponse | null>(null);

  const locale = i18n.resolvedLanguage === 'kk' ? 'kk-KZ' : i18n.resolvedLanguage === 'en' ? 'en-US' : 'ru-RU';

  return (
    <div className="space-y-6">
      <Link to="/parkings" className="text-sm text-brand-600 hover:underline">
        {t('parkingDetail.backToList')}
      </Link>

      {parking && (
        <div className="card">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <h1 className="text-2xl font-bold">{parking.name}</h1>
              <p className="text-slate-600">{parking.address}</p>
            </div>
            <div className="text-right">
              <div className="text-3xl font-bold text-emerald-600">{parking.freeSpots}</div>
              <div className="text-sm text-slate-500">
                {t('parkingDetail.of')} {parking.totalSpots} {t('parkingDetail.freeSpots')}
              </div>
            </div>
          </div>
          <div className="mt-3 flex gap-2 text-xs flex-wrap">
            <span className="badge bg-slate-100 text-slate-700">{t(`parkingTypes.${parking.type}`)}</span>
            {parking.workingHoursFrom && parking.workingHoursTo && (
              <span className="badge bg-slate-100 text-slate-700">
                {parking.workingHoursFrom.slice(0, 5)} – {parking.workingHoursTo.slice(0, 5)}
              </span>
            )}
            <Link to={`/forecast/${parking.id}`} className="badge bg-brand-100 text-brand-700 hover:bg-brand-200">
              📈 {t('common.forecast')}
            </Link>
          </div>
        </div>
      )}

      {coverage && <CoverageBanner sub={coverage} locale={locale} />}

      {tariffs && tariffs.length > 0 && (
        <div className="card">
          <h2 className="text-lg font-semibold mb-3">{t('parkingDetail.tariffs')}</h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
            {tariffs.map((tar) => (
              <div key={tar.id} className="border border-slate-200 rounded-md p-3">
                <div className="font-medium">{tar.name}</div>
                <div className="text-2xl font-bold mt-1">
                  {tar.pricePerHour}{' '}
                  <span className="text-sm font-normal text-slate-500">
                    {t('parkingDetail.pricePerHour', { currency: tar.currency })}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      <div className="card">
        <div className="flex flex-wrap items-center justify-between gap-2 mb-3">
          <h2 className="text-lg font-semibold">{t('parkingDetail.spots')}</h2>
          <SensorStatusBadge connected={wsStatus.connected} lastEventAt={wsStatus.lastEventAt} />
        </div>
        <div className="grid grid-cols-4 sm:grid-cols-6 md:grid-cols-8 lg:grid-cols-10 gap-2">
          {spots?.map((s) => {
            const isFlashing = flashing.has(s.id);
            return (
              <button
                key={s.id}
                onClick={() => s.status === 'FREE' && setSelected(s)}
                disabled={s.status !== 'FREE'}
                className={`border rounded-md py-3 text-sm font-mono transition duration-300 ${STATUS_COLOR[s.status]} ${
                  s.status === 'FREE' ? 'cursor-pointer hover:scale-105' : 'cursor-not-allowed opacity-70'
                } ${isFlashing ? 'ring-2 ring-brand-500 ring-offset-1 scale-110 shadow-md' : ''}`}
                title={`${s.spotNumber} · ${s.type} · ${t(`spotStatus.${s.status}`)}`}
              >
                {s.spotNumber}
              </button>
            );
          })}
        </div>
        <div className="flex flex-wrap gap-3 mt-4 text-xs text-slate-600">
          <Legend color="bg-emerald-100 border-emerald-200" label={t('spotStatus.FREE')} />
          <Legend color="bg-red-100 border-red-200" label={t('spotStatus.OCCUPIED')} />
          <Legend color="bg-amber-100 border-amber-200" label={t('spotStatus.RESERVED')} />
          <Legend color="bg-slate-100 border-slate-200" label={t('spotStatus.MAINTENANCE')} />
        </div>
      </div>

      {selected && (
        <BookingDialog spot={selected} onClose={() => setSelected(null)} parkingId={parkingId} hasCoverage={!!coverage} />
      )}
    </div>
  );
}

function CoverageBanner({ sub, locale }: { sub: { validTo: string; price: number; currency: string }; locale: string }) {
  const { t } = useTranslation();
  const daysLeft = Math.max(0, Math.ceil((new Date(sub.validTo).getTime() - Date.now()) / 86_400_000));
  const expiringSoon = daysLeft <= 7;
  return (
    <div className={`card ${expiringSoon ? 'bg-amber-50 border-amber-200' : 'bg-gradient-to-r from-accent-50 to-brand-50 border-accent-200'}`}>
      <div className="flex items-start gap-3">
        <div className={`w-10 h-10 rounded-xl grid place-items-center shrink-0 ${expiringSoon ? 'bg-amber-100 text-amber-700' : 'bg-accent-100 text-accent-700'}`}>
          {expiringSoon ? <AlertTriangle className="w-5 h-5" /> : <Sparkles className="w-5 h-5" />}
        </div>
        <div className="flex-1">
          <div className="font-semibold text-slate-900">{t('subscriptions.coverageActive')}</div>
          <p className="text-sm text-slate-600 mt-0.5">
            {t('subscriptions.coverageValidUntil', {
              date: new Date(sub.validTo).toLocaleDateString(locale),
              count: daysLeft,
            })}
            {expiringSoon && ` · ${t('subscriptions.coverageExpiringSoon')}`}
          </p>
        </div>
        <Link to="/subscriptions" className="btn-secondary text-xs whitespace-nowrap">
          {t('common.manage')}
        </Link>
      </div>
    </div>
  );
}

function Legend({ color, label }: { color: string; label: string }) {
  return (
    <span className="flex items-center gap-1">
      <span className={`inline-block w-3 h-3 border rounded ${color}`} /> {label}
    </span>
  );
}

function SensorStatusBadge({
  connected,
  lastEventAt,
}: {
  connected: boolean;
  lastEventAt: number | null;
}) {
  const { t } = useTranslation();
  if (!connected) {
    return (
      <span className="inline-flex items-center gap-1.5 text-xs px-2 py-1 rounded-md bg-slate-100 text-slate-500 border border-slate-200">
        <WifiOff className="w-3.5 h-3.5" />
        {t('parkingDetail.sensorsOffline')}
      </span>
    );
  }
  const relative = lastEventAt ? formatRelative(lastEventAt, t) : null;
  return (
    <span className="inline-flex items-center gap-2 text-xs px-2 py-1 rounded-md bg-emerald-50 text-emerald-700 border border-emerald-200">
      <span className="relative inline-flex w-2 h-2">
        <span className="absolute inset-0 rounded-full bg-emerald-500 animate-ping opacity-70" />
        <span className="relative inline-block w-2 h-2 rounded-full bg-emerald-500" />
      </span>
      <Radio className="w-3.5 h-3.5" />
      <span>{t('parkingDetail.sensorsOnline')}</span>
      {relative && <span className="text-emerald-600/70">· {relative}</span>}
    </span>
  );
}

function formatRelative(ts: number, t: (k: string, opts?: any) => string): string {
  const diff = Math.max(0, Math.floor((Date.now() - ts) / 1000));
  if (diff < 3) return t('parkingDetail.justNow');
  if (diff < 60) return t('parkingDetail.secondsAgo', { count: diff });
  const mins = Math.floor(diff / 60);
  if (mins < 60) return t('parkingDetail.minutesAgo', { count: mins });
  return t('parkingDetail.hoursAgo', { count: Math.floor(mins / 60) });
}

function BookingDialog({
  spot,
  onClose,
  parkingId,
  hasCoverage,
}: {
  spot: SpotResponse;
  onClose: () => void;
  parkingId: number;
  hasCoverage: boolean;
}) {
  const { t } = useTranslation();
  const qc = useQueryClient();
  const toast = useToast();
  const { data: vehicles } = useQuery({ queryKey: ['vehicles'], queryFn: vehiclesApi.list });
  const inOneHour = new Date(Date.now() + 60 * 60_000);
  const inThree = new Date(Date.now() + 3 * 60 * 60_000);
  const [start, setStart] = useState(localISO(inOneHour));
  const [end, setEnd] = useState(localISO(inThree));
  const [vehicleId, setVehicleId] = useState<number | undefined>(undefined);
  const [error, setError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: () =>
      bookingsApi.create({
        spotId: spot.id,
        vehicleId,
        startTime: new Date(start).toISOString(),
        endTime: new Date(end).toISOString(),
      }),
    onSuccess: (booking) => {
      qc.invalidateQueries({ queryKey: ['parking', parkingId, 'spots'] });
      qc.invalidateQueries({ queryKey: ['bookings'] });
      if (booking.coveredBySubscription) {
        toast.success(t('parkingDetail.bookedCoveredToast'));
      } else {
        toast.success(t('parkingDetail.bookedToast', { amount: booking.totalAmount, currency: booking.currency }));
      }
      onClose();
    },
    onError: (err: any) => {
      setError(err?.response?.data?.message ?? t('parkingDetail.bookFailed'));
    },
  });

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-20 p-4" onClick={onClose}>
      <div className="card w-full max-w-md" onClick={(e) => e.stopPropagation()}>
        <h3 className="text-lg font-semibold mb-4">
          {t('parkingDetail.bookSpot', { number: spot.spotNumber })}
        </h3>
        {hasCoverage && (
          <div className="mb-4 flex items-center gap-2 text-sm text-accent-700 bg-accent-50 border border-accent-200 rounded-lg px-3 py-2">
            <Sparkles className="w-4 h-4" />
            <span>{t('subscriptions.bookingFreeByCoverage')}</span>
          </div>
        )}
        <div className="space-y-3">
          <div>
            <label className="label">{t('parkingDetail.start')}</label>
            <input type="datetime-local" className="input" value={start} onChange={(e) => setStart(e.target.value)} />
          </div>
          <div>
            <label className="label">{t('parkingDetail.end')}</label>
            <input type="datetime-local" className="input" value={end} onChange={(e) => setEnd(e.target.value)} />
          </div>
          <div>
            <label className="label">{t('parkingDetail.vehicleOptional')}</label>
            <select className="input" value={vehicleId ?? ''} onChange={(e) => setVehicleId(e.target.value ? Number(e.target.value) : undefined)}>
              <option value="">{t('parkingDetail.vehicleNone')}</option>
              {vehicles?.map((v) => (
                <option key={v.id} value={v.id}>
                  {v.licensePlate} {v.make ? `· ${v.make} ${v.model ?? ''}` : ''}
                </option>
              ))}
            </select>
          </div>
          {error && <div className="text-sm text-red-600 bg-red-50 border border-red-200 rounded p-2">{error}</div>}
          <div className="flex gap-2 pt-2">
            <button className="btn-secondary flex-1" onClick={onClose}>{t('common.cancel')}</button>
            <button className="btn-primary flex-1" disabled={mutation.isPending} onClick={() => mutation.mutate()}>
              {mutation.isPending ? t('parkingDetail.booking') : t('parkingDetail.book')}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

function localISO(d: Date) {
  const tzOff = d.getTimezoneOffset() * 60_000;
  return new Date(d.getTime() - tzOff).toISOString().slice(0, 16);
}
