-- V5: Seed multiple parkings around Almaty + operator and driver demo users.
-- All demo users share password 'admin123' for simplicity.

-- ---- Demo users (operator, driver) -----------------------------------------
INSERT INTO users (email, password_hash, full_name, phone, role) VALUES
  ('operator@parking.local',
   '$2a$10$CsDhyyAhqy8ZwI0Hm5Icu.4AhCoU6R9DJJ2ppnw7nmwXQNo0nquse',
   'Operator Demo', '+77000000001', 'OPERATOR'),
  ('driver@parking.local',
   '$2a$10$CsDhyyAhqy8ZwI0Hm5Icu.4AhCoU6R9DJJ2ppnw7nmwXQNo0nquse',
   'Driver Demo',   '+77000000002', 'DRIVER');

-- ---- New parkings around Almaty --------------------------------------------
-- We rely on BIGSERIAL to assign ids, then resync the sequence after.
INSERT INTO parkings (name, address, latitude, longitude, type, total_spots, working_hours_from, working_hours_to) VALUES
  ('TRC Mega Almaty',       'Розыбакиева 247А',         43.2152, 76.9050, 'MULTILEVEL',  30, '08:00', '23:00'),
  ('Esentai Mall',          'Аль-Фараби 77/8',          43.2046, 76.9276, 'UNDERGROUND', 25, '00:00', '23:59'),
  ('Dostyk Plaza',          'Самал-2, 111',             43.2287, 76.9551, 'MULTILEVEL',  20, '09:00', '22:00'),
  ('Forum Almaty',          'Сейфуллина 617',           43.2535, 76.9485, 'UNDERGROUND', 18, '10:00', '22:00'),
  ('Площадь Республики',    'Сатпаева х Достык',        43.2387, 76.9521, 'STREET',      12, '00:00', '23:59'),
  ('Almaty-I (вокзал)',     'Жибек Жолы 16',            43.2746, 76.9416, 'GROUND',      15, '00:00', '23:59'),
  ('Esentai Park',          'Аль-Фараби 77',            43.1985, 76.9290, 'GROUND',      16, '08:00', '23:00');

-- ---- Spots for each new parking ---------------------------------------------
-- Mega Almaty: 28 regular + 1 disabled + 1 electric on level 1
INSERT INTO parking_spots (parking_id, spot_number, level, type, status)
SELECT p.id, 'M' || gs, 1, 'REGULAR', 'FREE'
  FROM parkings p, generate_series(1, 28) gs
 WHERE p.name = 'TRC Mega Almaty';
INSERT INTO parking_spots (parking_id, spot_number, level, type, status)
SELECT p.id, 'M-D1', 1, 'DISABLED', 'FREE' FROM parkings p WHERE p.name = 'TRC Mega Almaty'
UNION ALL
SELECT p.id, 'M-E1', 1, 'ELECTRIC', 'FREE' FROM parkings p WHERE p.name = 'TRC Mega Almaty';

-- Esentai Mall: underground, 23 regular + 1 disabled + 1 electric
INSERT INTO parking_spots (parking_id, spot_number, level, type, status)
SELECT p.id, 'B' || gs, -1, 'REGULAR', 'FREE'
  FROM parkings p, generate_series(1, 23) gs
 WHERE p.name = 'Esentai Mall';
INSERT INTO parking_spots (parking_id, spot_number, level, type, status)
SELECT p.id, 'B-D1', -1, 'DISABLED', 'FREE' FROM parkings p WHERE p.name = 'Esentai Mall'
UNION ALL
SELECT p.id, 'B-E1', -1, 'ELECTRIC', 'FREE' FROM parkings p WHERE p.name = 'Esentai Mall';

-- Dostyk Plaza: 19 regular + 1 disabled, level 2
INSERT INTO parking_spots (parking_id, spot_number, level, type, status)
SELECT p.id, 'D' || gs, 2, 'REGULAR', 'FREE'
  FROM parkings p, generate_series(1, 19) gs
 WHERE p.name = 'Dostyk Plaza';
INSERT INTO parking_spots (parking_id, spot_number, level, type, status)
SELECT p.id, 'D-D1', 2, 'DISABLED', 'FREE' FROM parkings p WHERE p.name = 'Dostyk Plaza';

-- Forum Almaty: 16 regular + 1 disabled + 1 electric
INSERT INTO parking_spots (parking_id, spot_number, level, type, status)
SELECT p.id, 'F' || gs, -1, 'REGULAR', 'FREE'
  FROM parkings p, generate_series(1, 16) gs
 WHERE p.name = 'Forum Almaty';
INSERT INTO parking_spots (parking_id, spot_number, level, type, status)
SELECT p.id, 'F-D1', -1, 'DISABLED', 'FREE' FROM parkings p WHERE p.name = 'Forum Almaty'
UNION ALL
SELECT p.id, 'F-E1', -1, 'ELECTRIC', 'FREE' FROM parkings p WHERE p.name = 'Forum Almaty';

-- Площадь Республики: 12 street spots
INSERT INTO parking_spots (parking_id, spot_number, level, type, status)
SELECT p.id, 'R' || gs, 1, 'REGULAR', 'FREE'
  FROM parkings p, generate_series(1, 12) gs
 WHERE p.name = 'Площадь Республики';

-- Almaty-I вокзал: 14 regular + 1 disabled
INSERT INTO parking_spots (parking_id, spot_number, level, type, status)
SELECT p.id, 'V' || gs, 1, 'REGULAR', 'FREE'
  FROM parkings p, generate_series(1, 14) gs
 WHERE p.name = 'Almaty-I (вокзал)';
INSERT INTO parking_spots (parking_id, spot_number, level, type, status)
SELECT p.id, 'V-D1', 1, 'DISABLED', 'FREE' FROM parkings p WHERE p.name = 'Almaty-I (вокзал)';

-- Esentai Park: 14 regular + 1 disabled + 1 electric
INSERT INTO parking_spots (parking_id, spot_number, level, type, status)
SELECT p.id, 'P' || gs, 1, 'REGULAR', 'FREE'
  FROM parkings p, generate_series(1, 14) gs
 WHERE p.name = 'Esentai Park';
INSERT INTO parking_spots (parking_id, spot_number, level, type, status)
SELECT p.id, 'P-D1', 1, 'DISABLED', 'FREE' FROM parkings p WHERE p.name = 'Esentai Park'
UNION ALL
SELECT p.id, 'P-E1', 1, 'ELECTRIC', 'FREE' FROM parkings p WHERE p.name = 'Esentai Park';

-- ---- Tariffs for new parkings (KZT for Almaty atmosphere) ------------------
INSERT INTO tariffs (parking_id, name, price_per_hour, currency, dynamic_multiplier)
SELECT id, 'Стандарт',     500.00, 'KZT', 1.00 FROM parkings WHERE name = 'TRC Mega Almaty';
INSERT INTO tariffs (parking_id, name, price_per_hour, currency, dynamic_multiplier)
SELECT id, 'Премиум',     1200.00, 'KZT', 1.20 FROM parkings WHERE name = 'Esentai Mall';
INSERT INTO tariffs (parking_id, name, price_per_hour, currency, dynamic_multiplier)
SELECT id, 'Дневной',      700.00, 'KZT', 1.00 FROM parkings WHERE name = 'Dostyk Plaza';
INSERT INTO tariffs (parking_id, name, price_per_hour, currency, dynamic_multiplier)
SELECT id, 'Парковка ТЦ',  600.00, 'KZT', 1.00 FROM parkings WHERE name = 'Forum Almaty';
INSERT INTO tariffs (parking_id, name, price_per_hour, currency, dynamic_multiplier)
SELECT id, 'Уличная',      300.00, 'KZT', 1.00 FROM parkings WHERE name = 'Площадь Республики';
INSERT INTO tariffs (parking_id, name, price_per_hour, currency, dynamic_multiplier)
SELECT id, 'Вокзальная',   400.00, 'KZT', 1.00 FROM parkings WHERE name = 'Almaty-I (вокзал)';
INSERT INTO tariffs (parking_id, name, price_per_hour, currency, dynamic_multiplier)
SELECT id, 'Парк',         500.00, 'KZT', 1.00 FROM parkings WHERE name = 'Esentai Park';

-- Weekend-evening discount at Mega (Saturday 18:00–23:00)
INSERT INTO tariffs (parking_id, name, price_per_hour, currency, day_of_week, hour_from, hour_to, dynamic_multiplier)
SELECT id, 'Выходные вечер', 400.00, 'KZT', 'SATURDAY', '18:00', '23:00', 0.80
  FROM parkings WHERE name = 'TRC Mega Almaty';
