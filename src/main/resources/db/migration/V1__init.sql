-- Initial schema for parking system

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(255) NOT NULL,
    phone           VARCHAR(32),
    role            VARCHAR(32)  NOT NULL,
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_users_email ON users(email);

CREATE TABLE vehicles (
    id              BIGSERIAL PRIMARY KEY,
    license_plate   VARCHAR(32)  NOT NULL UNIQUE,
    make            VARCHAR(64),
    model           VARCHAR(64),
    color           VARCHAR(32),
    owner_id        BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at      TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_vehicles_plate ON vehicles(license_plate);
CREATE INDEX idx_vehicles_owner ON vehicles(owner_id);

CREATE TABLE parkings (
    id                 BIGSERIAL PRIMARY KEY,
    name               VARCHAR(255) NOT NULL,
    address            VARCHAR(512) NOT NULL,
    latitude           DOUBLE PRECISION NOT NULL,
    longitude          DOUBLE PRECISION NOT NULL,
    type               VARCHAR(32)  NOT NULL,
    total_spots        INTEGER      NOT NULL,
    working_hours_from TIME,
    working_hours_to   TIME,
    created_at         TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_parkings_location ON parkings(latitude, longitude);

CREATE TABLE parking_spots (
    id              BIGSERIAL PRIMARY KEY,
    parking_id      BIGINT       NOT NULL REFERENCES parkings(id) ON DELETE CASCADE,
    spot_number     VARCHAR(16)  NOT NULL,
    level           INTEGER,
    type            VARCHAR(32)  NOT NULL DEFAULT 'REGULAR',
    status          VARCHAR(32)  NOT NULL DEFAULT 'FREE',
    version         BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_spot_per_parking UNIQUE (parking_id, spot_number)
);
CREATE INDEX idx_spots_parking ON parking_spots(parking_id);
CREATE INDEX idx_spots_status  ON parking_spots(status);

CREATE TABLE tariffs (
    id                  BIGSERIAL PRIMARY KEY,
    parking_id          BIGINT       NOT NULL REFERENCES parkings(id) ON DELETE CASCADE,
    name                VARCHAR(128) NOT NULL,
    price_per_hour      NUMERIC(12,2) NOT NULL,
    currency            VARCHAR(8)   NOT NULL DEFAULT 'USD',
    day_of_week         VARCHAR(16),
    hour_from           TIME,
    hour_to             TIME,
    vehicle_type        VARCHAR(32),
    dynamic_multiplier  NUMERIC(5,2) NOT NULL DEFAULT 1.00,
    created_at          TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_tariff_parking ON tariffs(parking_id);

CREATE TABLE bookings (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT      NOT NULL REFERENCES users(id),
    spot_id         BIGINT      NOT NULL REFERENCES parking_spots(id),
    vehicle_id      BIGINT      REFERENCES vehicles(id),
    start_time      TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time        TIMESTAMP WITH TIME ZONE NOT NULL,
    status          VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    total_amount    NUMERIC(12,2),
    currency        VARCHAR(8)  NOT NULL DEFAULT 'USD',
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_booking_user      ON bookings(user_id);
CREATE INDEX idx_booking_spot_time ON bookings(spot_id, start_time, end_time);
CREATE INDEX idx_booking_status    ON bookings(status);

CREATE TABLE payments (
    id                       BIGSERIAL PRIMARY KEY,
    booking_id               BIGINT       NOT NULL UNIQUE REFERENCES bookings(id) ON DELETE CASCADE,
    amount                   NUMERIC(12,2) NOT NULL,
    currency                 VARCHAR(8)   NOT NULL DEFAULT 'USD',
    method                   VARCHAR(32)  NOT NULL,
    status                   VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    external_transaction_id  VARCHAR(128),
    paid_at                  TIMESTAMP WITH TIME ZONE,
    created_at               TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_payment_status ON payments(status);

CREATE TABLE subscriptions (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    parking_id      BIGINT      NOT NULL REFERENCES parkings(id) ON DELETE CASCADE,
    valid_from      TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_to        TIMESTAMP WITH TIME ZONE NOT NULL,
    price           NUMERIC(12,2) NOT NULL,
    currency        VARCHAR(8)  NOT NULL DEFAULT 'USD',
    status          VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_sub_user    ON subscriptions(user_id);
CREATE INDEX idx_sub_parking ON subscriptions(parking_id);

CREATE TABLE sensor_readings (
    id          BIGSERIAL PRIMARY KEY,
    spot_id     BIGINT      NOT NULL REFERENCES parking_spots(id) ON DELETE CASCADE,
    occupied    BOOLEAN     NOT NULL,
    timestamp   TIMESTAMP WITH TIME ZONE NOT NULL,
    sensor_id   VARCHAR(64)
);
CREATE INDEX idx_sensor_spot_ts ON sensor_readings(spot_id, timestamp);
CREATE INDEX idx_sensor_ts      ON sensor_readings(timestamp);

CREATE TABLE access_events (
    id                         BIGSERIAL PRIMARY KEY,
    parking_id                 BIGINT      NOT NULL REFERENCES parkings(id) ON DELETE CASCADE,
    vehicle_id                 BIGINT      REFERENCES vehicles(id),
    license_plate_recognized   VARCHAR(32),
    event_type                 VARCHAR(16) NOT NULL,
    timestamp                  TIMESTAMP WITH TIME ZONE NOT NULL,
    photo_url                  VARCHAR(512)
);
CREATE INDEX idx_access_parking_ts ON access_events(parking_id, timestamp);
CREATE INDEX idx_access_plate      ON access_events(license_plate_recognized);
