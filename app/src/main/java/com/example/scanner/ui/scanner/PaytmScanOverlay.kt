package com.example.scanner.ui.scanner

import android.graphics.RectF
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scanner.scanner.ScanContentType
import com.example.scanner.theme.PaytmCyan
import com.example.scanner.theme.PaytmGreen
import kotlin.math.roundToInt

data class CaptureAnimationState(
    val normalizedBounds: RectF?,
    val contentType: ScanContentType,
    val label: String,
    val triggerId: Long,
)

@Composable
fun PaytmScanOverlay(
    captureState: CaptureAnimationState?,
    tapFocusPoint: Offset?,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        // Matches Xiaomi Scanner's upper-center square framing, responsive on both phones and tablets
        val idleBoxSizePx = minOf(widthPx * 0.68f, with(density) { 240.dp.toPx() })
        val idleLeft = (widthPx - idleBoxSizePx) / 2f
        val idleTop = maxOf(with(density) { 110.dp.toPx() }, heightPx * 0.22f)
        val idleRect = Rect(
            left = idleLeft,
            top = idleTop,
            right = idleLeft + idleBoxSizePx,
            bottom = idleTop + idleBoxSizePx,
        )

        val isCaptured = captureState != null
        val isUpi = captureState?.contentType == ScanContentType.UPI

        val targetRect = remember(captureState, widthPx, heightPx) {
            val norm = captureState?.normalizedBounds
            if (norm == null) {
                idleRect
            } else {
                val pad = with(density) { 12.dp.toPx() }
                val minSide = with(density) { 96.dp.toPx() }
                var l = norm.left * widthPx - pad
                var t = norm.top * heightPx - pad
                var r = norm.right * widthPx + pad
                var b = norm.bottom * heightPx + pad
                if (r - l < minSide) {
                    val cx = (l + r) / 2f
                    l = cx - minSide / 2f
                    r = cx + minSide / 2f
                }
                if (b - t < minSide) {
                    val cy = (t + b) / 2f
                    t = cy - minSide / 2f
                    b = cy + minSide / 2f
                }
                Rect(
                    left = l.coerceIn(16f, widthPx - 32f),
                    top = t.coerceIn(16f, heightPx - 32f),
                    right = r.coerceIn(32f, widthPx - 16f),
                    bottom = b.coerceIn(32f, heightPx - 16f),
                )
            }
        }

        // Smooth spring snap of the 4 brackets onto the scanned QR box
        val animSpec = spring<Float>(dampingRatio = 0.68f, stiffness = 460f)
        val curLeft by animateFloatAsState(if (isCaptured) targetRect.left else idleRect.left, animSpec, label = "l")
        val curTop by animateFloatAsState(if (isCaptured) targetRect.top else idleRect.top, animSpec, label = "t")
        val curRight by animateFloatAsState(if (isCaptured) targetRect.right else idleRect.right, animSpec, label = "r")
        val curBottom by animateFloatAsState(if (isCaptured) targetRect.bottom else idleRect.bottom, animSpec, label = "b")

        val bracketColor by animateColorAsState(
            targetValue = when {
                !isCaptured -> Color.White
                isUpi -> PaytmCyan
                else -> PaytmGreen
            },
            animationSpec = tween(220),
            label = "bracketColor",
        )

        val scrimAlpha by animateFloatAsState(
            targetValue = if (isCaptured) 0.56f else 0.34f,
            animationSpec = tween(220),
            label = "scrimAlpha",
        )

        // Paytm success badge & ripple animation timeline
        val badgeScale = remember { Animatable(0f) }
        val checkTrim = remember { Animatable(0f) }
        val rippleProgress = remember { Animatable(0f) }
        val burstFlash = remember { Animatable(0f) }

        LaunchedEffect(captureState?.triggerId) {
            if (captureState != null) {
                badgeScale.snapTo(0f)
                checkTrim.snapTo(0f)
                rippleProgress.snapTo(0f)
                burstFlash.snapTo(1f)

                // Run Paytm-style lock flash + ripple + spring badge + checkmark stroke
                burstFlash.animateTo(0f, tween(260, easing = FastOutSlowInEasing))
            } else {
                badgeScale.snapTo(0f)
                checkTrim.snapTo(0f)
                rippleProgress.snapTo(0f)
                burstFlash.snapTo(0f)
            }
        }

        LaunchedEffect(captureState?.triggerId) {
            if (captureState != null) {
                badgeScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                )
            }
        }

        LaunchedEffect(captureState?.triggerId) {
            if (captureState != null) {
                kotlinx.coroutines.delay(90)
                checkTrim.animateTo(1f, tween(210, easing = FastOutSlowInEasing))
            }
        }

        LaunchedEffect(captureState?.triggerId) {
            if (captureState != null) {
                rippleProgress.animateTo(1f, tween(480, easing = FastOutSlowInEasing))
            }
        }

        // Continuous idle laser beam sweep
        val infiniteTransition = rememberInfiniteTransition(label = "scanBeam")
        val beamProgress by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1900, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "beamProgress",
        )

        // Tap-to-focus ring pulse
        val focusRingAlpha = remember { Animatable(0f) }
        val focusRingScale = remember { Animatable(1.35f) }
        LaunchedEffect(tapFocusPoint) {
            if (tapFocusPoint != null) {
                focusRingAlpha.snapTo(1f)
                focusRingScale.snapTo(1.35f)
                focusRingScale.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
                kotlinx.coroutines.delay(450)
                focusRingAlpha.animateTo(0f, tween(250))
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val activeRect = Rect(curLeft, curTop, curRight, curBottom)

            // Dark translucent scrim outside active viewfinder box
            val scrimPath = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(Offset.Zero, size))
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        rect = activeRect,
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    ),
                )
            }
            drawPath(scrimPath, color = Color.Black.copy(alpha = scrimAlpha))

            // Idle sweeping scan line (mirrors Xiaomi Scanner's glowing horizontal white beam)
            if (!isCaptured) {
                val padX = 6.dp.toPx()
                val travelH = (activeRect.height - 20.dp.toPx()).coerceAtLeast(10f)
                val lineY = activeRect.top + 10.dp.toPx() + travelH * beamProgress
                val beamLeft = activeRect.left + padX
                val beamRight = activeRect.right - padX

                // Soft glow trail above the beam
                val trailH = 28.dp.toPx()
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.24f)),
                        startY = (lineY - trailH).coerceAtLeast(activeRect.top),
                        endY = lineY,
                    ),
                    topLeft = Offset(beamLeft, (lineY - trailH).coerceAtLeast(activeRect.top)),
                    size = Size(beamRight - beamLeft, (lineY - (lineY - trailH).coerceAtLeast(activeRect.top))),
                )

                // Bright tapered laser line
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.95f),
                            Color.White,
                            Color.White.copy(alpha = 0.95f),
                            Color.Transparent,
                        ),
                        startX = beamLeft,
                        endX = beamRight,
                    ),
                    start = Offset(beamLeft, lineY),
                    end = Offset(beamRight, lineY),
                    strokeWidth = 2.6.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            // Capture flash highlight inside locked QR box
            if (isCaptured && burstFlash.value > 0f) {
                val accent = if (isUpi) PaytmCyan else PaytmGreen
                drawRect(
                    color = accent.copy(alpha = 0.28f * burstFlash.value),
                    topLeft = activeRect.topLeft,
                    size = activeRect.size,
                )
            }

            // Corner brackets (white when idle, Paytm cyan/green when locked)
            drawCornerBrackets(
                rect = activeRect,
                color = bracketColor,
                strokeWidthPx = 2.8.dp.toPx(),
                armLengthPx = 19.dp.toPx(),
            )

            // Paytm-style expanding ripple + verified checkmark badge at center of scanned QR
            if (isCaptured) {
                val center = activeRect.center
                val accentColor = if (isUpi) PaytmCyan else PaytmGreen

                if (rippleProgress.value > 0f && rippleProgress.value < 1f) {
                    val maxRadius = maxOf(activeRect.width, activeRect.height) * 0.72f
                    val radius = 20.dp.toPx() + maxRadius * rippleProgress.value
                    drawCircle(
                        color = accentColor.copy(alpha = (1f - rippleProgress.value) * 0.55f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 3.dp.toPx()),
                    )
                }

                val badgeR = 28.dp.toPx() * badgeScale.value
                if (badgeR > 1f) {
                    // Subtle outer white halo
                    drawCircle(
                        color = Color.White.copy(alpha = 0.25f),
                        radius = badgeR + 4.dp.toPx(),
                        center = center,
                    )
                    // Primary badge circle
                    drawCircle(
                        color = accentColor,
                        radius = badgeR,
                        center = center,
                    )

                    // Animated checkmark stroke
                    if (checkTrim.value > 0f) {
                        val checkPath = Path().apply {
                            moveTo(center.x - badgeR * 0.40f, center.y + badgeR * 0.03f)
                            lineTo(center.x - badgeR * 0.10f, center.y + badgeR * 0.34f)
                            lineTo(center.x + badgeR * 0.44f, center.y - badgeR * 0.28f)
                        }
                        val pm = PathMeasure()
                        pm.setPath(checkPath, false)
                        val partialPath = Path()
                        pm.getSegment(0f, pm.length * checkTrim.value, partialPath, true)
                        drawPath(
                            path = partialPath,
                            color = Color.White,
                            style = Stroke(
                                width = 3.8.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                            ),
                        )
                    }
                }
            }

            // Tap-to-focus ring
            if (tapFocusPoint != null && focusRingAlpha.value > 0f) {
                drawCircle(
                    color = Color.White.copy(alpha = focusRingAlpha.value * 0.85f),
                    radius = 28.dp.toPx() * focusRingScale.value,
                    center = tapFocusPoint,
                    style = Stroke(
                        width = 1.8.dp.toPx(),
                        pathEffect = PathEffect.cornerPathEffect(4f),
                    ),
                )
            }
        }

        // Tip text directly below the idle frame — exact match to Xiaomi Scanner
        AnimatedVisibility(
            visible = !isCaptured,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(120)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset {
                    IntOffset(
                        x = 0,
                        y = (idleRect.bottom + with(density) { 14.dp.toPx() }).roundToInt(),
                    )
                },
        ) {
            Text(
                text = "Place the code inside the frame",
                color = Color.White.copy(alpha = 0.92f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
            )
        }

        // Brief Paytm-style status pill beneath locked QR badge
        AnimatedVisibility(
            visible = isCaptured,
            enter = fadeIn(tween(160)),
            exit = fadeOut(tween(120)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset {
                    IntOffset(
                        x = 0,
                        y = (curBottom + with(density) { 12.dp.toPx() }).roundToInt(),
                    )
                },
        ) {
            val pillColor = if (isUpi) PaytmCyan else PaytmGreen
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Text(
                    text = captureState?.label ?: "QR Scanned",
                    color = pillColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

private fun DrawScope.drawCornerBrackets(
    rect: Rect,
    color: Color,
    strokeWidthPx: Float,
    armLengthPx: Float,
) {
    val half = strokeWidthPx / 2f
    val l = rect.left + half
    val t = rect.top + half
    val r = rect.right - half
    val b = rect.bottom - half
    val arm = minOf(armLengthPx, rect.width * 0.35f, rect.height * 0.35f)

    // Top-left
    drawLine(color, Offset(l, t + arm), Offset(l, t), strokeWidthPx, StrokeCap.Square)
    drawLine(color, Offset(l, t), Offset(l + arm, t), strokeWidthPx, StrokeCap.Square)

    // Top-right
    drawLine(color, Offset(r - arm, t), Offset(r, t), strokeWidthPx, StrokeCap.Square)
    drawLine(color, Offset(r, t), Offset(r, t + arm), strokeWidthPx, StrokeCap.Square)

    // Bottom-left
    drawLine(color, Offset(l, b - arm), Offset(l, b), strokeWidthPx, StrokeCap.Square)
    drawLine(color, Offset(l, b), Offset(l + arm, b), strokeWidthPx, StrokeCap.Square)

    // Bottom-right
    drawLine(color, Offset(r - arm, b), Offset(r, b), strokeWidthPx, StrokeCap.Square)
    drawLine(color, Offset(r, b), Offset(r, b - arm), strokeWidthPx, StrokeCap.Square)
}
