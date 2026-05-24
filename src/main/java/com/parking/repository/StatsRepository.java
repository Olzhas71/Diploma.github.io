package com.parking.repository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Read-only aggregations for the analytics dashboard.
 *
 * Uses NamedParameterJdbcTemplate with ANSI SQL so that the same queries run
 * unchanged on PostgreSQL (prod / dev) and H2 PostgreSQL mode (dev-embedded).
 */
@Repository
public class StatsRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public StatsRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public BigDecimal totalRevenue() {
        Map<String, Object> p = Map.of();
        Number n = jdbc.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE status = 'SUCCESS'",
                p, Number.class);
        return n == null ? BigDecimal.ZERO : new BigDecimal(n.toString());
    }

    public long count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + safeTable(table),
                Map.of(), Long.class);
    }

    public long countByStatus(String table, String status) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + safeTable(table) + " WHERE status = :s",
                new MapSqlParameterSource("s", status), Long.class);
    }

    public long sumIntColumn(String table, String column) {
        Number n = jdbc.queryForObject(
                "SELECT COALESCE(SUM(" + safeColumn(column) + "), 0) FROM " + safeTable(table),
                Map.of(), Number.class);
        return n == null ? 0L : n.longValue();
    }

    public List<RevenueRow> revenueByDay(Instant from, Instant to) {
        var p = new MapSqlParameterSource()
                .addValue("from", java.sql.Timestamp.from(from))
                .addValue("to",   java.sql.Timestamp.from(to));
        return jdbc.query("""
                SELECT CAST(start_time AS DATE) AS d,
                       COALESCE(SUM(total_amount), 0) AS revenue,
                       COUNT(*) AS bookings
                FROM bookings
                WHERE start_time >= :from
                  AND start_time <  :to
                  AND status IN ('CONFIRMED','ACTIVE','COMPLETED')
                GROUP BY CAST(start_time AS DATE)
                ORDER BY d
                """, p, (rs, i) -> new RevenueRow(
                        rs.getDate("d").toLocalDate(),
                        rs.getBigDecimal("revenue"),
                        rs.getLong("bookings")));
    }

    public List<PopularRow> popularParkings(Instant from, int limit) {
        int safeLimit = Math.max(1, Math.min(100, limit));
        var p = new MapSqlParameterSource()
                .addValue("from", java.sql.Timestamp.from(from));
        return jdbc.query("""
                SELECT p.id AS parking_id,
                       p.name AS name,
                       COUNT(b.id) AS bookings,
                       COALESCE(SUM(b.total_amount), 0) AS revenue
                FROM parkings p
                LEFT JOIN parking_spots ps ON ps.parking_id = p.id
                LEFT JOIN bookings b ON b.spot_id = ps.id
                       AND b.start_time >= :from
                       AND b.status IN ('CONFIRMED','ACTIVE','COMPLETED')
                GROUP BY p.id, p.name
                ORDER BY bookings DESC, revenue DESC
                LIMIT """ + " " + safeLimit, p, (rs, i) -> new PopularRow(
                        rs.getLong("parking_id"),
                        rs.getString("name"),
                        rs.getLong("bookings"),
                        rs.getBigDecimal("revenue")));
    }

    public List<HourlyOccupancyRow> averageOccupancyByHour(Instant from, Instant to) {
        var p = new MapSqlParameterSource()
                .addValue("from", java.sql.Timestamp.from(from))
                .addValue("to",   java.sql.Timestamp.from(to));
        return jdbc.query("""
                SELECT CAST(EXTRACT(HOUR FROM sr.timestamp) AS INTEGER) AS hour_of_day,
                       AVG(CASE WHEN sr.occupied THEN 1.0 ELSE 0.0 END) AS rate,
                       COUNT(*) AS samples
                FROM sensor_readings sr
                WHERE sr.timestamp >= :from AND sr.timestamp < :to
                GROUP BY 1
                ORDER BY 1
                """, p, (rs, i) -> new HourlyOccupancyRow(
                        rs.getInt("hour_of_day"),
                        rs.getDouble("rate"),
                        rs.getLong("samples")));
    }

    private String safeTable(String t) {
        if (!t.matches("[a-z_][a-z0-9_]*")) throw new IllegalArgumentException("invalid table");
        return t;
    }

    private String safeColumn(String c) {
        if (!c.matches("[a-z_][a-z0-9_]*")) throw new IllegalArgumentException("invalid column");
        return c;
    }

    public record RevenueRow(LocalDate day, BigDecimal revenue, long bookings) {}
    public record PopularRow(Long parkingId, String name, long bookings, BigDecimal revenue) {}
    public record HourlyOccupancyRow(int hourOfDay, double rate, long samples) {}
}
