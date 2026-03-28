package com.future.ultimate.admin.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.future.ultimate.admin.AdminApp
import com.future.ultimate.admin.ui.viewmodel.AdminViewModelFactory
import com.future.ultimate.admin.ui.viewmodel.ContactsViewModel
import com.future.ultimate.core.common.model.ContactDraft
import com.future.ultimate.core.common.repository.ContactListItem

@Composable
fun ContactsScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as AdminApp
    val viewModel: ContactsViewModel = viewModel(factory = AdminViewModelFactory(app.container.repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isDialogOpen by remember { mutableStateOf(false) }
    var editedContact by remember { mutableStateOf<ContactListItem?>(null) }
    var dialogMode by remember { mutableStateOf(ContactDialogMode.Employee) }
    val selectedTab = remember { mutableIntStateOf(0) }

    val searchableContacts = remember(uiState.items, uiState.query) {
        uiState.items.filter {
            val blob = "${it.name} ${it.surname} ${it.email} ${it.pesel} ${it.phone} ${it.workplace} ${it.apartment} ${it.notes}".lowercase()
            uiState.query.isBlank() || uiState.query.lowercase() in blob
        }
    }
    val employeeContacts = remember(searchableContacts) { searchableContacts.filter(::isEmployeeContact) }
    val futureContacts = remember(searchableContacts) { searchableContacts.filter(::isFutureContact) }
    val plantContacts = remember(searchableContacts) { searchableContacts.filter { it.workplace.isNotBlank() } }

    ScreenColumn("Kontakty", "Szukaj kontaktów i wykonuj szybkie akcje bez opuszczania listy.") {
        item {
            SectionCard {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = viewModel::updateQuery,
                    label = {
                        Text(
                            when (selectedTab.intValue) {
                                0 -> "Szukaj pracownika"
                                1 -> "Szukaj kontaktu Future"
                                else -> "Szukaj zakładu"
                            },
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                TabRow(selectedTabIndex = selectedTab.intValue) {
                    listOf("Pracownicy", "Future", "Zakłady").forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab.intValue == index,
                            onClick = { selectedTab.intValue = index },
                            text = { Text(title) },
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    FilledIconButton(
                        onClick = {
                            viewModel.updateEditor(ContactDraft())
                            editedContact = null
                            dialogMode = when (selectedTab.intValue) {
                                1 -> ContactDialogMode.Future
                                2 -> ContactDialogMode.Plant
                                else -> ContactDialogMode.Employee
                            }
                            isDialogOpen = true
                        },
                        modifier = Modifier.size(42.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Dodaj nowy kontakt",
                        )
                    }
                }
            }
        }

        when (selectedTab.intValue) {
            0 -> {
                if (employeeContacts.isEmpty()) {
                    item {
                        SectionCard {
                            Text(
                                text = "Brak pracowników pasujących do wyszukiwania.",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                } else {
                    employeeContacts.forEach { contact ->
                        item {
                            ContactCard(
                                contact = contact,
                                onCall = { openDialer(context, contact.phone) },
                                onWhatsApp = { openWhatsApp(context, contact.phone) },
                                onEmail = { openEmail(context, contact.email) },
                                onEdit = {
                                    editedContact = contact
                                    dialogMode = ContactDialogMode.Employee
                                    viewModel.updateEditor(contact.toDraft())
                                    isDialogOpen = true
                                },
                            )
                        }
                    }
                }
            }
            1 -> {
                if (futureContacts.isEmpty()) {
                    item {
                        SectionCard {
                            Text("Brak kontaktów Future.")
                        }
                    }
                } else {
                    futureContacts.forEach { contact ->
                        item {
                            ContactCard(
                                contact = contact,
                                onCall = { openDialer(context, contact.phone) },
                                onWhatsApp = { openWhatsApp(context, contact.phone) },
                                onEmail = { openEmail(context, contact.email) },
                                onEdit = {
                                    editedContact = contact
                                    dialogMode = ContactDialogMode.Future
                                    viewModel.updateEditor(contact.toDraft())
                                    isDialogOpen = true
                                },
                            )
                        }
                    }
                }
            }
            else -> {
                if (plantContacts.isEmpty()) {
                    item {
                        SectionCard {
                            Text("Brak zakładów pasujących do wyszukiwania.")
                        }
                    }
                } else {
                    plantContacts
                        .groupBy { it.workplace.trim() }
                        .toSortedMap()
                        .forEach { (workplace, contacts) ->
                            item {
                                SectionCard(title = workplace) {
                                    contacts.forEach { contact ->
                                        Text("${contact.name} ${contact.surname}".trim())
                                        if (contact.phone.isNotBlank()) Text("Telefon: ${contact.phone}")
                                        if (contact.email.isNotBlank()) Text("Email: ${contact.email}")
                                    }
                                }
                            }
                        }
                }
            }
        }
    }

    if (isDialogOpen) {
        AddContactDialog(
            draft = uiState.editor,
            isSaving = uiState.isSaving,
            isEditing = editedContact != null,
            mode = dialogMode,
            plantSuggestions = uiState.plantSuggestions,
            onDraftChange = viewModel::updateEditor,
            onDismiss = {
                isDialogOpen = false
                editedContact = null
                viewModel.updateEditor(ContactDraft())
            },
            onSave = {
                viewModel.save()
                isDialogOpen = false
                editedContact = null
            },
        )
    }
}

private enum class ContactDialogMode {
    Employee,
    Future,
    Plant,
}

@Composable
private fun ContactCard(
    contact: ContactListItem,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit,
    onEmail: () -> Unit,
    onEdit: () -> Unit,
) {
    SectionCard(
        title = listOf(contact.name, contact.surname).joinToString(" ").trim().ifBlank { "Bez nazwy" },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            FilledIconButton(
                onClick = onEdit,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Edytuj kontakt",
                )
            }
        }
        Text("Zakład: ${contact.workplace.ifBlank { "Brak przypisanego zakładu" }}")
        if (contact.pesel.isNotBlank()) {
            Text("PESEL: ${contact.pesel}")
        }
        Text("Telefon: ${contact.phone.ifBlank { "Brak numeru" }}")
        if (contact.email.isNotBlank()) {
            Text("Email: ${contact.email}")
        }
        if (contact.apartment.isNotBlank()) {
            Text("Mieszkanie: ${contact.apartment}")
        }
        if (contact.notes.isNotBlank()) {
            Text("Uwagi: ${contact.notes}")
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onCall,
                modifier = Modifier.weight(1f),
                enabled = contact.phone.isNotBlank(),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Call,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text("Zadzwoń")
            }
            Button(
                onClick = onWhatsApp,
                modifier = Modifier.weight(1f),
                enabled = contact.phone.isNotBlank(),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Chat,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text("WhatsApp")
            }
        }
        Button(
            onClick = onEmail,
            modifier = Modifier.fillMaxWidth(),
            enabled = contact.email.isNotBlank(),
        ) {
            Icon(
                imageVector = Icons.Rounded.Email,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text("Wyślij e-mail")
        }
    }
}

@Composable
private fun AddContactDialog(
    draft: ContactDraft,
    isSaving: Boolean,
    isEditing: Boolean,
    mode: ContactDialogMode,
    plantSuggestions: List<String>,
    onDraftChange: (ContactDraft) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    val isSaveEnabled = when (mode) {
        ContactDialogMode.Future -> draft.name.isNotBlank() && draft.surname.isNotBlank() && draft.phone.isNotBlank() && draft.email.isNotBlank()
        ContactDialogMode.Plant -> draft.workplace.isNotBlank() && draft.name.isNotBlank() && draft.surname.isNotBlank() && draft.phone.isNotBlank() && draft.email.isNotBlank()
        ContactDialogMode.Employee -> draft.name.isNotBlank() && draft.surname.isNotBlank() && draft.phone.isNotBlank()
    }
    var isPlantPickerOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edytuj kontakt" else "Nowy kontakt",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = { onDraftChange(draft.copy(name = it)) },
                    label = { Text("Imię *") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = draft.surname,
                    onValueChange = { onDraftChange(draft.copy(surname = it)) },
                    label = { Text("Nazwisko *") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = draft.phone,
                    onValueChange = { onDraftChange(draft.copy(phone = it)) },
                    label = { Text(if (mode == ContactDialogMode.Employee) "Telefon *" else "Telefon") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = draft.email,
                    onValueChange = { onDraftChange(draft.copy(email = it)) },
                    label = { Text(if (mode == ContactDialogMode.Employee) "Email" else "Email *") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (mode == ContactDialogMode.Plant || mode == ContactDialogMode.Employee) {
                    OutlinedTextField(
                        value = draft.workplace,
                        onValueChange = {},
                        label = { Text(if (mode == ContactDialogMode.Plant) "Nazwa zakładu *" else "Zakład") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isPlantPickerOpen = true },
                        readOnly = true,
                    )
                    TextButton(
                        onClick = { isPlantPickerOpen = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Wybierz zakład z listy")
                    }
                }
                if (mode == ContactDialogMode.Employee) {
                    OutlinedTextField(
                        value = draft.pesel,
                        onValueChange = { onDraftChange(draft.copy(pesel = it)) },
                        label = { Text("PESEL") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = draft.apartment,
                        onValueChange = { onDraftChange(draft.copy(apartment = it)) },
                        label = { Text("Mieszkanie") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = draft.notes,
                        onValueChange = { onDraftChange(draft.copy(notes = it)) },
                        label = { Text("Uwagi") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = isSaveEnabled && !isSaving,
            ) {
                Text(
                    when {
                        isSaving -> "Zapisywanie..."
                        isEditing -> "Zapisz zmiany"
                        else -> "Dodaj kontakt"
                    },
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        },
    )

    if (isPlantPickerOpen && (mode == ContactDialogMode.Plant || mode == ContactDialogMode.Employee)) {
        AlertDialog(
            onDismissRequest = { isPlantPickerOpen = false },
            title = { Text("Wybierz zakład", fontWeight = FontWeight.Bold) },
            text = {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (plantSuggestions.isEmpty()) {
                        Text("Brak dostępnych zakładów.")
                    } else {
                        plantSuggestions.forEach { plantName ->
                            Button(
                                onClick = {
                                    onDraftChange(draft.copy(workplace = plantName))
                                    isPlantPickerOpen = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(plantName)
                            }
                        }
                    }
                    TextButton(
                        onClick = {
                            onDraftChange(draft.copy(workplace = ""))
                            isPlantPickerOpen = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Wyczyść zakład")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { isPlantPickerOpen = false }) {
                    Text("Zamknij")
                }
            },
        )
    }
}

private fun openDialer(context: android.content.Context, phone: String) {
    val sanitizedPhone = phone.trim()
    if (sanitizedPhone.isBlank()) return
    context.startActivity(
        Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(sanitizedPhone)}")),
    )
}

private fun openWhatsApp(context: android.content.Context, phone: String) {
    val normalizedPhone = phone.filter { it.isDigit() || it == '+' }
    if (normalizedPhone.isBlank()) return
    context.startActivity(
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://wa.me/${normalizedPhone.trimStart('+')}"),
        ),
    )
}

private fun openEmail(context: android.content.Context, email: String) {
    val normalizedEmail = email.trim()
    if (normalizedEmail.isBlank()) return
    context.startActivity(
        Intent(
            Intent.ACTION_SENDTO,
            Uri.parse("mailto:${Uri.encode(normalizedEmail)}"),
        ),
    )
}

private fun ContactListItem.toDraft(): ContactDraft = ContactDraft(
    name = name,
    surname = surname,
    email = email,
    pesel = pesel,
    phone = phone,
    workplace = workplace,
    apartment = apartment,
    notes = notes,
)

private fun isFutureContact(contact: ContactListItem): Boolean {
    val blob = "${contact.name} ${contact.surname} ${contact.workplace} ${contact.notes}".lowercase()
    return "future" in blob || "kadry" in blob || "bryg" in blob
}

private fun isEmployeeContact(contact: ContactListItem): Boolean = !isFutureContact(contact)
