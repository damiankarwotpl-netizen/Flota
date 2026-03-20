package com.future.ultimate.admin.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ScreenColumn(
    title: String,
    subtitle: String? = null,
    content: ColumnScopeLike.() -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    subtitle?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        val scope = ColumnScopeLike(this)
        scope.content()
    }
}

class ColumnScopeLike(private val list: androidx.compose.foundation.lazy.LazyListScope) {
    fun item(content: @Composable () -> Unit) = list.item { content() }

    fun items(strings: List<String>) = list.items(strings) { text ->
        SectionCard {
            Text(text)
        }
    }
}

@Composable
fun SectionCard(
    title: String? = null,
    subtitle: String? = null,
    content: @Composable () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            title?.let {
                Text(it, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
        }
    }
}

@Composable
fun ActionButtonRow(primary: String, secondary: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text(primary) }
        secondary?.let { OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text(it) } }
    }
}

@Composable
fun ReadOnlyField(label: String, value: String = "") {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        enabled = false,
    )
}
