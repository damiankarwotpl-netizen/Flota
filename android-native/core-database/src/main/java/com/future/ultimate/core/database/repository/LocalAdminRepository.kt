package com.future.ultimate.core.database.repository

import android.content.Context
import com.future.ultimate.core.common.model.CarDraft
import com.future.ultimate.core.common.model.ClothesSizeDraft
import com.future.ultimate.core.common.model.ContactDraft
import com.future.ultimate.core.common.model.PlantDraft
import com.future.ultimate.core.common.model.VehicleReportDraft
import com.future.ultimate.core.common.model.WorkerDraft
import com.future.ultimate.core.common.pdf.VehicleReportPdfExporter
import com.future.ultimate.core.common.repository.AdminRepository
import com.future.ultimate.core.common.repository.CarListItem
import com.future.ultimate.core.common.repository.ClothesSizeListItem
import com.future.ultimate.core.common.repository.ContactListItem
import com.future.ultimate.core.common.repository.DashboardStats
import com.future.ultimate.core.common.repository.EmailTemplateData
import com.future.ultimate.core.common.repository.PlantListItem
import com.future.ultimate.core.common.repository.SessionReportListItem
import com.future.ultimate.core.common.repository.SmtpSettingsData
import com.future.ultimate.core.common.repository.WorkerListItem
import com.future.ultimate.core.database.dao.AppDao
import com.future.ultimate.core.database.entity.CarEntity
import com.future.ultimate.core.database.entity.ClothesSizeEntity
import com.future.ultimate.core.database.entity.ContactEntity
import com.future.ultimate.core.database.entity.DriverAccountEntity
import com.future.ultimate.core.database.entity.PlantEntity
import com.future.ultimate.core.database.entity.SettingEntity
import com.future.ultimate.core.database.entity.WorkerEntity
import kotlin.random.Random
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class LocalAdminRepository(
    private val dao: AppDao,
    private val context: Context,
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

    override fun observeCars(): Flow<List<CarListItem>> = dao.observeCars().combine(dao.observeDriverAccounts()) { items, accounts ->
        val accountsByRegistration = accounts.associateBy { it.registration.uppercase() }
        items.map {
            val driverAccount = accountsByRegistration[it.registration.uppercase()]
            CarListItem(
                id = it.id,
                name = it.name,
                registration = it.registration,
                driver = it.driver,
                mileage = it.mileage,
                serviceInterval = it.serviceInterval,
                lastService = it.lastService,
                driverLogin = driverAccount?.login.orEmpty(),
                driverPassword = driverAccount?.password.orEmpty(),
                changePasswordRequired = driverAccount?.changePassword == 1,
            )
        }
    }

    override suspend fun saveCar(draft: CarDraft) {
        val serviceInterval = draft.serviceInterval.toIntOrNull()?.coerceAtLeast(1) ?: 15000
        val registration = draft.registration.trim().uppercase()
        dao.upsertCar(
            CarEntity(
                name = draft.name.trim(),
                registration = registration,
                driver = draft.driver.trim(),
                serviceInterval = serviceInterval,
            ),
        )
        syncDriverAccount(draft.driver, registration)
    }

    override suspend fun updateCarMileage(id: Long, mileage: Int) = dao.updateMileage(id, mileage.coerceAtLeast(0))

    override suspend fun updateCarDriver(id: Long, driver: String) {
        val normalizedDriver = driver.trim()
        dao.updateDriver(id, normalizedDriver)
        val car = dao.getCar(id) ?: return
        syncDriverAccount(normalizedDriver, car.registration)
    }

    override suspend fun confirmCarService(id: Long) = dao.confirmService(id)

    override suspend fun deleteCar(id: Long) {
        val car = dao.getCar(id)
        dao.deleteCar(id)
        car?.registration?.let { dao.deleteDriverAccountByRegistration(it) }
    }

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

    override fun observeClothesSizes(): Flow<List<ClothesSizeListItem>> = dao.observeClothesSizes().map { items ->
        items.map {
            ClothesSizeListItem(
                id = it.id,
                name = it.name,
                surname = it.surname,
                plant = it.plant,
                shirt = it.shirt,
                hoodie = it.hoodie,
                pants = it.pants,
                jacket = it.jacket,
                shoes = it.shoes,
            )
        }
    }

    override suspend fun saveClothesSize(draft: ClothesSizeDraft) {
        dao.upsertClothesSize(
            ClothesSizeEntity(
                id = draft.id ?: 0,
                name = draft.name.trim(),
                surname = draft.surname.trim(),
                plant = draft.plant.trim(),
                shirt = draft.shirt.trim(),
                hoodie = draft.hoodie.trim(),
                pants = draft.pants.trim(),
                jacket = draft.jacket.trim(),
                shoes = draft.shoes.trim(),
            ),
        )
    }

    override suspend fun deleteClothesSize(id: Long) = dao.deleteClothesSize(id)

    override fun observeSmtpSettings(): Flow<SmtpSettingsData> = dao.observeSettings().map { settings ->
        val map = settings.associateBy({ it.key }, { it.valText })
        SmtpSettingsData(
            host = map["smtp_host"].orEmpty(),
            port = map["smtp_port"].orEmpty().ifBlank { "587" },
            user = map["smtp_user"].orEmpty(),
            password = map["smtp_password"].orEmpty(),
        )
    }

    override suspend fun saveSmtpSettings(settings: SmtpSettingsData) {
        dao.upsertSetting(SettingEntity(key = "smtp_host", valText = settings.host.trim()))
        dao.upsertSetting(SettingEntity(key = "smtp_port", valText = settings.port.trim()))
        dao.upsertSetting(SettingEntity(key = "smtp_user", valText = settings.user.trim()))
        dao.upsertSetting(SettingEntity(key = "smtp_password", valText = settings.password))
    }

    override fun observeEmailTemplate(): Flow<EmailTemplateData> = dao.observeSettings().map { settings ->
        val map = settings.associateBy({ it.key }, { it.valText })
        EmailTemplateData(
            subject = map["t_sub"].orEmpty(),
            body = map["t_body"].orEmpty(),
        )
    }

    override suspend fun saveEmailTemplate(template: EmailTemplateData) {
        dao.upsertSetting(SettingEntity(key = "t_sub", valText = template.subject.trim()))
        dao.upsertSetting(SettingEntity(key = "t_body", valText = template.body.trim()))
    }

    override fun observeSessionReports(): Flow<List<SessionReportListItem>> = dao.observeReports().map { items ->
        items.map {
            SessionReportListItem(
                date = it.date,
                ok = it.ok,
                fail = it.fail,
                skip = it.skip,
                details = it.details,
            )
        }
    }

    override fun observeDashboardStats(): Flow<DashboardStats> =
        combine(dao.observeContacts(), dao.observeWorkers()) { contacts, workers -> contacts.size to workers.size }
            .combine(dao.observeCars()) { (contactCount, workerCount), cars -> Triple(contactCount, workerCount, cars.size) }
            .combine(dao.observePlants()) { (contactCount, workerCount, carCount), plants ->
                DashboardStats(
                    contactCount = contactCount,
                    workerCount = workerCount,
                    carCount = carCount,
                    plantCount = plants.size,
                )
            }

    override suspend fun saveVehicleReportDraft(draft: VehicleReportDraft) {
        dao.upsertSetting(SettingEntity(key = "vehicle_report_last_registration", valText = draft.rej))
        dao.upsertSetting(SettingEntity(key = "vehicle_report_last_payload", valText = draft.toString()))
    }

    override suspend fun exportVehicleReportPdf(draft: VehicleReportDraft): String =
        VehicleReportPdfExporter.export(context, draft, ownerTag = "admin")

    private suspend fun syncDriverAccount(driverName: String, registration: String) {
        val normalizedDriver = driverName.trim()
        val normalizedRegistration = registration.trim().uppercase()
        if (normalizedDriver.isBlank() || normalizedRegistration.isBlank()) return

        dao.upsertDriverAccount(
            DriverAccountEntity(
                registration = normalizedRegistration,
                login = generateLogin(normalizedDriver),
                password = generatePassword(),
                driverName = normalizedDriver,
                changePassword = 1,
            ),
        )
    }

    private fun generateLogin(name: String): String {
        val sanitized = name.trim().lowercase()
            .replace(" ", ".")
            .replace(Regex("[^a-z0-9._-]"), "")
            .replace(Regex("\\.{2,}"), ".")
            .trim('.')
        return sanitized.ifBlank { "driver" }
    }

    private fun generatePassword(length: Int = 6): String = buildString {
        repeat(length) { append(Random.nextInt(0, 10)) }
    }
}
