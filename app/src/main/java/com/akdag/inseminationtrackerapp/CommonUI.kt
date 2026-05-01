package com.akdag.inseminationtrackerapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akdag.inseminationtrackerapp.ui.theme.*

@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val (color, bg, label) = when (status) {
        "Başarılı" -> Triple(GreenPrimary, StatusGebeBg, "Gebe")
        "Tohumlama Yapıldı" -> Triple(YellowAccent, StatusBeklemeBg, "Beklemede")
        "Başarısız" -> Triple(RedAccent, StatusBasarisizBg, "Başarısız")
        "Doğum Yaptı" -> Triple(BlueAccent, StatusDogumBg, "Doğurdu")
        "Düşük Yaptı" -> Triple(OrangeAccent, StatusDusukBg, "Düşük")
        else -> Triple(TextMid, Bg3, status.ifEmpty { "Kayıtsız" })
    }
    Box(
        modifier = modifier
            .background(bg, RoundedCornerShape(20.dp))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.3.sp)
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
                ) { Text("←", color = TextPrimary, fontSize = 18.sp) }
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
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
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
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
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
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = StatusBasarisizBg, contentColor = RedAccent)
    ) { Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
}
