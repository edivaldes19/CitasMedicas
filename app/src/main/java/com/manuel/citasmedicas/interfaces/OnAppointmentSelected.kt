package com.manuel.citasmedicas.interfaces

import com.manuel.citasmedicas.models.Appointment

interface OnAppointmentSelected {
    fun getAppointmentSelected(): Appointment?
}