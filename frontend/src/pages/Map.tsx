import { useEffect, useMemo, useState } from 'react';
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
import L from 'leaflet';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { parkingsApi } from '@/api/parkings';

delete (L.Icon.Default.prototype as any)._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
});

function occupancyIcon(freeSpots: number, totalSpots: number) {
  const ratio = totalSpots > 0 ? 1 - freeSpots / totalSpots : 0;
  const color = ratio > 0.85 ? '#ef4444' : ratio > 0.6 ? '#f59e0b' : '#10b981';
  const label = totalSpots > 0 ? String(freeSpots) : '–';
  const html = `
    <div style="
      background:${color};color:#fff;
      width:38px;height:38px;border-radius:50%;
      border:3px solid white;box-shadow:0 2px 6px rgba(0,0,0,.35);
      display:flex;align-items:center;justify-content:center;
      font-size:13px;font-weight:700;font-family:system-ui,sans-serif;
      transition:transform .2s ease;
    ">${label}</div>`;
  return L.divIcon({
    className: 'parking-occupancy-marker',
    html,
    iconSize: [38, 38],
    iconAnchor: [19, 19],
    popupAnchor: [0, -22],
  });
}

export function MapPage() {
  const { t } = useTranslation();
  const [center, setCenter] = useState<[number, number]>([43.2389, 76.8897]);
  const [radiusKm, setRadiusKm] = useState(10);
  const [hasLocation, setHasLocation] = useState(false);

  useEffect(() => {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          setCenter([pos.coords.latitude, pos.coords.longitude]);
          setHasLocation(true);
        },
        () => {},
        { timeout: 4000 }
      );
    }
  }, []);

  const { data: parkings } = useQuery({
    queryKey: ['parkings', 'nearby', center[0], center[1], radiusKm],
    queryFn: () => parkingsApi.nearby(center[0], center[1], radiusKm),
    refetchInterval: 15_000,
  });

  const icons = useMemo(() => {
    const map = new Map<number, L.DivIcon>();
    parkings?.forEach((p) => map.set(p.id, occupancyIcon(p.freeSpots, p.totalSpots)));
    return map;
  }, [parkings]);

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center gap-3">
        <h1 className="text-2xl font-bold flex-1">{t('map.title')}</h1>
        <label className="text-sm text-slate-600 flex items-center gap-2">
          {t('map.radiusLabel')}
          <input
            type="number"
            className="input w-24"
            min={1}
            max={100}
            value={radiusKm}
            onChange={(e) => setRadiusKm(Number(e.target.value) || 5)}
          />
        </label>
        {!hasLocation && (
          <span className="text-xs text-slate-500">{t('map.geoUnavailable')}</span>
        )}
      </div>

      <div className="flex flex-wrap items-center gap-3 text-xs text-slate-600">
        <span className="text-slate-500">{t('map.legend')}:</span>
        <LegendDot color="#10b981" label={t('map.lowLoad')} />
        <LegendDot color="#f59e0b" label={t('map.medLoad')} />
        <LegendDot color="#ef4444" label={t('map.highLoad')} />
        <span className="text-slate-400 ml-auto">{t('map.autoRefresh')}</span>
      </div>

      <div className="rounded-lg overflow-hidden border border-slate-200" style={{ height: 600 }}>
        <MapContainer center={center} zoom={13} style={{ height: '100%', width: '100%' }}>
          <TileLayer
            attribution='&copy; OpenStreetMap'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          {parkings?.map((p) => (
            <Marker key={p.id} position={[p.latitude, p.longitude]} icon={icons.get(p.id)}>
              <Popup>
                <div className="space-y-1">
                  <div className="font-semibold">{p.name}</div>
                  <div className="text-xs text-slate-600">{p.address}</div>
                  <div className="text-sm">
                    {t('map.free')}: <b>{p.freeSpots}</b> / {p.totalSpots}
                  </div>
                  <Link to={`/parkings/${p.id}`} className="text-brand-600 hover:underline text-sm">
                    {t('map.more')}
                  </Link>
                </div>
              </Popup>
            </Marker>
          ))}
        </MapContainer>
      </div>
    </div>
  );
}

function LegendDot({ color, label }: { color: string; label: string }) {
  return (
    <span className="inline-flex items-center gap-1.5">
      <span
        className="inline-block w-3 h-3 rounded-full border border-white"
        style={{ background: color, boxShadow: '0 0 0 1px rgba(0,0,0,.15)' }}
      />
      {label}
    </span>
  );
}
