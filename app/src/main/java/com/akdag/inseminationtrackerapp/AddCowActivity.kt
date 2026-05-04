package com.akdag.inseminationtrackerapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.akdag.inseminationtrackerapp.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddCowActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InseminationTrackerTheme {
                AddCowScreen(
                    onBack = { finish() },
                    onSaved = { setResult(RESULT_OK); finish() }
                )
            }
        }
    }
}

@Composable
fun AddCowScreen(onBack: () -> Unit, onSaved: () -> Unit) {
    val db      = remember { FirebaseFirestore.getInstance() }
    val auth    = remember { FirebaseAuth.getInstance() }
    val uid     = auth.currentUser?.uid ?: ""
    val context = LocalContext.current

    val isPremium by PremiumManager.isPremium.collectAsState()

    var name             by remember { mutableStateOf("") }
    var earTag           by remember { mutableStateOf("") }
    var error            by remember { mutableStateOf("") }
    var loading          by remember { mutableStateOf(false) }
    var saved            by remember { mutableStateOf(false) }
    var showPremiumGate  by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Bg0)) {
        AppTopBar("İnek Ekle", onBack = onBack)

        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FormTextField(label = "İSİM (OPSİYONEL)", value = name,
                onValueChange = { name = it }, placeholder = "Sarı, Karabaş, Beyaz…")
            FormTextField(label = "KÜPE NUMARASI", value = earTag,
                onValueChange = { earTag = it }, placeholder = "TR-1234")

            if (error.isNotEmpty()) Text(error, color = RedAccent, fontSize = 12.sp)

            if (saved) {
                Box(
                    Modifier.fillMaxWidth().background(StatusGebeBg, RoundedCornerShape(14.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) { Text("✓ Kaydedildi!", color = GreenPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            } else {
                PrimaryButton(
                    text = if (loading) "Kaydediliyor…" else "Kaydet",
                    enabled = !loading,
                    onClick = {
                        if (earTag.isBlank()) { error = "Küpe numarası gereklidir."; return@PrimaryButton }
                        loading = true; error = ""

                        // 1. Önce toplam inek sayısını kontrol et
                        db.collection("Cows").whereEqualTo("user_id", uid).get()
                            .addOnSuccessListener { countSnap ->
                                val cowCount = countSnap.size()
                                if (!isPremium && cowCount >= PremiumManager.FREE_COW_LIMIT) {
                                    loading = false
                                    showPremiumGate = true
                                    return@addOnSuccessListener
                                }

                                // 2. Mükerrer küpe kontrolü
                                db.collection("Cows")
                                    .whereEqualTo("user_id", uid)
                                    .whereEqualTo("ear_tag", earTag.trim())
                                    .get()
                                    .addOnSuccessListener { snap ->
                                        if (!snap.isEmpty) {
                                            error = "Bu küpe numarası zaten kayıtlı."
                                            loading = false
                                            return@addOnSuccessListener
                                        }
                                        // 3. Kaydet
                                        db.collection("Cows").add(
                                            mapOf(
                                                "user_id"              to uid,
                                                "ear_tag"              to earTag.trim(),
                                                "name"                 to name.trim(),
                                                "is_pregnant"          to false,
                                                "drying_off_date"      to null,
                                                "insemination_records" to emptyList<Any>(),
                                                "vaccinations"         to emptyList<Any>()
                                            )
                                        ).addOnSuccessListener {
                                            loading = false; saved = true
                                            android.os.Handler(android.os.Looper.getMainLooper())
                                                .postDelayed({ onSaved() }, 800)
                                        }.addOnFailureListener {
                                            loading = false; error = "Kayıt başarısız."
                                        }
                                    }
                                    .addOnFailureListener { loading = false; error = "Bağlantı hatası." }
                            }
                            .addOnFailureListener { loading = false; error = "Bağlantı hatası." }
                    }
                )
            }
        }
    }

    // Premium kapısı dialog'u
    if (showPremiumGate) {
        Dialog(onDismissRequest = { showPremiumGate = false }) {
            Column(
                Modifier.fillMaxWidth().background(Bg2, RoundedCornerShape(20.dp)).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier.size(68.dp)
                        .background(PremiumGoldDim.copy(alpha = 0.35f), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) { Text("🔒", fontSize = 30.sp) }

                Spacer(Modifier.height(16.dp))
                Text(
                    "Limit Doldu",
                    fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Ücretsiz planda en fazla ${PremiumManager.FREE_COW_LIMIT} inek ekleyebilirsiniz.",
                    fontSize = 14.sp, color = TextMid, textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(20.dp))

                listOf(
                    "Sınırsız inek ekle",
                    "Akıllı bildirimler",
                    "Gelişmiş istatistikler"
                ).forEach { feature ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Box(
                            Modifier.size(22.dp)
                                .background(PremiumGoldDim.copy(alpha = 0.35f), RoundedCornerShape(7.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Check, null, tint = PremiumGold, modifier = Modifier.size(13.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(feature, fontSize = 14.sp, color = TextPrimary)
                    }
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        showPremiumGate = false
                        context.startActivity(Intent(context, PremiumActivity::class.java))
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PremiumGold,
                        contentColor = Color(0xFF1A1000)
                    )
                ) { Text("👑  Premium'a Geç", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp) }

                Spacer(Modifier.height(6.dp))
                TextButton(
                    onClick = { showPremiumGate = false },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Şimdi Değil", color = TextDim, fontSize = 14.sp) }
            }
        }
    }
}
