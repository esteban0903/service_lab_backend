package com.example.taskassignment.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("CalendarService - Pure Calendar and Placement Logic")
class CalendarServiceTest {

    private CalendarService calendarService;

    @BeforeEach
    void setUp() {
        calendarService = new CalendarService();
    }

    @Nested
    @DisplayName("1. Single Day & Consecutive Working Days")
    class ConsecutiveDaysTests {

        @Test
        @DisplayName("1. Task fits completely in one working day")
        void plan_fitsInOneDay() {
            // 2026-10-15 is a Thursday
            LocalDate startDate = LocalDate.of(2026, 10, 15);
            int effortHours = 4;
            int hoursPerDay = 8;

            Placement placement = calendarService.plan(startDate, effortHours, hoursPerDay, Collections.emptyMap());

            assertThat(placement.effectiveStartDate()).isEqualTo(LocalDate.of(2026, 10, 15)); // Thursday
            assertThat(placement.endDate()).isEqualTo(LocalDate.of(2026, 10, 15)); // Thursday
            assertThat(placement.allocations()).containsExactly(
                    new DailyAllocation(LocalDate.of(2026, 10, 15), 4) // Thursday: 4h
            );
        }

        @Test
        @DisplayName("2. Task spans several consecutive working days within the same week")
        void plan_spansConsecutiveWorkingDays() {
            // 2026-10-12 is a Monday
            LocalDate startDate = LocalDate.of(2026, 10, 12);
            int effortHours = 18;
            int hoursPerDay = 8;

            Placement placement = calendarService.plan(startDate, effortHours, hoursPerDay, Collections.emptyMap());

            assertThat(placement.effectiveStartDate()).isEqualTo(LocalDate.of(2026, 10, 12)); // Monday
            assertThat(placement.endDate()).isEqualTo(LocalDate.of(2026, 10, 14)); // Wednesday
            assertThat(placement.allocations()).containsExactly(
                    new DailyAllocation(LocalDate.of(2026, 10, 12), 8), // Monday: 8h
                    new DailyAllocation(LocalDate.of(2026, 10, 13), 8), // Tuesday: 8h
                    new DailyAllocation(LocalDate.of(2026, 10, 14), 2)  // Wednesday: 2h
            );
        }
    }

    @Nested
    @DisplayName("2. Weekend Handling")
    class WeekendHandlingTests {

        @Test
        @DisplayName("3. Task crosses a weekend: 10h task, 4h/day, starting Thursday -> Thu 4, Fri 4, Mon 2, endDate Monday")
        void plan_crossesWeekend() {
            // 2026-10-15 is a Thursday
            LocalDate startDate = LocalDate.of(2026, 10, 15);
            int effortHours = 10;
            int hoursPerDay = 4;

            Placement placement = calendarService.plan(startDate, effortHours, hoursPerDay, Collections.emptyMap());

            // Weekend dates: 2026-10-17 (Saturday) and 2026-10-18 (Sunday) must be skipped
            assertThat(placement.effectiveStartDate()).isEqualTo(LocalDate.of(2026, 10, 15)); // Thursday
            assertThat(placement.endDate()).isEqualTo(LocalDate.of(2026, 10, 19)); // Monday
            assertThat(placement.allocations()).containsExactly(
                    new DailyAllocation(LocalDate.of(2026, 10, 15), 4), // Thursday: 4h
                    new DailyAllocation(LocalDate.of(2026, 10, 16), 4), // Friday: 4h
                    new DailyAllocation(LocalDate.of(2026, 10, 19), 2)  // Monday: 2h
            );
        }

        @Test
        @DisplayName("4a. startDate on Saturday moves effective start date to next Monday")
        void plan_startDateOnSaturday_movesToMonday() {
            // 2026-10-17 is a Saturday
            LocalDate saturday = LocalDate.of(2026, 10, 17);
            int effortHours = 6;
            int hoursPerDay = 8;

            Placement placement = calendarService.plan(saturday, effortHours, hoursPerDay, Collections.emptyMap());

            assertThat(placement.effectiveStartDate()).isEqualTo(LocalDate.of(2026, 10, 19)); // Monday
            assertThat(placement.endDate()).isEqualTo(LocalDate.of(2026, 10, 19)); // Monday
            assertThat(placement.allocations()).containsExactly(
                    new DailyAllocation(LocalDate.of(2026, 10, 19), 6) // Monday: 6h
            );
        }

        @Test
        @DisplayName("4b. startDate on Sunday moves effective start date to next Monday")
        void plan_startDateOnSunday_movesToMonday() {
            // 2026-10-18 is a Sunday
            LocalDate sunday = LocalDate.of(2026, 10, 18);
            int effortHours = 6;
            int hoursPerDay = 8;

            Placement placement = calendarService.plan(sunday, effortHours, hoursPerDay, Collections.emptyMap());

            assertThat(placement.effectiveStartDate()).isEqualTo(LocalDate.of(2026, 10, 19)); // Monday
            assertThat(placement.endDate()).isEqualTo(LocalDate.of(2026, 10, 19)); // Monday
            assertThat(placement.allocations()).containsExactly(
                    new DailyAllocation(LocalDate.of(2026, 10, 19), 6) // Monday: 6h
            );
        }

        @Test
        @DisplayName("New 3. Friday fully booked -> work skips the weekend and starts on Monday")
        void plan_fridayFullyBooked_skipsWeekendAndStartsMonday() {
            // 2026-10-16 is a Friday
            LocalDate friday = LocalDate.of(2026, 10, 16);
            int effortHours = 4;
            int hoursPerDay = 8;

            // Friday is 100% booked (8h load)
            Map<LocalDate, Integer> existingLoad = Map.of(
                    LocalDate.of(2026, 10, 16), 8 // Friday
            );

            Placement placement = calendarService.plan(friday, effortHours, hoursPerDay, existingLoad);

            // Friday is skipped, Saturday & Sunday are skipped, work starts on Monday
            assertThat(placement.effectiveStartDate()).isEqualTo(LocalDate.of(2026, 10, 19)); // Monday
            assertThat(placement.endDate()).isEqualTo(LocalDate.of(2026, 10, 19)); // Monday
            assertThat(placement.allocations()).containsExactly(
                    new DailyAllocation(LocalDate.of(2026, 10, 19), 4) // Monday: 4h
            );
        }
    }

    @Nested
    @DisplayName("3. Existing Load & Capacity Handling")
    class ExistingLoadTests {

        @Test
        @DisplayName("5. Worker with prior load: only remaining free capacity is used")
        void plan_withPriorLoad_usesOnlyFreeCapacity() {
            // 2026-10-15 is a Thursday
            LocalDate startDate = LocalDate.of(2026, 10, 15);
            int effortHours = 6;
            int hoursPerDay = 8;

            // Worker already has 5h booked on Thursday, so free capacity on Thursday is 8 - 5 = 3h
            Map<LocalDate, Integer> existingLoad = Map.of(
                    LocalDate.of(2026, 10, 15), 5 // Thursday
            );

            Placement placement = calendarService.plan(startDate, effortHours, hoursPerDay, existingLoad);

            assertThat(placement.effectiveStartDate()).isEqualTo(LocalDate.of(2026, 10, 15)); // Thursday
            assertThat(placement.endDate()).isEqualTo(LocalDate.of(2026, 10, 16)); // Friday
            assertThat(placement.allocations()).containsExactly(
                    new DailyAllocation(LocalDate.of(2026, 10, 15), 3), // Thursday: min(6, 3) = 3h
                    new DailyAllocation(LocalDate.of(2026, 10, 16), 3)  // Friday: remaining 3h
            );
        }

        @Test
        @DisplayName("6. Fully booked day is skipped completely")
        void plan_fullyBookedDay_isSkipped() {
            // 2026-10-15 is a Thursday
            LocalDate startDate = LocalDate.of(2026, 10, 15);
            int effortHours = 4;
            int hoursPerDay = 8;

            // Thursday is 100% booked (8h load, 0h free)
            Map<LocalDate, Integer> existingLoad = Map.of(
                    LocalDate.of(2026, 10, 15), 8 // Thursday
            );

            Placement placement = calendarService.plan(startDate, effortHours, hoursPerDay, existingLoad);

            // Thursday is skipped, work effectively starts on Friday
            assertThat(placement.effectiveStartDate()).isEqualTo(LocalDate.of(2026, 10, 16)); // Friday
            assertThat(placement.endDate()).isEqualTo(LocalDate.of(2026, 10, 16)); // Friday
            assertThat(placement.allocations()).containsExactly(
                    new DailyAllocation(LocalDate.of(2026, 10, 16), 4) // Friday: 4h
            );
        }

        @Test
        @DisplayName("New 2. Existing load greater than hoursPerDay is treated as zero free capacity (never negative) and skipped")
        void plan_overbookedDay_treatedAsZeroFreeCapacityAndSkipped() {
            // 2026-10-15 is a Thursday
            LocalDate startDate = LocalDate.of(2026, 10, 15);
            int effortHours = 4;
            int hoursPerDay = 6;

            // Thursday has 8h load, but worker capacity is only 6h/day (overbooked by 2h)
            Map<LocalDate, Integer> existingLoad = Map.of(
                    LocalDate.of(2026, 10, 15), 8 // Thursday: load 8, capacity 6 -> free capacity = 0
            );

            Placement placement = calendarService.plan(startDate, effortHours, hoursPerDay, existingLoad);

            // Thursday has 0 free capacity (not negative), so it's skipped; Friday gets the 4h
            assertThat(placement.effectiveStartDate()).isEqualTo(LocalDate.of(2026, 10, 16)); // Friday
            assertThat(placement.endDate()).isEqualTo(LocalDate.of(2026, 10, 16)); // Friday
            assertThat(placement.allocations()).containsExactly(
                    new DailyAllocation(LocalDate.of(2026, 10, 16), 4) // Friday: 4h
            );
        }
    }

    @Nested
    @DisplayName("4. Boundaries & Invariants")
    class BoundaryTests {

        @Test
        @DisplayName("7. Effort exactly equal to a day's full capacity (boundary)")
        void plan_effortExactlyEqualsDayCapacity() {
            // 2026-10-13 is a Tuesday
            LocalDate startDate = LocalDate.of(2026, 10, 13);
            int effortHours = 8;
            int hoursPerDay = 8;

            Placement placement = calendarService.plan(startDate, effortHours, hoursPerDay, Collections.emptyMap());

            assertThat(placement.effectiveStartDate()).isEqualTo(LocalDate.of(2026, 10, 13)); // Tuesday
            assertThat(placement.endDate()).isEqualTo(LocalDate.of(2026, 10, 13)); // Tuesday
            assertThat(placement.allocations()).containsExactly(
                    new DailyAllocation(LocalDate.of(2026, 10, 13), 8) // Tuesday: 8h
            );
        }

        @Test
        @DisplayName("8. Task that ends exactly on a Friday does not spill into next week")
        void plan_endsExactlyOnFriday() {
            // 2026-10-15 is a Thursday
            LocalDate startDate = LocalDate.of(2026, 10, 15);
            int effortHours = 16;
            int hoursPerDay = 8;

            Placement placement = calendarService.plan(startDate, effortHours, hoursPerDay, Collections.emptyMap());

            assertThat(placement.effectiveStartDate()).isEqualTo(LocalDate.of(2026, 10, 15)); // Thursday
            assertThat(placement.endDate()).isEqualTo(LocalDate.of(2026, 10, 16)); // Friday
            assertThat(placement.allocations()).containsExactly(
                    new DailyAllocation(LocalDate.of(2026, 10, 15), 8), // Thursday: 8h
                    new DailyAllocation(LocalDate.of(2026, 10, 16), 8)  // Friday: 8h
            );
        }

        @Test
        @DisplayName("1 & 9. Sum of planned hours equals effortHours and exact allocation list is preserved")
        void plan_sumOfAllocatedHours_alwaysEqualsEffortHours_andExactAllocations() {
            // 2026-10-14 is a Wednesday
            LocalDate startDate = LocalDate.of(2026, 10, 14);
            int effortHours = 27;
            int hoursPerDay = 6;

            Map<LocalDate, Integer> existingLoad = Map.of(
                    LocalDate.of(2026, 10, 14), 4, // Wednesday: 6 - 4 = 2h free
                    LocalDate.of(2026, 10, 15), 6, // Thursday: 6 - 6 = 0h free (skipped)
                    LocalDate.of(2026, 10, 16), 1, // Friday: 6 - 1 = 5h free
                    LocalDate.of(2026, 10, 19), 3  // Monday: 6 - 3 = 3h free
            );

            Placement placement = calendarService.plan(startDate, effortHours, hoursPerDay, existingLoad);

            int totalAllocatedHours = placement.allocations().stream()
                    .mapToInt(DailyAllocation::hours)
                    .sum();

            assertThat(totalAllocatedHours).isEqualTo(effortHours);
            assertThat(placement.effectiveStartDate()).isEqualTo(LocalDate.of(2026, 10, 14)); // Wednesday
            assertThat(placement.endDate()).isEqualTo(LocalDate.of(2026, 10, 22)); // Thursday

            // Exact allocations: Wed 2, Fri 5, Mon 3, Tue 6, Wed 6, Thu 5
            assertThat(placement.allocations()).containsExactly(
                    new DailyAllocation(LocalDate.of(2026, 10, 14), 2), // Wednesday: 2h
                    new DailyAllocation(LocalDate.of(2026, 10, 16), 5), // Friday: 5h
                    new DailyAllocation(LocalDate.of(2026, 10, 19), 3), // Monday: 3h
                    new DailyAllocation(LocalDate.of(2026, 10, 20), 6), // Tuesday: 6h
                    new DailyAllocation(LocalDate.of(2026, 10, 21), 6), // Wednesday: 6h
                    new DailyAllocation(LocalDate.of(2026, 10, 22), 5)  // Thursday: 5h
            );
        }

        @Test
        @DisplayName("New 4. Long task spanning several weeks and month boundary: verify endDate and no weekend allocations")
        void plan_longTaskSpanningWeeksAndMonthBoundary() {
            // 2026-10-26 is a Monday
            LocalDate startDate = LocalDate.of(2026, 10, 26);
            int effortHours = 100;
            int hoursPerDay = 8;

            // 100h / 8h = 12 full days (96h) + 1 day with 4h = 13 working days total
            // Week 1: Oct 26 to Oct 30 (5 days = 40h)
            // Month boundary: Oct 31 (Sat), Nov 01 (Sun)
            // Week 2: Nov 02 to Nov 06 (5 days = 40h, total 80h)
            // Weekend: Nov 07 (Sat), Nov 08 (Sun)
            // Week 3: Nov 09 (Mon, 8h = 88h), Nov 10 (Tue, 8h = 96h), Nov 11 (Wed, 4h = 100h)
            Placement placement = calendarService.plan(startDate, effortHours, hoursPerDay, Collections.emptyMap());

            assertThat(placement.effectiveStartDate()).isEqualTo(LocalDate.of(2026, 10, 26)); // Monday
            assertThat(placement.endDate()).isEqualTo(LocalDate.of(2026, 11, 11)); // Wednesday

            int totalHours = placement.allocations().stream()
                    .mapToInt(DailyAllocation::hours)
                    .sum();
            assertThat(totalHours).isEqualTo(100);

            // Assert that no allocation ever falls on a Saturday or Sunday
            assertThat(placement.allocations())
                    .allSatisfy(allocation -> {
                        DayOfWeek dow = allocation.date().getDayOfWeek();
                        assertThat(dow).isNotEqualTo(DayOfWeek.SATURDAY);
                        assertThat(dow).isNotEqualTo(DayOfWeek.SUNDAY);
                    });

            assertThat(placement.allocations()).hasSize(13);
            assertThat(placement.allocations().getLast()).isEqualTo(new DailyAllocation(LocalDate.of(2026, 11, 11), 4));
        }
    }

    @Nested
    @DisplayName("5. Input Validations")
    class InputValidationTests {

        @ParameterizedTest(name = "effortHours = {0} should throw IllegalArgumentException")
        @ValueSource(ints = {0, -1, -10})
        @DisplayName("10a. Non-positive effortHours throws IllegalArgumentException")
        void plan_invalidEffortHours_throwsException(int invalidEffort) {
            // 2026-10-15 is a Thursday
            LocalDate startDate = LocalDate.of(2026, 10, 15);

            assertThatThrownBy(() -> calendarService.plan(startDate, invalidEffort, 8, Collections.emptyMap()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Effort hours must be greater than 0");
        }

        @ParameterizedTest(name = "hoursPerDay = {0} should throw IllegalArgumentException")
        @ValueSource(ints = {0, -1, -8})
        @DisplayName("10b. Non-positive hoursPerDay throws IllegalArgumentException")
        void plan_invalidHoursPerDay_throwsException(int invalidHoursPerDay) {
            // 2026-10-15 is a Thursday
            LocalDate startDate = LocalDate.of(2026, 10, 15);

            assertThatThrownBy(() -> calendarService.plan(startDate, 8, invalidHoursPerDay, Collections.emptyMap()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Hours per day must be greater than 0");
        }

        @Test
        @DisplayName("10c. Null startDate throws IllegalArgumentException")
        void plan_nullStartDate_throwsException() {
            assertThatThrownBy(() -> calendarService.plan(null, 8, 8, Collections.emptyMap()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Start date must not be null");
        }

        @Test
        @DisplayName("10d. Null existingLoadByDate is treated gracefully as empty map")
        void plan_nullExistingLoad_treatedAsEmpty() {
            // 2026-10-15 is a Thursday
            LocalDate startDate = LocalDate.of(2026, 10, 15);

            Placement placement = calendarService.plan(startDate, 4, 8, null);

            assertThat(placement.allocations()).containsExactly(
                    new DailyAllocation(LocalDate.of(2026, 10, 15), 4)
            );
        }
    }
}
