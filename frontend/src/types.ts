export type Role = 'ADMIN' | 'OPERATOR' | 'DRIVER';
export type ParkingType = 'GROUND' | 'UNDERGROUND' | 'MULTILEVEL' | 'STREET';
export type SpotType = 'REGULAR' | 'DISABLED' | 'ELECTRIC' | 'RESERVED';
export type SpotStatus = 'FREE' | 'OCCUPIED' | 'RESERVED' | 'MAINTENANCE';
export type BookingStatus = 'PENDING' | 'CONFIRMED' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED';
export type PaymentMethod = 'CARD' | 'WALLET' | 'SUBSCRIPTION';
export type PaymentStatus = 'PENDING' | 'SUCCESS' | 'FAILED' | 'REFUNDED';

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  userId: number;
  email: string;
  role: Role;
}

export interface UserResponse {
  id: number;
  email: string;
  fullName: string;
  phone?: string;
  role: Role;
  enabled: boolean;
  createdAt: string;
}

export interface VehicleResponse {
  id: number;
  licensePlate: string;
  make?: string;
  model?: string;
  color?: string;
  ownerId: number;
}

export interface ParkingResponse {
  id: number;
  name: string;
  address: string;
  latitude: number;
  longitude: number;
  type: ParkingType;
  totalSpots: number;
  freeSpots: number;
  workingHoursFrom?: string;
  workingHoursTo?: string;
}

export interface SpotResponse {
  id: number;
  parkingId: number;
  spotNumber: string;
  level?: number;
  type: SpotType;
  status: SpotStatus;
}

export interface TariffResponse {
  id: number;
  parkingId: number;
  name: string;
  pricePerHour: number;
  currency: string;
  dynamicMultiplier: number;
}

export interface BookingResponse {
  id: number;
  userId: number;
  spotId: number;
  spotNumber: string;
  parkingId: number;
  parkingName: string;
  vehicleId?: number;
  startTime: string;
  endTime: string;
  status: BookingStatus;
  totalAmount: number;
  currency: string;
  coveredBySubscription: boolean;
}

export interface PaymentResponse {
  id: number;
  bookingId: number;
  amount: number;
  currency: string;
  method: PaymentMethod;
  status: PaymentStatus;
  externalTransactionId?: string;
  paidAt?: string;
}

export interface OccupancyForecast {
  parkingId: number;
  generatedAt: string;
  hourly: {
    hourOfDay: number;
    predictedOccupancyRate: number;
    predictedFreeSpots: number;
  }[];
}

export interface SpotStatusEvent {
  type: 'SPOT_STATUS';
  parkingId: number;
  spotId: number;
  spotNumber: string;
  status: SpotStatus;
  timestamp: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
