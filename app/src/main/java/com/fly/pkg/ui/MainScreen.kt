package com.fly.pkg.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MainScreen(
    floatEnabled: Boolean,
    overlayGranted: Boolean,
    a11yEnabled: Boolean,
    serviceRunning: Boolean,
    onToggleFloat: (Boolean) -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenA11ySettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("悬浮窗总开关", style = MaterialTheme.typography.titleMedium)
            Switch(checked = floatEnabled, onCheckedChange = onToggleFloat)
        }

        Spacer(Modifier.height(24.dp))

        PermissionCard(
            title = "悬浮窗权限",
            granted = overlayGranted,
            highlight = floatEnabled && !overlayGranted,
            onOpenSettings = onOpenOverlaySettings,
        )

        Spacer(Modifier.height(12.dp))

        PermissionCard(
            title = "无障碍服务",
            granted = a11yEnabled,
            highlight = floatEnabled && !a11yEnabled,
            onOpenSettings = onOpenA11ySettings,
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = if (serviceRunning) "检测服务运行中" else "检测服务未运行（请在无障碍设置中开启）",
            color = if (serviceRunning) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun PermissionCard(
    title: String,
    granted: Boolean,
    highlight: Boolean,
    onOpenSettings: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = if (granted) "已开启" else "未开启",
                    color = when {
                        granted -> MaterialTheme.colorScheme.primary
                        highlight -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            if (!granted) {
                Button(onClick = onOpenSettings) { Text("去开启") }
            }
        }
    }
}
