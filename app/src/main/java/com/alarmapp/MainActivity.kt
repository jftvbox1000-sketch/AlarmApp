package com.alarmapp

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.alarmapp.ui.navigation.AlarmNavHost
import com.alarmapp.ui.theme.AlarmAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AlarmAppTheme {
                var showFullScreenDialog by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= 34 &&
                        checkSelfPermission(Manifest.permission.USE_FULL_SCREEN_INTENT) != PackageManager.PERMISSION_GRANTED) {
                        showFullScreenDialog = true
                    }
                }

                AlarmNavHost()

                if (showFullScreenDialog) {
                    FullScreenPermissionDialog(
                        onOpenSettings = {
                            val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                                data = Uri.parse("package:$packageName")
                            }
                            startActivity(intent)
                            showFullScreenDialog = false
                        },
                        onDismiss = { showFullScreenDialog = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun FullScreenPermissionDialog(onOpenSettings: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Full Screen Permission Needed") },
        text = {
            Text("AlarmApp needs full-screen permission to show the alarm screen even when your phone is locked. Please grant this permission in Settings.")
        },
        confirmButton = {
            TextButton(onClick = onOpenSettings) { Text("Open Settings") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Later") }
        }
    )
}
