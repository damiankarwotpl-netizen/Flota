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
import com.future.ultimate.core.common.export.SimpleXlsxWorkbookWriter
import com.future.ultimate.core.common.pdf.ClothesOrderPdfExporter
import com.future.ultimate.core.common.pdf.VehicleReportPdfExporter
import com.future.ultimate.core.common.repository.AdminRepository
import com.future.ultimate.core.common.repository.CarListItem
import com.future.ultimate.core.common.repository.ClothesOrderXlsxExport
import com.future.ultimate.core.common.repository.ClothesOrderItemListItem
import com.future.ultimate.core.common.repository.ClothesOrderListItem
import com.future.ultimate.core.common.repository.ClothesOrderWorkerListItem
import com.future.ultimate.core.common.repository.ClothesSizeListItem
import com.future.ultimate.core.common.repository.ClothesHistoryListItem
import com.future.ultimate.core.common.repository.ContactListItem
import com.future.ultimate.core.common.repository.DashboardStats
import com.future.ultimate.core.common.repository.DriverAccountCredentials
import com.future.ultimate.core.common.repository.EmailTemplateData
import com.future.ultimate.core.common.repository.PlantListItem
import com.future.ultimate.core.common.repository.SessionReportListItem
import com.future.ultimate.core.common.repository.SmtpSettingsData
import com.future.ultimate.core.common.repository.WorkerListItem
import com.future.ultimate.core.database.dao.AppDao
import com.future.ultimate.core.database.entity.CarEntity
import com.future.ultimate.core.database.entity.ClothesHistoryEntity
import com.future.ultimate.core.database.entity.ClothesOrderEntity
import com.future.ultimate.core.database.entity.ClothesOrderItemEntity
import com.future.ultimate.core.database.entity.ClothesSizeEntity
import com.future.ultimate.core.database.entity.ContactEntity
import com.future.ultimate.core.database.entity.DriverAccountEntity
import com.future.ultimate.core.database.entity.PlantEntity
import com.future.ultimate.core.database.entity.SettingEntity
import com.future.ultimate.core.database.entity.WorkerEntity
import java.io.File
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
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

    override suspend fun resetCarDriverCredentials(id: Long): DriverAccountCredentials {
        val car = dao.getCar(id) ?: return DriverAccountCredentials()
        val normalizedDriver = car.driver.trim()
        if (normalizedDriver.isBlank()) {
            dao.deleteDriverAccountByRegistration(car.registration)
            return DriverAccountCredentials()
        }
        val account = syncDriverAccount(normalizedDriver, car.registration, forceReset = true)
        return DriverAccountCredentials(
            login = account?.login.orEmpty(),
            password = account?.password.orEmpty(),
        )
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
                id = draft.id ?: 0,
                date = draft.date.ifBlank { LocalDate.now().toString() },
                plant = draft.plant.trim(),
                status = draft.status.trim().ifBlank { "Nowe" },
                orderDesc = draft.orderDesc.trim(),
            ),
        )
    }

    override suspend fun deleteClothesOrder(orderId: Long) {
        dao.deleteClothesOrderItemsByOrderId(orderId)
        dao.deleteClothesOrder(orderId)
    }

    override fun observeClothesOrderWorkers(): Flow<List<ClothesOrderWorkerListItem>> =
        dao.observeWorkers().combine(dao.observeClothesSizes()) { workers, sizes ->
            val sizeByWorker = sizes.associateBy { "${it.name.trim().lowercase()}|${it.surname.trim().lowercase()}" }
            workers.map { worker ->
                val size = sizeByWorker["${worker.name.trim().lowercase()}|${worker.surname.trim().lowercase()}"]
                ClothesOrderWorkerListItem(
                    id = worker.id,
                    name = worker.name,
                    surname = worker.surname,
                    plant = worker.plant.ifBlank { size?.plant.orEmpty() },
                    shirt = size?.shirt.orEmpty(),
                    hoodie = size?.hoodie.orEmpty(),
                    pants = size?.pants.orEmpty(),
                    jacket = size?.jacket.orEmpty(),
                    shoes = size?.shoes.orEmpty(),
                )
            }.sortedWith(compareBy({ it.surname.lowercase() }, { it.name.lowercase() }))
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
        val existingItem = draft.id?.let { dao.getClothesOrderItem(it) }
        val worker = dao.getWorkerByName(cleanName, cleanSurname)
        val sizeEntity = dao.getClothesSizeByName(cleanName, cleanSurname)
        val resolvedSize = draft.size.trim().ifBlank {
            when (cleanItem.lowercase()) {
                "koszulka", "shirt", "t-shirt", "tshirt" -> sizeEntity?.shirt.orEmpty()
                "bluza", "hoodie" -> sizeEntity?.hoodie.orEmpty()
                "spodnie", "pants" -> sizeEntity?.pants.orEmpty()
                "kurtka", "jacket" -> sizeEntity?.jacket.orEmpty()
                "buty", "shoes" -> sizeEntity?.shoes.orEmpty()
                else -> existingItem?.size.orEmpty()
            }
        }
        dao.upsertClothesOrderItems(
            listOf(
                ClothesOrderItemEntity(
                    id = draft.id ?: 0,
                    orderId = orderId,
                    workerId = worker?.id ?: existingItem?.workerId ?: 0,
                    name = cleanName,
                    surname = cleanSurname,
                    item = cleanItem,
                    size = resolvedSize,
                    qty = draft.qty.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                    issued = existingItem?.issued ?: 0,
                ),
            ),
        )
        syncClothesOrderIssueStatus(orderId)
    }

    override suspend fun createClothesOrderStarter(
        draft: ClothesOrderDraft,
        workerIds: Set<Long>,
        shirtQty: Int,
        hoodieQty: Int,
        pantsQty: Int,
        jacketQty: Int,
        shoesQty: Int,
    ): Long? {
        val selectedWorkers = dao.observeWorkers().first().filter { it.id in workerIds }
        if (selectedWorkers.isEmpty()) return null

        val sizeByWorker = dao.observeClothesSizes().first().associateBy {
            "${it.name.trim().lowercase()}|${it.surname.trim().lowercase()}"
        }
        val inferredPlant = selectedWorkers.map { it.plant.trim() }.filter { it.isNotBlank() }.distinct().singleOrNull().orEmpty()

        val orderId = dao.upsertClothesOrder(
            ClothesOrderEntity(
                id = 0,
                date = draft.date.ifBlank { LocalDate.now().toString() },
                plant = draft.plant.trim().ifBlank { inferredPlant },
                status = draft.status.trim().ifBlank { "Nowe" },
                orderDesc = draft.orderDesc.trim(),
            ),
        )

        val items = buildList {
            selectedWorkers.forEach { worker ->
                val size = sizeByWorker["${worker.name.trim().lowercase()}|${worker.surname.trim().lowercase()}"]
                fun addItem(label: String, itemSize: String, qty: Int) {
                    if (qty <= 0) return
                    add(
                        ClothesOrderItemEntity(
                            orderId = orderId,
                            workerId = worker.id,
                            name = worker.name.trim(),
                            surname = worker.surname.trim(),
                            item = label,
                            size = itemSize,
                            qty = qty,
                            issued = 0,
                        ),
                    )
                }
                addItem("Koszulka", size?.shirt.orEmpty(), shirtQty)
                addItem("Bluza", size?.hoodie.orEmpty(), hoodieQty)
                addItem("Spodnie", size?.pants.orEmpty(), pantsQty)
                addItem("Kurtka", size?.jacket.orEmpty(), jacketQty)
                addItem("Buty", size?.shoes.orEmpty(), shoesQty)
            }
        }
        if (items.isEmpty()) {
            dao.deleteClothesOrder(orderId)
            return null
        }
        dao.upsertClothesOrderItems(items)
        return orderId
    }

    override suspend fun deleteClothesOrderItem(id: Long) {
        val item = dao.getClothesOrderItem(id)
        dao.deleteClothesOrderItem(id)
        item?.orderId?.let { syncClothesOrderIssueStatus(it) }
    }

    override suspend fun markClothesOrderOrdered(orderId: Long) {
        val order = dao.getClothesOrder(orderId) ?: return
        if (!canMarkClothesOrderOrdered(order.status)) return
        dao.updateClothesOrderStatus(orderId, "Zamówione")
    }

    override suspend fun issueClothesOrderItem(id: Long) {
        val item = dao.getClothesOrderItem(id) ?: return
        val order = dao.getClothesOrder(item.orderId) ?: return
        if (item.issued != 0 || !canIssueClothesOrder(order.status)) return
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
        syncClothesOrderIssueStatus(item.orderId)
    }

    override suspend fun issueAllClothesOrderItems(orderId: Long) {
        val order = dao.getClothesOrder(orderId) ?: return
        if (!canIssueClothesOrder(order.status)) return
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
        syncClothesOrderIssueStatus(orderId)
    }

    override suspend fun exportClothesOrderPdf(orderId: Long): String {
        val order = dao.getClothesOrder(orderId) ?: return ""
        val items = dao.getClothesOrderItems(orderId).map {
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
        if (items.isEmpty()) return ""
        return ClothesOrderPdfExporter.export(
            context = context,
            orderId = order.id,
            date = order.date,
            plant = order.plant,
            status = order.status,
            description = order.orderDesc,
            items = items,
        )
    }

    override suspend fun exportClothesIssuePdf(orderId: Long): String {
        val order = dao.getClothesOrder(orderId) ?: return ""
        val items = dao.getUnissuedClothesOrderItems(orderId).map {
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
        if (items.isEmpty()) return ""
        return ClothesOrderPdfExporter.exportIssueReport(
            context = context,
            orderId = order.id,
            date = order.date,
            plant = order.plant,
            status = order.status,
            description = order.orderDesc,
            items = items,
        )
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

    override suspend fun exportClothesOrderXlsx(orderId: Long): ClothesOrderXlsxExport {
        if (dao.getClothesOrder(orderId) == null) return ClothesOrderXlsxExport()
        val items = dao.getClothesOrderItems(orderId)
        if (items.isEmpty()) return ClothesOrderXlsxExport()

        val outputDir = context.getExternalFilesDir(null) ?: context.filesDir
        val supplierFile = File(outputDir, "zamowienie_hurtownia_${orderId}.xlsx")
        val issueFile = File(outputDir, "raport_wydania_${orderId}.xlsx")
        val pendingItems = items.filter { it.issued == 0 }

        val supplierRows = buildList {
            add(
                listOf(
                    SimpleXlsxWorkbookWriter.Cell.text("Pozycja"),
                    SimpleXlsxWorkbookWriter.Cell.text("Rozmiar"),
                    SimpleXlsxWorkbookWriter.Cell.text("Ilość"),
                ),
            )
            items
                .groupBy { it.item to it.size.ifBlank { "-" } }
                .toSortedMap(compareBy({ it.first.lowercase() }, { it.second.lowercase() }))
                .forEach { (key, groupedItems) ->
                    add(
                        listOf(
                            SimpleXlsxWorkbookWriter.Cell.text(key.first),
                            SimpleXlsxWorkbookWriter.Cell.text(key.second),
                            SimpleXlsxWorkbookWriter.Cell.number(groupedItems.sumOf { it.qty }),
                        ),
                    )
                }
        }

        val issueRows = buildList {
            add(
                listOf(
                    SimpleXlsxWorkbookWriter.Cell.text("Pracownik"),
                    SimpleXlsxWorkbookWriter.Cell.text("Pozycja"),
                    SimpleXlsxWorkbookWriter.Cell.text("Rozmiar"),
                    SimpleXlsxWorkbookWriter.Cell.text("Ilość"),
                ),
            )
            if (pendingItems.isEmpty()) {
                add(
                    listOf(
                        SimpleXlsxWorkbookWriter.Cell.text("Brak pozycji do wydania"),
                        SimpleXlsxWorkbookWriter.Cell.text("-"),
                        SimpleXlsxWorkbookWriter.Cell.text("-"),
                        SimpleXlsxWorkbookWriter.Cell.text("-"),
                    ),
                )
            } else {
                pendingItems
                    .sortedWith(compareBy({ it.surname.lowercase() }, { it.name.lowercase() }, { it.item.lowercase() }))
                    .forEach { item ->
                        add(
                            listOf(
                                SimpleXlsxWorkbookWriter.Cell.text("${item.name} ${item.surname}".trim()),
                                SimpleXlsxWorkbookWriter.Cell.text(item.item),
                                SimpleXlsxWorkbookWriter.Cell.text(item.size.ifBlank { "-" }),
                                SimpleXlsxWorkbookWriter.Cell.number(item.qty),
                            ),
                        )
                    }
            }
        }

        SimpleXlsxWorkbookWriter.writeSingleSheet(
            file = supplierFile,
            sheetName = "Hurtownia",
            rows = supplierRows,
        )
        SimpleXlsxWorkbookWriter.writeSingleSheet(
            file = issueFile,
            sheetName = "Wydanie",
            rows = issueRows,
        )

        return ClothesOrderXlsxExport(
            supplierPath = supplierFile.absolutePath,
            issuePath = issueFile.absolutePath,
        )
    }

    private suspend fun syncClothesOrderIssueStatus(orderId: Long) {
        val order = dao.getClothesOrder(orderId) ?: return
        val totalCount = dao.countClothesOrderItems(orderId)
        if (totalCount <= 0) return
        val issuedCount = dao.countIssuedClothesOrderItems(orderId)
        val nextStatus = when {
            issuedCount >= totalCount -> "Wydane"
            issuedCount > 0 -> "Częściowo wydane"
            isClothesOrderIssueWorkflowStatus(order.status) -> "Zamówione"
            else -> order.status
        }
        if (nextStatus != order.status) {
            dao.updateClothesOrderStatus(orderId, nextStatus)
        }
    }

    private fun canIssueClothesOrder(status: String): Boolean {
        val normalized = status.trim().lowercase()
        return normalized == "zamówione" || normalized == "częściowo wydane"
    }

    private fun canMarkClothesOrderOrdered(status: String): Boolean {
        val normalized = status.trim().lowercase()
        return normalized != "częściowo wydane" && normalized != "wydane"
    }

    private fun isClothesOrderIssueWorkflowStatus(status: String): Boolean {
        val normalized = status.trim().lowercase()
        return normalized == "zamówione" || normalized == "częściowo wydane" || normalized == "wydane"
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

    override suspend fun exportDatabaseSnapshot(): String {
        val outputDir = context.getExternalFilesDir(null) ?: context.filesDir
        val outputFile = File(outputDir, "future_v20_snapshot.zip")
        val databaseFiles = listOf(
            context.getDatabasePath("future_v20.db"),
            context.getDatabasePath("future_v20.db-wal"),
            context.getDatabasePath("future_v20.db-shm"),
        ).filter { it.exists() }

        ZipOutputStream(outputFile.outputStream().buffered()).use { zip ->
            databaseFiles.forEach { file ->
                zip.putNextEntry(ZipEntry(file.name))
                file.inputStream().use { input -> input.copyTo(zip) }
                zip.closeEntry()
            }
        }
        return outputFile.absolutePath
    }

    override suspend fun exportContactsCsv(): String {
        val outputDir = context.getExternalFilesDir(null) ?: context.filesDir
        val outputFile = File(outputDir, "contacts_table.csv")
        val rows = dao.observeContacts().first().sortedWith(compareBy<ContactEntity> { it.surname }.thenBy { it.name })
        outputFile.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.appendLine("name,surname,email,phone,workplace,apartment,notes")
            rows.forEach { row ->
                writer.appendCsvLine(
                    row.name,
                    row.surname,
                    row.email,
                    row.phone,
                    row.workplace,
                    row.apartment,
                    row.notes,
                )
            }
        }
        return outputFile.absolutePath
    }

    override suspend fun exportContactRowXlsx(name: String, surname: String): String {
        val contact = dao.getContact(name.trim().lowercase(), surname.trim().lowercase())
            ?: return ""
        val outputDir = context.getExternalFilesDir(null) ?: context.filesDir
        val safeName = sanitizeFilePart(contact.name.ifBlank { "kontakt" })
        val safeSurname = sanitizeFilePart(contact.surname.ifBlank { "rekord" })
        val outputFile = File(outputDir, "kontakt_${safeName}_${safeSurname}.xlsx")
        SimpleXlsxWorkbookWriter.writeSingleSheet(
            file = outputFile,
            sheetName = "Kontakty",
            rows = listOf(
                listOf(
                    SimpleXlsxWorkbookWriter.Cell.text("Imię"),
                    SimpleXlsxWorkbookWriter.Cell.text("Nazwisko"),
                    SimpleXlsxWorkbookWriter.Cell.text("Email"),
                    SimpleXlsxWorkbookWriter.Cell.text("Telefon"),
                    SimpleXlsxWorkbookWriter.Cell.text("Miejsce pracy"),
                    SimpleXlsxWorkbookWriter.Cell.text("Mieszkanie"),
                    SimpleXlsxWorkbookWriter.Cell.text("Notatki"),
                ),
                listOf(
                    SimpleXlsxWorkbookWriter.Cell.text(contact.name),
                    SimpleXlsxWorkbookWriter.Cell.text(contact.surname),
                    SimpleXlsxWorkbookWriter.Cell.text(contact.email),
                    SimpleXlsxWorkbookWriter.Cell.text(contact.phone),
                    SimpleXlsxWorkbookWriter.Cell.text(contact.workplace),
                    SimpleXlsxWorkbookWriter.Cell.text(contact.apartment),
                    SimpleXlsxWorkbookWriter.Cell.text(contact.notes),
                ),
            ),
        )
        return outputFile.absolutePath
    }

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

    private suspend fun syncDriverAccount(
        driverName: String,
        registration: String,
        forceReset: Boolean = false,
    ): DriverAccountEntity? {
        val normalizedDriver = driverName.trim()
        val normalizedRegistration = registration.trim().uppercase()
        if (normalizedRegistration.isBlank()) return null
        if (normalizedDriver.isBlank()) {
            dao.deleteDriverAccountByRegistration(normalizedRegistration)
            return null
        }

        val existing = dao.getDriverAccountByRegistration(normalizedRegistration)
        val shouldRotateCredentials = forceReset || existing == null || !existing.driverName.equals(normalizedDriver, ignoreCase = true)
        val account = DriverAccountEntity(
            registration = normalizedRegistration,
            login = generateLogin(normalizedDriver),
            password = if (shouldRotateCredentials) generatePassword() else existing.password,
            driverName = normalizedDriver,
            changePassword = if (shouldRotateCredentials) 1 else existing.changePassword,
        )
        dao.upsertDriverAccount(account)
        return account
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

    private fun sanitizeFilePart(value: String): String = value
        .trim()
        .replace(Regex("[^A-Za-z0-9_-]+"), "_")
        .trim('_')
        .ifBlank { "export" }

    private fun generatePassword(length: Int = 6): String = buildString {
        repeat(length) { append(Random.nextInt(0, 10)) }
    }
}
