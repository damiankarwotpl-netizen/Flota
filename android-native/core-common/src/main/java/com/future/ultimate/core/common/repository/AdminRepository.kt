package com.future.ultimate.core.common.repository

import com.future.ultimate.core.common.model.CarDraft
import com.future.ultimate.core.common.model.ClothesOrderDraft
import com.future.ultimate.core.common.model.ClothesOrderItemDraft
import com.future.ultimate.core.common.model.ClothesSizeDraft
import com.future.ultimate.core.common.model.ContactDraft
import com.future.ultimate.core.common.model.PlantDraft
import com.future.ultimate.core.common.model.VehicleReportDraft
import com.future.ultimate.core.common.model.WorkerDraft
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

data class ContactListItem(
    val name: String,
    val surname: String,
    val email: String,
    val pesel: String,
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
    val driverPlant: String = "",
    val mileage: Int,
    val serviceInterval: Int,
    val lastService: Int,
    val lastInspectionDate: String = "",
    val driverLogin: String = "",
    val driverPassword: String = "",
    val changePasswordRequired: Boolean = false,
    val licenseType: String = "",
    val licenseValidUntil: String = "",
    val pendingMileageSync: Boolean = false,
    val queuedMileage: Int? = null,
    val lastMileageSyncAt: String = "",
    val lastMileageSyncStatus: String = "",
    val remoteDriverSyncAt: String = "",
    val remoteDriverSyncStatus: String = "",
    val remoteDriverSyncError: String = "",
    val driverAccounts: List<DriverAccountListItem> = emptyList(),
) {
    val remainingToService: Int
        get() = serviceInterval - (mileage - lastService)

    val nextInspectionDate: String
        get() = lastInspectionDate.toLocalDateOrNull()?.plusYears(1)?.toString().orEmpty()
}

data class DriverAccountListItem(
    val driverName: String = "",
    val login: String = "",
    val password: String = "",
    val changePasswordRequired: Boolean = false,
    val licenseType: String = "",
    val licenseValidUntil: String = "",
)

private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()

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

data class ClothesSizeListItem(
    val id: Long,
    val name: String,
    val surname: String,
    val plant: String,
    val shirt: String,
    val hoodie: String,
    val pants: String,
    val jacket: String,
    val shoes: String,
)

data class ClothesOrderListItem(
    val id: Long,
    val date: String,
    val plant: String,
    val status: String,
    val orderDesc: String,
)

data class ClothesOrderItemListItem(
    val id: Long,
    val orderId: Long,
    val workerId: Long,
    val name: String,
    val surname: String,
    val item: String,
    val size: String,
    val qty: Int,
    val issued: Boolean,
)

data class ClothesOrderWorkerListItem(
    val id: Long,
    val name: String,
    val surname: String,
    val plant: String,
    val shirt: String,
    val hoodie: String,
    val pants: String,
    val jacket: String,
    val shoes: String,
)

data class ClothesHistoryListItem(
    val id: Long,
    val workerId: Long,
    val name: String,
    val surname: String,
    val item: String,
    val size: String,
    val date: String,
)

data class SmtpSettingsData(
    val host: String = "",
    val port: String = "587",
    val user: String = "",
    val password: String = "",
    val security: String = "STARTTLS",
    val senderName: String = "",
    val throttleMs: String = "0",
)

data class DriverRemoteSettingsData(
    val apiUrl: String = "",
)

data class EmailTemplateData(
    val subject: String = "",
    val body: String = "",
)

data class SessionReportListItem(
    val date: String,
    val ok: Int,
    val fail: Int,
    val skip: Int,
    val details: String,
)

data class MailDispatchResult(
    val ok: Int,
    val fail: Int,
    val skip: Int,
    val details: String,
)

data class MailDispatchProgress(
    val processed: Int = 0,
    val total: Int = 0,
    val ok: Int = 0,
    val fail: Int = 0,
    val skip: Int = 0,
    val currentRecipient: String = "",
)

data class MailApprovalRequest(
    val recipientName: String = "",
    val recipientEmail: String = "",
)

data class DashboardStats(
    val contactCount: Int = 0,
    val workerCount: Int = 0,
    val carCount: Int = 0,
    val plantCount: Int = 0,
    val clothesSizeCount: Int = 0,
    val clothesOrderCount: Int = 0,
    val clothesHistoryCount: Int = 0,
)

data class PayrollWorkbookRow(
    val name: String,
    val surname: String,
    val workplace: String,
    val email: String,
    val amount: String,
)

data class PayrollPreviewRow(
    val index: Int,
    val cells: List<String>,
    val name: String,
    val surname: String,
)

data class ClothesOrderImportRow(
    val name: String,
    val surname: String,
    val item: String,
    val size: String,
    val qty: String,
)

data class DriverAccountCredentials(
    val login: String = "",
    val password: String = "",
)

data class ClothesOrderXlsxExport(
    val supplierPath: String = "",
    val issuePath: String = "",
)

interface AdminRepository {
    fun observeContacts(): Flow<List<ContactListItem>>
    suspend fun saveContact(draft: ContactDraft)
    suspend fun deleteContact(name: String, surname: String)

    fun observeCars(): Flow<List<CarListItem>>
    fun observeKnownCarDrivers(): Flow<List<String>>
    suspend fun saveCar(draft: CarDraft)
    suspend fun updateCarMileage(id: Long, mileage: Int)
    suspend fun updateCarDriver(id: Long, driver: String)
    suspend fun updateCarDriverLicense(id: Long, driverName: String, licenseType: String, validUntil: String)
    suspend fun resetCarDriverCredentials(id: Long, driverName: String): DriverAccountCredentials
    suspend fun retryCarDriverRemoteSync(id: Long)
    suspend fun deleteKnownCarDriver(driver: String)
    suspend fun confirmCarService(id: Long)
    suspend fun confirmCarInspection(id: Long)
    suspend fun deleteCar(id: Long)

    fun observeWorkers(): Flow<List<WorkerListItem>>
    suspend fun saveWorker(draft: WorkerDraft)
    suspend fun deleteWorker(id: Long)

    fun observePlants(): Flow<List<PlantListItem>>
    suspend fun savePlant(draft: PlantDraft)
    suspend fun deletePlant(id: Long)

    fun observeClothesSizes(): Flow<List<ClothesSizeListItem>>
    suspend fun saveClothesSize(draft: ClothesSizeDraft)
    suspend fun deleteClothesSize(id: Long)

    fun observeClothesOrders(): Flow<List<ClothesOrderListItem>>
    suspend fun saveClothesOrder(draft: ClothesOrderDraft)
    suspend fun deleteClothesOrder(orderId: Long)
    fun observeClothesOrderWorkers(): Flow<List<ClothesOrderWorkerListItem>>
    fun observeClothesOrderItems(orderId: Long): Flow<List<ClothesOrderItemListItem>>
    suspend fun saveClothesOrderItem(orderId: Long, draft: ClothesOrderItemDraft)
    suspend fun importClothesOrderItems(orderId: Long, rows: List<ClothesOrderImportRow>): Int
    suspend fun createClothesOrderStarter(
        draft: ClothesOrderDraft,
        workerIds: Set<Long>,
        shirtQty: Int,
        hoodieQty: Int,
        pantsQty: Int,
        jacketQty: Int,
        shoesQty: Int,
    ): Long?
    suspend fun createClothesOrderFromSelections(
        draft: ClothesOrderDraft,
        workerParts: Map<Long, Set<String>>,
    ): Long?
    suspend fun deleteClothesOrderItem(id: Long)
    suspend fun markClothesOrderOrdered(orderId: Long)
    suspend fun issueClothesOrderItem(id: Long)
    suspend fun issueAllClothesOrderItems(orderId: Long)
    suspend fun exportClothesOrderPdf(orderId: Long): String
    suspend fun exportClothesIssuePdf(orderId: Long): String
    suspend fun exportClothesOrderCsv(orderId: Long): String
    suspend fun exportClothesOrderXlsx(orderId: Long): ClothesOrderXlsxExport
    fun observeClothesHistory(): Flow<List<ClothesHistoryListItem>>

    fun observeSmtpSettings(): Flow<SmtpSettingsData>
    suspend fun saveSmtpSettings(settings: SmtpSettingsData)
    suspend fun validateSmtpConnection(settings: SmtpSettingsData)
    fun observeDriverRemoteSettings(): Flow<DriverRemoteSettingsData>
    suspend fun saveDriverRemoteSettings(settings: DriverRemoteSettingsData)
    suspend fun validateDriverRemoteSettings(settings: DriverRemoteSettingsData): String
    suspend fun importDriverRemoteLogs(settings: DriverRemoteSettingsData): Int
    suspend fun clearAllTestData(settings: DriverRemoteSettingsData): String

    fun observeEmailTemplate(): Flow<EmailTemplateData>
    suspend fun saveEmailTemplate(template: EmailTemplateData)
    suspend fun sendSinglePreviewMail(attachmentPaths: List<String>): String
    suspend fun sendMassMailing(
        attachmentPaths: List<String>,
        autoMode: Boolean,
        onProgress: suspend (MailDispatchProgress) -> Unit = {},
        awaitResume: suspend () -> Unit = {},
        awaitApproval: suspend (MailApprovalRequest) -> Boolean = { true },
    ): MailDispatchResult
    suspend fun sendSpecialMailing(
        recipients: List<ContactListItem>,
        attachmentPaths: List<String>,
        subject: String,
        body: String,
        onProgress: suspend (MailDispatchProgress) -> Unit = {},
        awaitResume: suspend () -> Unit = {},
    ): MailDispatchResult

    fun observeSessionReports(): Flow<List<SessionReportListItem>>
    fun observeDashboardStats(): Flow<DashboardStats>

    suspend fun saveVehicleReportDraft(draft: VehicleReportDraft)
    suspend fun exportVehicleReportPdf(draft: VehicleReportDraft): String
    suspend fun exportDatabaseSnapshot(): String
    suspend fun importDatabaseWorkbook(
        fileName: String?,
        mimeType: String?,
        bytes: ByteArray,
    ): DatabaseWorkbookImportResult
    suspend fun exportContactsCsv(): String
    suspend fun exportContactRowXlsx(name: String, surname: String): String
    suspend fun exportPayrollPackage(contacts: List<ContactListItem>): String
    suspend fun exportPayrollWorkbookCsv(rows: List<PayrollWorkbookRow>): String
    suspend fun exportPayrollRowsXlsx(
        headers: List<String>,
        rows: List<List<String>>,
        filePrefix: String = "PPI",
        nameHint: String = "",
        surnameHint: String = "",
    ): String
    suspend fun exportPayrollCashReportXlsx(
        headers: List<String>,
        rows: List<List<String>>,
        totalAmount: String,
    ): String
    suspend fun exportClothesHistoryCsv(): String
    suspend fun exportSessionReportsCsv(): String
}

data class DatabaseWorkbookImportResult(
    val contactsImported: Int = 0,
    val workersImported: Int = 0,
    val plantsImported: Int = 0,
    val clothesSizesImported: Int = 0,
)
