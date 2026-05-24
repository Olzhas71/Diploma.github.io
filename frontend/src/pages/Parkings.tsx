import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { MapPin, Map as MapIcon, AlertCircle, Building2, TreePine, Layers, Mountain } from 'lucide-react';
import clsx from 'clsx';
import { parkingsApi } from '@/api/parkings';
import { SkeletonCard } from '@/components/Skeleton';
import { useOccupancyWebSocket } from '@/hooks/useOccupancyWebSocket';
import type { ParkingResponse, ParkingType } from '@/types';

const TYPE_ICON: Record<ParkingType, React.ComponentType<{ className?: string }>> = {
  GROUND: TreePine,
  UNDERGROUND: Mountain,
  MULTILEVEL: Layers,
  STREET: Building2,
};

export function ParkingsPage() {
  const { t } = useTranslation();
  const qc = useQueryClient();
  const { data, isLoading, error } = useQuery({
    queryKey: ['parkings'],
    queryFn: parkingsApi.list,
    refetchInterval: 30_000,
  });

  const [flashing, setFlashing] = useState<Set<number>>(new Set());
  useOccupancyWebSocket((event) => {
    qc.invalidateQueries({ queryKey: ['parkings'] });
    setFlashing((prev) => {
      const next = new Set(prev);
      next.add(event.parkingId);
      return next;
    });
    window.setTimeout(() => {
      setFlashing((prev) => {
        if (!prev.has(event.parkingId)) return prev;
        const next = new Set(prev);
        next.delete(event.parkingId);
        return next;
      });
    }, 1500);
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-3xl font-bold text-slate-900">{t('parkingsList.title')}</h1>
          <p className="text-sm text-slate-500 mt-1">
            {data ? t('parkingsList.subtitle', { count: data.length }) : t('parkingsList.subtitleEmpty')}
          </p>
        </div>
        <Link to="/map" className="btn-secondary">
          <MapIcon className="w-4 h-4" /> {t('common.onMap')}
        </Link>
      </div>

      {error && (
        <div className="card flex items-center gap-3 text-red-700">
          <AlertCircle className="w-5 h-5" />
          {t('parkingsList.loadFailed')}
        </div>
      )}

      {isLoading && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          <SkeletonCard /><SkeletonCard /><SkeletonCard />
        </div>
      )}

      {data && data.length === 0 && (
        <div className="card text-center py-16">
          <Building2 className="w-12 h-12 text-slate-300 mx-auto mb-3" />
          <div className="text-slate-500">{t('parkingsList.empty')}</div>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {data?.map((p) => <ParkingCard key={p.id} parking={p} flashing={flashing.has(p.id)} />)}
      </div>
    </div>
  );
}

function ParkingCard({ parking, flashing }: { parking: ParkingResponse; flashing: boolean }) {
  const { t } = useTranslation();
  const occupancy = parking.totalSpots ? 1 - parking.freeSpots / parking.totalSpots : 0;
  const tone =
    occupancy > 0.85 ? { bar: 'bg-red-500', text: 'text-red-600', label: t('parkingsList.statusFull') }
    : occupancy > 0.6 ? { bar: 'bg-amber-500', text: 'text-amber-600', label: t('parkingsList.statusFilling') }
    : { bar: 'bg-emerald-500', text: 'text-emerald-600', label: t('parkingsList.statusFree') };
  const TypeIcon = TYPE_ICON[parking.type];

  return (
    <Link
      to={`/parkings/${parking.id}`}
      className={clsx(
        'card-hover group transition duration-300',
        flashing && 'ring-2 ring-brand-500 ring-offset-2 shadow-lg'
      )}
    >
      <div className="flex items-start justify-between gap-3 mb-3">
        <div className="flex items-start gap-3 min-w-0">
          <div className="w-10 h-10 rounded-xl bg-brand-50 text-brand-700 grid place-items-center shrink-0 group-hover:bg-brand-100 transition">
            <TypeIcon className="w-5 h-5" />
          </div>
          <div className="min-w-0">
            <h2 className="font-semibold text-slate-900 truncate">{parking.name}</h2>
            <p className="text-xs text-slate-500 flex items-center gap-1 mt-0.5">
              <MapPin className="w-3 h-3" /> {parking.address}
            </p>
          </div>
        </div>
        <span className={clsx('badge text-[10px] uppercase', tone.text, 'bg-transparent')}>
          {tone.label}
        </span>
      </div>

      <div className="flex items-center justify-between text-xs mb-2">
        <span className="text-slate-500">{t(`parkingTypes.${parking.type}`)}</span>
        <span className="tabular-nums text-slate-700">
          <span className={clsx('font-bold text-base', tone.text)}>{parking.freeSpots}</span>
          <span className="text-slate-400"> / {parking.totalSpots}</span>
        </span>
      </div>

      <div className="h-2 rounded-full bg-slate-100 overflow-hidden">
        <div
          className={clsx('h-full rounded-full transition-all duration-500', tone.bar)}
          style={{ width: `${Math.round(occupancy * 100)}%` }}
        />
      </div>
    </Link>
  );
}
