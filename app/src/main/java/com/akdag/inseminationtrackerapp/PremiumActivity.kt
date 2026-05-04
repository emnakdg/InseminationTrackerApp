package com.akdag.inseminationtrackerapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akdag.inseminationtrackerapp.ui.theme.*
import com.android.billingclient.api.ProductDetails

val PremiumGold    = Color(0xFFFFD166)
val PremiumGoldDim = Color(0xFF5C4200)

class PremiumActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InseminationTrackerTheme {
                PremiumScreen(onClose = { finish() })
            }
        }
    }
}

@Composable
fun PremiumScreen(onClose: () -> Unit) {
    val activity = LocalContext.current as? android.app.Activity

    val isPremium      by PremiumManager.isPremium.collectAsState()
    val monthlyDetails by PremiumManager.monthlyDetails.collectAsState()
    val yearlyDetails  by PremiumManager.yearlyDetails.collectAsState()

    var selectedPlan by remember { mutableStateOf("yearly") }

    LaunchedEffect(isPremium) {
        if (isPremium) onClose()
    }

    val features = listOf(
        "Sınırsız inek ekle",
        "Tohumlama & gebelik takibi",
        "Akıllı bildirimler & hatırlatmalar",
        "Gelişmiş istatistikler",
        "Veri yedekleme",
        "Öncelikli destek",
    )

    Column(Modifier.fillMaxSize().background(Bg0)) {

        // Top bar
        Box(
            Modifier.fillMaxWidth().background(Bg1)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.Rounded.Close, contentDescription = "Kapat", tint = TextMid)
            }
            Text(
                "Premium",
                modifier = Modifier.align(Alignment.Center),
                fontSize = 17.sp, fontWeight = FontWeight.Bold, color = PremiumGold
            )
        }
        HorizontalDivider(color = BorderColor)

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(28.dp))

            // Hero
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(80.dp).background(
                        Brush.linearGradient(listOf(PremiumGoldDim, PremiumGold)),
                        RoundedCornerShape(24.dp)
                    ), contentAlignment = Alignment.Center
                ) { Text("👑", fontSize = 38.sp) }
                Spacer(Modifier.height(16.dp))
                Text("İnek Takip Premium", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                Spacer(Modifier.height(6.dp))
                Text("Sürünüzü sınırsız yönetin", fontSize = 14.sp, color = TextMid)
            }

            Spacer(Modifier.height(28.dp))

            // Features
            Column(
                Modifier.fillMaxWidth()
                    .background(CardColor, RoundedCornerShape(16.dp))
                    .border(1.dp, PremiumGoldDim.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                features.forEach { feature ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(24.dp)
                                .background(PremiumGoldDim.copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Check, null, tint = PremiumGold, modifier = Modifier.size(14.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(feature, fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Plan selector
            Text("PLAN SEÇ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMid, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(10.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PlanCard(
                    modifier = Modifier.weight(1f),
                    title = "Yıllık",
                    price = yearlyDetails?.formattedYearlyPrice() ?: "590 ₺",
                    perPeriod = "/ yıl",
                    badge = "%38 tasarruf",
                    selected = selectedPlan == "yearly",
                    onClick = { selectedPlan = "yearly" }
                )
                PlanCard(
                    modifier = Modifier.weight(1f),
                    title = "Aylık",
                    price = monthlyDetails?.formattedMonthlyPrice() ?: "79 ₺",
                    perPeriod = "/ ay",
                    badge = null,
                    selected = selectedPlan == "monthly",
                    onClick = { selectedPlan = "monthly" }
                )
            }

            Spacer(Modifier.height(24.dp))

            // Subscribe button
            val selectedDetails: ProductDetails? =
                if (selectedPlan == "yearly") yearlyDetails else monthlyDetails

            Button(
                onClick = {
                    if (selectedDetails != null && activity != null) {
                        PremiumManager.launchBillingFlow(activity, selectedDetails)
                    }
                },
                enabled = selectedDetails != null,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PremiumGold,
                    contentColor = Color(0xFF1A1000),
                    disabledContainerColor = PremiumGoldDim,
                    disabledContentColor = Color(0xFF8A7000)
                )
            ) {
                Text("👑  Abone Ol", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(12.dp))

            Text(
                "Satın alma işlemlerini geri yükle",
                modifier = Modifier.fillMaxWidth()
                    .clickable { PremiumManager.querySubscriptionStatus() },
                textAlign = TextAlign.Center,
                fontSize = 13.sp, color = TextDim,
                textDecoration = TextDecoration.Underline
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "İstediğiniz zaman iptal edebilirsiniz.\nAbonelik otomatik yenilenir.",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 11.sp, color = TextDim, lineHeight = 16.sp
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
fun PlanCard(
    modifier: Modifier,
    title: String,
    price: String,
    perPeriod: String,
    badge: String?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) PremiumGold else BorderColor,
        label = "planBorder"
    )
    Box(modifier) {
        Column(
            Modifier.fillMaxWidth()
                .background(
                    if (selected) PremiumGoldDim.copy(alpha = 0.18f) else CardColor,
                    RoundedCornerShape(14.dp)
                )
                .border(2.dp, borderColor, RoundedCornerShape(14.dp))
                .clickable { onClick() }
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                color = if (selected) PremiumGold else TextPrimary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                price, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
                color = if (selected) PremiumGold else TextPrimary
            )
            Text(perPeriod, fontSize = 12.sp, color = TextDim)
        }
        if (badge != null) {
            Box(
                Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-8).dp)
                    .background(PremiumGold, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(badge, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1000))
            }
        }
    }
}

private fun ProductDetails.formattedMonthlyPrice(): String =
    subscriptionOfferDetails?.firstOrNull()
        ?.pricingPhases?.pricingPhaseList?.lastOrNull()
        ?.formattedPrice ?: "79 ₺"

private fun ProductDetails.formattedYearlyPrice(): String =
    subscriptionOfferDetails?.firstOrNull()
        ?.pricingPhases?.pricingPhaseList?.lastOrNull()
        ?.formattedPrice ?: "590 ₺"
