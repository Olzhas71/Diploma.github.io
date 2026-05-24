-- Track whether a booking is covered by an active subscription (payment skipped).
ALTER TABLE bookings
    ADD COLUMN covered_by_subscription BOOLEAN NOT NULL DEFAULT FALSE;
