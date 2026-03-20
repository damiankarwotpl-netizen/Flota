package com.future.ultimate.admin.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Text
import com.future.ultimate.core.common.patch.PatchLoader

@Composable
fun HousingScreen() {
    val context = LocalContext.current
    val workspace = remember {
        PatchLoader.safeExternalDir(
            context = context,
            subDirectory = "mieszkania",
            feature = "mieszkania",
        ).absolutePath
    }
    val fallbackMessage = remember { PatchLoader.fallbackImportMessage("Mieszkania") }

    ScreenColumn("Mieszkania", "Placeholder modułu przygotowany pod dalszy rozwój.") {
        item {
            SectionCard(
                title = "Stan modułu",
                subtitle = "Na ten etap moduł używa bezpiecznego fallbacku PatchLoadera.",
            ) {
                Text(fallbackMessage)
                Text("Katalog roboczy: $workspace")
            }
        }
    }
}
