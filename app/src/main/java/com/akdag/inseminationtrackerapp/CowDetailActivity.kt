package com.akdag.inseminationtrackerapp

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.akdag.inseminationtrackerapp.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class CowDetailActivity : ComponentActivity() {

    private val refreshState = mutableStateOf(0)

    private val childLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        refreshState.value++
    }

    override fun onResume() {
        super.onResume()
        refreshState.value++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val cowId = intent.getStringExtra("cowId") ?: run { finish(); return }

        setContent {
            InseminationTrackerTheme {
                CowDetailScreen(
                    cowId = cowId,
                    refreshTrigger = refreshState.value,
                    onBack = { finish() },
                    onAddInsemination = {
                        childLauncher.launch(Intent(this, AddInseminationActivity::class.java).putExtra("preselectedCowId", cowId))
                    },
                    onAddVaccine = {
                        childLauncher.launch(Intent(this, AddVaccineActivity::class.java).putExtra("preselectedCowId", cowId))
                    },
                    onDeleted = { finish() }
                )
            }
        }
    }
}

@Composable
fun CowDetailScreen(
    cowId: String,
    refreshTrigger: Int,
    onBack: () -> Unit,
    onAddInsemination: () -> Unit,
    onAddVaccine: () -> Unit,
    onDeleted: () -> Unit
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    val uid = auth.currentUser?.uid ?: ""
    val context = androidx.compose.ui.platform.LocalContext.current

    var cow by remember { mutableStateOf<CowData?>(null) }
    var loading by remember { mutableStateOf(true) }
    var activeTab by remember { mutableStateOf(0) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var actionLoading by remember { mutableStateOf(false) }

    LaunchedEffect(refreshTrigger) {
        db.collection("Cows").document(cowId).get()
            .addOnSuccessListener { doc ->
                cow = if (doc.exists()) doc.toCowData() else null
                loading = false
            }
            .addOnFailureListener { loading = false }
    }

    fun updateStatus(newStatus: String) {
        val currentCow = cow ?: return
        actionLoading = true
        db.collection("Cows").document(cowId).get()
            .addOnSuccessListener { doc ->
                @Suppress("UNCHECKED_CAST")
                val records = (doc.get("insemination_records") as? List<Map<String, Any>> ?: emptyList()).toMutableList()
                if (records.isEmpty()) { actionLoading = false; return@addOnSuccessListener }

                val updated = records[0].toMutableMap().apply { put("status", newStatus) }
                records[0] = updated

                val isNowPregnant = newStatus == "Başarılı"
                val updates = mutableMapOf<String, Any>(
                    "insemination_records" to records,
                    "is_pregnant" to isNowPregnant
                )
                if (isNowPregnant) {
                    val insDate = (records[0]["date"] as? com.google.firebase.Timestamp)?.toDate()
                    if (insDate != null) {
                        val dryOffDate = addDays(insDate, 195)
                        updates["drying_off_date"] = com.google.firebase.Timestamp(dryOffDate)
                    }
                } else {
                    updates["drying_off_date"] = FieldValue.delete()
                }

                db.collection("Cows").document(cowId).update(updates)
                    .addOnSuccessListener {
                        if (isNowPregnant) {
                            val insDate = (records[0]["date"] as? com.google.firebase.Timestamp)?.toDate()
                            if (insDate != null) {
                                scheduleDryOffNotification(context, currentCow.earTag, insDate)
                            }
                        }
                        actionLoading = false
                        // reload
                        db.collection("Cows").document(cowId).get()
                            .addOnSuccessListener { d -> cow = if (d.exists()) d.toCowData() else null }
                    }
                    .addOnFailureListener { actionLoading = false }
            }
    }

    fun deleteCow() {
        db.collection("Cows").document(cowId).delete()
            .addOnSuccessListener { onDeleted() }
    }

    if (loading) {
        Box(Modifier.fillMaxSize().background(Bg0), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = GreenPrimary)
        }
        return
    }

    val currentCow = cow
    if (currentCow == null) {
        Box(Modifier.fillMaxSize().background(Bg0), contentAlignment = Alignment.Center) {
            Text("İnek bulunamadı", color = TextMid)
        }
        return
    }

    val latestStatus = currentCow.latestStatus()
    val days = currentCow.dryingOffDate?.let { daysUntil(it.toDate()) }

    Box(Modifier.fillMaxSize().background(Bg0)) {
        Column(Modifier.fillMaxSize()) {
            // TopBar
            AppTopBar(
                title = "${currentCow.name} — #${currentCow.earTag}",
                onBack = onBack,
                rightContent = {
                    Box(
                        Modifier
                            .size(36.dp)
                            .background(StatusBasarisizBg, RoundedCornerShape(10.dp))
                            .clickable { showDeleteConfirm = true },
                        contentAlignment = Alignment.Center
                    ) { Text("🗑", fontSize = 16.sp) }
                }
            )

            LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                // Hero
                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(if (currentCow.isPregnant) StatusGebeBg.copy(alpha = 0.5f) else Bg1)
                            .padding(horizontal = 20.dp, vertical = 20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(64.dp)
                                    .background(if (currentCow.isPregnant) StatusGebeBg else Bg3, RoundedCornerShape(20.dp)),
                                contentAlignment = Alignment.Center
                            ) { Text("🐄", fontSize = 32.sp) }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(currentCow.name, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                                Text("Küpe: #${currentCow.earTag}", fontSize = 13.sp, color = TextDim)
                                if (latestStatus != null) {
                                    Spacer(Modifier.height(6.dp))
                                    StatusBadge(latestStatus)
                                }
                            }
                        }

                        if (currentCow.isPregnant && days != null) {
                            Spacer(Modifier.height(16.dp))
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .background(Bg3, RoundedCornerShape(14.dp))
                                    .border(1.dp, if (days <= 14) RedAccent.copy(alpha = 0.35f) else BorderColor, RoundedCornerShape(14.dp))
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("KURUYA ÇIKMA", fontSize = 11.sp, color = TextMid)
                                    Text(formatDate(currentCow.dryingOffDate?.toDate()), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("KALAN", fontSize = 11.sp, color = TextMid)
                                    Text("${days}g", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold,
                                        color = if (days <= 14) RedAccent else GreenPrimary)
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = BorderColor)
                }

                // Action buttons
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!currentCow.isPregnant && latestStatus != "Tohumlama Yapıldı") {
                            GhostButton("+ Tohumlama Ekle", onClick = onAddInsemination, modifier = Modifier.weight(1f))
                        }
                        if (latestStatus == "Tohumlama Yapıldı") {
                            GhostButton("✓ Başarılı", onClick = { updateStatus("Başarılı") }, modifier = Modifier.weight(1f))
                            DangerButton("✗ Başarısız", onClick = { updateStatus("Başarısız") }, modifier = Modifier.weight(1f))
                        }
                        if (currentCow.isPregnant) {
                            GhostButton("🐣 Doğum", onClick = { updateStatus("Doğum Yaptı") }, modifier = Modifier.weight(1f))
                            DangerButton("Düşük", onClick = { updateStatus("Düşük Yaptı") }, modifier = Modifier.weight(1f))
                        }
                        GhostButton("💉 Aşı", onClick = onAddVaccine, modifier = Modifier.wrapContentWidth())
                    }
                    HorizontalDivider(color = BorderColor)
                }

                // Tabs
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        listOf("Geçmiş" to 0, "Aşılar (${currentCow.vaccinations.size})" to 1).forEach { (label, idx) ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .clickable { activeTab = idx }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                        color = if (activeTab == idx) GreenPrimary else TextMid)
                                    Spacer(Modifier.height(4.dp))
                                    Box(
                                        Modifier
                                            .height(2.dp)
                                            .fillMaxWidth(0.6f)
                                            .background(if (activeTab == idx) GreenPrimary else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(1.dp))
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = BorderColor)
                }

                if (activeTab == 0) {
                    // Insemination timeline
                    if (currentCow.inseminationRecords.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                                Text("Henüz tohumlama kaydı yok", color = TextDim, fontSize = 13.sp)
                            }
                        }
                    } else {
                        items(currentCow.inseminationRecords.size) { i ->
                            val rec = currentCow.inseminationRecords[i]
                            val isLast = i == currentCow.inseminationRecords.size - 1
                            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(20.dp)) {
                                    Box(
                                        Modifier.size(12.dp)
                                            .background(if (i == 0) GreenPrimary else TextDim, RoundedCornerShape(6.dp))
                                    )
                                    if (!isLast) Box(Modifier.width(2.dp).height(40.dp).background(BorderColor))
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(
                                    Modifier
                                        .weight(1f)
                                        .background(CardColor, RoundedCornerShape(12.dp))
                                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        StatusBadge(rec.status)
                                        Text(formatDate(rec.date?.toDate()), fontSize = 12.sp, color = TextDim)
                                    }
                                    if (rec.status == "Başarılı" && i == 0 && currentCow.dryingOffDate != null) {
                                        Text(
                                            "Kuruya çıkma: ${formatDate(currentCow.dryingOffDate.toDate())}",
                                            fontSize = 12.sp, color = TextMid,
                                            modifier = Modifier.padding(top = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Vaccines
                    if (currentCow.vaccinations.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                                Text("Aşı kaydı yok", color = TextDim, fontSize = 13.sp)
                            }
                        }
                    } else {
                        items(currentCow.vaccinations) { vac ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .background(CardColor, RoundedCornerShape(12.dp))
                                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(vac.vaccineName, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                                    Text(formatDate(vac.date?.toDate()), fontSize = 12.sp, color = TextDim, modifier = Modifier.padding(top = 2.dp))
                                }
                                Text("💉", fontSize = 20.sp)
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }

        // Delete confirmation
        if (showDeleteConfirm) {
            Dialog(onDismissRequest = { showDeleteConfirm = false }) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Bg2, RoundedCornerShape(20.dp))
                        .padding(24.dp)
                ) {
                    Text("İneği Sil?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${currentCow.name} (#${currentCow.earTag}) ve tüm kayıtları silinecek. Bu işlem geri alınamaz.",
                        fontSize = 14.sp, color = TextMid
                    )
                    Spacer(Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { showDeleteConfirm = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                        ) { Text("İptal") }
                        Button(
                            onClick = { showDeleteConfirm = false; deleteCow() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusBasarisizBg, contentColor = RedAccent)
                        ) { Text("Sil") }
                    }
                }
            }
        }

        if (actionLoading) {
            Box(Modifier.fillMaxSize().background(Bg0.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GreenPrimary)
            }
        }
    }
}
