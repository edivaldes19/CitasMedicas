package com.manuel.citasmedicas.interfaces

import com.manuel.citasmedicas.models.Patient

interface OnPatientListener {
    fun onClick(patient: Patient)
    fun onLongClick(patient: Patient)
}