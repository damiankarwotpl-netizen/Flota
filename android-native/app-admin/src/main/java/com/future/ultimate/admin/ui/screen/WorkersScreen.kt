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
fun WorkersScreen() {
    val query = remember { mutableStateOf("") }
    ScreenColumn("Pracownicy", "Lista i zarządzanie") {
        item {
            Column {
                OutlinedTextField(query.value, { query.value = it }, label = { Text("Szukaj pracownika") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Dodaj") }
            }
        }
        items(listOf("Pracownik • Edytuj • Usuń"))
    }
}
