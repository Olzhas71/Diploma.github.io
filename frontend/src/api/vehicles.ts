import { api } from './client';
import type { VehicleResponse } from '@/types';

export type VehicleInput = {
  licensePlate: string;
  make?: string;
  model?: string;
  color?: string;
};

export const vehiclesApi = {
  list: () => api.get<VehicleResponse[]>('/vehicles').then((r) => r.data),
  create: (input: VehicleInput) => api.post<VehicleResponse>('/vehicles', input).then((r) => r.data),
  update: (id: number, input: VehicleInput) =>
    api.put<VehicleResponse>(`/vehicles/${id}`, input).then((r) => r.data),
  remove: (id: number) => api.delete(`/vehicles/${id}`).then(() => undefined),
};
