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
    val id: Long? = null,
    val name: String = "",
    val registration: String = "",
    val driver: String = "",
    val initialMileage: String = "0",
    val serviceInterval: String = "15000",
    val lastInspectionDate: String = "",
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
    val company: String = "",
    val nip: String = "",
    val city: String = "",
    val address: String = "",
    val contactPhone: String = "",
    val notes: String = "",
)

data class ClothesSizeDraft(
    val id: Long? = null,
    val name: String = "",
    val surname: String = "",
    val plant: String = "",
    val shirt: String = "",
    val hoodie: String = "",
    val pants: String = "",
    val jacket: String = "",
    val shoes: String = "",
)

data class ClothesOrderDraft(
    val id: Long? = null,
    val date: String = "",
    val plant: String = "",
    val status: String = "Nowe",
    val orderDesc: String = "",
)

data class ClothesOrderItemDraft(
    val id: Long? = null,
    val name: String = "",
    val surname: String = "",
    val item: String = "",
    val size: String = "",
    val qty: String = "1",
)

data class VehicleReportDraft(
    val marka: String = "",
    val rej: String = "",
    val seats: String = "4",
    val filledBy: String = "",
    val noDamage: Boolean = true,
    val damageSince: String = "",
    val damageDescription: String = "",
    val przebieg: String = "",
    val olej: String = "OK",
    val paliwo: String = "OK",
    val cleaned: Boolean = false,
    val tireProducer: String = "",
    val rodzajPaliwa: String = "",
    val lp: String = "",
    val pp: String = "",
    val lt: String = "",
    val pt: String = "",
    val warningLights: Boolean = false,
    val warningLightsDescription: String = "",
    val photoPaths: List<String> = emptyList(),
    val dashboardPhotoPath: String = "",
    val damagePhotoPaths: List<String> = emptyList(),
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
