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
import com.future.ultimate.core.common.patch.PatchLoader
import com.future.ultimate.core.common.repository.AdminRepository
import com.future.ultimate.core.common.repository.CarListItem
import com.future.ultimate.core.common.repository.ClothesOrderXlsxExport
import com.future.ultimate.core.common.repository.ClothesOrderItemListItem
import com.future.ultimate.core.common.repository.ClothesOrderImportRow
import com.future.ultimate.core.common.repository.ClothesOrderListItem
import com.future.ultimate.core.common.repository.ClothesOrderWorkerListItem
import com.future.ultimate.core.common.repository.ClothesSizeListItem
import com.future.ultimate.core.common.repository.ClothesHistoryListItem
import com.future.ultimate.core.common.repository.ContactListItem
import com.future.ultimate.core.common.repository.DashboardStats
import com.future.ultimate.core.common.repository.DriverAccountCredentials
import com.future.ultimate.core.common.repository.EmailTemplateData
import com.future.ultimate.core.common.repository.MailDispatchResult
import com.future.ultimate.core.common.repository.PlantListItem
import com.future.ultimate.core.common.repository.PayrollWorkbookRow
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
import com.future.ultimate.core.database.entity.ReportEntity
import com.future.ultimate.core.database.entity.SettingEntity
import com.future.ultimate.core.database.entity.WorkerEntity
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.random.Random
import javax.activation.DataHandler
import javax.activation.FileDataSource
import javax.mail.Message
import javax.mail.Multipart
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

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

    override fun observeCars(): Flow<List<CarListItem>> =
        dao.observeCars()
            .combine(dao.observeDriverAccounts()) { items, accounts -> items to accounts }
            .combine(dao.observeSettings()) { (items, accounts), settings ->
                Triple(items, accounts, settings.associateBy({ it.key }, { it.valText }))
            }
            .map { (items, accounts, settings) ->
                val accountsByRegistration = accounts.associateBy { it.registration.uppercase() }
                items.map {
                    val registrationKey = it.registration.uppercase()
                    val driverAccount = accountsByRegistration[registrationKey]
                    val queuedMileage = settings["driver_mileage_sync_pending_$registrationKey"]
                        ?.substringBefore("|")
                        ?.toIntOrNull()
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
                        pendingMileageSync = queuedMileage != null,
                        queuedMileage = queuedMileage,
                        lastMileageSyncAt = settings["driver_mileage_sync_at_$registrationKey"].orEmpty(),
                        lastMileageSyncStatus = settings["driver_mileage_sync_status_$registrationKey"].orEmpty(),
                    )
                }
            }

    override suspend fun saveCar(draft: CarDraft) {
        val serviceInterval = draft.serviceInterval.toIntOrNull()?.coerceAtLeast(1) ?: 15000
        val registration = draft.registration.trim().uppercase()
        val existingCar = if (draft.id != null) {
            dao.getCar(draft.id)
        } else {
            dao.getCarByRegistration(registration)
        }
        dao.upsertCar(
            CarEntity(
                id = existingCar?.id ?: draft.id ?: 0,
                name = draft.name.trim(),
                registration = registration,
                driver = draft.driver.trim(),
                serviceInterval = serviceInterval,
                mileage = existingCar?.mileage ?: 0,
                lastService = existingCar?.lastService ?: 0,
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

    override suspend fun importClothesOrderItems(orderId: Long, rows: List<ClothesOrderImportRow>): Int {
        rows.forEach { row ->
            saveClothesOrderItem(
                orderId = orderId,
                draft = ClothesOrderItemDraft(
                    name = row.name,
                    surname = row.surname,
                    item = row.item,
                    size = row.size,
                    qty = row.qty,
                ),
            )
        }
        return rows.size
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
        if (dao.countClothesOrderItems(orderId) <= 0) return
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
        val outputDir = PatchLoader.safeExternalDir(context, feature = "clothes_order_csv")
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

        val outputDir = PatchLoader.safeExternalDir(context, feature = "clothes_order_xlsx")
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
        if (totalCount <= 0) {
            if (isClothesOrderIssueWorkflowStatus(order.status)) {
                dao.updateClothesOrderStatus(orderId, "Nowe")
            }
            return
        }
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

    override suspend fun validateSmtpConnection(settings: SmtpSettingsData) {
        withContext(Dispatchers.IO) {
            val normalized = settings.normalized()
            val transport = buildMailSession(normalized).getTransport("smtp")
            try {
                transport.connect(normalized.host, normalized.port.toInt(), normalized.user, normalized.password)
            } finally {
                transport.close()
            }
        }
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

    override suspend fun sendSinglePreviewMail(attachmentPaths: List<String>): String {
        return withContext(Dispatchers.IO) {
            val settings = loadSavedSmtpSettings().normalized()
            val template = loadSavedEmailTemplate().validated()
            val today = LocalDate.now()
            sendMail(
                settings = settings,
                recipient = settings.user,
                subject = renderTemplate(template.subject, "Podgląd SMTP", today),
                body = buildString {
                    appendLine(renderTemplate(template.body, "Podgląd SMTP", today))
                    appendLine()
                    append("To jest testowa wiadomość podglądowa wysłana z natywnego modułu Android.")
                },
                attachments = sanitizeAttachments(attachmentPaths),
            )
            settings.user
        }
    }

    override suspend fun sendMassMailing(attachmentPaths: List<String>, autoMode: Boolean): MailDispatchResult {
        return withContext(Dispatchers.IO) {
            val settings = loadSavedSmtpSettings().normalized()
            val template = loadSavedEmailTemplate().validated()
            val attachments = sanitizeAttachments(attachmentPaths)
            val contacts = dao.observeContacts().first()
            val today = LocalDate.now()
            var ok = 0
            var fail = 0
            var skip = 0
            val details = mutableListOf<String>()

            contacts.forEach { contact ->
                val email = contact.email.trim()
                val fullName = listOf(contact.name, contact.surname).joinToString(" ").trim()
                if (email.isBlank()) {
                    skip += 1
                    details += "SKIP: ${fullName.ifBlank { "Bez nazwy" }} — brak email"
                    return@forEach
                }

                runCatching {
                    sendMail(
                        settings = settings,
                        recipient = email,
                        subject = renderTemplate(template.subject, contact.name.ifBlank { fullName }, today),
                        body = renderTemplate(template.body, contact.name.ifBlank { fullName }, today),
                        attachments = attachments,
                    )
                }.onSuccess {
                    ok += 1
                    details += "OK: ${fullName.ifBlank { email }} <$email>"
                }.onFailure { error ->
                    fail += 1
                    details += "FAIL: ${fullName.ifBlank { email }} <$email> — ${error.message.orEmpty().ifBlank { "nieznany błąd" }}"
                }
            }

            val result = MailDispatchResult(
                ok = ok,
                fail = fail,
                skip = skip,
                details = details.joinToString("\n"),
            )
            dao.insertReport(
                ReportEntity(
                    date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    ok = ok,
                    fail = fail,
                    skip = skip,
                    auto = if (autoMode) 1 else 0,
                    details = result.details,
                ),
            )
            result
        }
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
        val outputDir = PatchLoader.safeExternalDir(context, feature = "database_snapshot")
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
        val outputDir = PatchLoader.safeExternalDir(context, feature = "contacts_csv")
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
        val outputDir = PatchLoader.safeExternalDir(context, feature = "contact_row_xlsx")
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

    override suspend fun exportPayrollPackage(contacts: List<ContactListItem>): String {
        if (contacts.isEmpty()) return ""
        val outputDir = PatchLoader.safeExternalDir(context, feature = "payroll_package")
        val outputFile = File(
            outputDir,
            "payroll_package_${DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now())}.zip",
        )
        val selectedContacts = contacts
            .distinctBy { "${it.name.trim().lowercase()}|${it.surname.trim().lowercase()}" }
            .sortedWith(compareBy<ContactListItem> { it.surname.lowercase() }.thenBy { it.name.lowercase() })
        val summaryCsv = File(outputDir, "payroll_package_contacts.csv")
        summaryCsv.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.appendLine("name,surname,email,phone,workplace,apartment,notes")
            selectedContacts.forEach { row ->
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
        val attachments = buildList {
            add(summaryCsv)
            selectedContacts.forEach { contact ->
                val path = exportContactRowXlsx(contact.name, contact.surname)
                if (path.isNotBlank()) add(File(path))
            }
        }.distinctBy { it.absolutePath }

        ZipOutputStream(outputFile.outputStream().buffered()).use { zip ->
            attachments.filter { it.exists() }.forEach { file ->
                zip.putNextEntry(ZipEntry(file.name))
                file.inputStream().use { input -> input.copyTo(zip) }
                zip.closeEntry()
            }
        }
        return outputFile.absolutePath
    }

    override suspend fun exportPayrollWorkbookCsv(rows: List<PayrollWorkbookRow>): String {
        if (rows.isEmpty()) return ""
        val outputDir = PatchLoader.safeExternalDir(context, feature = "payroll_workbook_csv")
        val outputFile = File(outputDir, "payroll_workbook_stage.csv")
        outputFile.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.appendLine("name,surname,workplace,email,amount")
            rows.forEach { row ->
                writer.appendCsvLine(
                    row.name,
                    row.surname,
                    row.workplace,
                    row.email,
                    row.amount,
                )
            }
        }
        return outputFile.absolutePath
    }

    override suspend fun exportClothesHistoryCsv(): String {
        val outputDir = PatchLoader.safeExternalDir(context, feature = "clothes_history_csv")
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
        val outputDir = PatchLoader.safeExternalDir(context, feature = "session_reports_csv")
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

    private suspend fun loadSavedSmtpSettings(): SmtpSettingsData {
        val settings = dao.observeSettings().first().associateBy({ it.key }, { it.valText })
        return SmtpSettingsData(
            host = settings["smtp_host"].orEmpty(),
            port = settings["smtp_port"].orEmpty().ifBlank { "587" },
            user = settings["smtp_user"].orEmpty(),
            password = settings["smtp_password"].orEmpty(),
        )
    }

    private suspend fun loadSavedEmailTemplate(): EmailTemplateData {
        val settings = dao.observeSettings().first().associateBy({ it.key }, { it.valText })
        return EmailTemplateData(
            subject = settings["t_sub"].orEmpty(),
            body = settings["t_body"].orEmpty(),
        )
    }

    private fun SmtpSettingsData.normalized(): SmtpSettingsData {
        val normalizedPort = port.trim().ifBlank { "587" }
        require(host.trim().isNotBlank()) { "Brak hosta SMTP" }
        require(user.trim().isNotBlank()) { "Brak loginu SMTP" }
        require(password.isNotBlank()) { "Brak hasła SMTP" }
        require(normalizedPort.toIntOrNull() != null) { "Port SMTP musi być liczbą" }
        return copy(host = host.trim(), port = normalizedPort, user = user.trim())
    }

    private fun EmailTemplateData.validated(): EmailTemplateData {
        require(subject.trim().isNotBlank()) { "Brak tematu email" }
        require(body.trim().isNotBlank()) { "Brak treści email" }
        return copy(subject = subject.trim(), body = body.trim())
    }

    private fun sanitizeAttachments(attachmentPaths: List<String>): List<File> =
        attachmentPaths
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .map(::File)
            .filter { it.exists() && it.isFile }

    private fun renderTemplate(template: String, name: String, date: LocalDate): String =
        template
            .replace("{Imię}", name.ifBlank { "Pracownik" })
            .replace("{Data}", date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))

    private fun buildMailSession(settings: SmtpSettingsData): Session {
        val props = Properties().apply {
            put("mail.smtp.host", settings.host)
            put("mail.smtp.port", settings.port)
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.connectiontimeout", "25000")
            put("mail.smtp.timeout", "25000")
            put("mail.smtp.writetimeout", "25000")
        }
        return Session.getInstance(
            props,
            object : javax.mail.Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication =
                    PasswordAuthentication(settings.user, settings.password)
            },
        )
    }

    private fun sendMail(
        settings: SmtpSettingsData,
        recipient: String,
        subject: String,
        body: String,
        attachments: List<File>,
    ) {
        val message = MimeMessage(buildMailSession(settings)).apply {
            setFrom(InternetAddress(settings.user))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient))
            setSubject(subject, Charsets.UTF_8.name())
            setContent(buildMultipartBody(body, attachments))
            sentDate = java.util.Date()
        }
        Transport.send(message)
    }

    private fun buildMultipartBody(body: String, attachments: List<File>): Multipart =
        MimeMultipart().apply {
            addBodyPart(
                MimeBodyPart().apply {
                    setText(body, Charsets.UTF_8.name())
                },
            )
            attachments.forEach { file ->
                addBodyPart(
                    MimeBodyPart().apply {
                        dataHandler = DataHandler(FileDataSource(file))
                        fileName = file.name
                    },
                )
            }
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
