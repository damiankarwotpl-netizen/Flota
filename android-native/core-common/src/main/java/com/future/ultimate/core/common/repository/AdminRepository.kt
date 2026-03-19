package com.future.ultimate.core.common.repository

import com.future.ultimate.core.common.model.CarDraft
import com.future.ultimate.core.common.model.ClothesOrderDraft
import com.future.ultimate.core.common.model.ClothesOrderItemDraft
import com.future.ultimate.core.common.model.ClothesSizeDraft
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
    val driverLogin: String = "",
    val driverPassword: String = "",
    val changePasswordRequired: Boolean = false,
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

data class DashboardStats(
    val contactCount: Int = 0,
    val workerCount: Int = 0,
    val carCount: Int = 0,
    val plantCount: Int = 0,
    val clothesSizeCount: Int = 0,
    val clothesOrderCount: Int = 0,
    val clothesHistoryCount: Int = 0,
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

    fun observeClothesSizes(): Flow<List<ClothesSizeListItem>>
    suspend fun saveClothesSize(draft: ClothesSizeDraft)
    suspend fun deleteClothesSize(id: Long)

    fun observeClothesOrders(): Flow<List<ClothesOrderListItem>>
    suspend fun saveClothesOrder(draft: ClothesOrderDraft)
    suspend fun deleteClothesOrder(orderId: Long)
    fun observeClothesOrderItems(orderId: Long): Flow<List<ClothesOrderItemListItem>>
    suspend fun saveClothesOrderItem(orderId: Long, draft: ClothesOrderItemDraft)
    suspend fun deleteClothesOrderItem(id: Long)
    suspend fun markClothesOrderOrdered(orderId: Long)
    suspend fun issueClothesOrderItem(id: Long)
    suspend fun issueAllClothesOrderItems(orderId: Long)
    suspend fun exportClothesOrderCsv(orderId: Long): String
    suspend fun exportClothesOrderXlsx(orderId: Long): ClothesOrderXlsxExport
    fun observeClothesHistory(): Flow<List<ClothesHistoryListItem>>

    fun observeSmtpSettings(): Flow<SmtpSettingsData>
    suspend fun saveSmtpSettings(settings: SmtpSettingsData)

    fun observeEmailTemplate(): Flow<EmailTemplateData>
    suspend fun saveEmailTemplate(template: EmailTemplateData)

    fun observeSessionReports(): Flow<List<SessionReportListItem>>
    fun observeDashboardStats(): Flow<DashboardStats>

    suspend fun saveVehicleReportDraft(draft: VehicleReportDraft)
    suspend fun exportVehicleReportPdf(draft: VehicleReportDraft): String
    suspend fun exportContactsCsv(): String
    suspend fun exportContactRowXlsx(name: String, surname: String): String
    suspend fun exportClothesHistoryCsv(): String
    suspend fun exportSessionReportsCsv(): String
}
