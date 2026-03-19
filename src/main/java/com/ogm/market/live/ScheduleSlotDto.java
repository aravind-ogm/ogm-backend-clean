package com.ogm.market.live;

import lombok.Data;

@Data
public class ScheduleSlotDto {
    private String dayOfWeek;  // MON, TUE, WED, THU, FRI, SAT, SUN
    private String startTime;  // HH:mm
    private String endTime;    // HH:mm
    private boolean active;
}