package com.liquiddebloat.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Liquid-white palette: near-white frosted glass over a cool pastel field. */
object GlassPalette {
    val bgTop = Color(0xFFF9FBFF)
    val bgMid = Color(0xFFEEF3FB)
    val bgBottom = Color(0xFFE3EBF7)

    val blobSky = Color(0xFF9CC9FF)
    val blobMint = Color(0xFFA6EFDD)
    val blobPeach = Color(0xFFFBD9A5)

    val accent = Color(0xFF0B7DC4)
    val accentSoft = Color(0xFF5CBDF3)
    val accentDeep = Color(0xFF06689F)

    val textPrimary = Color(0xFF0B1524)
    val textSecondary = Color(0xFF546579)
    val textTertiary = Color(0xFF8A99AE)

    val danger = Color(0xFFC0362C)
    val success = Color(0xFF12795F)

    val hairline = Color(0xFFD6E0EE)
}

@Composable
fun LiquidBackground(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        // Base wash: cool near-white, slightly deeper at the bottom.
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFBFDFF), Color(0xFFEFF4FC), Color(0xFFE4ECF8)),
                startY = 0f,
                endY = h,
            )
        )
        // Soft colour glows. Radial gradients are smooth on every API level and
        // avoid the hard rectangular blocks that blur() produced on older devices.
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFA9CEFF).copy(alpha = 0.55f), Color.Transparent),
                center = Offset(w * 0.92f, h * 0.05f),
                radius = w * 0.95f,
            )
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFB6F3E4).copy(alpha = 0.40f), Color.Transparent),
                center = Offset(w * 0.02f, h * 0.42f),
                radius = w * 0.85f,
            )
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFCE2BA).copy(alpha = 0.34f), Color.Transparent),
                center = Offset(w * 0.88f, h * 0.92f),
                radius = w * 0.75f,
            )
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.75f), Color.Transparent),
                center = Offset(w * 0.18f, h * 0.98f),
                radius = w * 0.7f,
            )
        )
    }
}

/**
 * Frosted white panel: soft shadow for lift, translucent white fill so the
 * pastel blobs read through, and a white rim that catches light top-left.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    corner: Dp = 24.dp,
    shape: Shape = RoundedCornerShape(corner),
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier
            .shadow(
                elevation = 16.dp,
                shape = shape,
                clip = false,
                ambientColor = Color(0x33164A8A),
                spotColor = Color(0x2A164A8A),
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.86f),
                        Color.White.copy(alpha = 0.52f),
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White,
                        Color.White.copy(alpha = 0.55f),
                        GlassPalette.hairline.copy(alpha = 0.65f),
                        Color.White,
                    )
                ),
                shape = shape,
            )
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(size.width * 0.16f, 0f),
                    radius = size.width * 0.8f,
                ),
            )
        }
        content()
    }
}

@Composable
fun GlassPillButton(
    text: String,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    accent: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 600f),
        label = "pill-scale",
    )
    val shape = RoundedCornerShape(100)
    val ink = if (accent) Color.White else GlassPalette.textPrimary
    Row(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.45f)
            .scale(scale)
            .shadow(
                elevation = if (accent) 12.dp else 6.dp,
                shape = shape,
                clip = false,
                ambientColor = if (accent) Color(0x552B8BC4) else Color(0x22164A8A),
                spotColor = if (accent) Color(0x442B8BC4) else Color(0x1E164A8A),
            )
            .clip(shape)
            .background(
                if (accent) {
                    Brush.horizontalGradient(listOf(Color(0xFF3FB4EE), Color(0xFF0B7DC4)))
                } else {
                    Brush.horizontalGradient(
                        listOf(Color.White.copy(alpha = 0.92f), Color.White.copy(alpha = 0.70f))
                    )
                }
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.White, Color.White.copy(alpha = if (accent) 0.35f else 0.45f))
                ),
                shape = shape,
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 18.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ink,
                modifier = Modifier.size(16.dp),
            )
            Box(Modifier.width(7.dp))
        }
        Text(text = text, color = ink, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    tint: Color = GlassPalette.textPrimary,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, label = "icon-scale")

    Box(
        modifier
            .size(38.dp)
            .scale(scale)
            .shadow(6.dp, CircleShape, clip = false, ambientColor = Color(0x22164A8A))
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.78f))
            .border(1.dp, Color.White, CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(19.dp))
    }
}

@Composable
fun GlassToggle(
    checked: Boolean,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit,
) {
    val knobOffset by animateDpAsState(
        targetValue = if (checked) 24.dp else 3.dp,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
        label = "toggle-knob",
    )
    val trackShape = RoundedCornerShape(100)
    Box(
        modifier
            .size(50.dp, 27.dp)
            .clip(trackShape)
            .background(
                if (checked) {
                    Brush.horizontalGradient(listOf(Color(0xFF55C0F2), Color(0xFF0B7DC4)))
                } else {
                    Brush.horizontalGradient(listOf(Color(0xFFE2E9F4), Color(0xFFEFF4FA)))
                }
            )
            .border(1.dp, if (checked) Color.White.copy(alpha = 0.6f) else GlassPalette.hairline, trackShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onCheckedChange(!checked) },
    ) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = knobOffset)
                .size(21.dp)
                .shadow(4.dp, CircleShape, clip = false, ambientColor = Color(0x33164A8A))
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = GlassPalette.textTertiary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier.padding(start = 6.dp, bottom = 8.dp),
    )
}

@Composable
fun GlassDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, GlassPalette.hairline.copy(alpha = 0.85f), Color.Transparent)
                )
            )
    )
}

/** Small frosted tag for 系统 / 已卸载 / 受保护. */
@Composable
fun GlassTag(text: String, tint: Color, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(100)
    Box(
        modifier
            .clip(shape)
            .background(tint.copy(alpha = 0.10f))
            .border(1.dp, tint.copy(alpha = 0.28f), shape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text = text, color = tint, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}
