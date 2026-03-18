package com.future.ultimate.core.common.ui

import com.future.ultimate.core.common.model.CarDraft
import com.future.ultimate.core.common.model.ClothesOrderDraft
import com.future.ultimate.core.common.model.ClothesOrderItemDraft
import com.future.ultimate.core.common.model.ClothesSizeDraft
import com.future.ultimate.core.common.model.ContactDraft
import com.future.ultimate.core.common.model.PlantDraft
import com.future.ultimate.core.common.model.VehicleReportDraft
import com.future.ultimate.core.common.model.WorkerDraft
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

data class ContactsUiState(
    val query: String = "",
    val workplaceFilter: String = "",
    val cityFilter: String = "",
    val items: List<ContactListItem> = emptyList(),
    val editor: ContactDraft = ContactDraft(),
    val isSaving: Boolean = false,
)

data class CarsUiState(
    val query: String = "",
    val items: List<CarListItem> = emptyList(),
    val editor: CarDraft = CarDraft(),
    val isSaving: Boolean = false,
    val mileageDrafts: Map<Long, String> = emptyMap(),
    val driverDrafts: Map<Long, String> = emptyMap(),
    val actionInFlightId: Long? = null,
)

data class VehicleReportUiState(
    val draft: VehicleReportDraft = VehicleReportDraft(),
    val isSaving: Boolean = false,
    val exportMessage: String? = null,
)

data class PayrollUiState(
    val autoSend: Boolean = false,
    val attachmentCount: Int = 0,
    val totalRecipients: Int = 0,
    val progressLabel: String = "Gotowy",
    val isMailingRunning: Boolean = false,
)

data class WorkersUiState(
    val query: String = "",
    val items: List<WorkerListItem> = emptyList(),
    val editor: WorkerDraft = WorkerDraft(),
    val isSaving: Boolean = false,
)

data class PlantsUiState(
    val query: String = "",
    val items: List<PlantListItem> = emptyList(),
    val editor: PlantDraft = PlantDraft(),
    val isSaving: Boolean = false,
)

data class ClothesSizesUiState(
    val query: String = "",
    val items: List<ClothesSizeListItem> = emptyList(),
    val editor: ClothesSizeDraft = ClothesSizeDraft(),
    val isSaving: Boolean = false,
)

data class ClothesOrdersUiState(
    val items: List<ClothesOrderListItem> = emptyList(),
    val selectedOrderId: Long? = null,
    val selectedOrderItems: List<ClothesOrderItemListItem> = emptyList(),
    val editor: ClothesOrderDraft = ClothesOrderDraft(),
    val itemEditor: ClothesOrderItemDraft = ClothesOrderItemDraft(),
    val isSaving: Boolean = false,
    val isSavingItem: Boolean = false,
)

data class ClothesReportsUiState(
    val year: String = "",
    val history: List<ClothesHistoryListItem> = emptyList(),
    val yearlySummary: List<String> = emptyList(),
    val exportMessage: String? = null,
    val isExporting: Boolean = false,
)

data class SmtpUiState(
    val settings: SmtpSettingsData = SmtpSettingsData(),
    val message: String? = null,
    val isSaving: Boolean = false,
)

data class TemplateUiState(
    val template: EmailTemplateData = EmailTemplateData(),
    val message: String? = null,
    val isSaving: Boolean = false,
)

data class ReportsUiState(
    val items: List<SessionReportListItem> = emptyList(),
)

data class SettingsUiState(
    val stats: DashboardStats = DashboardStats(),
)
