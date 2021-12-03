package com.manuel.citasmedicas.interfaces

import com.manuel.citasmedicas.models.Appointment

interface OnAppointmentListener {
    fun onClick(appointment: Appointment)
    fun onLongClick(appointment: Appointment)
}