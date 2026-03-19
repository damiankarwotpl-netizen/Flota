package com.future.ultimate.core.database.repository

import android.content.Context
import com.future.ultimate.core.common.model.CarDraft
import com.future.ultimate.core.common.model.ClothesOrderDraft
import com.future.ultimate.core.common.model.ClothesOrderItemDraft
import com.future.ultimate.core.common.model.ClothesSizeDraft
import com.future.ultimate.core.common.model.ContactDraft
import com.future.ultimate.core.common.model.PlantDraft
import com.future.ultimate.core.common.model.VehicleReportDraft
import com.future.ultimate.core.common.model.WorkerDraft
import com.future.ultimate.core.common.pdf.VehicleReportPdfExporter
import com.future.ultimate.core.common.repository.AdminRepository
import com.future.ultimate.core.common.repository.CarListItem
import com.future.ultimate.core.common.repository.ClothesOrderItemListItem
import com.future.ultimate.core.common.repository.ClothesOrderListItem
import com.future.ultimate.core.common.repository.ClothesSizeListItem
import com.future.ultimate.core.common.repository.ClothesHistoryListItem
import com.future.ultimate.core.common.repository.ContactListItem
import com.future.ultimate.core.common.repository.DashboardStats
import com.future.ultimate.core.common.repository.EmailTemplateData
import com.future.ultimate.core.common.repository.PlantListItem
import com.future.ultimate.core.common.repository.SessionReportListItem
import com.future.ultimate.core.common.repository.SmtpSettingsData
import com.future.ultimate.core.common.repository.WorkerListItem
import com.future.ultimate.core.database.dao.AppDao
import com.future.ultimate.core.database.entity.CarEntity
import com.future.ultimate.core.database.entity.ClothesHistoryEntity
import com.future.ultimate.core.database.entity.ClothesOrderEntity
import com.future.ultimate.core.database.entity.ClothesSizeEntity
import com.future.ultimate.core.database.entity.ContactEntity
import com.future.ultimate.core.database.entity.DriverAccountEntity
import com.future.ultimate.core.database.entity.PlantEntity
import com.future.ultimate.core.database.entity.SettingEntity
import com.future.ultimate.core.database.entity.WorkerEntity
import java.io.File
import java.time.LocalDate
import kotlin.random.Random
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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

    override fun observeClothesOrders(): Flow<List<ClothesOrderListItem>> = dao.observeClothesOrders().map { items ->
        items.map {
            ClothesOrderListItem(
                id = it.id,
                date = it.date,
                plant = it.plant,
                status = it.status,
                orderDesc = it.orderDesc,
            )
        }
    }

    override suspend fun saveClothesOrder(draft: ClothesOrderDraft) {
        dao.upsertClothesOrder(
            ClothesOrderEntity(
                date = draft.date.ifBlank { LocalDate.now().toString() },
                plant = draft.plant.trim(),
                status = draft.status.trim().ifBlank { "Nowe" },
                orderDesc = draft.orderDesc.trim(),
            ),
        )
    }

    override fun observeClothesOrderItems(orderId: Long): Flow<List<ClothesOrderItemListItem>> =
        dao.observeClothesOrderItems(orderId).map { items ->
            items.map {
                ClothesOrderItemListItem(
                    id = it.id,
                    orderId = it.orderId,
                    workerId = it.workerId,
                    name = it.name,
                    surname = it.surname,
                    item = it.item,
                    size = it.size,
                    qty = it.qty,
                    issued = it.issued != 0,
                )
            }
        }

    override suspend fun saveClothesOrderItem(orderId: Long, draft: ClothesOrderItemDraft) {
        val cleanName = draft.name.trim()
        val cleanSurname = draft.surname.trim()
        val cleanItem = draft.item.trim()
        val worker = dao.getWorkerByName(cleanName, cleanSurname)
        val sizeEntity = dao.getClothesSizeByName(cleanName, cleanSurname)
        val resolvedSize = draft.size.trim().ifBlank {
            when (cleanItem.lowercase()) {
                "koszulka", "shirt", "t-shirt", "tshirt" -> sizeEntity?.shirt.orEmpty()
                "bluza", "hoodie" -> sizeEntity?.hoodie.orEmpty()
                "spodnie", "pants" -> sizeEntity?.pants.orEmpty()
                "kurtka", "jacket" -> sizeEntity?.jacket.orEmpty()
                "buty", "shoes" -> sizeEntity?.shoes.orEmpty()
                else -> ""
            }
        }
        dao.upsertClothesOrderItems(
            listOf(
                ClothesOrderItemEntity(
                    orderId = orderId,
                    workerId = worker?.id ?: 0,
                    name = cleanName,
                    surname = cleanSurname,
                    item = cleanItem,
                    size = resolvedSize,
                    qty = draft.qty.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                ),
            ),
        )
    }

    override suspend fun deleteClothesOrderItem(id: Long) = dao.deleteClothesOrderItem(id)

    override suspend fun markClothesOrderOrdered(orderId: Long) {
        dao.updateClothesOrderStatus(orderId, "Zamówione")
    }

    override suspend fun issueClothesOrderItem(id: Long) {
        val item = dao.getClothesOrderItem(id) ?: return
        val order = dao.getClothesOrder(item.orderId) ?: return
        if (item.issued != 0 || order.status.trim().lowercase() != "zamówione") return
        dao.insertClothesHistory(
            ClothesHistoryEntity(
                workerId = item.workerId,
                name = item.name,
                surname = item.surname,
                item = item.item,
                size = item.size,
                date = LocalDate.now().toString(),
            ),
        )
        dao.updateClothesOrderItemIssued(id, 1)
        refreshClothesOrderIssueStatus(item.orderId)
    }

    override suspend fun issueAllClothesOrderItems(orderId: Long) {
        val order = dao.getClothesOrder(orderId) ?: return
        if (order.status.trim().lowercase() != "zamówione") return
        dao.getUnissuedClothesOrderItems(orderId).forEach { item ->
            dao.insertClothesHistory(
                ClothesHistoryEntity(
                    workerId = item.workerId,
                    name = item.name,
                    surname = item.surname,
                    item = item.item,
                    size = item.size,
                    date = LocalDate.now().toString(),
                ),
            )
            dao.updateClothesOrderItemIssued(item.id, 1)
        }
        refreshClothesOrderIssueStatus(orderId)
    }

    override suspend fun exportClothesOrderCsv(orderId: Long): String {
        val order = dao.getClothesOrder(orderId) ?: return ""
        val outputDir = context.getExternalFilesDir(null) ?: context.filesDir
        val outputFile = File(outputDir, "clothes_order_${orderId}.csv")
        val items = dao.getClothesOrderItems(orderId)
        outputFile.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.appendLine("order_id,date,plant,status,description")
            writer.appendCsvLine(
                order.id.toString(),
                order.date,
                order.plant,
                order.status,
                order.orderDesc,
            )
            writer.appendLine()
            writer.appendLine("worker_id,name,surname,item,size,qty,issued")
            items.forEach { item ->
                writer.appendCsvLine(
                    item.workerId.toString(),
                    item.name,
                    item.surname,
                    item.item,
                    item.size,
                    item.qty.toString(),
                    if (item.issued != 0) "1" else "0",
                )
            }
        }
        return outputFile.absolutePath
    }

    private suspend fun refreshClothesOrderIssueStatus(orderId: Long) {
        val totalCount = dao.countClothesOrderItems(orderId)
        if (totalCount <= 0) return
        val issuedCount = dao.countIssuedClothesOrderItems(orderId)
        when {
            issuedCount >= totalCount -> dao.updateClothesOrderStatus(orderId, "Wydane")
            issuedCount > 0 -> dao.updateClothesOrderStatus(orderId, "Częściowo wydane")
        }
    }

    override fun observeClothesHistory(): Flow<List<ClothesHistoryListItem>> = dao.observeClothesHistory().map { items ->
        items.map {
            ClothesHistoryListItem(
                id = it.id,
                workerId = it.workerId,
                name = it.name,
                surname = it.surname,
                item = it.item,
                size = it.size,
                date = it.date,
            )
        }
    }

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
                arrayOf(contactCount, workerCount, carCount, plants.size)
            }
            .combine(dao.observeClothesSizes()) { stats, clothesSizes ->
                stats + clothesSizes.size
            }
            .combine(dao.observeClothesOrders()) { stats, clothesOrders ->
                stats + clothesOrders.size
            }
            .combine(dao.observeClothesHistory()) { stats, clothesHistory ->
                DashboardStats(
                    contactCount = stats[0],
                    workerCount = stats[1],
                    carCount = stats[2],
                    plantCount = stats[3],
                    clothesSizeCount = stats[4],
                    clothesOrderCount = stats[5],
                    clothesHistoryCount = clothesHistory.size,
                )
            }

    override suspend fun saveVehicleReportDraft(draft: VehicleReportDraft) {
        dao.upsertSetting(SettingEntity(key = "vehicle_report_last_registration", valText = draft.rej))
        dao.upsertSetting(SettingEntity(key = "vehicle_report_last_payload", valText = draft.toString()))
    }

    override suspend fun exportVehicleReportPdf(draft: VehicleReportDraft): String =
        VehicleReportPdfExporter.export(context, draft, ownerTag = "admin")

    override suspend fun exportClothesHistoryCsv(): String {
        val outputDir = context.getExternalFilesDir(null) ?: context.filesDir
        val outputFile = File(outputDir, "clothes_history.csv")
        val rows = dao.observeClothesHistory().map { items ->
            items.sortedWith(compareByDescending<com.future.ultimate.core.database.entity.ClothesHistoryEntity> { it.date }.thenByDescending { it.id })
        }
        val latestRows = rows.first()
        outputFile.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.appendLine("worker_id,name,surname,item,size,date")
            latestRows.forEach { row ->
                writer.appendCsvLine(
                    row.workerId.toString(),
                    row.name,
                    row.surname,
                    row.item,
                    row.size,
                    row.date,
                )
            }
        }
        return outputFile.absolutePath
    }

    override suspend fun exportSessionReportsCsv(): String {
        val outputDir = context.getExternalFilesDir(null) ?: context.filesDir
        val outputFile = File(outputDir, "session_reports.csv")
        val rows = dao.observeReports().map { items ->
            items.sortedByDescending { it.id }
        }.first()
        outputFile.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.appendLine("date,ok,fail,skip,auto,details")
            rows.forEach { row ->
                writer.appendCsvLine(
                    row.date,
                    row.ok.toString(),
                    row.fail.toString(),
                    row.skip.toString(),
                    row.auto.toString(),
                    row.details,
                )
            }
        }
        return outputFile.absolutePath
    }

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

    private fun Appendable.appendCsvLine(vararg columns: String) {
        appendLine(columns.joinToString(",") { value ->
            "\"${value.replace("\"", "\"\"")}\""
        })
    }

    private fun generatePassword(length: Int = 6): String = buildString {
        repeat(length) { append(Random.nextInt(0, 10)) }
    }
}
