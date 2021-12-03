package com.manuel.citasmedicas.models

data class Patient(
    var id: String? = null,
    var nombre: String? = null,
    var apellidoPaterno: String? = null,
    var apellidoMaterno: String? = null,
    var fechaNacimiento: String? = null,
    var sexo: String? = null,
    var altura: String? = null,
    var peso: String? = null,
    var alergias: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Patient
        if (id != other.id) return false
        return true
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }
}