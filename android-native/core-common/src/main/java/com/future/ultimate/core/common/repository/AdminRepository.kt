package com.future.ultimate.core.common.repository

import com.future.ultimate.core.common.model.CarDraft
import com.future.ultimate.core.common.model.ContactDraft
import com.future.ultimate.core.common.model.PlantDraft
import com.future.ultimate.core.common.model.VehicleReportDraft
import com.future.ultimate.core.common.model.WorkerDraft
import kotlinx.coroutines.flow.Flow

data class ContactListItem(
    val name: String,
    val surname: String,
    val email: String,
    val phone: String,
    val workplace: String,
    val apartment: String,
    val notes: String,
)

data class CarListItem(
    val id: Long,
    val name: String,
    val registration: String,
    val driver: String,
    val mileage: Int,
    val serviceInterval: Int,
    val lastService: Int,
) {
    val remainingToService: Int
        get() = serviceInterval - (mileage - lastService)
}

data class WorkerListItem(
    val id: Long,
    val name: String,
    val surname: String,
    val plant: String,
    val phone: String,
    val position: String,
    val hireDate: String,
)

data class PlantListItem(
    val id: Long,
    val name: String,
    val city: String,
    val address: String,
    val contactPhone: String,
    val notes: String,
)

interface AdminRepository {
    fun observeContacts(): Flow<List<ContactListItem>>
    suspend fun saveContact(draft: ContactDraft)
    suspend fun deleteContact(name: String, surname: String)

    fun observeCars(): Flow<List<CarListItem>>
    suspend fun saveCar(draft: CarDraft)
    suspend fun updateCarMileage(id: Long, mileage: Int)
    suspend fun updateCarDriver(id: Long, driver: String)
    suspend fun confirmCarService(id: Long)
    suspend fun deleteCar(id: Long)

    fun observeWorkers(): Flow<List<WorkerListItem>>
    suspend fun saveWorker(draft: WorkerDraft)
    suspend fun deleteWorker(id: Long)

    fun observePlants(): Flow<List<PlantListItem>>
    suspend fun savePlant(draft: PlantDraft)
    suspend fun deletePlant(id: Long)

    suspend fun saveVehicleReportDraft(draft: VehicleReportDraft)
}
