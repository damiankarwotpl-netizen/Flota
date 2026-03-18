package com.future.ultimate.core.common.ui

import com.future.ultimate.core.common.model.CarDraft
import com.future.ultimate.core.common.model.ContactDraft
import com.future.ultimate.core.common.model.VehicleReportDraft
import com.future.ultimate.core.common.repository.CarListItem
import com.future.ultimate.core.common.repository.ContactListItem
import com.future.ultimate.core.common.repository.PlantListItem
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
)

data class PlantsUiState(
    val query: String = "",
    val items: List<PlantListItem> = emptyList(),
)
