package com.future.ultimate.admin.ui.screen

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddComment
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val filteredItems = uiState.items.filter {
        val blob = "${it.name} ${it.surname} ${it.email} ${it.phone} ${it.workplace} ${it.apartment} ${it.notes}".lowercase()
        uiState.query.isBlank() || uiState.query.lowercase() in blob
    }

    ScreenColumn("Czaty i kontakty", "Widok inspirowany WhatsApp: szybkie wyszukiwanie, avatar, połączenie i wiadomość.") {
        item {
            SectionCard(title = "Szukaj czatu", subtitle = "Filtruj po nazwie, telefonie, e-mailu lub zakładzie.") {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = viewModel::updateQuery,
                    label = { Text("Szukaj kontaktu") },
                    leadingIcon = { Icon(Icons.Outlined.Call, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                )
            }
        }
        item {
            SectionCard(title = "Nowy kontakt", subtitle = "Szybkie dodawanie nowego czatu do bazy.") {
                ContactEditor(
                    draft = uiState.editor,
                    isSaving = uiState.isSaving,
                    onChange = viewModel::updateEditor,
                    onSave = viewModel::save,
                )
            }
        }
        filteredItems.forEach { item ->
            this.item {
                ContactChatCard(
                    item = item,
                    onCall = { launchDial(context, item.phone) },
                    onWhatsApp = { launchWhatsApp(context, item.phone) },
                    onEdit = {
                        viewModel.updateEditor(
                            ContactDraft(
                                name = item.name,
                                surname = item.surname,
                                email = item.email,
                                phone = item.phone,
                                workplace = item.workplace,
                                apartment = item.apartment,
                                notes = item.notes,
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun ContactEditor(
    draft: ContactDraft,
    isSaving: Boolean,
    onChange: (ContactDraft) -> Unit,
    onSave: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = draft.name,
            onValueChange = { onChange(draft.copy(name = it)) },
            label = { Text("Imię") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
        )
        OutlinedTextField(
            value = draft.surname,
            onValueChange = { onChange(draft.copy(surname = it)) },
            label = { Text("Nazwisko") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
        )
        OutlinedTextField(
            value = draft.phone,
            onValueChange = { onChange(draft.copy(phone = it)) },
            label = { Text("Telefon") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
        )
        OutlinedTextField(
            value = draft.email,
            onValueChange = { onChange(draft.copy(email = it)) },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
        )
        OutlinedTextField(
            value = draft.workplace,
            onValueChange = { onChange(draft.copy(workplace = it)) },
            label = { Text("Zakład pracy") },
            leadingIcon = { Icon(Icons.Outlined.Work, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
        )
        Button(onClick = onSave, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Text(if (isSaving) "Zapisywanie..." else "Dodaj kontakt")
        }
    }
}

@Composable
private fun ContactChatCard(
    item: ContactListItem,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit,
    onEdit: () -> Unit,
) {
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initialsOf(item),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${item.name} ${item.surname}".trim(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edytuj kontakt")
                    }
                }
                Text(
                    text = item.workplace.ifBlank { "Brak zakładu pracy" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = listOf(item.phone.ifBlank { "brak telefonu" }, item.email.ifBlank { "brak e-maila" }).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (item.notes.isNotBlank()) {
                    Text(
                        text = item.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onCall, shape = RoundedCornerShape(16.dp)) {
                        Icon(Icons.Outlined.Call, contentDescription = null)
                        Text(" Zadzwoń")
                    }
                    Button(onClick = onWhatsApp, shape = RoundedCornerShape(16.dp)) {
                        Icon(Icons.Outlined.AddComment, contentDescription = null)
                        Text(" WhatsApp")
                    }
                }
            }
        }
    }
}

private fun initialsOf(item: ContactListItem): String {
    val parts = listOf(item.name, item.surname).filter { it.isNotBlank() }
    return parts.joinToString(separator = "") { it.take(1).uppercase() }.ifBlank { "?" }
}

private fun launchDial(context: android.content.Context, phone: String) {
    if (phone.isBlank()) {
        Toast.makeText(context, "Brak numeru telefonu", Toast.LENGTH_SHORT).show()
        return
    }
    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
}

private fun launchWhatsApp(context: android.content.Context, phone: String) {
    if (phone.isBlank()) {
        Toast.makeText(context, "Brak numeru telefonu", Toast.LENGTH_SHORT).show()
        return
    }
    val normalized = phone.filter { it.isDigit() || it == '+' }
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${normalized.trimStart('+')}"))
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "Nie można otworzyć WhatsApp", Toast.LENGTH_SHORT).show()
    }
}
