import { api } from './client';
import type { PaymentMethod, PaymentResponse } from '@/types';

export const paymentsApi = {
  pay: (bookingId: number, method: PaymentMethod) =>
    api.post<PaymentResponse>('/payments', { bookingId, method }).then((r) => r.data),
  byBooking: (bookingId: number) =>
    api.get<PaymentResponse>(`/payments/by-booking/${bookingId}`).then((r) => r.data),
};
