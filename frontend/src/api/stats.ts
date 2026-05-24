import { api } from './client';

export interface StatsOverview {
  totalParkings: number;
  totalSpots: number;
  freeSpots: number;
  activeBookings: number;
  totalBookings: number;
  totalRevenue: number;
  currency: string;
  averageOccupancyRate: number;
}

export interface RevenuePoint {
  day: string;
  revenue: number;
  bookings: number;
}

export interface PopularParking {
  parkingId: number;
  name: string;
  bookings: number;
  revenue: number;
}

export interface HourlyOccupancyPoint {
  hourOfDay: number;
  averageOccupancyRate: number;
  samples: number;
}

export const statsApi = {
  overview: () => api.get<StatsOverview>('/stats/overview').then((r) => r.data),
  revenue: (days = 30) =>
    api.get<RevenuePoint[]>('/stats/revenue', { params: { days } }).then((r) => r.data),
  popular: (days = 30, limit = 5) =>
    api.get<PopularParking[]>('/stats/popular-parkings', { params: { days, limit } }).then((r) => r.data),
  occupancyByHour: (days = 7) =>
    api.get<HourlyOccupancyPoint[]>('/stats/occupancy-by-hour', { params: { days } }).then((r) => r.data),
};
