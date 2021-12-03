package com.manuel.citasmedicas.models

data class Appointment(
    var idCita: String? = null,
    var idPaciente: String? = null,
    var fechaCita: String? = null,
    var horaCita: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Appointment
        if (idCita != other.idCita) return false
        return true
    }

    override fun hashCode(): Int {
        return idCita?.hashCode() ?: 0
    }
}