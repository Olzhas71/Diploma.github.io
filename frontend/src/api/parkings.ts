import { api } from './client';
import type { ParkingResponse, SpotResponse, TariffResponse } from '@/types';

export const parkingsApi = {
  list: () => api.get<ParkingResponse[]>('/parkings').then((r) => r.data),
  get: (id: number) => api.get<ParkingResponse>(`/parkings/${id}`).then((r) => r.data),
  nearby: (lat: number, lon: number, radiusKm = 5) =>
    api.get<ParkingResponse[]>('/parkings/nearby', { params: { lat, lon, radiusKm } }).then((r) => r.data),
  create: (data: Omit<ParkingResponse, 'id' | 'freeSpots'>) =>
    api.post<ParkingResponse>('/parkings', data).then((r) => r.data),
  spots: (id: number) =>
    api.get<SpotResponse[]>(`/parkings/${id}/spots`).then((r) => r.data),
  bulkAddSpots: (id: number, count: number, prefix = 'S') =>
    api.post<SpotResponse[]>(`/parkings/${id}/spots/bulk`, null, { params: { count, prefix } })
       .then((r) => r.data),
  tariffs: (id: number) =>
    api.get<TariffResponse[]>(`/parkings/${id}/tariffs`).then((r) => r.data),
  addTariff: (id: number, data: { name: string; pricePerHour: number; currency?: string }) =>
    api.post<TariffResponse>(`/parkings/${id}/tariffs`, { dynamicMultiplier: 1.0, currency: 'USD', ...data })
       .then((r) => r.data),
  remove: (id: number) =>
    api.delete(`/parkings/${id}`).then(() => undefined),
};
