package com.epam.trainer_session_management.service.impl;

import com.epam.trainer_session_management.dto.TrainerWorkloadRequest;
import com.epam.trainer_session_management.dto.TrainerWorkloadResponse;
import com.epam.trainer_session_management.model.TrainerWorkingHours;
import com.epam.trainer_session_management.service.TrainerWorkingHoursService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TrainerWorkingHoursServiceImpl implements TrainerWorkingHoursService {

    private static final Map<String, TrainerWorkingHours> TRAINER_WORKLOAD_MAP = new ConcurrentHashMap<>();

    @Override
    public TrainerWorkloadResponse calculateAndSave(TrainerWorkloadRequest request) {
        LocalDate localDate = toLocalDate(request.getTrainingDate());
        String year = String.valueOf(localDate.getYear());
        String month = String.valueOf(localDate.getMonthValue());

        Float durationHours = request.getTrainingDuration() / 60.0F;

        TRAINER_WORKLOAD_MAP.compute(request.getTrainerUsername(), (username, existingData) -> {
            if (existingData == null) {
                return createNewTrainerRecord(request, year, month, durationHours);
            }
            return updateTrainerRecord(existingData, year, month, durationHours);
        });

        TrainerWorkingHours updated = TRAINER_WORKLOAD_MAP.get(request.getTrainerUsername());
        TrainerWorkingHours.Month updatedMonth = findMonth(updated, year, month);

        return TrainerWorkloadResponse.builder()
                .trainerUsername(updated.getTrainerUsername())
                .year(year)
                .month(month)
                .workingHours(updatedMonth.getWorkingHours())
                .build();
    }

    private static LocalDate toLocalDate(Date trainingDate) {
        return trainingDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private TrainerWorkingHours createNewTrainerRecord(TrainerWorkloadRequest request, String year, String month, Float durationHours) {
        TrainerWorkingHours.Month newMonth = TrainerWorkingHours.Month.builder()
                .month(month)
                .workingHours(durationHours)
                .build();

        TrainerWorkingHours.Year newYear = TrainerWorkingHours.Year.builder()
                .year(year)
                .months(new ArrayList<>(List.of(newMonth)))
                .build();

        return TrainerWorkingHours.builder()
                .trainerUsername(request.getTrainerUsername())
                .trainerFirstName(request.getTrainerFirstName())
                .trainerLastName(request.getTrainerLastName())
                .isActive(request.getIsActive())
                .years(new ArrayList<>(List.of(newYear)))
                .build();
    }

    private TrainerWorkingHours updateTrainerRecord(TrainerWorkingHours existing, String year, String month, Float durationHours) {
        List<TrainerWorkingHours.Year> years = existing.getYears();

        TrainerWorkingHours.Year yearEntry = years.stream()
                .filter(y -> y.getYear().equals(year))
                .findFirst()
                .orElseGet(() -> {
                    TrainerWorkingHours.Year newYear = TrainerWorkingHours.Year.builder()
                            .year(year)
                            .months(new ArrayList<>())
                            .build();
                    years.add(newYear);
                    return newYear;
                });

        List<TrainerWorkingHours.Month> months = yearEntry.getMonths();
        TrainerWorkingHours.Month monthEntry = months.stream()
                .filter(m -> m.getMonth().equals(month))
                .findFirst()
                .orElseGet(() -> {
                    TrainerWorkingHours.Month newMonth = TrainerWorkingHours.Month.builder()
                            .month(month)
                            .workingHours(0.0f)
                            .build();
                    months.add(newMonth);
                    return newMonth;
                });

        monthEntry.setWorkingHours(monthEntry.getWorkingHours() + durationHours);
        return existing;
    }

    private TrainerWorkingHours.Month findMonth(TrainerWorkingHours trainer, String year, String month) {
        return trainer.getYears().stream()
                .filter(y -> y.getYear().equals(year))
                .flatMap(y -> y.getMonths().stream())
                .filter(m -> m.getMonth().equals(month))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Month not found for trainer"));
    }
}
