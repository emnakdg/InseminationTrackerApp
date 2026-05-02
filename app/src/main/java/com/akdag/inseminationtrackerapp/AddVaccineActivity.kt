package com.akdag.inseminationtrackerapp

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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

class AddVaccineActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preselectedCowId = intent.getStringExtra("preselectedCowId")
        setContent {
            InseminationTrackerTheme {
                AddVaccineScreen(
                    preselectedCowId = preselectedCowId,
                    onBack = { finish() },
                    onSaved = { setResult(RESULT_OK); finish() }
                )
            }
        }
    }
}

@Composable
fun AddVaccineScreen(
    preselectedCowId: String?,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    val uid = auth.currentUser?.uid ?: ""
    val context = LocalContext.current

    val commonVaccines = listOf(
        // Viral
        "Şap", "BVD", "IBR", "PI3", "BRSV", "LSD (Nodüler)",
        "Rotavirus", "Coronavirus",
        // Bakteriyel
        "Brucella", "Leptospirozis", "Tetanoz", "Pastöröl",
        "Şarbon", "Kara Şarbon", "Salmonella", "Kolibakilloz", "Clostridial",
        // Paraziter
        "Theileria", "Anaplazmosis"
    )

    var allCows by remember { mutableStateOf<List<CowData>>(emptyList()) }
    var selectedCowId by remember { mutableStateOf(preselectedCowId ?: "") }
    var vaccineName by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    var cowsLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        db.collection("Cows").whereEqualTo("user_id", uid).get()
            .addOnSuccessListener { snap ->
                allCows = snap.documents.map { it.toCowData() }.sortedBy { it.earTag }
                if (selectedCowId.isEmpty() && allCows.isNotEmpty()) selectedCowId = allCows[0].id
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

    val selectedCow = allCows.find { it.id == selectedCowId }

    @Composable
    fun VaccineChip(label: String) {
        val selected = vaccineName == label
        Row(
            Modifier
                .background(if (selected) GreenPrimary else Bg3, RoundedCornerShape(20.dp))
                .border(1.dp, if (selected) GreenPrimary else BorderColor, RoundedCornerShape(20.dp))
                .clickable { vaccineName = if (selected) "" else label }
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (selected) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Bg0,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                label,
                fontSize = 12.sp,
                color = if (selected) Bg0 else TextMid,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
            )
        }
    }

    Column(Modifier.fillMaxSize().background(Bg0)) {
        AppTopBar("Aşı Kaydı Ekle", onBack = onBack)

        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Cow dropdown
            Column {
                Text("İNEK", color = TextMid, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp, modifier = Modifier.padding(bottom = 8.dp))

                if (cowsLoading) {
                    Box(Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = GreenPrimary, modifier = Modifier.size(24.dp))
                    }
                } else if (allCows.isEmpty()) {
                    Box(
                        Modifier.fillMaxWidth()
                            .background(CardColor, RoundedCornerShape(12.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) { Text("Kayıtlı inek bulunamadı", color = TextDim, fontSize = 13.sp) }
                } else {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(Bg3, RoundedCornerShape(12.dp))
                                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                .clickable { expanded = true }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                selectedCow?.let { "${it.name} (#${it.earTag})" } ?: "İnek seçin",
                                color = if (selectedCow != null) TextPrimary else TextDim, fontSize = 15.sp
                            )
                            Text("▾", color = TextMid, fontSize = 14.sp)
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(Bg2)
                        ) {
                            allCows.forEach { cow ->
                                DropdownMenuItem(
                                    text = { Text("${cow.name} (#${cow.earTag})", color = TextPrimary) },
                                    onClick = { selectedCowId = cow.id; expanded = false },
                                    modifier = Modifier.background(if (cow.id == selectedCowId) StatusGebeBg else Bg2)
                                )
                            }
                        }
                    }
                }
            }

            // Vaccine name
            Column {
                Text("AŞI ADI", color = TextMid, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp, modifier = Modifier.padding(bottom = 8.dp))
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    commonVaccines.forEach { v -> VaccineChip(v) }
                }
                FormTextField(
                    label = "",
                    value = vaccineName,
                    onValueChange = { vaccineName = it },
                    placeholder = "veya başka bir aşı adı girin"
                )
            }

            // Date picker
            FormTextField(
                label = "TARİH",
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
                    enabled = !loading && selectedCowId.isNotEmpty() && vaccineName.isNotBlank(),
                    onClick = {
                        if (selectedCowId.isEmpty()) { error = "Lütfen inek seçin."; return@PrimaryButton }
                        if (vaccineName.isBlank()) { error = "Aşı adı gereklidir."; return@PrimaryButton }
                        loading = true; error = ""

                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val parsedDate = try { sdf.parse(date) ?: Date() } catch (e: Exception) { Date() }
                        val newVac = mapOf(
                            "vaccine_name" to vaccineName.trim(),
                            "date" to com.google.firebase.Timestamp(parsedDate)
                        )

                        db.collection("Cows").document(selectedCowId)
                            .update("vaccinations", FieldValue.arrayUnion(newVac))
                            .addOnSuccessListener {
                                loading = false; saved = true
                                android.os.Handler(android.os.Looper.getMainLooper())
                                    .postDelayed({ onSaved() }, 800)
                            }
                            .addOnFailureListener { loading = false; error = "Kayıt başarısız." }
                    }
                )
            }
        }
    }
}
