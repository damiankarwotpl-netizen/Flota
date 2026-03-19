package com.future.ultimate.core.common.patch

import android.content.Context
import android.util.Log
import java.io.File

object PatchLoader {
    private const val TAG = "PatchLoader"

    fun safeExternalDir(
        context: Context,
        preferredType: String? = null,
        subDirectory: String? = null,
        feature: String,
    ): File {
        val baseDir = context.getExternalFilesDir(preferredType) ?: context.filesDir.also {
            Log.w(TAG, "Fallback to internal filesDir for feature=$feature")
        }
        return if (subDirectory.isNullOrBlank()) {
            baseDir
        } else {
            File(baseDir, subDirectory).apply { mkdirs() }
        }
    }

    fun fallbackImportMessage(moduleName: String): String {
        Log.w(TAG, "Fallback import used for module=$moduleName")
        return "PatchLoader: import dla modułu \"$moduleName\" nie jest jeszcze gotowy, używany jest bezpieczny tryb lokalny."
    }

    fun fallbackMailMessage(totalRecipients: Int, attachmentCount: Int): String {
        Log.w(TAG, "Fallback mail pipeline used; recipients=$totalRecipients attachments=$attachmentCount")
        return "PatchLoader: SMTP/API jest zastąpione lokalnym fallbackiem (${totalRecipients} odbiorców, ${attachmentCount} załączników)."
    }

    fun fallbackUserLabel(): String {
        Log.w(TAG, "Fallback user label requested")
        return "Lokalny użytkownik"
    }
}
