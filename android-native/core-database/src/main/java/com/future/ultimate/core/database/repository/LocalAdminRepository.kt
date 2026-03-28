package com.future.ultimate.core.database.repository

import android.content.Context
import android.database.sqlite.SQLiteDatabase
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
import com.future.ultimate.core.common.repository.DatabaseWorkbookImportResult
import com.future.ultimate.core.common.repository.DashboardStats
import com.future.ultimate.core.common.repository.DriverAccountCredentials
import com.future.ultimate.core.common.repository.DriverAccountListItem
import com.future.ultimate.core.common.repository.DriverRemoteSettingsData
import com.future.ultimate.core.common.repository.EmailTemplateData
import com.future.ultimate.core.common.repository.MailApprovalRequest
import com.future.ultimate.core.common.repository.MailDispatchProgress
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
import java.io.ByteArrayInputStream
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.random.Random
import org.json.JSONObject
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFWorkbook

class LocalAdminRepository(
    private val dao: AppDao,
    private val context: Context,
) : AdminRepository {
    private companion object {
        const val CarDriverHistoryPrefix = "car_driver_history_"
    }
    private val remoteSyncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    override fun observeContacts(): Flow<List<ContactListItem>> = dao.observeContacts().map { items ->
        items.map {
            ContactListItem(
                name = it.name,
                surname = it.surname,
                email = it.email,
                pesel = it.pesel,
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
                Triple(items, accounts, settings)
            }
            .combine(dao.observeContacts()) { (items, accounts, settings), contacts ->
                Pair(Triple(items, accounts, settings), contacts)
            }
            .combine(dao.observeWorkers()) { (bundle, contacts), workers ->
                val (items, accounts, settings) = bundle
                Quintuple(items, accounts, settings.associateBy({ it.key }, { it.valText }), contacts, workers)
            }
            .map { (items, accounts, settings, contacts, workers) ->
                val accountsByRegistration = accounts.groupBy { it.registration.uppercase() }
                val plantByDriver = mutableMapOf<String, String>()
                contacts.forEach { contact ->
                    val fullName = listOf(contact.name.trim(), contact.surname.trim())
                        .filter { it.isNotBlank() }
                        .joinToString(" ")
                        .lowercase()
                    if (fullName.isNotBlank() && contact.workplace.isNotBlank()) {
                        plantByDriver[fullName] = contact.workplace.trim()
                    }
                }
                workers.forEach { worker ->
                    val fullName = listOf(worker.name.trim(), worker.surname.trim())
                        .filter { it.isNotBlank() }
                        .joinToString(" ")
                        .lowercase()
                    if (fullName.isNotBlank() && worker.plant.isNotBlank()) {
                        plantByDriver.putIfAbsent(fullName, worker.plant.trim())
                    }
                }
                items.map {
                    val registrationKey = it.registration.uppercase()
                    val accountsForCar = accountsByRegistration[registrationKey].orEmpty()
                    val assignedDrivers = parseDriverNames(it.driver)
                    val primaryAssignedDriver = assignedDrivers.firstOrNull().orEmpty()
                    val driverAccounts = accountsForCar.map { account ->
                        DriverAccountListItem(
                            driverName = account.driverName,
                            login = account.login,
                            password = account.password,
                            changePasswordRequired = account.changePassword == 1,
                            licenseType = account.licenseType,
                            licenseValidUntil = account.licenseValidUntil,
                        )
                    }
                    val driverAccount = accountsForCar.firstOrNull { account ->
                        account.driverName.equals(primaryAssignedDriver, ignoreCase = true)
                    } ?: accountsForCar.firstOrNull { account ->
                        assignedDrivers.any { assignedDriver -> account.driverName.equals(assignedDriver, ignoreCase = true) }
                    } ?: accountsForCar.firstOrNull()
                    val driverPlant = plantByDriver[primaryAssignedDriver.lowercase()].orEmpty()
                    val queuedMileage = settings["driver_mileage_sync_pending_$registrationKey"]
                        ?.substringBefore("|")
                        ?.toIntOrNull()
                    CarListItem(
                        id = it.id,
                        name = it.name,
                        registration = it.registration,
                        driver = it.driver,
                        driverPlant = driverPlant,
                        mileage = it.mileage,
                        serviceInterval = it.serviceInterval,
                        lastService = it.lastService,
                        lastInspectionDate = it.lastInspectionDate,
                        driverLogin = driverAccount?.login.orEmpty(),
                        driverPassword = driverAccount?.password.orEmpty(),
                        changePasswordRequired = driverAccount?.changePassword == 1,
                        licenseType = driverAccount?.licenseType.orEmpty(),
                        licenseValidUntil = driverAccount?.licenseValidUntil.orEmpty(),
                        pendingMileageSync = queuedMileage != null,
                        queuedMileage = queuedMileage,
                        lastMileageSyncAt = settings["driver_mileage_sync_at_$registrationKey"].orEmpty(),
                        lastMileageSyncStatus = settings["driver_mileage_sync_status_$registrationKey"].orEmpty(),
                        remoteDriverSyncAt = settings["driver_remote_sync_at_$registrationKey"].orEmpty(),
                        remoteDriverSyncStatus = settings["driver_remote_sync_status_$registrationKey"].orEmpty(),
                        remoteDriverSyncError = settings["driver_remote_sync_error_$registrationKey"].orEmpty(),
                        driverAccounts = driverAccounts,
                    )
                }
            }

    private data class Quintuple<A, B, C, D, E>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
        val fifth: E,
    )

    override fun observeKnownCarDrivers(): Flow<List<String>> =
        dao.observeCars().combine(dao.observeSettings()) { cars, settings ->
            val activeDrivers = cars.flatMap { parseDriverNames(it.driver) }
            val historicalDrivers = settings
                .asSequence()
                .filter { it.key.startsWith(CarDriverHistoryPrefix) }
                .map { it.valText.trim() }
                .filter { it.isNotBlank() }
                .toList()
            (activeDrivers + historicalDrivers)
                .distinctBy { it.lowercase() }
                .sortedBy { it.lowercase() }
        }

    override suspend fun saveCar(draft: CarDraft) {
        val serviceInterval = draft.serviceInterval.toIntOrNull()?.coerceAtLeast(1) ?: 15000
        val registration = draft.registration.trim().uppercase()
        val initialMileage = draft.initialMileage.toIntOrNull()?.coerceAtLeast(0) ?: 0
        val draftId = draft.id
        val existingCar = if (draftId != null) {
            dao.getCar(draftId)
        } else {
            dao.getCarByRegistration(registration)
        }
        val normalizedDrivers = parseDriverNames(draft.driver)
        dao.upsertCar(
            CarEntity(
                id = existingCar?.id ?: draftId ?: 0,
                name = draft.name.trim(),
                registration = registration,
                driver = normalizedDrivers.joinToString(", "),
                serviceInterval = serviceInterval,
                mileage = existingCar?.mileage ?: initialMileage,
                lastService = existingCar?.lastService ?: initialMileage,
                lastInspectionDate = draft.lastInspectionDate.trim(),
            ),
        )
        rememberKnownCarDrivers(normalizedDrivers)
        val previousDrivers = parseDriverNames(existingCar?.driver.orEmpty())
        syncDriverAccountsForRegistration(
            drivers = normalizedDrivers,
            registration = registration,
            previousDrivers = previousDrivers,
        )
    }

    override suspend fun updateCarMileage(id: Long, mileage: Int) = dao.updateMileage(id, mileage.coerceAtLeast(0))

    override suspend fun updateCarDriver(id: Long, driver: String) {
        val car = dao.getCar(id) ?: return
        val normalizedDrivers = parseDriverNames(driver)
        dao.updateDriver(id, normalizedDrivers.joinToString(", "))
        rememberKnownCarDrivers(normalizedDrivers)
        val previousDrivers = parseDriverNames(car.driver)
        syncDriverAccountsForRegistration(
            drivers = normalizedDrivers,
            registration = car.registration,
            previousDrivers = previousDrivers,
        )
    }

    override suspend fun updateCarDriverLicense(id: Long, driverName: String, licenseType: String, validUntil: String) {
        val car = dao.getCar(id) ?: return
        val normalizedDriver = driverName.trim().ifBlank { parseDriverNames(car.driver).firstOrNull().orEmpty() }
        if (normalizedDriver.isBlank()) return
        val normalizedLicenseType = licenseType.trim().ifBlank { "PL" }
        val normalizedValidUntil = validUntil.trim()
        val primaryAccount = dao.getDriverAccountByRegistrationAndDriverName(car.registration, normalizedDriver)
            ?: syncDriverAccount(normalizedDriver, car.registration)
            ?: return
        val registrationsToUpdate = dao.getDriverAccountsByLogin(primaryAccount.login)
            .map { it.registration.trim().uppercase() }
            .filter { it.isNotBlank() }
            .distinct()
            .ifEmpty { listOf(primaryAccount.registration.trim().uppercase()) }
        registrationsToUpdate.forEach { registration ->
            dao.updateDriverLicense(
                registration = registration,
                licenseType = normalizedLicenseType,
                validUntil = normalizedValidUntil,
            )
        }
    }

    override suspend fun resetCarDriverCredentials(id: Long, driverName: String): DriverAccountCredentials {
        val car = dao.getCar(id) ?: return DriverAccountCredentials()
        val normalizedDriver = driverName.trim().ifBlank { parseDriverNames(car.driver).firstOrNull().orEmpty() }
        if (normalizedDriver.isBlank()) {
            dao.deleteDriverAccountByRegistration(car.registration)
            syncRemoteDriverDeletion(car.registration)
            return DriverAccountCredentials()
        }
        val account = syncDriverAccount(normalizedDriver, car.registration, forceReset = true) ?: return DriverAccountCredentials()
        dao.observeCars().first().filter { candidate ->
            parseDriverNames(candidate.driver).any { it.equals(normalizedDriver, ignoreCase = true) }
        }
            .asSequence()
            .map { it.registration.trim().uppercase() }
            .filter { it.isNotBlank() && !it.equals(account.registration, ignoreCase = true) }
            .distinct()
            .forEach { registration ->
                syncDriverAccount(normalizedDriver, registration, forceReset = false, forceRemote = true)
            }
        return DriverAccountCredentials(
            login = account.login,
            password = account.password,
        )
    }

    override suspend fun retryCarDriverRemoteSync(id: Long) {
        val car = dao.getCar(id) ?: return
        syncDriverAccount(car.driver, car.registration, forceReset = false, forceRemote = true)
    }

    override suspend fun deleteKnownCarDriver(driver: String) {
        val normalizedDriver = driver.trim()
        if (normalizedDriver.isBlank()) return
        val matchingCars = dao.observeCars().first().filter { car ->
            parseDriverNames(car.driver).any { it.equals(normalizedDriver, ignoreCase = true) }
        }
        matchingCars.forEach { car ->
            val currentDrivers = parseDriverNames(car.driver)
            val remainingDrivers = parseDriverNames(car.driver)
                .filterNot { it.equals(normalizedDriver, ignoreCase = true) }
            dao.updateDriver(car.id, remainingDrivers.joinToString(", "))
            syncDriverAccountsForRegistration(
                drivers = remainingDrivers,
                registration = car.registration,
                previousDrivers = currentDrivers,
            )
        }
        val key = CarDriverHistoryPrefix + normalizedDriver.lowercase().replace(" ", "_")
        dao.deleteSetting(key)
    }

    override suspend fun confirmCarService(id: Long) = dao.confirmService(id)

    override suspend fun confirmCarInspection(id: Long) {
        dao.updateLastInspectionDate(id, LocalDate.now().toString())
    }

    override suspend fun deleteCar(id: Long) {
        val car = dao.getCar(id)
        dao.deleteCar(id)
        car?.registration?.let { registration ->
            dao.deleteDriverAccountByRegistration(registration)
            syncRemoteDriverDeletion(registration)
        }
    }

    private suspend fun rememberKnownCarDrivers(drivers: List<String>) {
        drivers.forEach { driver ->
            val normalizedDriver = driver.trim()
            if (normalizedDriver.isBlank()) return@forEach
            val key = CarDriverHistoryPrefix + normalizedDriver.lowercase().replace(" ", "_")
            dao.upsertSetting(SettingEntity(key = key, valText = normalizedDriver))
        }
    }

    private suspend fun syncDriverAccountsForRegistration(
        drivers: List<String>,
        registration: String,
        previousDrivers: List<String> = emptyList(),
    ) {
        val normalizedRegistration = registration.trim().uppercase()
        if (normalizedRegistration.isBlank()) return
        val normalizedDrivers = drivers
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
        if (normalizedDrivers.isEmpty()) {
            dao.deleteDriverAccountByRegistration(normalizedRegistration)
            syncRemoteDriverDeletion(normalizedRegistration)
            return
        }

        val existingAccounts = dao.getDriverAccountsByRegistration(normalizedRegistration)
        val previousDriverSet = previousDrivers
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
        val removedDrivers = existingAccounts.filter { account ->
            normalizedDrivers.none { it.equals(account.driverName, ignoreCase = true) }
        }
        removedDrivers.forEach { removedAccount ->
            dao.deleteDriverAccountByRegistrationAndDriverName(normalizedRegistration, removedAccount.driverName)
        }

        val addedDrivers = normalizedDrivers.filter { newDriver ->
            previousDriverSet.none { it.equals(newDriver, ignoreCase = true) }
        }
        val unchangedDrivers = normalizedDrivers.filter { newDriver ->
            previousDriverSet.any { it.equals(newDriver, ignoreCase = true) }
        }

        addedDrivers.forEach { driver ->
            syncDriverAccount(driver, normalizedRegistration)
        }
        unchangedDrivers.forEach { driver ->
            val accountExists = existingAccounts.any { account ->
                account.driverName.equals(driver, ignoreCase = true)
            }
            if (!accountExists) {
                syncDriverAccount(driver, normalizedRegistration)
            }
        }
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

    override suspend fun createClothesOrderFromSelections(
        draft: ClothesOrderDraft,
        workerPartQuantities: Map<Long, Map<String, Int>>,
    ): Long? {
        val selectedWorkers = dao.observeWorkers().first().filter { it.id in workerPartQuantities.keys }
        if (selectedWorkers.isEmpty()) return null

        val sizeByWorker = dao.observeClothesSizes().first().associateBy {
            "${it.name.trim().lowercase()}|${it.surname.trim().lowercase()}"
        }
        val inferredPlant = selectedWorkers.map { it.plant.trim() }.filter { it.isNotBlank() }.distinct().singleOrNull().orEmpty()
        val date = draft.date.ifBlank { LocalDate.now().toString() }
        val plant = draft.plant.trim().ifBlank { inferredPlant }
        val description = draft.orderDesc.trim().ifBlank { "$plant • $date" }

        val orderId = dao.upsertClothesOrder(
            ClothesOrderEntity(
                id = 0,
                date = date,
                plant = plant,
                status = draft.status.trim().ifBlank { "Zamówione" },
                orderDesc = description,
            ),
        )

        val items = buildList {
            selectedWorkers.forEach { worker ->
                val quantities = workerPartQuantities[worker.id].orEmpty()
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
                addItem("Koszulka", size?.shirt.orEmpty(), quantities["Koszulka"] ?: 0)
                addItem("Bluza", size?.hoodie.orEmpty(), quantities["Bluza"] ?: 0)
                addItem("Spodnie", size?.pants.orEmpty(), quantities["Spodnie"] ?: 0)
                addItem("Kurtka", size?.jacket.orEmpty(), quantities["Kurtka"] ?: 0)
                addItem("Buty", size?.shoes.orEmpty(), quantities["Buty"] ?: 0)
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
            security = map["smtp_security"].orEmpty().ifBlank { "STARTTLS" },
            senderName = map["smtp_sender_name"].orEmpty(),
            throttleMs = map["smtp_throttle_ms"].orEmpty().ifBlank { "0" },
        )
    }

    override suspend fun saveSmtpSettings(settings: SmtpSettingsData) {
        dao.upsertSetting(SettingEntity(key = "smtp_host", valText = settings.host.trim()))
        dao.upsertSetting(SettingEntity(key = "smtp_port", valText = settings.port.trim()))
        dao.upsertSetting(SettingEntity(key = "smtp_user", valText = settings.user.trim()))
        dao.upsertSetting(SettingEntity(key = "smtp_password", valText = settings.password))
        dao.upsertSetting(SettingEntity(key = "smtp_security", valText = settings.security.trim().uppercase()))
        dao.upsertSetting(SettingEntity(key = "smtp_sender_name", valText = settings.senderName.trim()))
        dao.upsertSetting(SettingEntity(key = "smtp_throttle_ms", valText = settings.throttleMs.trim()))
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

    override fun observeDriverRemoteSettings(): Flow<DriverRemoteSettingsData> = dao.observeSettings().map { settings ->
        val map = settings.associateBy({ it.key }, { it.valText })
        DriverRemoteSettingsData(
            apiUrl = map[DriverRemoteSyncGateway.EndpointSettingKey]
                .orEmpty()
                .ifBlank { DriverRemoteSyncGateway.DefaultDriverRemoteApiUrl },
        )
    }

    override suspend fun saveDriverRemoteSettings(settings: DriverRemoteSettingsData) {
        DriverRemoteSyncGateway.saveEndpoint(dao, settings.apiUrl)
    }

    override suspend fun validateDriverRemoteSettings(settings: DriverRemoteSettingsData): String =
        DriverRemoteSyncGateway.validateEndpoint(dao, settings.apiUrl)

    override suspend fun importDriverRemoteLogs(settings: DriverRemoteSettingsData): Int {
        val logs = DriverRemoteSyncGateway.fetchRemoteLogs(dao, settings.apiUrl)
        var imported = 0
        logs.forEach { log ->
            val existingCar = dao.getCarByRegistration(log.registration)
            val existingAccount = dao.getDriverAccountByRegistrationAndLogin(log.registration, log.login)
                ?: dao.getDriverAccountByRegistrationAndDriverName(log.registration, log.driverName)
            dao.upsertCar(
                CarEntity(
                    id = existingCar?.id ?: 0,
                    name = existingCar?.name?.takeIf { it.isNotBlank() }
                        ?: log.driverName.ifBlank { "Auto ${log.registration}" },
                    registration = log.registration,
                    driver = existingCar?.driver?.takeIf { it.isNotBlank() }
                        ?: log.driverName,
                    mileage = maxOf(existingCar?.mileage ?: 0, log.mileage),
                    serviceInterval = existingCar?.serviceInterval ?: 15000,
                    lastService = existingCar?.lastService ?: 0,
                    lastInspectionDate = existingCar?.lastInspectionDate.orEmpty(),
                ),
            )
            if (existingAccount == null && (log.driverName.isNotBlank() || log.login.isNotBlank())) {
                dao.upsertDriverAccount(
                    DriverAccountEntity(
                        registration = log.registration,
                        login = log.login.ifBlank { log.driverName.lowercase().replace(" ", ".") },
                        password = "",
                        driverName = log.driverName.ifBlank { existingCar?.driver.orEmpty() },
                        changePassword = 0,
                        licenseType = existingAccount?.licenseType ?: "PL",
                        licenseValidUntil = existingAccount?.licenseValidUntil.orEmpty(),
                    ),
                )
            }
            dao.upsertSetting(
                SettingEntity(
                    key = "driver_mileage_sync_at_${log.registration}",
                    valText = log.timestamp.ifBlank { LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) },
                ),
            )
            dao.upsertSetting(SettingEntity(key = "driver_mileage_sync_status_${log.registration}", valText = "Zaimportowano z endpointu"))
            dao.upsertSetting(SettingEntity(key = "driver_mileage_sync_error_${log.registration}", valText = ""))
            imported += 1
        }
        return imported
    }

    override suspend fun clearAllTestData(settings: DriverRemoteSettingsData): String {
        val remoteResult = runCatching {
            DriverRemoteSyncGateway.clearRemoteTestData(dao, settings.apiUrl)
        }
        dao.clearClothesOrderItems()
        dao.clearClothesOrders()
        dao.clearClothesHistory()
        dao.clearClothesSizes()
        dao.clearWorkers()
        dao.clearDriverAccounts()
        dao.clearCars()
        dao.clearPlants()
        dao.clearContacts()
        dao.clearReports()
        dao.clearSettings()
        dao.resetAutoincrement()
        DriverRemoteSyncGateway.saveEndpoint(dao, DriverRemoteSyncGateway.DefaultDriverRemoteApiUrl)
        return remoteResult.getOrElse { error ->
            "Lokalna baza została wyczyszczona, ale endpoint zwrócił błąd: ${error.message ?: "nieznany błąd"}"
        }.ifBlank {
            "Wyczyszczono lokalną bazę i zdalny endpoint testowy"
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
                attachments = resolveAttachments(attachmentPaths),
            )
            settings.user
        }
    }

    override suspend fun sendMassMailing(
        attachmentPaths: List<String>,
        autoMode: Boolean,
        onProgress: suspend (MailDispatchProgress) -> Unit,
        awaitResume: suspend () -> Unit,
        awaitApproval: suspend (MailApprovalRequest) -> Boolean,
    ): MailDispatchResult =
        withContext(Dispatchers.IO) {
            dispatchMailBatch(
                recipients = dao.observeContacts().first().map {
                    ContactListItem(
                        name = it.name,
                        surname = it.surname,
                        email = it.email,
                        pesel = it.pesel,
                        phone = it.phone,
                        workplace = it.workplace,
                        apartment = it.apartment,
                        notes = it.notes,
                    )
                },
                attachmentPaths = attachmentPaths,
                autoMode = autoMode,
                subjectOverride = null,
                bodyOverride = null,
                onProgress = onProgress,
                awaitResume = awaitResume,
                requireApproval = !autoMode,
                awaitApproval = awaitApproval,
            )
        }

    override suspend fun sendSpecialMailing(
        recipients: List<ContactListItem>,
        attachmentPaths: List<String>,
        subject: String,
        body: String,
        onProgress: suspend (MailDispatchProgress) -> Unit,
        awaitResume: suspend () -> Unit,
    ): MailDispatchResult =
        withContext(Dispatchers.IO) {
            dispatchMailBatch(
                recipients = recipients,
                attachmentPaths = attachmentPaths,
                autoMode = false,
                subjectOverride = subject,
                bodyOverride = body,
                onProgress = onProgress,
                awaitResume = awaitResume,
                requireApproval = false,
                awaitApproval = { true },
            )
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
        val exportedAt = LocalDateTime.now()
        checkpointDatabase()
        val outputFile = File(outputDir, "future_v20_snapshot_${exportedAt.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))}.zip")
        val databaseFiles = listOf(
            context.getDatabasePath("future_v20.db"),
            context.getDatabasePath("future_v20.db-wal"),
            context.getDatabasePath("future_v20.db-shm"),
        ).filter { it.exists() }

        ZipOutputStream(outputFile.outputStream().buffered()).use { zip ->
            val manifest = JSONObject().apply {
                put("exportedAt", exportedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                put("databaseName", "future_v20.db")
                put(
                    "entries",
                    databaseFiles.fold(org.json.JSONArray()) { array, file ->
                        array.put(
                            JSONObject().apply {
                                put("name", file.name)
                                put("sizeBytes", file.length())
                                put("lastModified", file.lastModified())
                            },
                        )
                    },
                )
            }
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(manifest.toString(2).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            databaseFiles.forEach { file ->
                zip.putNextEntry(ZipEntry(file.name))
                file.inputStream().use { input -> input.copyTo(zip) }
                zip.closeEntry()
            }
        }
        return outputFile.absolutePath
    }

    override suspend fun importDatabaseWorkbook(
        fileName: String?,
        mimeType: String?,
        bytes: ByteArray,
    ): DatabaseWorkbookImportResult {
        if (bytes.isEmpty()) return DatabaseWorkbookImportResult()
        val workbookData = parseImportWorkbook(fileName = fileName, mimeType = mimeType, bytes = bytes)
        var importedContacts = 0
        var importedWorkers = 0
        var importedPlants = 0
        var importedClothesSizes = 0

        workbookData.plants
            .distinctBy { listOf(it.name, it.city, it.address).joinToString("|") { part -> part.trim().lowercase() } }
            .filter { it.name.isNotBlank() }
            .forEach { draft ->
                savePlant(draft)
                importedPlants += 1
            }

        workbookData.contacts
            .distinctBy { "${it.name.trim().lowercase()}|${it.surname.trim().lowercase()}" }
            .filter { it.name.isNotBlank() || it.surname.isNotBlank() }
            .forEach { draft ->
                saveContact(draft)
                importedContacts += 1
            }

        workbookData.workers
            .distinctBy { "${it.name.trim().lowercase()}|${it.surname.trim().lowercase()}" }
            .filter { it.name.isNotBlank() || it.surname.isNotBlank() }
            .forEach { draft ->
                saveWorker(draft)
                importedWorkers += 1
            }

        workbookData.clothesSizes
            .distinctBy { "${it.name.trim().lowercase()}|${it.surname.trim().lowercase()}" }
            .filter { it.name.isNotBlank() || it.surname.isNotBlank() }
            .forEach { draft ->
                saveClothesSize(draft)
                importedClothesSizes += 1
            }

        return DatabaseWorkbookImportResult(
            contactsImported = importedContacts,
            workersImported = importedWorkers,
            plantsImported = importedPlants,
            clothesSizesImported = importedClothesSizes,
        )
    }

    private fun checkpointDatabase() {
        val databaseFile = context.getDatabasePath("future_v20.db")
        if (!databaseFile.exists()) return
        runCatching {
            SQLiteDatabase.openDatabase(databaseFile.absolutePath, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
                db.execSQL("PRAGMA wal_checkpoint(FULL)")
            }
        }
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

    private data class ParsedDatabaseWorkbook(
        val contacts: List<ContactDraft> = emptyList(),
        val workers: List<WorkerDraft> = emptyList(),
        val plants: List<PlantDraft> = emptyList(),
        val clothesSizes: List<ClothesSizeDraft> = emptyList(),
    )

    private fun parseImportWorkbook(
        fileName: String?,
        mimeType: String?,
        bytes: ByteArray,
    ): ParsedDatabaseWorkbook {
        val isXlsx = mimeType.orEmpty().contains("spreadsheetml", ignoreCase = true) ||
            fileName.orEmpty().endsWith(".xlsx", ignoreCase = true)
        require(isXlsx) { "Obsługiwany jest tylko plik Excel .xlsx" }

        val contacts = mutableListOf<ContactDraft>()
        val workers = mutableListOf<WorkerDraft>()
        val plants = mutableListOf<PlantDraft>()
        val clothesSizes = mutableListOf<ClothesSizeDraft>()

        ByteArrayInputStream(bytes).use { input ->
            WorkbookFactory.create(input).use { workbook ->
                val formatter = DataFormatter()
                for (sheetIndex in 0 until workbook.numberOfSheets) {
                    val sheet = workbook.getSheetAt(sheetIndex) ?: continue
                    val rows = readSheetRows(sheetIndex = sheetIndex, workbook = workbook, formatter = formatter)
                    if (rows.isEmpty()) continue
                    val headers = rows.first()
                    val normalizedHeaders = headers.map(::normalizeImportKey)
                    val dataRows = rows.drop(1)
                    val sheetName = normalizeImportKey(sheet.sheetName)
                    val sheetHasPlantFields = normalizedHeaders.any { it in setOf("miasto", "adres", "telefonkontaktowy", "telefonzakladu") }
                    val sheetHasSizeFields = normalizedHeaders.any { it in setOf("koszulka", "bluza", "spodnie", "kurtka", "buty") }
                    val sheetHasWorkerFields = normalizedHeaders.any { it in setOf("stanowisko", "datazatrudnienia") }
                    val sheetHasContactFields = normalizedHeaders.any { it in setOf("email", "telefon", "mieszkanie", "notatki") }

                    dataRows.forEach { row ->
                        if (row.all { it.isBlank() }) return@forEach
                        when {
                            sheetName.contains("zaklad") || sheetHasPlantFields -> createPlantDraft(headers, row)?.let(plants::add)
                        }
                        when {
                            sheetName.contains("rozmiar") || sheetName.contains("ubran") || sheetHasSizeFields ->
                                createClothesSizeDraft(headers, row)?.let(clothesSizes::add)
                        }
                        when {
                            sheetName.contains("pracown") || sheetHasWorkerFields -> createWorkerDraft(headers, row)?.let(workers::add)
                        }
                        when {
                            sheetName.contains("kontakt") || sheetHasContactFields || (!sheetHasPlantFields && !sheetHasSizeFields) ->
                                createContactDraft(headers, row)?.let(contacts::add)
                        }

                        if (!sheetName.contains("zaklad")) {
                            createPlantDraft(headers, row)?.let(plants::add)
                        }
                        if (!sheetName.contains("rozmiar")) {
                            createClothesSizeDraft(headers, row)?.let(clothesSizes::add)
                        }
                        if (!sheetName.contains("pracown")) {
                            createWorkerDraft(headers, row)?.let(workers::add)
                        }
                        if (!sheetName.contains("kontakt")) {
                            createContactDraft(headers, row)?.let(contacts::add)
                        }
                    }
                }
            }
        }

        return ParsedDatabaseWorkbook(
            contacts = contacts,
            workers = workers,
            plants = plants,
            clothesSizes = clothesSizes,
        )
    }

    private fun readSheetRows(
        sheetIndex: Int,
        workbook: org.apache.poi.ss.usermodel.Workbook,
        formatter: DataFormatter,
    ): List<List<String>> {
        val sheet = workbook.getSheetAt(sheetIndex) ?: return emptyList()
        val maxColumns = (sheet.firstRowNum..sheet.lastRowNum)
            .mapNotNull { rowIndex -> sheet.getRow(rowIndex)?.lastCellNum?.toInt()?.takeIf { it > 0 } }
            .maxOrNull() ?: 0
        if (maxColumns == 0) return emptyList()
        return buildList {
            for (rowIndex in sheet.firstRowNum..sheet.lastRowNum) {
                val row = sheet.getRow(rowIndex)
                val values = (0 until maxColumns).map { columnIndex ->
                    formatter.formatCellValue(row?.getCell(columnIndex)).trim()
                }
                if (values.any { it.isNotBlank() }) {
                    add(values)
                }
            }
        }
    }

    private fun createContactDraft(headers: List<String>, row: List<String>): ContactDraft? {
        val name = row.valueFor(headers, "imie", "name")
        val surname = row.valueFor(headers, "nazwisko", "surname")
        val workplace = row.valueFor(headers, "zaklad", "plant", "workplace", "miejscepracy")
        val email = row.valueFor(headers, "email", "mail")
        val phone = row.valueFor(headers, "telefon", "phone", "nrtelefonu")
        val apartment = row.valueFor(headers, "mieszkanie", "apartment", "lokal", "pokoj")
        val pesel = row.valueFor(headers, "pesel")
        val notes = row.valueFor(headers, "notatki", "uwagi", "notes")
        if (name.isBlank() && surname.isBlank() && email.isBlank() && phone.isBlank() && workplace.isBlank()) return null
        return ContactDraft(
            name = name,
            surname = surname,
            email = email,
            pesel = pesel,
            phone = phone,
            workplace = workplace,
            apartment = apartment,
            notes = notes,
        )
    }

    private fun createWorkerDraft(headers: List<String>, row: List<String>): WorkerDraft? {
        val name = row.valueFor(headers, "imie", "name")
        val surname = row.valueFor(headers, "nazwisko", "surname")
        val plant = row.valueFor(headers, "zaklad", "plant", "workplace", "miejscepracy")
        val phone = row.valueFor(headers, "telefon", "phone", "nrtelefonu")
        val position = row.valueFor(headers, "stanowisko", "position")
        val hireDate = row.valueFor(headers, "datazatrudnienia", "hiredate")
        if (name.isBlank() && surname.isBlank() && plant.isBlank() && phone.isBlank()) return null
        return WorkerDraft(
            name = name,
            surname = surname,
            plant = plant,
            phone = phone,
            position = position,
            hireDate = hireDate,
        )
    }

    private fun createPlantDraft(headers: List<String>, row: List<String>): PlantDraft? {
        val name = row.valueFor(headers, "nazwazakladu", "zaklad", "plant", "nazwa")
        val city = row.valueFor(headers, "miasto", "city")
        val address = row.valueFor(headers, "adres", "address")
        val contactPhone = row.valueFor(headers, "telefonkontaktowy", "telefonzakladu", "telefon", "phone", "contactphone")
        val notes = row.valueFor(headers, "notatki", "uwagi", "notes")
        if (name.isBlank()) return null
        return PlantDraft(
            name = name,
            city = city,
            address = address,
            contactPhone = contactPhone,
            notes = notes,
        )
    }

    private fun createClothesSizeDraft(headers: List<String>, row: List<String>): ClothesSizeDraft? {
        val name = row.valueFor(headers, "imie", "name")
        val surname = row.valueFor(headers, "nazwisko", "surname")
        val plant = row.valueFor(headers, "zaklad", "plant", "workplace", "miejscepracy")
        val shirt = row.valueFor(headers, "koszulka", "shirt", "tshirt")
        val hoodie = row.valueFor(headers, "bluza", "hoodie")
        val pants = row.valueFor(headers, "spodnie", "pants")
        val jacket = row.valueFor(headers, "kurtka", "jacket")
        val shoes = row.valueFor(headers, "buty", "shoes")
        if (name.isBlank() && surname.isBlank() && shirt.isBlank() && hoodie.isBlank() && pants.isBlank() && jacket.isBlank() && shoes.isBlank()) return null
        return ClothesSizeDraft(
            name = name,
            surname = surname,
            plant = plant,
            shirt = shirt,
            hoodie = hoodie,
            pants = pants,
            jacket = jacket,
            shoes = shoes,
        )
    }

    private fun List<String>.valueFor(headers: List<String>, vararg candidates: String): String {
        val headerIndex = headers.indexOfFirst { normalizeImportKey(it) in candidates.toSet() }
        return if (headerIndex in indices) this[headerIndex].trim() else ""
    }

    private fun normalizeImportKey(value: String): String =
        value.trim()
            .lowercase()
            .replace('ą', 'a')
            .replace('ć', 'c')
            .replace('ę', 'e')
            .replace('ł', 'l')
            .replace('ń', 'n')
            .replace('ó', 'o')
            .replace('ś', 's')
            .replace('ż', 'z')
            .replace('ź', 'z')
            .replace(Regex("[^a-z0-9]+"), "")

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

    override suspend fun exportPayrollRowsXlsx(
        headers: List<String>,
        rows: List<List<String>>,
        filePrefix: String,
        nameHint: String,
        surnameHint: String,
    ): String {
        if (rows.isEmpty()) return ""
        val outputDir = PatchLoader.safeExternalDir(context, feature = "payroll_row_xlsx")
        val safeName = nameHint.trim().ifBlank { "rekord" }.replace("\\s+".toRegex(), "_")
        val safeSurname = surnameHint.trim().ifBlank { "rekord" }.replace("\\s+".toRegex(), "_")
        val stamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now())
        val outputFile = File(outputDir, "${filePrefix}_${safeName}_${safeSurname}_$stamp.xlsx")
        val maxColumns = maxOf(headers.size, rows.maxOfOrNull { it.size } ?: 0)
        val normalizedHeaders = (0 until maxColumns).map { index -> headers.getOrElse(index) { "kolumna_${index + 1}" } }
        val dataRows = rows.map { row ->
            (0 until maxColumns).map { index ->
                SimpleXlsxWorkbookWriter.Cell.text(row.getOrElse(index) { "" })
            }
        }
        val allRows = buildList {
            add(normalizedHeaders.map { SimpleXlsxWorkbookWriter.Cell.text(it) })
            addAll(dataRows)
        }
        SimpleXlsxWorkbookWriter.writeSingleSheet(
            file = outputFile,
            sheetName = "Paski",
            rows = allRows,
        )
        return outputFile.absolutePath
    }

    override suspend fun exportPayrollCashReportXlsx(
        headers: List<String>,
        rows: List<List<String>>,
        totalAmount: String,
    ): String {
        if (rows.isEmpty()) return ""
        val outputDir = PatchLoader.safeExternalDir(context, feature = "payroll_cash_report")
        val stamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now())
        val outputFile = File(outputDir, "raport_gotowki_$stamp.xlsx")

        XSSFWorkbook().use { workbook ->
            val sheet = workbook.createSheet("Raport gotówki")
            val headerStyle = workbook.createCellStyle().apply {
                fillForegroundColor = IndexedColors.LIGHT_CORNFLOWER_BLUE.index
                fillPattern = FillPatternType.SOLID_FOREGROUND
                alignment = HorizontalAlignment.CENTER
                verticalAlignment = VerticalAlignment.CENTER
                setFont(workbook.createFont().apply { bold = true })
                borderBottom = org.apache.poi.ss.usermodel.BorderStyle.THIN
                borderTop = org.apache.poi.ss.usermodel.BorderStyle.THIN
                borderLeft = org.apache.poi.ss.usermodel.BorderStyle.THIN
                borderRight = org.apache.poi.ss.usermodel.BorderStyle.THIN
            }
            val bodyStyle = workbook.createCellStyle().apply {
                alignment = HorizontalAlignment.CENTER
                verticalAlignment = VerticalAlignment.CENTER
                borderBottom = org.apache.poi.ss.usermodel.BorderStyle.THIN
                borderTop = org.apache.poi.ss.usermodel.BorderStyle.THIN
                borderLeft = org.apache.poi.ss.usermodel.BorderStyle.THIN
                borderRight = org.apache.poi.ss.usermodel.BorderStyle.THIN
            }

            val reportHeaders = listOf("LP") + headers + listOf("DATA", "PODPIS")
            val nameColumnIndex = reportHeaders.indexOfFirst { it.contains("imi", ignoreCase = true) }.takeIf { it >= 0 } ?: 1
            val surnameColumnIndex = reportHeaders.indexOfFirst { it.contains("nazw", ignoreCase = true) }.takeIf { it >= 0 } ?: 2
            val amountColumnIndex = reportHeaders.indexOfFirst {
                it.contains("suma", ignoreCase = true) ||
                    it.contains("netto", ignoreCase = true) ||
                    it.contains("różnica", ignoreCase = true) ||
                    it.contains("/mtx", ignoreCase = true)
            }.takeIf { it >= 0 } ?: 3
            val dateColumnIndex = reportHeaders.lastIndex - 1
            val signatureColumnIndex = reportHeaders.lastIndex
            var rowIndex = 0
            val headerRow = sheet.createRow(rowIndex++)
            reportHeaders.forEachIndexed { columnIndex, title ->
                headerRow.createCell(columnIndex).apply {
                    setCellValue(title)
                    cellStyle = headerStyle
                }
            }

            rows.forEachIndexed { index, row ->
                val sheetRow = sheet.createRow(rowIndex++)
                val values = listOf((index + 1).toString()) + row.map(::roundNumericString) + listOf("", "")
                values.forEachIndexed { columnIndex, value ->
                    sheetRow.createCell(columnIndex).apply {
                        setCellValue(value)
                        cellStyle = bodyStyle
                    }
                }
            }

            val statementRow = sheet.createRow(rowIndex++)
            statementRow.createCell(0).apply {
                setCellValue("ja niżej podpisany odebrałem całość gotówki")
                cellStyle = headerStyle
            }
            sheet.addMergedRegion(CellRangeAddress(statementRow.rowNum, statementRow.rowNum, 0, reportHeaders.lastIndex))
            for (columnIndex in 1..reportHeaders.lastIndex) {
                statementRow.createCell(columnIndex).cellStyle = headerStyle
            }

            val signatureHeaderRow = sheet.createRow(rowIndex++)
            reportHeaders.indices.forEach { columnIndex ->
                signatureHeaderRow.createCell(columnIndex).cellStyle = bodyStyle
            }
            listOf(
                nameColumnIndex to "IMIĘ",
                surnameColumnIndex to "NAZWISKO",
                amountColumnIndex to "SUMA",
                dateColumnIndex to "DATA",
                signatureColumnIndex to "PODPIS",
            ).forEach { (columnIndex, title) ->
                signatureHeaderRow.getCell(columnIndex).apply {
                    setCellValue(title)
                    cellStyle = headerStyle
                }
            }

            val signatureValueRow = sheet.createRow(rowIndex)
            reportHeaders.indices.forEach { columnIndex ->
                signatureValueRow.createCell(columnIndex).apply {
                    setCellValue("")
                    cellStyle = bodyStyle
                }
            }
            signatureValueRow.getCell(amountColumnIndex).setCellValue(roundNumericString(totalAmount))

            val widthRows = buildList {
                add(reportHeaders)
                addAll(rows.mapIndexed { index, row -> listOf((index + 1).toString()) + row.map(::roundNumericString) + listOf("", "") })
                add(List(reportHeaders.size) { columnIndex ->
                    when (columnIndex) {
                        nameColumnIndex -> "IMIĘ"
                        surnameColumnIndex -> "NAZWISKO"
                        amountColumnIndex -> "SUMA"
                        dateColumnIndex -> "DATA"
                        signatureColumnIndex -> "PODPIS"
                        else -> ""
                    }
                })
                add(List(reportHeaders.size) { columnIndex -> if (columnIndex == amountColumnIndex) roundNumericString(totalAmount) else "" })
            }
            applyColumnWidths(sheet = sheet, rows = widthRows, totalColumns = reportHeaders.size.coerceAtLeast(5))
            outputFile.outputStream().use { workbook.write(it) }
        }

        return outputFile.absolutePath
    }

    private fun applyColumnWidths(
        sheet: org.apache.poi.ss.usermodel.Sheet,
        rows: List<List<String>>,
        totalColumns: Int,
    ) {
        (0 until totalColumns).forEach { columnIndex ->
            val maxLength = rows.maxOfOrNull { row ->
                row.getOrNull(columnIndex)
                    ?.replace('\n', ' ')
                    ?.length
                    ?: 0
            } ?: 0
            val width = ((maxLength + 4).coerceIn(10, 40)) * 256
            sheet.setColumnWidth(columnIndex, width)
        }
    }

    private fun roundNumericString(value: String): String {
        val normalized = value.trim().replace(" ", "").replace(",", ".")
        val parsed = normalized.toDoubleOrNull() ?: return value
        return kotlin.math.round(parsed).toInt().toString()
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
            security = settings["smtp_security"].orEmpty().ifBlank { "STARTTLS" },
            senderName = settings["smtp_sender_name"].orEmpty(),
            throttleMs = settings["smtp_throttle_ms"].orEmpty().ifBlank { "0" },
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
        val normalizedSecurity = security.trim().uppercase().ifBlank { "STARTTLS" }
        val normalizedThrottle = throttleMs.trim().ifBlank { "0" }
        require(host.trim().isNotBlank()) { "Brak hosta SMTP" }
        require(user.trim().isNotBlank()) { "Brak loginu SMTP" }
        require(password.isNotBlank()) { "Brak hasła SMTP" }
        require(normalizedPort.toIntOrNull() != null) { "Port SMTP musi być liczbą" }
        require(normalizedSecurity in setOf("STARTTLS", "SSL/TLS", "PLAINTEXT")) { "Nieprawidłowy tryb bezpieczeństwa SMTP" }
        require((normalizedThrottle.toLongOrNull() ?: -1L) >= 0L) { "Opóźnienie między emailami musi być liczbą >= 0" }
        return copy(
            host = host.trim(),
            port = normalizedPort,
            user = user.trim(),
            security = normalizedSecurity,
            senderName = senderName.trim(),
            throttleMs = normalizedThrottle,
        )
    }

    private fun EmailTemplateData.validated(): EmailTemplateData {
        require(subject.trim().isNotBlank()) { "Brak tematu email" }
        require(body.trim().isNotBlank()) { "Brak treści email" }
        return copy(subject = subject.trim(), body = body.trim())
    }

    private fun resolveAttachments(attachmentPaths: List<String>): List<File> {
        val resolved = attachmentPaths
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        val missing = resolved.map(::File).filterNot { it.exists() && it.isFile }
        require(missing.isEmpty()) {
            "Brakujące załączniki: ${missing.joinToString { it.name }}"
        }
        return resolved.map(::File)
    }

    private fun renderTemplate(template: String, name: String, date: LocalDate): String =
        template
            .replace("{Imię}", name.ifBlank { "Pracownik" })
            .replace("{Data}", date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))

    private fun buildMailSession(settings: SmtpSettingsData): Session {
        val props = Properties().apply {
            put("mail.smtp.host", settings.host)
            put("mail.smtp.port", settings.port)
            put("mail.smtp.auth", "true")
            put("mail.smtp.connectiontimeout", "25000")
            put("mail.smtp.timeout", "25000")
            put("mail.smtp.writetimeout", "25000")
        }
        when (settings.security) {
            "STARTTLS" -> props.put("mail.smtp.starttls.enable", "true")
            "SSL/TLS" -> {
                props.put("mail.smtp.ssl.enable", "true")
                props.put("mail.smtp.socketFactory.port", settings.port)
                props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
            }
            "PLAINTEXT" -> Unit
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
            setFrom(
                InternetAddress(
                    settings.user,
                    settings.senderName.ifBlank { settings.user },
                    Charsets.UTF_8.name(),
                ),
            )
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
        forceRemote: Boolean = false,
    ): DriverAccountEntity? {
        val normalizedDriver = driverName.trim()
        val normalizedRegistration = registration.trim().uppercase()
        if (normalizedRegistration.isBlank()) return null
        if (normalizedDriver.isBlank()) {
            dao.deleteDriverAccountByRegistration(normalizedRegistration)
            syncRemoteDriverDeletion(normalizedRegistration)
            return null
        }

        val generatedLogin = generateLogin(normalizedDriver)
        val existingByRegistration = dao.getDriverAccountByRegistrationAndDriverName(
            registration = normalizedRegistration,
            driverName = normalizedDriver,
        )
        val existingByLogin = dao.getDriverAccountsByLogin(generatedLogin).firstOrNull()
            ?: runCatching { DriverRemoteSyncGateway.findDriverAccount(dao, generatedLogin) }.getOrNull()
        val loginBelongsToSameDriver = existingByLogin?.driverName?.equals(normalizedDriver, ignoreCase = true) == true
        val resolvedLogin = when {
            loginBelongsToSameDriver -> existingByLogin?.login ?: generatedLogin
            else -> resolveUniqueLogin(normalizedDriver, generatedLogin)
        }
        val existingByResolvedLogin = dao.getDriverAccountsByLogin(resolvedLogin).firstOrNull()
            ?: runCatching { DriverRemoteSyncGateway.findDriverAccount(dao, resolvedLogin) }.getOrNull()
        val authoritativeExisting = when {
            existingByResolvedLogin != null &&
                existingByResolvedLogin.driverName.equals(normalizedDriver, ignoreCase = true) -> existingByResolvedLogin
            existingByRegistration != null && existingByRegistration.driverName.equals(normalizedDriver, ignoreCase = true) -> existingByRegistration
            else -> null
        }

        val shouldRotateCredentials = forceReset || authoritativeExisting == null
        val account = DriverAccountEntity(
            registration = normalizedRegistration,
            login = authoritativeExisting?.login ?: resolvedLogin,
            password = if (shouldRotateCredentials) generatePassword() else authoritativeExisting.password,
            driverName = normalizedDriver,
            changePassword = if (shouldRotateCredentials) 1 else authoritativeExisting.changePassword,
            licenseType = authoritativeExisting?.licenseType ?: "PL",
            licenseValidUntil = authoritativeExisting?.licenseValidUntil.orEmpty(),
        )
        dao.upsertDriverAccount(account)
        if (shouldRotateCredentials || forceRemote) {
            queueRemoteSync(
                registration = normalizedRegistration,
                inProgressStatus = "Trwa zdalna synchronizacja konta kierowcy",
            ) {
                syncRemoteDriverUpsert(account, action = if (forceReset) "reset_driver" else "create_driver")
            }
        } else {
            queueRemoteSync(
                registration = normalizedRegistration,
                inProgressStatus = "Trwa zdalna synchronizacja przypisania kierowcy",
            ) {
                syncRemoteDriverAssignment(account)
            }
        }
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

    private suspend fun resolveUniqueLogin(driverName: String, baseLogin: String): String {
        val normalizedDriver = driverName.trim()
        val normalizedBase = baseLogin.trim().ifBlank { "driver" }
        val baseOwner = dao.getDriverAccountsByLogin(normalizedBase).firstOrNull()
            ?: runCatching { DriverRemoteSyncGateway.findDriverAccount(dao, normalizedBase) }.getOrNull()
        if (baseOwner == null || baseOwner.driverName.equals(normalizedDriver, ignoreCase = true)) {
            return normalizedBase
        }
        for (index in 2..999) {
            val candidate = "$normalizedBase.$index"
            val owner = dao.getDriverAccountsByLogin(candidate).firstOrNull()
                ?: runCatching { DriverRemoteSyncGateway.findDriverAccount(dao, candidate) }.getOrNull()
            if (owner == null || owner.driverName.equals(normalizedDriver, ignoreCase = true)) {
                return candidate
            }
        }
        return "${normalizedBase}.${System.currentTimeMillis()}"
    }

    private fun parseDriverNames(value: String): List<String> = value
        .split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase() }

    private fun Appendable.appendCsvLine(vararg columns: String) {
        appendLine(columns.joinToString(",") { value ->
            "\"${value.replace("\"", "\"\"")}\""
        })
    }

    private suspend fun dispatchMailBatch(
        recipients: List<ContactListItem>,
        attachmentPaths: List<String>,
        autoMode: Boolean,
        subjectOverride: String?,
        bodyOverride: String?,
        onProgress: suspend (MailDispatchProgress) -> Unit,
        awaitResume: suspend () -> Unit,
        requireApproval: Boolean,
        awaitApproval: suspend (MailApprovalRequest) -> Boolean,
    ): MailDispatchResult {
        val settings = loadSavedSmtpSettings().normalized()
        val template = loadSavedEmailTemplate().validated()
        val attachments = resolveAttachments(attachmentPaths)
        val today = LocalDate.now()
        val throttleMs = settings.throttleMs.toLong()
        val resolvedSubject = subjectOverride?.trim().orEmpty().ifBlank { template.subject }
        val resolvedBody = bodyOverride?.trim().orEmpty().ifBlank { template.body }
        require(resolvedSubject.isNotBlank()) { "Brak tematu email" }
        require(resolvedBody.isNotBlank()) { "Brak treści email" }

        var ok = 0
        var fail = 0
        var skip = 0
        val details = mutableListOf<String>()

        recipients.forEachIndexed { index, contact ->
            awaitResume()

            val email = contact.email.trim()
            val fullName = listOf(contact.name, contact.surname).joinToString(" ").trim()
            val recipientLabel = fullName.ifBlank { email.ifBlank { "Bez nazwy" } }
            if (email.isBlank()) {
                skip += 1
                details += "SKIP: $recipientLabel — brak email"
                onProgress(
                    MailDispatchProgress(
                        processed = index + 1,
                        total = recipients.size,
                        ok = ok,
                        fail = fail,
                        skip = skip,
                        currentRecipient = recipientLabel,
                    ),
                )
                return@forEachIndexed
            }
            if (requireApproval) {
                val approved = awaitApproval(
                    MailApprovalRequest(
                        recipientName = recipientLabel,
                        recipientEmail = email,
                    ),
                )
                if (!approved) {
                    skip += 1
                    details += "SKIP: ${fullName.ifBlank { email }} <$email> — pominięte przez operatora"
                    onProgress(
                        MailDispatchProgress(
                            processed = index + 1,
                            total = recipients.size,
                            ok = ok,
                            fail = fail,
                            skip = skip,
                            currentRecipient = recipientLabel,
                        ),
                    )
                    return@forEachIndexed
                }
            }

            runCatching {
                sendMail(
                    settings = settings,
                    recipient = email,
                    subject = renderTemplate(resolvedSubject, contact.name.ifBlank { fullName }, today),
                    body = renderTemplate(resolvedBody, contact.name.ifBlank { fullName }, today),
                    attachments = attachments,
                )
            }.onSuccess {
                ok += 1
                details += "OK: ${fullName.ifBlank { email }} <$email>"
            }.onFailure { error ->
                fail += 1
                details += "FAIL: ${fullName.ifBlank { email }} <$email> — ${error.message.orEmpty().ifBlank { "nieznany błąd" }}"
            }

            onProgress(
                MailDispatchProgress(
                    processed = index + 1,
                    total = recipients.size,
                    ok = ok,
                    fail = fail,
                    skip = skip,
                    currentRecipient = recipientLabel,
                ),
            )

            if (throttleMs > 0 && index < recipients.lastIndex) {
                delay(throttleMs)
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
        return result
    }

    private fun sanitizeFilePart(value: String): String = value
        .trim()
        .replace(Regex("[^A-Za-z0-9_-]+"), "_")
        .trim('_')
        .ifBlank { "export" }

    private fun generatePassword(length: Int = 6): String = buildString {
        repeat(length) { append(Random.nextInt(0, 10)) }
    }

    private suspend fun syncRemoteDriverUpsert(account: DriverAccountEntity, action: String) {
        DriverRemoteSyncGateway.syncDriverUpsert(dao, account, action)
    }

    private suspend fun syncRemoteDriverAssignment(account: DriverAccountEntity) {
        DriverRemoteSyncGateway.syncDriverAssignment(dao, account)
    }

    private suspend fun syncRemoteDriverDeletion(registration: String) {
        DriverRemoteSyncGateway.syncDriverDeletion(dao, registration)
    }

    private suspend fun queueRemoteSync(
        registration: String,
        inProgressStatus: String,
        block: suspend () -> Unit,
    ) {
        val normalizedRegistration = registration.trim().uppercase()
        if (normalizedRegistration.isBlank()) return
        markRemoteSyncState(
            registration = normalizedRegistration,
            status = inProgressStatus,
            error = "",
        )
        remoteSyncScope.launch {
            runCatching { block() }
                .onFailure { error ->
                    markRemoteSyncState(
                        registration = normalizedRegistration,
                        status = "Błąd zdalnej synchronizacji kierowcy",
                        error = error.message ?: "Nieznany błąd zdalnej synchronizacji",
                    )
                }
        }
    }

    private suspend fun markRemoteSyncState(
        registration: String,
        status: String,
        error: String,
    ) {
        val normalizedRegistration = registration.trim().uppercase()
        if (normalizedRegistration.isBlank()) return
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        dao.upsertSetting(SettingEntity(key = "driver_remote_sync_at_$normalizedRegistration", valText = timestamp))
        dao.upsertSetting(SettingEntity(key = "driver_remote_sync_status_$normalizedRegistration", valText = status))
        dao.upsertSetting(SettingEntity(key = "driver_remote_sync_error_$normalizedRegistration", valText = error))
    }
}
