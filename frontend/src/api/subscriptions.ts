import { api } from './client';

export type SubscriptionStatus = 'ACTIVE' | 'EXPIRED' | 'CANCELLED';

export interface SubscriptionResponse {
  id: number;
  userId: number;
  parkingId: number;
  parkingName: string;
  validFrom: string;
  validTo: string;
  price: number;
  currency: string;
  status: SubscriptionStatus;
}

export interface SubscriptionQuote {
  parkingId: number;
  durationDays: number;
  price: number;
  currency: string;
}

export const subscriptionsApi = {
  list: (status?: SubscriptionStatus) =>
    api.get<SubscriptionResponse[]>('/subscriptions', { params: status ? { status } : undefined })
       .then((r) => r.data),

  coverage: (parkingId: number) =>
    api.get<SubscriptionResponse>('/subscriptions/coverage', { params: { parkingId } })
       .then((r) => r.data)
       .catch((err) => (err?.response?.status === 404 ? null : Promise.reject(err))),

  quote: (parkingId: number, durationDays = 30) =>
    api.get<SubscriptionQuote>('/subscriptions/quote', { params: { parkingId, durationDays } }).then((r) => r.data),

  buy: (parkingId: number, durationDays: number) =>
    api.post<SubscriptionResponse>('/subscriptions', { parkingId, durationDays }).then((r) => r.data),

  renew: (id: number) =>
    api.post<SubscriptionResponse>(`/subscriptions/${id}/renew`).then((r) => r.data),

  cancel: (id: number) =>
    api.post<SubscriptionResponse>(`/subscriptions/${id}/cancel`).then((r) => r.data),

  adminAll: () =>
    api.get<SubscriptionResponse[]>('/subscriptions/admin/all').then((r) => r.data),
};
