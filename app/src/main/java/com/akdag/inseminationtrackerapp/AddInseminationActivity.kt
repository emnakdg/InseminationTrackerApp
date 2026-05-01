package com.akdag.inseminationtrackerapp

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akdag.inseminationtrackerapp.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AddInseminationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preselectedCowId = intent.getStringExtra("preselectedCowId")
        setContent {
            InseminationTrackerTheme {
                AddInseminationScreen(
                    preselectedCowId = preselectedCowId,
                    onBack = { finish() },
                    onSaved = { setResult(RESULT_OK); finish() }
                )
            }
        }
    }
}

@Composable
fun AddInseminationScreen(
    preselectedCowId: String?,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    val uid = auth.currentUser?.uid ?: ""
    val context = LocalContext.current

    var availableCows by remember { mutableStateOf<List<CowData>>(emptyList()) }
    var selectedCowId by remember { mutableStateOf(preselectedCowId ?: "") }
    var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    var cowsLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        db.collection("Cows").whereEqualTo("user_id", uid).get()
            .addOnSuccessListener { snap ->
                availableCows = snap.documents.map { it.toCowData() }
                    .filter { cow -> !cow.isPregnant && cow.latestStatus() != "Tohumlama Yapıldı" }
                    .sortedBy { it.earTag }
                if (selectedCowId.isEmpty() && availableCows.isNotEmpty()) {
                    selectedCowId = availableCows[0].id
                }
                cowsLoading = false
            }
            .addOnFailureListener { cowsLoading = false }
    }

    fun formatDisplayDate(isoDate: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            formatDate(sdf.parse(isoDate))
        } catch (e: Exception) { isoDate }
    }

    Column(Modifier.fillMaxSize().background(Bg0)) {
        AppTopBar("Tohumlama Ekle", onBack = onBack)

        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Cow picker
            Column {
                Text("İNEK SEÇ", color = TextMid, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp, modifier = Modifier.padding(bottom = 8.dp))

                if (cowsLoading) {
                    Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = GreenPrimary, modifier = Modifier.size(24.dp))
                    }
                } else if (availableCows.isEmpty()) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(CardColor, RoundedCornerShape(14.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("Tohumlanabilir inek bulunamadı", color = TextDim, fontSize = 13.sp) }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        availableCows.forEach { cow ->
                            val isSelected = selectedCowId == cow.id
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .background(if (isSelected) StatusGebeBg else CardColor, RoundedCornerShape(14.dp))
                                    .border(2.dp, if (isSelected) GreenPrimary else BorderColor, RoundedCornerShape(14.dp))
                                    .clickable { selectedCowId = cow.id }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🐄", fontSize = 22.sp, modifier = Modifier.padding(end = 12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(cow.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                    Text("#${cow.earTag}", color = TextDim, fontSize = 12.sp)
                                }
                                if (isSelected) Text("✓", fontSize = 18.sp, color = GreenPrimary)
                            }
                        }
                    }
                }
            }

            // Date picker
            FormTextField(
                label = "TOHUMLAMA TARİHİ",
                value = formatDisplayDate(date),
                onValueChange = {},
                placeholder = "Tarih seçin",
                readOnly = true,
                onClick = {
                    val cal = Calendar.getInstance()
                    DatePickerDialog(context, { _, y, m, d ->
                        date = "%04d-%02d-%02d".format(y, m + 1, d)
                    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                }
            )

            // Info box
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Bg3, RoundedCornerShape(14.dp))
                    .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Text("ℹ️", fontSize = 18.sp, modifier = Modifier.padding(end = 10.dp, top = 2.dp))
                Text(
                    "Tohumlama başarılı olarak işaretlendiğinde, 195 gün sonra kuruya çıkma bildirimi gönderilecek.",
                    color = TextMid, fontSize = 12.sp, lineHeight = 18.sp
                )
            }

            if (error.isNotEmpty()) Text(error, color = RedAccent, fontSize = 12.sp)

            if (saved) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(StatusGebeBg, RoundedCornerShape(14.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) { Text("✓ Kaydedildi!", color = GreenPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            } else {
                PrimaryButton(
                    text = if (loading) "Kaydediliyor…" else "Kaydet",
                    enabled = !loading && selectedCowId.isNotEmpty() && availableCows.isNotEmpty(),
                    onClick = {
                        if (selectedCowId.isEmpty()) { error = "Lütfen inek seçin."; return@PrimaryButton }
                        loading = true; error = ""

                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val parsedDate = try { sdf.parse(date) ?: Date() } catch (e: Exception) { Date() }
                        val timestamp = com.google.firebase.Timestamp(parsedDate)
                        val newRecord = mapOf("date" to timestamp, "status" to "Tohumlama Yapıldı")

                        db.collection("Cows").document(selectedCowId)
                            .update("insemination_records", FieldValue.arrayUnion(newRecord))
                            .addOnSuccessListener {
                                // Move new record to front by reading+rewriting
                                db.collection("Cows").document(selectedCowId).get()
                                    .addOnSuccessListener { doc ->
                                        @Suppress("UNCHECKED_CAST")
                                        val records = (doc.get("insemination_records") as? List<Map<String, Any>> ?: emptyList())
                                            .sortedByDescending { (it["date"] as? com.google.firebase.Timestamp)?.seconds ?: 0L }
                                        db.collection("Cows").document(selectedCowId)
                                            .update("insemination_records", records)
                                            .addOnCompleteListener {
                                                loading = false; saved = true
                                                android.os.Handler(android.os.Looper.getMainLooper())
                                                    .postDelayed({ onSaved() }, 800)
                                            }
                                    }
                            }
                            .addOnFailureListener { loading = false; error = "Kayıt başarısız." }
                    }
                )
            }
        }
    }
}
