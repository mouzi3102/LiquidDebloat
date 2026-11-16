package com.liquiddebloat.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liquiddebloat.model.AppEntry

@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassSurface(modifier = modifier.fillMaxWidth(), corner = 100.dp) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = null,
                tint = GlassPalette.textTertiary,
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(9.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text("搜索应用名或包名", color = GlassPalette.textTertiary, fontSize = 13.sp)
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(color = GlassPalette.textPrimary, fontSize = 13.sp),
                    cursorBrush = SolidColor(GlassPalette.accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (query.isNotEmpty()) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "清空",
                    tint = GlassPalette.textSecondary,
                    modifier = Modifier
                        .size(17.dp)
                        .clickable { onQueryChange("") },
                )
            }
        }
    }
}

@Composable
fun ScopeTabs(
    tabs: List<Pair<String, Int>>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassSurface(modifier = modifier.fillMaxWidth(), corner = 100.dp) {
        Row(Modifier.padding(4.dp)) {
            tabs.forEachIndexed { index, (label, count) ->
                val active = index == selectedIndex
                val shape = RoundedCornerShape(100)
                Row(
                    Modifier
                        .weight(1f)
                        .clip(shape)
                        .background(
                            if (active) {
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    listOf(Color.White, Color.White.copy(alpha = 0.86f))
                                )
                            } else androidx.compose.ui.graphics.SolidColor(Color.Transparent)
                        )
                        .border(1.dp, if (active) Color.White else Color.Transparent, shape)
                        .clickable { onSelect(index) }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        color = if (active) GlassPalette.accentDeep else GlassPalette.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                    )
                    if (count > 0) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = count.toString(),
                            color = if (active) GlassPalette.accent else GlassPalette.textTertiary,
                            fontSize = 10.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
fun AppRow(
    entry: AppEntry,
    protected: Boolean,
    busy: Boolean,
    onAction: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassSurface(modifier = modifier.fillMaxWidth(), corner = 20.dp) {
        Row(
            Modifier.padding(start = 12.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(entry)
            Spacer(Modifier.width(12.dp))

            Column(
                Modifier
                    .weight(1f)
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                        onLongClick = onLongPress,
                    )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.label,
                        color = GlassPalette.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (protected) {
                        Spacer(Modifier.width(6.dp))
                        GlassTag("受保护", GlassPalette.danger)
                    } else if (entry.isUninstalled) {
                        Spacer(Modifier.width(6.dp))
                        GlassTag("已卸载", GlassPalette.success)
                    } else if (entry.isSystem) {
                        Spacer(Modifier.width(6.dp))
                        GlassTag("系统", GlassPalette.accent)
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = entry.packageName,
                    color = GlassPalette.textTertiary,
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.width(8.dp))
            RowActionButton(
                restoring = entry.isUninstalled,
                enabled = !protected && !busy,
                onClick = onAction,
            )
        }
    }
}

/** Round frosted trash (or restore) button sitting at the end of every row. */
@Composable
private fun RowActionButton(
    restoring: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val tint = when {
        !enabled -> GlassPalette.textTertiary
        restoring -> GlassPalette.success
        else -> GlassPalette.danger
    }
    val fill = when {
        !enabled -> GlassPalette.hairline.copy(alpha = 0.35f)
        restoring -> GlassPalette.success.copy(alpha = 0.10f)
        else -> GlassPalette.danger.copy(alpha = 0.10f)
    }
    Box(
        Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(fill)
            .border(1.dp, tint.copy(alpha = if (enabled) 0.26f else 0.15f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .scale(1f),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (restoring) Icons.Rounded.Restore else Icons.Rounded.DeleteOutline,
            contentDescription = if (restoring) "恢复" else "卸载",
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun AppIcon(entry: AppEntry) {
    val shape = RoundedCornerShape(11.dp)
    val icon = entry.icon
    if (icon != null) {
        Image(
            bitmap = icon,
            contentDescription = null,
            modifier = Modifier
                .size(42.dp)
                .clip(shape),
        )
    } else {
        Box(
            Modifier
                .size(42.dp)
                .clip(shape)
                .background(Color.White.copy(alpha = 0.55f))
                .border(1.dp, GlassPalette.hairline, shape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = entry.label.take(1).uppercase(),
                color = GlassPalette.textSecondary,
                fontSize = 15.sp,
            )
        }
    }
}

@Composable
fun AppList(
    apps: List<AppEntry>,
    protectedPackages: Set<String>,
    busyPackage: String?,
    onAction: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit = {},
    modifier: Modifier = Modifier,
    bottomPadding: Int = 32,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(top = 2.dp, bottom = bottomPadding.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(apps, key = { it.packageName }) { entry ->
            AppRow(
                entry = entry,
                protected = entry.packageName in protectedPackages,
                busy = busyPackage == entry.packageName,
                onAction = { onAction(entry) },
                onLongPress = { onLongPress(entry) },
            )
        }
    }
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(top = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.6f))
                .border(1.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = null,
                tint = GlassPalette.textTertiary,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(text, color = GlassPalette.textSecondary, fontSize = 12.sp)
    }
}
