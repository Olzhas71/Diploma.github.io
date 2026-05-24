import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Calendar, Clock, MapPin, CalendarX, Receipt, Loader2, CalendarSearch } from 'lucide-react';
import clsx from 'clsx';
import { bookingsApi } from '@/api/bookings';
import { paymentsApi } from '@/api/payments';
import { useToast } from '@/components/Toast';
import { SkeletonRows } from '@/components/Skeleton';
import type { BookingResponse, BookingStatus, PaymentMethod } from '@/types';

const STATUS_BADGE: Record<BookingStatus, string> = {
  PENDING: 'bg-slate-100 text-slate-700',
  CONFIRMED: 'bg-brand-100 text-brand-700',
  ACTIVE: 'bg-accent-100 text-accent-700',
  COMPLETED: 'bg-slate-100 text-slate-500',
  CANCELLED: 'bg-red-100 text-red-700',
};

export function BookingsPage() {
  const { t, i18n } = useTranslation();
  const qc = useQueryClient();
  const toast = useToast();
  const { data, isLoading } = useQuery({
    queryKey: ['bookings'],
    queryFn: () => bookingsApi.myBookings(0, 50),
  });

  const cancel = useMutation({
    mutationFn: (id: number) => bookingsApi.cancel(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['bookings'] });
      toast.success(t('bookings.cancelled'));
    },
    onError: (err: any) => toast.error(err?.response?.data?.message ?? t('bookings.cancelError')),
  });

  const locale = i18n.resolvedLanguage === 'kk' ? 'kk-KZ' : i18n.resolvedLanguage === 'en' ? 'en-US' : 'ru-RU';

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-slate-900">{t('bookings.title')}</h1>
        <p className="text-sm text-slate-500 mt-1">{t('bookings.subtitle')}</p>
      </div>

      {isLoading && <SkeletonRows count={3} />}

      {data && data.content.length === 0 && (
        <div className="card text-center py-16">
          <CalendarSearch className="w-12 h-12 text-slate-300 mx-auto mb-3" />
          <p className="text-slate-500 mb-3">{t('bookings.empty')}</p>
          <Link to="/parkings" className="btn-primary inline-flex">{t('bookings.findParking')}</Link>
        </div>
      )}

      <div className="space-y-3">
        {data?.content.map((b) => (
          <BookingRow key={b.id} booking={b} onCancel={() => cancel.mutate(b.id)} locale={locale} />
        ))}
      </div>
    </div>
  );
}

function BookingRow({ booking, onCancel, locale }: { booking: BookingResponse; onCancel: () => void; locale: string }) {
  const { t } = useTranslation();
  const qc = useQueryClient();
  const toast = useToast();
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('CARD');
  const [paying, setPaying] = useState(false);

  const canCancel = booking.status === 'PENDING' || booking.status === 'CONFIRMED' || booking.status === 'ACTIVE';
  const canPay = (booking.status === 'CONFIRMED' || booking.status === 'PENDING') && !booking.coveredBySubscription;

  const pay = async () => {
    setPaying(true);
    try {
      const p = await paymentsApi.pay(booking.id, paymentMethod);
      toast.success(t('bookings.paid', { amount: p.amount, currency: p.currency }));
      qc.invalidateQueries({ queryKey: ['bookings'] });
    } catch (err: any) {
      toast.error(err?.response?.data?.message ?? t('bookings.paymentError'));
    } finally {
      setPaying(false);
    }
  };

  return (
    <div className="card flex flex-wrap items-center gap-4 hover:shadow-lift transition-shadow">
      <div className="w-12 h-12 rounded-xl bg-brand-50 text-brand-700 grid place-items-center shrink-0">
        <Calendar className="w-6 h-6" />
      </div>

      <div className="flex-1 min-w-[240px] space-y-1">
        <div className="flex items-center gap-2 flex-wrap">
          <Link to={`/parkings/${booking.parkingId}`} className="font-semibold text-slate-900 hover:text-brand-700">
            {booking.parkingName}
          </Link>
          <span className={clsx('badge', STATUS_BADGE[booking.status])}>{t(`bookingStatus.${booking.status}`)}</span>
        </div>
        <div className="text-sm text-slate-600 flex items-center gap-3 flex-wrap">
          <span className="flex items-center gap-1"><MapPin className="w-3 h-3" />{t('bookings.spot')} <b className="font-mono">{booking.spotNumber}</b></span>
          <span className="flex items-center gap-1"><Clock className="w-3 h-3" />{fmt(booking.startTime, locale)} — {fmt(booking.endTime, locale)}</span>
        </div>
        <div className="text-sm flex items-center gap-1 text-slate-700">
          <Receipt className="w-3 h-3 text-slate-400" />
          <b className="text-slate-900">{booking.totalAmount}</b> {booking.currency}
        </div>
      </div>

      <div className="flex flex-wrap items-center gap-2">
        {canPay && (
          <>
            <select
              className="input w-32 h-10 py-0"
              value={paymentMethod}
              onChange={(e) => setPaymentMethod(e.target.value as PaymentMethod)}
            >
              <option value="CARD">{t('bookings.methods.card')}</option>
              <option value="WALLET">{t('bookings.methods.wallet')}</option>
              <option value="SUBSCRIPTION">{t('bookings.methods.subscription')}</option>
            </select>
            <button className="btn-primary" onClick={pay} disabled={paying}>
              {paying ? <Loader2 className="w-4 h-4 animate-spin" /> : <Receipt className="w-4 h-4" />}
              {paying ? t('bookings.paying') : t('bookings.pay')}
            </button>
          </>
        )}
        {canCancel && (
          <button className="btn-ghost" onClick={onCancel}>
            <CalendarX className="w-4 h-4" /> {t('bookings.cancel')}
          </button>
        )}
      </div>
    </div>
  );
}

function fmt(iso: string, locale: string) {
  return new Date(iso).toLocaleString(locale, {
    day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit',
  });
}
