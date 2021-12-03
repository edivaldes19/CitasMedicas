package com.manuel.citasmedicas.interfaces

import com.manuel.citasmedicas.models.Patient

interface OnPatientSelected {
    fun getPatientSelected(): Patient?
}