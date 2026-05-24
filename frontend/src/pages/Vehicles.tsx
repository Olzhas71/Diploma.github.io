import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { vehiclesApi, type VehicleInput } from '@/api/vehicles';

export function VehiclesPage() {
  const { t } = useTranslation();
  const qc = useQueryClient();
  const { data, isLoading } = useQuery({ queryKey: ['vehicles'], queryFn: vehiclesApi.list });
  const [editing, setEditing] = useState<{ id?: number; input: VehicleInput } | null>(null);
  const [error, setError] = useState<string | null>(null);

  const save = useMutation({
    mutationFn: () =>
      editing!.id
        ? vehiclesApi.update(editing!.id!, editing!.input)
        : vehiclesApi.create(editing!.input),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['vehicles'] });
      setEditing(null);
      setError(null);
    },
    onError: (err: any) => setError(err?.response?.data?.message ?? t('common.error')),
  });

  const remove = useMutation({
    mutationFn: (id: number) => vehiclesApi.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['vehicles'] }),
  });

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">{t('vehicles.title')}</h1>
        <button className="btn-primary" onClick={() => setEditing({ input: { licensePlate: '' } })}>
          {t('vehicles.add')}
        </button>
      </div>

      {isLoading && <div className="text-slate-500">{t('common.loading')}</div>}
      {data && data.length === 0 && (
        <div className="card text-slate-500 text-center">{t('vehicles.empty')}</div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
        {data?.map((v) => (
          <div key={v.id} className="card flex items-center justify-between">
            <div>
              <div className="text-lg font-mono font-semibold">{v.licensePlate}</div>
              <div className="text-sm text-slate-600">
                {[v.make, v.model, v.color].filter(Boolean).join(' · ') || '—'}
              </div>
            </div>
            <div className="flex gap-2">
              <button className="btn-secondary" onClick={() => setEditing({ id: v.id, input: v })}>
                {t('vehicles.edit')}
              </button>
              <button className="btn-danger" onClick={() => remove.mutate(v.id)}>
                {t('vehicles.delete')}
              </button>
            </div>
          </div>
        ))}
      </div>

      {editing && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-20 p-4" onClick={() => setEditing(null)}>
          <div className="card w-full max-w-md" onClick={(e) => e.stopPropagation()}>
            <h3 className="text-lg font-semibold mb-4">
              {editing.id ? t('vehicles.editTitle') : t('vehicles.newTitle')}
            </h3>
            <div className="space-y-3">
              <Field label={t('vehicles.licensePlate')} value={editing.input.licensePlate} onChange={(v) => setEditing({ ...editing, input: { ...editing.input, licensePlate: v } })} required />
              <Field label={t('vehicles.make')}  value={editing.input.make  ?? ''} onChange={(v) => setEditing({ ...editing, input: { ...editing.input, make: v } })} />
              <Field label={t('vehicles.model')} value={editing.input.model ?? ''} onChange={(v) => setEditing({ ...editing, input: { ...editing.input, model: v } })} />
              <Field label={t('vehicles.color')} value={editing.input.color ?? ''} onChange={(v) => setEditing({ ...editing, input: { ...editing.input, color: v } })} />
              {error && <div className="text-sm text-red-600 bg-red-50 border border-red-200 rounded p-2">{error}</div>}
              <div className="flex gap-2 pt-2">
                <button className="btn-secondary flex-1" onClick={() => setEditing(null)}>{t('vehicles.cancel')}</button>
                <button className="btn-primary flex-1" disabled={save.isPending} onClick={() => save.mutate()}>
                  {save.isPending ? t('vehicles.saving') : t('vehicles.save')}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function Field({ label, value, onChange, required }: { label: string; value: string; onChange: (v: string) => void; required?: boolean }) {
  return (
    <div>
      <label className="label">{label}{required && <span className="text-red-500">*</span>}</label>
      <input className="input" value={value} onChange={(e) => onChange(e.target.value)} required={required} />
    </div>
  );
}
