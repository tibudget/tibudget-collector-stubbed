package com.tibudget.plugins.stubbed;

import com.tibudget.dto.RecurringPaymentDto;

import java.time.LocalDate;
import java.time.Month;

public class RecurringPaymentConfig {

    public final String seed;
    public final String label;
    public final double amount;
    public final Double ratio;
    public final RecurringPaymentDto.RecurrenceUnit unit;
    public final int interval;
    public final LocalDate start;
    public final LocalDate end; // nullable
    public final Month startMonth; // nullable
    public final Month endMonth;   // nullable

    public RecurringPaymentConfig(
            String seed,
            String label,
            double amount,
            Double ratio,
            RecurringPaymentDto.RecurrenceUnit unit,
            int interval,
            LocalDate start,
            LocalDate end,
            Month startMonth,
            Month endMonth
    ) {
        this.seed = seed;
        this.label = label;
        this.amount = amount;
        this.ratio = ratio;
        this.unit = unit;
        this.interval = interval;
        this.start = start;
        this.end = end;
        this.startMonth = startMonth;
        this.endMonth = endMonth;
    }
}
