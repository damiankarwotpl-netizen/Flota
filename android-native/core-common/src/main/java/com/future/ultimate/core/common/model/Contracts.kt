package com.future.ultimate.core.common.model

data class ContactDraft(
    val name: String = "",
    val surname: String = "",
    val email: String = "",
    val pesel: String = "",
    val phone: String = "",
    val workplace: String = "",
    val apartment: String = "",
    val notes: String = "",
)

data class CarDraft(
    val name: String = "",
    val registration: String = "",
    val driver: String = "",
    val serviceInterval: String = "15000",
)

data class WorkerDraft(
    val id: Long? = null,
    val name: String = "",
    val surname: String = "",
    val plant: String = "",
    val phone: String = "",
    val position: String = "",
    val hireDate: String = "",
)

data class PlantDraft(
    val id: Long? = null,
    val name: String = "",
    val city: String = "",
    val address: String = "",
    val contactPhone: String = "",
    val notes: String = "",
)

data class VehicleReportDraft(
    val marka: String = "",
    val rej: String = "",
    val przebieg: String = "",
    val olej: String = "",
    val paliwo: String = "",
    val rodzajPaliwa: String = "",
    val lp: String = "",
    val pp: String = "",
    val lt: String = "",
    val pt: String = "",
    val uszkodzenia: String = "",
    val odKiedy: String = "",
    val serwis: String = "",
    val przeglad: String = "",
    val uwagi: String = "",
    val trojkat: Boolean = false,
    val kamizelki: Boolean = false,
    val kolo: Boolean = false,
    val dowod: Boolean = false,
    val apteczka: Boolean = false,
)
