package com.parking.dto.stats;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RevenuePoint(LocalDate day, BigDecimal revenue, long bookings) {}
