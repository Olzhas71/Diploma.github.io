import { api } from './client';
import type { OccupancyForecast } from '@/types';

export interface ModelInfo {
  trained: boolean;
  algorithm: string;
  trainedAt: string | null;
  trainSamples: number;
  testSamples: number;
  rmse: number | null;
  mae: number | null;
  r2: number | null;
  features: string[];
}

export const mlApi = {
  forecast: (parkingId: number) =>
    api.get<OccupancyForecast>(`/ml/parkings/${parkingId}/forecast`).then((r) => r.data),
  modelInfo: () => api.get<ModelInfo>('/ml/model').then((r) => r.data),
  retrain: () => api.post<ModelInfo>('/ml/model/retrain').then((r) => r.data),
};
