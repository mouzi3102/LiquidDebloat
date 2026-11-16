package com.liquiddebloat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liquiddebloat.data.SettingsStore

@Composable
fun SettingsScreen(
    settings: SettingsStore,
    protectedCount: Int,
    backupRoot: String,
    versionLabel: String,
    onOpenBackupFolder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {

        SectionLabel("行为")
        GlassSurface(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(vertical = 6.dp)) {
                SettingToggleRow(
                    icon = Icons.Rounded.Backup,
                    title = "卸载前自动备份 APK",
                    subtitle = "把原 APK 复制到 $backupRoot/<包名>/base.apk",
                    checked = settings.backupEnabled,
                    onCheckedChange = settings::updateBackup,
                )
                GlassDivider(Modifier.padding(horizontal = 16.dp))
                SettingToggleRow(
                    icon = Icons.Rounded.WarningAmber,
                    title = "彻底删除模式",
                    subtitle = "不只从当前用户卸载，还会尝试删除只读分区里的 APK 目录。不可逆",
                    checked = settings.strongMode,
                    tint = GlassPalette.danger,
                    onCheckedChange = settings::updateStrong,
                )
            }
        }

        Spacer(Modifier.height(22.dp))

        SectionLabel("安全")
        GlassSurface(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircleIcon(Icons.Rounded.Security, GlassPalette.success)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "受保护应用白名单",
                            color = GlassPalette.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "内置 $protectedCount 个关键包（系统界面、设置、电话、权限控制器等），" +
                                "无论怎么勾选都不会被卸载。",
                            color = GlassPalette.textSecondary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        SectionLabel("备份位置")
        GlassSurface(Modifier.fillMaxWidth()) {
            Column {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenBackupFolder)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircleIcon(Icons.Rounded.FolderOpen, GlassPalette.accent)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            backupRoot,
                            color = GlassPalette.accentDeep,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "点击用文件管理器打开这个目录",
                            color = GlassPalette.textTertiary,
                            fontSize = 10.sp,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = "打开",
                        tint = GlassPalette.textTertiary,
                        modifier = Modifier.size(18.dp),
                    )
                }
                GlassDivider(Modifier.padding(horizontal = 16.dp))
                Text(
                    "把备份拷走或删掉都可以。想恢复某个应用时，可以在「已卸载」页点恢复，" +
                        "也可以手动 pm install -r base.apk。",
                    color = GlassPalette.textSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        Spacer(Modifier.height(22.dp))

        SectionLabel("关于")
        GlassSurface(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircleIcon(Icons.Rounded.Info, GlassPalette.accent)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "小卸载 $versionLabel",
                            color = GlassPalette.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "界面设计参考 LiquidGlass-Cpp / AndroidLiquidGlass（Kyant），" +
                                "Apache-2.0。本应用只在本机执行 pm 与文件操作，不联网、不上传任何数据。",
                            color = GlassPalette.textSecondary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "卸载系统应用有风险：个别组件被移除后会导致无法开机或 OTA 失败。" +
                "请务必保留备份，并确认自己知道怎么用 Recovery 恢复。",
            color = GlassPalette.textTertiary,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        Spacer(Modifier.height(120.dp))
    }
}

@Composable
private fun SettingToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    tint: Color = GlassPalette.accent,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIcon(icon, tint)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = GlassPalette.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = GlassPalette.textSecondary, fontSize = 11.sp, lineHeight = 15.sp)
        }
        Spacer(Modifier.width(12.dp))
        GlassToggle(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun CircleIcon(icon: ImageVector, tint: Color) {
    val shape = CircleShape
    Box(
        Modifier
            .size(34.dp)
            .clip(shape)
            .background(tint.copy(alpha = 0.14f))
            .border(1.dp, tint.copy(alpha = 0.22f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(17.dp))
    }
}
