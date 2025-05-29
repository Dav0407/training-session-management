package com.epam.trainer_session_management.service;

import com.epam.trainer_session_management.dto.TrainerWorkloadRequest;
import com.epam.trainer_session_management.dto.TrainerWorkloadResponse;
import com.epam.trainer_session_management.model.TrainerWorkingHours;

public interface TrainerWorkingHoursService {
    TrainerWorkloadResponse calculateAndSave(TrainerWorkloadRequest request);
}
