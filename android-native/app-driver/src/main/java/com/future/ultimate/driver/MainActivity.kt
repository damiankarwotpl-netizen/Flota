package com.future.ultimate.driver

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import com.future.ultimate.driver.ui.DriverRoot

class MainActivity : ComponentActivity() {
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestPermissionsOnFirstStart()
        setContent { DriverRoot() }
    }

    private fun requestPermissionsOnFirstStart() {
        val prefs = getSharedPreferences("driver_permissions", MODE_PRIVATE)
        if (prefs.getBoolean("requested_once", false)) return
        val wantedPermissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.POST_NOTIFICATIONS,
        ).filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }
        if (wantedPermissions.isNotEmpty()) {
            permissionLauncher.launch(wantedPermissions.toTypedArray())
        }
        prefs.edit().putBoolean("requested_once", true).apply()
    }
}
