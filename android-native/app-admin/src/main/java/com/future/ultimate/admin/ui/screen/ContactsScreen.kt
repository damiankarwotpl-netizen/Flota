package com.future.ultimate.admin.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
fun ContactsScreen() {
    val query = remember { mutableStateOf("") }
    val workplace = remember { mutableStateOf("") }
    val city = remember { mutableStateOf("") }
    ScreenColumn("Kontakty", "Szukaj, filtruj i zarządzaj kontaktami") {
        item {
            Column {
                OutlinedTextField(query.value, { query.value = it }, label = { Text("Szukaj po imieniu, nazwisku, email, telefonie...") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(workplace.value, { workplace.value = it }, label = { Text("Filtr zakład pracy") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(city.value, { city.value = it }, label = { Text("Filtr adres / mieszkanie") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Dodaj kontakt") }
            }
        }
        items(listOf("Kontakt 1 • Call / WhatsApp / Edytuj / Usuń", "Kontakt 2 • Call / WhatsApp / Edytuj / Usuń"))
    }
}
