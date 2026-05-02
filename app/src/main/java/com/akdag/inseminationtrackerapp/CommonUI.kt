package com.akdag.inseminationtrackerapp

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akdag.inseminationtrackerapp.ui.theme.*

@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val (targetColor, targetBg, label) = when (status) {
        "Başarılı"          -> Triple(GreenPrimary,  StatusGebeBg,      "Gebe")
        "Tohumlama Yapıldı" -> Triple(YellowAccent,  StatusBeklemeBg,   "Beklemede")
        "Başarısız"         -> Triple(RedAccent,     StatusBasarisizBg, "Başarısız")
        "Doğum Yaptı"       -> Triple(BlueAccent,    StatusDogumBg,     "Doğurdu")
        "Düşük Yaptı"       -> Triple(OrangeAccent,  StatusDusukBg,     "Düşük")
        else                -> Triple(TextMid,        Bg3,               status.ifEmpty { "Kayıtsız" })
    }
    val animColor by animateColorAsState(targetValue = targetColor, animationSpec = tween(300), label = "badge_color")
    val animBg    by animateColorAsState(targetValue = targetBg,    animationSpec = tween(300), label = "badge_bg")

    Box(
        modifier = modifier
            .background(animBg, RoundedCornerShape(20.dp))
            .border(1.dp, animColor.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, color = animColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.3.sp)
    }
}

@Composable
fun AppTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    rightContent: @Composable (() -> Unit)? = null
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Bg1)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Bg3, RoundedCornerShape(10.dp))
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "Geri",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
            }
            Text(title, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            rightContent?.invoke()
        }
        HorizontalDivider(color = BorderColor, thickness = 1.dp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Column(modifier = modifier) {
        Text(
            label, color = TextMid, fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Box {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(placeholder, color = TextDim, fontSize = 14.sp) },
                trailingIcon = trailingIcon,
                visualTransformation = visualTransformation,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                readOnly = readOnly || onClick != null,
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenPrimary,
                    unfocusedBorderColor = BorderColor,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = GreenPrimary,
                    focusedContainerColor = Bg3,
                    unfocusedContainerColor = Bg3,
                    disabledBorderColor = BorderColor,
                    disabledTextColor = TextPrimary,
                    disabledContainerColor = Bg3,
                )
            )
            if (onClick != null) {
                Box(
                    Modifier
                        .matchParentSize()
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onClick() }
                )
            }
        }
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val haptic = LocalHapticFeedback.current
    Button(
        onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick() },
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = GreenPrimary, contentColor = Bg0,
            disabledContainerColor = GreenPrimary.copy(alpha = 0.4f), disabledContentColor = Bg0.copy(alpha = 0.5f)
        )
    ) { Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(12.dp),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = androidx.compose.ui.graphics.SolidColor(GreenDim)
        ),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenPrimary)
    ) { Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun DangerButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    Button(
        onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick() },
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = StatusBasarisizBg, contentColor = RedAccent)
    ) { Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
}

// ── SHIMMER / SKELETON ────────────────────────────────────────

@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateX by transition.animateFloat(
        initialValue = -600f,
        targetValue = 1400f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_x"
    )
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Bg2, Bg3, Bg2),
                    start = Offset(translateX, 0f),
                    end = Offset(translateX + 600f, 0f)
                )
            )
    )
}

@Composable
fun ShimmerCowCard(modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(CardColor, RoundedCornerShape(16.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ShimmerBox(Modifier.size(40.dp))
        Spacer(Modifier.height(2.dp))
        ShimmerBox(Modifier.fillMaxWidth(0.65f).height(14.dp))
        ShimmerBox(Modifier.fillMaxWidth(0.4f).height(11.dp))
        Spacer(Modifier.height(2.dp))
        ShimmerBox(Modifier.width(70.dp).height(22.dp))
    }
}
