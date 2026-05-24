-- Replace the broken seeded admin password hash from V2 with a real BCrypt hash for "admin123".
UPDATE users
SET password_hash = '$2a$10$CsDhyyAhqy8ZwI0Hm5Icu.4AhCoU6R9DJJ2ppnw7nmwXQNo0nquse'
WHERE email = 'admin@parking.local';
