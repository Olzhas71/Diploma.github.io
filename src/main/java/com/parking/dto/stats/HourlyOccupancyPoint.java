package com.parking.dto.stats;

public record HourlyOccupancyPoint(int hourOfDay, double averageOccupancyRate, long samples) {}
