package com.future.ultimate.core.database.repository

import com.future.ultimate.core.common.model.CarDraft
import com.future.ultimate.core.common.model.ContactDraft
import com.future.ultimate.core.common.model.PlantDraft
import com.future.ultimate.core.common.model.VehicleReportDraft
import com.future.ultimate.core.common.model.WorkerDraft
import com.future.ultimate.core.common.repository.AdminRepository
import com.future.ultimate.core.common.repository.CarListItem
import com.future.ultimate.core.common.repository.ContactListItem
import com.future.ultimate.core.common.repository.PlantListItem
import com.future.ultimate.core.common.repository.WorkerListItem
import com.future.ultimate.core.database.dao.AppDao
import com.future.ultimate.core.database.entity.CarEntity
import com.future.ultimate.core.database.entity.ClothesSizeEntity
import com.future.ultimate.core.database.entity.ContactEntity
import com.future.ultimate.core.database.entity.PlantEntity
import com.future.ultimate.core.database.entity.SettingEntity
import com.future.ultimate.core.database.entity.WorkerEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalAdminRepository(
    private val dao: AppDao,
) : AdminRepository {
    override fun observeContacts(): Flow<List<ContactListItem>> = dao.observeContacts().map { items ->
        items.map {
            ContactListItem(
                name = it.name,
                surname = it.surname,
                email = it.email,
                phone = it.phone,
                workplace = it.workplace,
                apartment = it.apartment,
                notes = it.notes,
            )
        }
    }

    override suspend fun saveContact(draft: ContactDraft) {
        val normalizedName = draft.name.trim().lowercase()
        val normalizedSurname = draft.surname.trim().lowercase()
        dao.upsertContact(
            ContactEntity(
                name = normalizedName,
                surname = normalizedSurname,
                email = draft.email.trim(),
                pesel = draft.pesel.trim(),
                phone = draft.phone.trim(),
                workplace = draft.workplace.trim(),
                apartment = draft.apartment.trim(),
                notes = draft.notes.trim(),
            ),
        )

        val existingWorker = dao.getWorkerByName(normalizedName, normalizedSurname)
        dao.upsertWorker(
            WorkerEntity(
                id = existingWorker?.id ?: 0,
                name = draft.name.trim(),
                surname = draft.surname.trim(),
                plant = draft.workplace.trim(),
                phone = draft.phone.trim(),
                position = existingWorker?.position.orEmpty(),
                hireDate = existingWorker?.hireDate.orEmpty(),
            ),
        )

        val existingSize = dao.getClothesSizeByName(normalizedName, normalizedSurname)
        dao.upsertClothesSize(
            ClothesSizeEntity(
                id = existingSize?.id ?: 0,
                name = draft.name.trim(),
                surname = draft.surname.trim(),
                plant = draft.workplace.trim(),
                shirt = existingSize?.shirt.orEmpty(),
                hoodie = existingSize?.hoodie.orEmpty(),
                pants = existingSize?.pants.orEmpty(),
                jacket = existingSize?.jacket.orEmpty(),
                shoes = existingSize?.shoes.orEmpty(),
            ),
        )
    }

    override suspend fun deleteContact(name: String, surname: String) {
        dao.deleteContact(name, surname)
        dao.deleteWorkerByName(name, surname)
        dao.deleteClothesSizeByName(name, surname)
    }

    override fun observeCars(): Flow<List<CarListItem>> = dao.observeCars().map { items ->
        items.map {
            CarListItem(
                id = it.id,
                name = it.name,
                registration = it.registration,
                driver = it.driver,
                mileage = it.mileage,
                serviceInterval = it.serviceInterval,
                lastService = it.lastService,
            )
        }
    }

    override suspend fun saveCar(draft: CarDraft) {
        val serviceInterval = draft.serviceInterval.toIntOrNull()?.coerceAtLeast(1) ?: 15000
        dao.upsertCar(
            CarEntity(
                name = draft.name.trim(),
                registration = draft.registration.trim().uppercase(),
                driver = draft.driver.trim(),
                serviceInterval = serviceInterval,
            ),
        )
    }

    override suspend fun updateCarMileage(id: Long, mileage: Int) = dao.updateMileage(id, mileage.coerceAtLeast(0))
    override suspend fun updateCarDriver(id: Long, driver: String) = dao.updateDriver(id, driver.trim())
    override suspend fun confirmCarService(id: Long) = dao.confirmService(id)
    override suspend fun deleteCar(id: Long) = dao.deleteCar(id)

    override fun observeWorkers(): Flow<List<WorkerListItem>> = dao.observeWorkers().map { items ->
        items.map {
            WorkerListItem(
                id = it.id,
                name = it.name,
                surname = it.surname,
                plant = it.plant,
                phone = it.phone,
                position = it.position,
                hireDate = it.hireDate,
            )
        }
    }

    override suspend fun saveWorker(draft: WorkerDraft) {
        dao.upsertWorker(
            WorkerEntity(
                id = draft.id ?: 0,
                name = draft.name.trim(),
                surname = draft.surname.trim(),
                plant = draft.plant.trim(),
                phone = draft.phone.trim(),
                position = draft.position.trim(),
                hireDate = draft.hireDate.trim(),
            ),
        )
        val contact = dao.getContact(draft.name.trim().lowercase(), draft.surname.trim().lowercase())
        dao.upsertContact(
            ContactEntity(
                name = draft.name.trim().lowercase(),
                surname = draft.surname.trim().lowercase(),
                email = contact?.email.orEmpty(),
                pesel = contact?.pesel.orEmpty(),
                phone = draft.phone.trim(),
                workplace = draft.plant.trim(),
                apartment = contact?.apartment.orEmpty(),
                notes = contact?.notes.orEmpty(),
            ),
        )
    }

    override suspend fun deleteWorker(id: Long) = dao.deleteWorker(id)

    override fun observePlants(): Flow<List<PlantListItem>> = dao.observePlants().map { items ->
        items.map {
            PlantListItem(
                id = it.id,
                name = it.name,
                city = it.city,
                address = it.address,
                contactPhone = it.contactPhone,
                notes = it.notes,
            )
        }
    }

    override suspend fun savePlant(draft: PlantDraft) {
        dao.upsertPlant(
            PlantEntity(
                id = draft.id ?: 0,
                name = draft.name.trim(),
                city = draft.city.trim(),
                address = draft.address.trim(),
                contactPhone = draft.contactPhone.trim(),
                notes = draft.notes.trim(),
            ),
        )
    }

    override suspend fun deletePlant(id: Long) = dao.deletePlant(id)

    override suspend fun saveVehicleReportDraft(draft: VehicleReportDraft) {
        dao.upsertSetting(SettingEntity(key = "vehicle_report_last_registration", valText = draft.rej))
        dao.upsertSetting(SettingEntity(key = "vehicle_report_last_payload", valText = draft.toString()))
    }
}
