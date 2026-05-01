package com.akdag.inseminationtrackerapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val db = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    val uid = auth.currentUser?.uid ?: ""

    var name by remember { mutableStateOf("") }
    var earTag by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

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
            FormTextField(
                label = "İSİM",
                value = name,
                onValueChange = { name = it },
                placeholder = "Sarı, Karabaş, Beyaz…"
            )
            FormTextField(
                label = "KÜPE NUMARASI",
                value = earTag,
                onValueChange = { earTag = it },
                placeholder = "TR-1234"
            )

            if (error.isNotEmpty()) {
                Text(error, color = RedAccent, fontSize = 12.sp)
            }

            if (saved) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(StatusGebeBg, RoundedCornerShape(14.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✓ Kaydedildi!", color = GreenPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            } else {
                PrimaryButton(
                    text = if (loading) "Kaydediliyor…" else "Kaydet",
                    enabled = !loading,
                    onClick = {
                        if (name.isBlank()) { error = "İsim gereklidir."; return@PrimaryButton }
                        if (earTag.isBlank()) { error = "Küpe numarası gereklidir."; return@PrimaryButton }
                        loading = true; error = ""

                        // Check for duplicate ear tag
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
                                db.collection("Cows").add(
                                    mapOf(
                                        "user_id" to uid,
                                        "ear_tag" to earTag.trim(),
                                        "name" to name.trim(),
                                        "is_pregnant" to false,
                                        "drying_off_date" to null,
                                        "insemination_records" to emptyList<Any>(),
                                        "vaccinations" to emptyList<Any>()
                                    )
                                ).addOnSuccessListener {
                                    loading = false; saved = true
                                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ onSaved() }, 800)
                                }.addOnFailureListener {
                                    loading = false; error = "Kayıt başarısız."
                                }
                            }
                            .addOnFailureListener { loading = false; error = "Bağlantı hatası." }
                    }
                )
            }
        }
    }
}
