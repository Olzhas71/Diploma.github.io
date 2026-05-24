-- Seed admin user (password is fixed up in V3 with a real BCrypt hash for "admin123")
INSERT INTO users (email, password_hash, full_name, phone, role)
VALUES ('admin@parking.local',
        'placeholder-fixed-in-V3',
        'System Administrator', '+10000000000', 'ADMIN');

-- Seed sample parking
INSERT INTO parkings (name, address, latitude, longitude, type, total_spots, working_hours_from, working_hours_to)
VALUES ('Central Plaza Parking', '1 Main St, Downtown', 43.2389, 76.8897, 'MULTILEVEL', 10, '00:00', '23:59');

-- Seed parking spots (10 spots)
INSERT INTO parking_spots (parking_id, spot_number, level, type, status) VALUES
    (1, 'A1', 1, 'REGULAR',  'FREE'),
    (1, 'A2', 1, 'REGULAR',  'FREE'),
    (1, 'A3', 1, 'REGULAR',  'FREE'),
    (1, 'A4', 1, 'REGULAR',  'FREE'),
    (1, 'A5', 1, 'REGULAR',  'FREE'),
    (1, 'A6', 1, 'REGULAR',  'FREE'),
    (1, 'A7', 1, 'REGULAR',  'FREE'),
    (1, 'A8', 1, 'REGULAR',  'FREE'),
    (1, 'D1', 1, 'DISABLED', 'FREE'),
    (1, 'E1', 1, 'ELECTRIC', 'FREE');

-- Seed default tariff
INSERT INTO tariffs (parking_id, name, price_per_hour, currency, dynamic_multiplier)
VALUES (1, 'Standard hourly', 5.00, 'USD', 1.00);
