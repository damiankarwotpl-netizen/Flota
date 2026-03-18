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
fun SmtpScreen() {
    val host = remember { mutableStateOf("") }
    val port = remember { mutableStateOf("587") }
    val user = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    ScreenColumn("Ustawienia SMTP", "Konfiguracja serwera SMTP") {
        item {
            Column {
                OutlinedTextField(host.value, { host.value = it }, label = { Text("Host") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(port.value, { port.value = it }, label = { Text("Port") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(user.value, { user.value = it }, label = { Text("Email/Login") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(password.value, { password.value = it }, label = { Text("Hasło/Klucz") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Zapisz") }
                Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Test połączenia") }
            }
        }
    }
}
