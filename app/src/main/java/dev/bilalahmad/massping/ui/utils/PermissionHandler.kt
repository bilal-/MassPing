package dev.bilalahmad.massping.ui.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun rememberPermissionHandler(
    onPermissionsGranted: () -> Unit,
    onPermissionsDenied: (List<String>) -> Unit = {}
): PermissionHandler {
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        Log.d("PermissionHandler", "Permission result: $permissions")
        val deniedPermissions = permissions.filterValues { !it }.keys.toList()
        if (deniedPermissions.isEmpty()) {
            Log.d("PermissionHandler", "All permissions granted, calling onPermissionsGranted")
            onPermissionsGranted()
        } else {
            Log.d("PermissionHandler", "Some permissions denied: $deniedPermissions")
            onPermissionsDenied(deniedPermissions)
        }
    }

    return remember {
        PermissionHandler(context) { permissions ->
            permissionLauncher.launch(permissions)
        }
    }
}

class PermissionHandler(
    private val context: Context,
    private val launcher: (Array<String>) -> Unit
) {

    companion object {
        // Essential permissions required for core functionality
        val ESSENTIAL_PERMISSIONS = arrayOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_PHONE_STATE
        )

        // Optional permissions that enhance user experience
        val OPTIONAL_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyArray()
        }

        // Runtime permissions that need to be requested
        val RUNTIME_PERMISSIONS = ESSENTIAL_PERMISSIONS + OPTIONAL_PERMISSIONS

        // All permissions for display purposes
        val ALL_PERMISSIONS = RUNTIME_PERMISSIONS + arrayOf(
            Manifest.permission.FOREGROUND_SERVICE
        )
    }

    fun checkAndRequestPermissions() {
        val missingRuntimePermissions = RUNTIME_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingRuntimePermissions.isNotEmpty()) {
            launcher(missingRuntimePermissions.toTypedArray())
        }
    }

    fun hasEssentialPermissions(): Boolean {
        return ESSENTIAL_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasAllPermissions(): Boolean {
        // Only check essential permissions for app functionality
        // Notifications are optional and shouldn't block app usage
        return ESSENTIAL_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun getMissingPermissions(): List<String> {
        return ALL_PERMISSIONS.filter {
            if (it == Manifest.permission.FOREGROUND_SERVICE) {
                // FOREGROUND_SERVICE is always granted via manifest
                false
            } else {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }
        }
    }

    fun getPermissionExplanation(permission: String): String {
        return when (permission) {
            Manifest.permission.SEND_SMS ->
                "SMS permission is needed to send personalized messages to your contacts."
            Manifest.permission.READ_CONTACTS ->
                "Contacts permission is needed to access your contact list for messaging."
            Manifest.permission.READ_PHONE_STATE ->
                "Phone state permission is needed to track message delivery status."
            Manifest.permission.FOREGROUND_SERVICE ->
                "Foreground service permission is needed to send messages in the background."
            Manifest.permission.POST_NOTIFICATIONS ->
                "Notification permission is optional - shows sending progress and delivery updates."
            else -> "This permission is required for the app to function properly."
        }
    }
}