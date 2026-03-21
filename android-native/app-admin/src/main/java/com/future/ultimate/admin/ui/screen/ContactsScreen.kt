package com.future.ultimate.admin.ui.screen

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
    var isAddDialogOpen by remember { mutableStateOf(false) }

    val filteredContacts = remember(uiState.items, uiState.query) {
        uiState.items.filter {
            val blob = "${it.name} ${it.surname} ${it.email} ${it.phone} ${it.workplace} ${it.apartment} ${it.notes}".lowercase()
            uiState.query.isBlank() || uiState.query.lowercase() in blob
        }
    }

    ScreenColumn("Kontakty", "Szukaj kontaktów i wykonuj szybkie akcje bez opuszczania listy.") {
        item {
            SectionCard(title = "Wyszukiwarka", subtitle = "Szukaj po imieniu, nazwisku, emailu, telefonie lub zakładzie.") {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = viewModel::updateQuery,
                    label = { Text("Szukaj kontaktu") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    FilledIconButton(
                        onClick = {
                            viewModel.updateEditor(ContactDraft())
                            isAddDialogOpen = true
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

        if (filteredContacts.isEmpty()) {
            item {
                SectionCard {
                    Text(
                        text = "Brak kontaktów pasujących do wyszukiwania.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        } else {
            filteredContacts.forEach { contact ->
                item {
                    ContactCard(
                        contact = contact,
                        onCall = { openDialer(context, contact.phone) },
                        onWhatsApp = { openWhatsApp(context, contact.phone) },
                    )
                }
            }
        }
    }

    if (isAddDialogOpen) {
        AddContactDialog(
            draft = uiState.editor,
            isSaving = uiState.isSaving,
            onDraftChange = viewModel::updateEditor,
            onDismiss = {
                isAddDialogOpen = false
                viewModel.updateEditor(ContactDraft())
            },
            onSave = {
                viewModel.save()
                isAddDialogOpen = false
            },
        )
    }
}

@Composable
private fun ContactCard(
    contact: ContactListItem,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit,
) {
    SectionCard(
        title = listOf(contact.name, contact.surname).joinToString(" ").trim().ifBlank { "Bez nazwy" },
        subtitle = contact.workplace.ifBlank { "Brak przypisanego zakładu" },
    ) {
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
    }
}

@Composable
private fun AddContactDialog(
    draft: ContactDraft,
    isSaving: Boolean,
    onDraftChange: (ContactDraft) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    val isSaveEnabled = draft.name.isNotBlank() && draft.surname.isNotBlank() && draft.phone.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Nowy kontakt",
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
                    label = { Text("Telefon *") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = draft.email,
                    onValueChange = { onDraftChange(draft.copy(email = it)) },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = draft.workplace,
                    onValueChange = { onDraftChange(draft.copy(workplace = it)) },
                    label = { Text("Zakład pracy") },
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
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = isSaveEnabled && !isSaving,
            ) {
                Text(if (isSaving) "Zapisywanie..." else "Dodaj kontakt")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        },
    )
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
