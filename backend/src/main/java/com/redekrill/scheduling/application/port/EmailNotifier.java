package com.redekrill.scheduling.application.port;

import com.redekrill.scheduling.domain.model.Appointment;

import java.util.List;

public interface EmailNotifier {

    void sendAppointmentConfirmation(Appointment appointment, List<String> warnings);
}
