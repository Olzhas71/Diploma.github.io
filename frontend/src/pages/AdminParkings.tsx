import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { parkingsApi } from '@/api/parkings';
import type { ParkingResponse, ParkingType } from '@/types';

export function AdminParkingsPage() {
  const { t } = useTranslation();
  const qc = useQueryClient();
  const { data } = useQuery({ queryKey: ['parkings'], queryFn: parkingsApi.list });
  const [creating, setCreating] = useState(false);
  const [managing, setManaging] = useState<ParkingResponse | null>(null);
  const [form, setForm] = useState({
    name: '',
    address: '',
    latitude: 43.2389,
    longitude: 76.8897,
    type: 'GROUND' as ParkingType,
    totalSpots: 10,
  });
  const [error, setError] = useState<string | null>(null);

  const create = useMutation({
    mutationFn: () => parkingsApi.create(form),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['parkings'] });
      setCreating(false);
      setError(null);
    },
    onError: (err: any) => setError(err?.response?.data?.message ?? t('common.error')),
  });

  const remove = useMutation({
    mutationFn: (id: number) => parkingsApi.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['parkings'] }),
  });

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">{t('adminParkings.title')}</h1>
        <button className="btn-primary" onClick={() => setCreating(true)}>{t('adminParkings.new')}</button>
      </div>

      <div className="card overflow-x-auto">
        <table className="min-w-full text-sm">
          <thead>
            <tr className="text-left text-slate-500 border-b border-slate-200">
              <th className="py-2 pr-4">{t('adminParkings.id')}</th>
              <th className="py-2 pr-4">{t('adminParkings.name')}</th>
              <th className="py-2 pr-4">{t('adminParkings.address')}</th>
              <th className="py-2 pr-4">{t('adminParkings.type')}</th>
              <th className="py-2 pr-4">{t('adminParkings.spots')}</th>
              <th className="py-2 pr-4">{t('adminParkings.coords')}</th>
              <th className="py-2 pr-4 text-right">{t('adminParkings.actions')}</th>
            </tr>
          </thead>
          <tbody>
            {data?.map((p) => {
              const empty = p.totalSpots === 0 || (p.freeSpots === 0 && p.totalSpots > 0);
              return (
                <tr key={p.id} className="border-b border-slate-100 last:border-0">
                  <td className="py-2 pr-4 font-mono">{p.id}</td>
                  <td className="py-2 pr-4 font-medium">
                    <Link to={`/parkings/${p.id}`} className="hover:underline">{p.name}</Link>
                  </td>
                  <td className="py-2 pr-4 text-slate-600">{p.address}</td>
                  <td className="py-2 pr-4"><span className="badge bg-slate-100 text-slate-700">{t(`parkingTypes.${p.type}`)}</span></td>
                  <td className="py-2 pr-4 tabular-nums">
                    <span className={empty ? 'text-red-600 font-medium' : ''}>
                      {p.freeSpots} / {p.totalSpots}
                    </span>
                  </td>
                  <td className="py-2 pr-4 text-slate-500 font-mono text-xs">{p.latitude.toFixed(4)}, {p.longitude.toFixed(4)}</td>
                  <td className="py-2 pl-4 text-right whitespace-nowrap">
                    <button className="btn-secondary" onClick={() => setManaging(p)}>{t('adminParkings.manage')}</button>
                    <button className="btn-danger ml-2" onClick={() => {
                      if (confirm(t('adminParkings.deleteConfirm', { name: p.name }))) {
                        remove.mutate(p.id);
                      }
                    }}>×</button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {creating && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-20 p-4" onClick={() => setCreating(false)}>
          <div className="card w-full max-w-md" onClick={(e) => e.stopPropagation()}>
            <h3 className="text-lg font-semibold mb-4">{t('adminParkings.newDialog')}</h3>
            <p className="text-xs text-slate-500 mb-3">{t('adminParkings.newHint', { count: form.totalSpots })}</p>
            <div className="space-y-3">
              <div>
                <label className="label">{t('adminParkings.name')}</label>
                <input className="input" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
              </div>
              <div>
                <label className="label">{t('adminParkings.address')}</label>
                <input className="input" value={form.address} onChange={(e) => setForm({ ...form, address: e.target.value })} />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="label">{t('adminParkings.latitude')}</label>
                  <input className="input" type="number" step="0.0001" value={form.latitude}
                         onChange={(e) => setForm({ ...form, latitude: Number(e.target.value) })} />
                </div>
                <div>
                  <label className="label">{t('adminParkings.longitude')}</label>
                  <input className="input" type="number" step="0.0001" value={form.longitude}
                         onChange={(e) => setForm({ ...form, longitude: Number(e.target.value) })} />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="label">{t('adminParkings.type')}</label>
                  <select className="input" value={form.type} onChange={(e) => setForm({ ...form, type: e.target.value as ParkingType })}>
                    <option value="GROUND">{t('parkingTypes.GROUND')}</option>
                    <option value="UNDERGROUND">{t('parkingTypes.UNDERGROUND')}</option>
                    <option value="MULTILEVEL">{t('parkingTypes.MULTILEVEL')}</option>
                    <option value="STREET">{t('parkingTypes.STREET')}</option>
                  </select>
                </div>
                <div>
                  <label className="label">{t('adminParkings.totalSpots')}</label>
                  <input className="input" type="number" min={1} max={10000} value={form.totalSpots}
                         onChange={(e) => setForm({ ...form, totalSpots: Number(e.target.value) })} />
                </div>
              </div>
              {error && <div className="text-sm text-red-600 bg-red-50 border border-red-200 rounded p-2">{error}</div>}
              <div className="flex gap-2 pt-2">
                <button className="btn-secondary flex-1" onClick={() => setCreating(false)}>{t('common.cancel')}</button>
                <button className="btn-primary flex-1" disabled={create.isPending} onClick={() => create.mutate()}>
                  {create.isPending ? t('adminParkings.creating') : t('adminParkings.create')}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {managing && <ManageDialog parking={managing} onClose={() => setManaging(null)} />}
    </div>
  );
}

function ManageDialog({ parking, onClose }: { parking: ParkingResponse; onClose: () => void }) {
  const { t } = useTranslation();
  const qc = useQueryClient();
  const spots = useQuery({
    queryKey: ['parking', parking.id, 'spots'],
    queryFn: () => parkingsApi.spots(parking.id),
  });
  const tariffs = useQuery({
    queryKey: ['parking', parking.id, 'tariffs'],
    queryFn: () => parkingsApi.tariffs(parking.id),
  });

  const [bulkCount, setBulkCount] = useState(10);
  const [bulkPrefix, setBulkPrefix] = useState('S');
  const [tariffName, setTariffName] = useState('');
  const [tariffPrice, setTariffPrice] = useState(5);

  const bulk = useMutation({
    mutationFn: () => parkingsApi.bulkAddSpots(parking.id, bulkCount, bulkPrefix),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['parking', parking.id, 'spots'] });
      qc.invalidateQueries({ queryKey: ['parkings'] });
    },
  });

  const addTariff = useMutation({
    mutationFn: () => parkingsApi.addTariff(parking.id, { name: tariffName, pricePerHour: tariffPrice }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['parking', parking.id, 'tariffs'] });
      setTariffName('');
    },
  });

  const spotCount = spots.data?.length ?? 0;
  const isEmpty = spotCount === 0;

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-20 p-4" onClick={onClose}>
      <div className="card w-full max-w-2xl max-h-[90vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-start justify-between mb-4">
          <div>
            <h3 className="text-lg font-semibold">{t('adminParkings.manageTitle', { name: parking.name })}</h3>
            <p className="text-sm text-slate-500">{parking.address}</p>
          </div>
          <button className="text-slate-400 hover:text-slate-600 text-xl leading-none" onClick={onClose}>×</button>
        </div>

        {isEmpty && (
          <div className="bg-amber-50 border border-amber-200 rounded p-3 mb-4 text-sm">
            {t('adminParkings.emptyWarning')}
          </div>
        )}

        <section className="mb-6">
          <h4 className="font-semibold mb-2">{t('adminParkings.spotsHeading', { count: spotCount })}</h4>
          <div className="bg-slate-50 rounded p-3 space-y-3">
            <div className="grid grid-cols-3 gap-2 items-end">
              <div>
                <label className="label">{t('adminParkings.prefix')}</label>
                <input className="input font-mono" value={bulkPrefix} onChange={(e) => setBulkPrefix(e.target.value)} />
              </div>
              <div>
                <label className="label">{t('adminParkings.howMany')}</label>
                <input className="input" type="number" min={1} max={1000} value={bulkCount}
                       onChange={(e) => setBulkCount(Number(e.target.value))} />
              </div>
              <button className="btn-primary" onClick={() => bulk.mutate()} disabled={bulk.isPending}>
                {bulk.isPending ? '...' : t('adminParkings.addNSpots', { count: bulkCount })}
              </button>
            </div>
            <div className="text-xs text-slate-500">
              {t('adminParkings.willCreate', {
                from: `${bulkPrefix}${nextStart(spots.data ?? [], bulkPrefix)}`,
                to:   `${bulkPrefix}${nextStart(spots.data ?? [], bulkPrefix) + bulkCount - 1}`,
              })}
            </div>
          </div>

          {spotCount > 0 && (
            <div className="mt-3 grid grid-cols-8 sm:grid-cols-12 gap-1 max-h-48 overflow-y-auto">
              {spots.data?.map((s) => (
                <span key={s.id} className="bg-emerald-100 text-emerald-700 text-xs rounded px-1 py-0.5 text-center font-mono">
                  {s.spotNumber}
                </span>
              ))}
            </div>
          )}
        </section>

        <section>
          <h4 className="font-semibold mb-2">{t('adminParkings.tariffsHeading', { count: tariffs.data?.length ?? 0 })}</h4>
          <div className="space-y-2 mb-3">
            {tariffs.data?.map((tar) => (
              <div key={tar.id} className="flex justify-between bg-slate-50 rounded p-2 text-sm">
                <span>{tar.name}</span>
                <span className="font-semibold">{tar.pricePerHour} {tar.currency}/{t('adminParkings.usdPerHour').split('/')[1] ?? 'h'}</span>
              </div>
            ))}
          </div>
          <div className="bg-slate-50 rounded p-3 grid grid-cols-3 gap-2 items-end">
            <div className="col-span-2">
              <label className="label">{t('adminParkings.tariffName')}</label>
              <input className="input" placeholder={t('adminParkings.tariffPlaceholder')} value={tariffName}
                     onChange={(e) => setTariffName(e.target.value)} />
            </div>
            <div>
              <label className="label">{t('adminParkings.usdPerHour')}</label>
              <input className="input" type="number" step="0.5" min={0} value={tariffPrice}
                     onChange={(e) => setTariffPrice(Number(e.target.value))} />
            </div>
            <button className="btn-primary col-span-3"
                    disabled={!tariffName.trim() || addTariff.isPending}
                    onClick={() => addTariff.mutate()}>
              {addTariff.isPending ? '...' : t('adminParkings.addTariff')}
            </button>
          </div>
        </section>
      </div>
    </div>
  );
}

function nextStart(spots: { spotNumber: string }[], prefix: string): number {
  let max = 0;
  for (const s of spots) {
    if (s.spotNumber.startsWith(prefix)) {
      const v = parseInt(s.spotNumber.substring(prefix.length), 10);
      if (!isNaN(v) && v > max) max = v;
    }
  }
  return max + 1;
}
