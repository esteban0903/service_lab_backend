package com.example.taskassignment.service;

import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Pure calendar and capacity placement service.
 * Determines the distribution of task effort across working days (Monday through Friday)
 * considering a worker's daily capacity and existing load.
 */
@Service
public class CalendarService {

    /**
     * Plans the placement of a task given a start date, effort, daily capacity, and existing workload.
     *
     * @param startDate          the requested start date (weekends are skipped to the next Monday)
     * @param effortHours        total effort hours to allocate (must be > 0)
     * @param hoursPerDay        worker daily capacity in hours (must be > 0)
     * @param existingLoadByDate optional map of previously allocated hours per day
     * @return Placement containing effectiveStartDate, endDate, and daily allocations
     */
    public Placement plan(LocalDate startDate, int effortHours, int hoursPerDay, Map<LocalDate, Integer> existingLoadByDate) {
        if (startDate == null) {
            throw new IllegalArgumentException("Start date must not be null");
        }
        if (effortHours <= 0) {
            throw new IllegalArgumentException("Effort hours must be greater than 0");
        }
        if (hoursPerDay <= 0) {
            throw new IllegalArgumentException("Hours per day must be greater than 0");
        }

        Map<LocalDate, Integer> existingLoad = existingLoadByDate != null ? existingLoadByDate : Collections.emptyMap();

        List<DailyAllocation> allocations = new ArrayList<>();
        LocalDate currentDate = startDate;
        int remainingHours = effortHours;
        LocalDate effectiveStartDate = null;
        LocalDate endDate = null;

        while (remainingHours > 0) {
            DayOfWeek dayOfWeek = currentDate.getDayOfWeek();

            // Work is only performed Monday through Friday
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                int load = existingLoad.getOrDefault(currentDate, 0);
                int freeCapacity = Math.max(0, hoursPerDay - load);

                // If the worker has available capacity on this working day, allocate hours
                if (freeCapacity > 0) {
                    int allocatedHours = Math.min(remainingHours, freeCapacity);
                    allocations.add(new DailyAllocation(currentDate, allocatedHours));
                    remainingHours -= allocatedHours;

                    if (effectiveStartDate == null) {
                        effectiveStartDate = currentDate;
                    }
                    endDate = currentDate;
                }
            }

            currentDate = currentDate.plusDays(1);
        }

        return new Placement(effectiveStartDate, endDate, List.copyOf(allocations));
    }
}

