import { api } from './client';
import type { Page } from '@/types';

export type AccessEventType = 'ENTRY' | 'EXIT';

export interface AccessEventResponse {
  id: number;
  parkingId: number;
  parkingName: string;
  vehicleId: number | null;
  licensePlateRecognized: string;
  eventType: AccessEventType;
  timestamp: string;
  photoUrl: string | null;
}

export const accessEventsApi = {
  list: (params: { parkingId?: number; page?: number; size?: number } = {}) =>
    api.get<Page<AccessEventResponse>>('/access-events', { params }).then((r) => r.data),

  record: (input: { parkingId: number; licensePlate: string; eventType: AccessEventType; photoUrl?: string }) =>
    api.post<AccessEventResponse>('/access-events', input).then((r) => r.data),
};
