import { Route, Routes } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Layout } from '@/components/Layout';
import { ProtectedRoute } from '@/components/ProtectedRoute';
import { HomePage } from '@/pages/Home';
import { LoginPage } from '@/pages/Login';
import { RegisterPage } from '@/pages/Register';
import { ParkingsPage } from '@/pages/Parkings';
import { ParkingDetailPage } from '@/pages/ParkingDetail';
import { MapPage } from '@/pages/Map';
import { BookingsPage } from '@/pages/Bookings';
import { VehiclesPage } from '@/pages/Vehicles';
import { ForecastPage } from '@/pages/Forecast';
import { AdminParkingsPage } from '@/pages/AdminParkings';
import { AdminStatsPage } from '@/pages/AdminStats';
import { AdminAccessEventsPage } from '@/pages/AdminAccessEvents';
import { SubscriptionsPage } from '@/pages/Subscriptions';

export default function App() {
  const { t } = useTranslation();
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<HomePage />} />
        <Route path="login" element={<LoginPage />} />
        <Route path="register" element={<RegisterPage />} />

        <Route path="parkings" element={<ProtectedRoute><ParkingsPage /></ProtectedRoute>} />
        <Route path="parkings/:id" element={<ProtectedRoute><ParkingDetailPage /></ProtectedRoute>} />
        <Route path="map" element={<ProtectedRoute><MapPage /></ProtectedRoute>} />
        <Route path="bookings" element={<ProtectedRoute><BookingsPage /></ProtectedRoute>} />
        <Route path="vehicles" element={<ProtectedRoute><VehiclesPage /></ProtectedRoute>} />
        <Route path="forecast/:id" element={<ProtectedRoute><ForecastPage /></ProtectedRoute>} />
        <Route path="subscriptions" element={<ProtectedRoute><SubscriptionsPage /></ProtectedRoute>} />

        <Route
          path="admin/parkings"
          element={
            <ProtectedRoute roles={['ADMIN', 'OPERATOR']}>
              <AdminParkingsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="admin/stats"
          element={
            <ProtectedRoute roles={['ADMIN', 'OPERATOR']}>
              <AdminStatsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="admin/access-events"
          element={
            <ProtectedRoute roles={['ADMIN', 'OPERATOR']}>
              <AdminAccessEventsPage />
            </ProtectedRoute>
          }
        />

        <Route path="*" element={<div className="text-center text-slate-500 py-16">{t('common.notFound')}</div>} />
      </Route>
    </Routes>
  );
}
