package com.fly.pkg.floatwin

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fly.pkg.ForegroundPage
import kotlin.math.roundToInt

private val CardBackground = Color(0x8C141414)
private val CardBorder = Color(0x26FFFFFF)
private val MonoText = Color(0xFF9ECBFF)

@Composable
fun FloatCard(
    page: ForegroundPage?,
    icon: ImageBitmap?,
    onCopy: () -> Unit,
    onDragBy: (Int, Int) -> Unit,
    onDragEnd: () -> Unit,
    maxWidth: Dp,
) {
    Column(
        modifier = Modifier
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onCopy() })
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDragBy(dragAmount.x.roundToInt(), dragAmount.y.roundToInt())
                    },
                    onDragEnd = { onDragEnd() },
                )
            }
            .widthIn(max = maxWidth)
            .clip(RoundedCornerShape(14.dp))
            .background(CardBackground)
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Image(
                    bitmap = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)),
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = page?.appName ?: "等待页面切换…",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (page != null) {
            Spacer(Modifier.height(6.dp))
            Text(page.packageName, color = MonoText, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
            Text(page.fullClassName, color = MonoText, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
        }
    }
}

@Composable
fun FloatDot(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(CardBackground)
            .border(1.dp, CardBorder, CircleShape)
            .clickable(onClick = onClick),
    )
}
