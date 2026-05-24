import { api } from './client';
import type { BookingResponse, Page } from '@/types';

export const bookingsApi = {
  myBookings: (page = 0, size = 20) =>
    api.get<Page<BookingResponse>>('/bookings', { params: { page, size, sort: 'startTime,desc' } })
       .then((r) => r.data),

  create: (input: { spotId: number; vehicleId?: number; startTime: string; endTime: string }) =>
    api.post<BookingResponse>('/bookings', input).then((r) => r.data),

  cancel: (id: number) => api.post<BookingResponse>(`/bookings/${id}/cancel`).then((r) => r.data),
  activate: (id: number) => api.post<BookingResponse>(`/bookings/${id}/activate`).then((r) => r.data),
  complete: (id: number) => api.post<BookingResponse>(`/bookings/${id}/complete`).then((r) => r.data),
};
