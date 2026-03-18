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
fun TemplateScreen() {
    val subject = remember { mutableStateOf("") }
    val body = remember { mutableStateOf("") }
    ScreenColumn("Szablon email", "Edycja szablonu wiadomości") {
        item {
            Column {
                OutlinedTextField(subject.value, { subject.value = it }, label = { Text("Temat {Imię}") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(body.value, { body.value = it }, label = { Text("Treść...") }, modifier = Modifier.fillMaxWidth(), minLines = 6)
                Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Zapisz") }
            }
        }
    }
}
