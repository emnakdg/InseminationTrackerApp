package com.akdag.inseminationtrackerapp

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.WorkManager
import com.akdag.inseminationtrackerapp.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.Date

class HomeActivity : ComponentActivity() {

    private val refreshState = mutableStateOf(0)

    private val childActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        refreshState.value++
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* bildirim izni verildi/reddedildi, devam et */ }

    override fun onResume() {
        super.onResume()
        refreshState.value++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            InseminationTrackerTheme {
                MainAppContent(
                    refreshTrigger = refreshState.value,
                    onNavigateToCowDetail = { cowId ->
                        startActivity(Intent(this, CowDetailActivity::class.java).putExtra("cowId", cowId))
                    },
                    onNavigateToAddCow = {
                        childActivityLauncher.launch(Intent(this, AddCowActivity::class.java))
                    },
                    onNavigateToAddInsemination = { cowId ->
                        childActivityLauncher.launch(
                            Intent(this, AddInseminationActivity::class.java).apply {
                                if (cowId != null) putExtra("preselectedCowId", cowId)
                            }
                        )
                    },
                    onNavigateToAddVaccine = { cowId ->
                        childActivityLauncher.launch(
                            Intent(this, AddVaccineActivity::class.java).apply {
                                if (cowId != null) putExtra("preselectedCowId", cowId)
                            }
                        )
                    },
                    onLogout = {
                        WorkManager.getInstance(this).cancelAllWork()
                        FirebaseAuth.getInstance().signOut()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun MainAppContent(
    refreshTrigger: Int,
    onNavigateToCowDetail: (String) -> Unit,
    onNavigateToAddCow: () -> Unit,
    onNavigateToAddInsemination: (String?) -> Unit,
    onNavigateToAddVaccine: (String?) -> Unit,
    onLogout: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val uid = auth.currentUser?.uid ?: ""

    var cows by remember { mutableStateOf<List<CowData>>(emptyList()) }
    var userProfile by remember { mutableStateOf(UserProfile()) }
    var loading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(refreshTrigger) {
        if (uid.isEmpty()) return@LaunchedEffect
        loading = true
        db.collection("Cows").whereEqualTo("user_id", uid).get()
            .addOnSuccessListener { snap ->
                cows = snap.documents.map { it.toCowData() }.sortedBy { it.earTag }
                loading = false
            }
            .addOnFailureListener { loading = false }
        db.collection("Users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    userProfile = UserProfile(
                        uid = uid,
                        name = doc.getString("name") ?: "",
                        farmName = doc.getString("farm_name") ?: "",
                        email = doc.getString("email") ?: auth.currentUser?.email ?: ""
                    )
                }
            }
    }

    Box(Modifier.fillMaxSize().background(Bg0)) {
        when (selectedTab) {
            0 -> DashboardTab(
                cows = cows,
                userProfile = userProfile,
                onCowDetail = onNavigateToCowDetail,
                onTabChange = { selectedTab = it },
                onAddInsemination = { onNavigateToAddInsemination(null) },
                onAddVaccine = { onNavigateToAddVaccine(null) }
            )
            1 -> CowListTab(
                cows = cows,
                loading = loading,
                onCowDetail = onNavigateToCowDetail,
                onAddCow = onNavigateToAddCow
            )
            2 -> NotificationsTab(cows = cows, onCowDetail = onNavigateToCowDetail)
            3 -> ProfileTab(userProfile = userProfile, cows = cows, onLogout = onLogout)
        }
        BottomNavBar(
            selectedTab = selectedTab,
            onTabChange = { selectedTab = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// ── DASHBOARD ────────────────────────────────────────────────

@Composable
fun DashboardTab(
    cows: List<CowData>,
    userProfile: UserProfile,
    onCowDetail: (String) -> Unit,
    onTabChange: (Int) -> Unit,
    onAddInsemination: () -> Unit,
    onAddVaccine: () -> Unit
) {
    val pregnantCount = cows.count { it.isPregnant }
    val pendingCount = cows.count { it.latestStatus() == "Tohumlama Yapıldı" }
    val upcoming = cows
        .filter { it.dryingOffDate != null }
        .map { it to daysUntil(it.dryingOffDate!!.toDate()) }
        .filter { (_, d) -> d in 0..60 }
        .sortedBy { (_, d) -> d }

    Column(Modifier.fillMaxSize()) {
        // Header
        Column(
            Modifier
                .fillMaxWidth()
                .background(Bg1)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Hoş geldin,", fontSize = 12.sp, color = TextMid)
                    Text(
                        userProfile.farmName.ifEmpty { userProfile.name.ifEmpty { "Çiftçi" } },
                        fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary
                    )
                }
                Box(
                    Modifier.size(42.dp).background(Bg3, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) { Text("🐄", fontSize = 20.sp) }
            }
            Text(
                formatDate(Date()),
                fontSize = 12.sp, color = TextDim, modifier = Modifier.padding(top = 4.dp)
            )
        }
        HorizontalDivider(color = BorderColor)

        LazyColumn(Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp)) {
            // Stats
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(
                        Triple("Toplam İnek", cows.size, GreenPrimary),
                        Triple("Gebe", pregnantCount, YellowAccent),
                        Triple("Beklemede", pendingCount, OrangeAccent)
                    ).forEach { (label, value, color) ->
                        StatCard(label, value.toString(), color, Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            // Upcoming drying-off
            item {
                Text(
                    "YAKLAŞAN KURUYA ÇIKMA", fontSize = 12.sp,
                    fontWeight = FontWeight.Bold, color = TextMid, letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
            if (upcoming.isEmpty()) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(CardColor, RoundedCornerShape(14.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("Önümüzdeki 60 günde kuruya çıkma yok", color = TextDim, fontSize = 13.sp) }
                    Spacer(Modifier.height(20.dp))
                }
            } else {
                items(upcoming) { (cow, days) ->
                    UpcomingDryOffCard(cow, days, onClick = { onCowDetail(cow.id) })
                    Spacer(Modifier.height(8.dp))
                }
                item { Spacer(Modifier.height(12.dp)) }
            }

            // Quick access
            item {
                Text(
                    "HIZLI ERİŞİM", fontSize = 12.sp,
                    fontWeight = FontWeight.Bold, color = TextMid, letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickAccessCard("Tohumlama\nEkle", Icons.Rounded.Add, Modifier.weight(1f), GreenPrimary) { onAddInsemination() }
                    QuickAccessCard("Aşı Ekle", Icons.Rounded.Vaccines, Modifier.weight(1f), BlueAccent) { onAddVaccine() }
                    QuickAccessCard("İnek\nListesi", Icons.Rounded.FormatListBulleted, Modifier.weight(1f), GreenPrimary) { onTabChange(1) }
                    QuickAccessCard("Uyarılar", Icons.Rounded.Notifications, Modifier.weight(1f), YellowAccent) { onTabChange(2) }
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(CardColor, RoundedCornerShape(14.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
            .padding(vertical = 14.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = color)
        Text(label, fontSize = 11.sp, color = TextMid, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
fun UpcomingDryOffCard(cow: CowData, days: Int, onClick: () -> Unit) {
    val urgent = days <= 14
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardColor, RoundedCornerShape(14.dp))
            .border(1.dp, if (urgent) RedAccent.copy(alpha = 0.3f) else BorderColor, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(44.dp)
                .background(if (urgent) StatusBasarisizBg else StatusGebeBg, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("${days}g", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (urgent) RedAccent else GreenPrimary)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(cow.name, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                Spacer(Modifier.width(6.dp))
                Text("#${cow.earTag}", color = TextDim, fontSize = 12.sp)
            }
            Text(
                "Kuruya çıkma: ${formatDate(cow.dryingOffDate?.toDate())}",
                fontSize = 12.sp, color = TextMid, modifier = Modifier.padding(top = 2.dp)
            )
        }
        if (urgent) Icon(Icons.Rounded.Warning, contentDescription = null, tint = RedAccent, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun QuickAccessCard(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    iconTint: Color = GreenPrimary,
    onClick: () -> Unit
) {
    Column(
        modifier
            .background(CardColor, RoundedCornerShape(14.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 16.dp, horizontal = 6.dp)
            .heightIn(min = 88.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(42.dp)
                .background(iconTint.copy(alpha = 0.13f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextMid,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            lineHeight = 15.sp
        )
    }
}

// ── COW LIST ──────────────────────────────────────────────────

@Composable
fun CowListTab(cows: List<CowData>, loading: Boolean, onCowDetail: (String) -> Unit, onAddCow: () -> Unit) {
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("all") }
    var viewMode by remember { mutableStateOf("card") }

    val filtered = cows.filter { cow ->
        val matchSearch = cow.earTag.contains(search, ignoreCase = true) || cow.name.contains(search, ignoreCase = true)
        val status = cow.latestStatus()
        when (filter) {
            "pregnant" -> matchSearch && cow.isPregnant
            "pending" -> matchSearch && status == "Tohumlama Yapıldı"
            "idle" -> matchSearch && !cow.isPregnant && status != "Tohumlama Yapıldı"
            else -> matchSearch
        }
    }

    Column(Modifier.fillMaxSize()) {
        // Header
        Column(Modifier.fillMaxWidth().background(Bg1).padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("İnekler", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    // View toggle
                    Row(
                        Modifier.background(Bg3, RoundedCornerShape(10.dp)).padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        listOf("card" to "⊞", "list" to "≡").forEach { (mode, icon) ->
                            Box(
                                Modifier
                                    .background(if (viewMode == mode) Bg1 else Color.Transparent, RoundedCornerShape(7.dp))
                                    .clickable { viewMode = mode }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) { Text(icon, fontSize = 14.sp, color = if (viewMode == mode) TextPrimary else TextDim) }
                        }
                    }
                    // Add button
                    Box(
                        Modifier
                            .size(34.dp)
                            .background(GreenPrimary, RoundedCornerShape(10.dp))
                            .clickable { onAddCow() },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Rounded.Add, contentDescription = "İnek Ekle", tint = Bg0, modifier = Modifier.size(20.dp)) }
                }
            }
            Spacer(Modifier.height(10.dp))
            // Search
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Küpe no veya isim ara…", color = TextDim, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = TextDim, modifier = Modifier.size(20.dp)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenPrimary, unfocusedBorderColor = BorderColor,
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                    focusedContainerColor = Bg3, unfocusedContainerColor = Bg3, cursorColor = GreenPrimary
                )
            )
            Spacer(Modifier.height(10.dp))
            // Filter chips
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("all" to "Tümü", "pregnant" to "Gebe", "pending" to "Beklemede", "idle" to "Boşta").forEach { (key, label) ->
                    Box(
                        Modifier
                            .background(if (filter == key) GreenPrimary else Bg3, RoundedCornerShape(20.dp))
                            .clickable { filter = key }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            color = if (filter == key) Bg0 else TextMid)
                    }
                }
            }
        }
        HorizontalDivider(color = BorderColor)

        if (loading) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 88.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(6) { ShimmerCowCard(Modifier.fillMaxWidth()) }
            }
        } else if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🐄", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("İnek bulunamadı", color = TextDim, fontSize = 14.sp)
                }
            }
        } else if (viewMode == "card") {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 88.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered) { cow -> CowCard(cow, onClick = { onCowDetail(cow.id) }) }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
                items(filtered) { cow -> CowListRow(cow, onClick = { onCowDetail(cow.id) }) }
            }
        }
    }
}

@Composable
fun CowCard(cow: CowData, onClick: () -> Unit) {
    val status = cow.latestStatus() ?: ""
    val days = cow.dryingOffDate?.let { daysUntil(it.toDate()) }
    val urgent = days != null && days in 0..14
    val accentColor = when {
        urgent       -> RedAccent
        cow.isPregnant -> GreenPrimary
        else         -> androidx.compose.ui.graphics.Color.Transparent
    }

    Column(
        Modifier
            .shadow(
                elevation = if (urgent) 6.dp else 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = if (urgent) RedAccent.copy(0.25f) else GreenPrimary.copy(0.1f),
                spotColor = androidx.compose.ui.graphics.Color.Transparent
            )
            .clip(RoundedCornerShape(16.dp))
            .background(CardColor)
            .border(1.dp, if (urgent) RedAccent.copy(alpha = 0.4f) else BorderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        // Üst aksan şeridi
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(accentColor)
        )

        Column(Modifier.padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .background(
                            if (cow.isPregnant) StatusGebeBg else Bg3,
                            RoundedCornerShape(13.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) { Text("🐄", fontSize = 22.sp) }
                if (urgent) {
                    Icon(Icons.Rounded.Warning, null, tint = RedAccent, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                cow.name,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "#${cow.earTag}",
                fontSize = 11.sp,
                color = TextDim,
                modifier = Modifier.padding(top = 1.dp, bottom = 8.dp)
            )

            StatusBadge(status)

            if (days != null && days >= 0) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Schedule,
                        null,
                        tint = if (urgent) RedAccent else TextMid,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        "$days gün",
                        fontSize = 11.sp,
                        color = if (urgent) RedAccent else TextMid,
                        fontWeight = if (urgent) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun CowListRow(cow: CowData, onClick: () -> Unit) {
    val status = cow.latestStatus() ?: ""
    val days = cow.dryingOffDate?.let { daysUntil(it.toDate()) }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(44.dp)
                .background(if (cow.isPregnant) StatusGebeBg else Bg3, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) { Text("🐄", fontSize = 20.sp) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(cow.name, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
            Text("#${cow.earTag}", fontSize = 12.sp, color = TextDim)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            StatusBadge(status)
            if (days != null && days >= 0) {
                Text("⏱ ${days}g", fontSize = 11.sp, color = if (days <= 14) RedAccent else TextDim)
            }
        }
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = TextDim, modifier = Modifier.size(20.dp))
    }
    HorizontalDivider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
}

// ── NOTIFICATIONS ─────────────────────────────────────────────

@Composable
fun NotificationsTab(cows: List<CowData>, onCowDetail: (String) -> Unit) {
    data class Alert(val cow: CowData, val title: String, val desc: String, val days: Int?, val urgent: Boolean, val icon: String)

    val alerts = buildList {
        cows.forEach { cow ->
            if (cow.dryingOffDate != null) {
                val d = daysUntil(cow.dryingOffDate.toDate())
                if (d in 0..60) {
                    add(Alert(cow, "Kuruya Çıkma Yaklaşıyor", "${cow.name} (${cow.earTag}) — ${formatDate(cow.dryingOffDate.toDate())}", d, d <= 14, if (d <= 14) "🚨" else "⏰"))
                }
            }
            val status = cow.latestStatus()
            if (status == "Tohumlama Yapıldı") {
                val insDate = cow.inseminationRecords.firstOrNull()?.date?.toDate()
                if (insDate != null) {
                    val daysSince = -daysUntil(insDate)
                    if (daysSince >= 21) {
                        add(Alert(cow, "Gebelik Kontrolü Yapılmalı", "${cow.name} — $daysSince gün önce tohumlandı", null, false, "🔔"))
                    }
                }
            }
        }
    }.sortedByDescending { it.urgent }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().background(Bg1).padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text("Uyarılar", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            Text("${alerts.size} aktif bildirim", fontSize = 12.sp, color = TextDim, modifier = Modifier.padding(top = 2.dp))
        }
        HorizontalDivider(color = BorderColor)

        if (alerts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✅", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Bekleyen uyarı yok", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextMid)
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp)) {
                items(alerts) { alert ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(if (alert.urgent) StatusBasarisizBg else CardColor, RoundedCornerShape(14.dp))
                            .border(1.dp, if (alert.urgent) RedAccent.copy(alpha = 0.35f) else BorderColor, RoundedCornerShape(14.dp))
                            .clickable { onCowDetail(alert.cow.id) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(alert.icon, fontSize = 26.sp, modifier = Modifier.padding(top = 2.dp, end = 14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(alert.title, fontWeight = FontWeight.Bold, color = if (alert.urgent) RedAccent else TextPrimary, fontSize = 14.sp)
                            Text(alert.desc, fontSize = 13.sp, color = TextMid, modifier = Modifier.padding(top = 3.dp))
                            if (alert.days != null) {
                                Spacer(Modifier.height(6.dp))
                                Box(
                                    Modifier
                                        .background(if (alert.urgent) RedAccent.copy(alpha = 0.15f) else Bg3, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 10.dp, vertical = 3.dp)
                                ) {
                                    Text("${alert.days} gün kaldı", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                        color = if (alert.urgent) RedAccent else TextMid)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

// ── PROFILE ───────────────────────────────────────────────────

private const val PRIVACY_POLICY_URL = "https://chimerical-truffle-ace3bf.netlify.app/"

@Composable
fun ProfileTab(userProfile: UserProfile, cows: List<CowData>, onLogout: () -> Unit) {
    val totalVaccinations = cows.sumOf { it.vaccinations.size }
    val context = LocalContext.current
    var showAboutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountConfirm by remember { mutableStateOf(false) }
    var deleteAccountLoading by remember { mutableStateOf(false) }
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val uid = auth.currentUser?.uid ?: ""

    fun deleteAccount() {
        deleteAccountLoading = true
        db.collection("Cows").whereEqualTo("user_id", uid).get()
            .addOnSuccessListener { snap ->
                val batch = db.batch()
                snap.documents.forEach { batch.delete(it.reference) }
                batch.commit().addOnSuccessListener {
                    db.collection("Users").document(uid).delete().addOnSuccessListener {
                        WorkManager.getInstance(context).cancelAllWork()
                        auth.currentUser?.delete()
                            ?.addOnSuccessListener { onLogout() }
                            ?.addOnFailureListener {
                                deleteAccountLoading = false
                                // Oturum eskimiş — kullanıcı yeniden giriş yapmalı
                                auth.signOut()
                                onLogout()
                            }
                    }.addOnFailureListener { deleteAccountLoading = false }
                }.addOnFailureListener { deleteAccountLoading = false }
            }.addOnFailureListener { deleteAccountLoading = false }
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().background(Bg1).padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(60.dp).background(GreenPrimary, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        (userProfile.name.firstOrNull() ?: "?").toString(),
                        fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Bg0
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(userProfile.name.ifEmpty { "Kullanıcı" }, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                    Text(userProfile.farmName.ifEmpty { "Çiftlik" }, fontSize = 13.sp, color = TextMid)
                    Text(userProfile.email, fontSize = 12.sp, color = TextDim)
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    "Toplam İnek" to cows.size,
                    "Gebe" to cows.count { it.isPregnant },
                    "Aşı Kaydı" to totalVaccinations
                ).forEach { (label, value) ->
                    Column(
                        Modifier.weight(1f)
                            .background(Bg3, RoundedCornerShape(12.dp))
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(value.toString(), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = GreenPrimary)
                        Text(label, fontSize = 11.sp, color = TextMid, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }
        HorizontalDivider(color = BorderColor)

        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp)) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(CardColor, RoundedCornerShape(14.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                        .clickable {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            context.startActivity(intent)
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Notifications, null, tint = GreenPrimary, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Bildirim Ayarları", fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 14.sp)
                        Text("Hatırlatma süreleri", fontSize = 12.sp, color = TextDim)
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = TextDim, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.height(8.dp))
            }
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(CardColor, RoundedCornerShape(14.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                        .clickable { showAboutDialog = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.PhoneAndroid, null, tint = GreenPrimary, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Uygulama Hakkında", fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 14.sp)
                        Text("v2.0.0", fontSize = 12.sp, color = TextDim)
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = TextDim, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.height(8.dp))
            }

            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(CardColor, RoundedCornerShape(14.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                        .clickable {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)))
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Lock, null, tint = GreenPrimary, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Gizlilik Politikası", fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 14.sp)
                        Text("Verileriniz nasıl kullanılıyor?", fontSize = 12.sp, color = TextDim)
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = TextDim, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.height(8.dp))
            }

            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusBasarisizBg, contentColor = RedAccent)
                ) { Text("Çıkış Yap", fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showDeleteAccountConfirm = true },
                    enabled = !deleteAccountLoading,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(RedAccent.copy(alpha = 0.5f))
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RedAccent)
                ) {
                    Text(
                        if (deleteAccountLoading) "Siliniyor…" else "Hesabı ve Tüm Verileri Sil",
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    if (showDeleteAccountConfirm) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showDeleteAccountConfirm = false }) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Bg2, RoundedCornerShape(20.dp))
                    .padding(24.dp)
            ) {
                Text("⚠️ Hesabı Sil", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = RedAccent)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Bu işlem geri alınamaz. Aşağıdakiler kalıcı olarak silinecek:",
                    fontSize = 14.sp, color = TextMid
                )
                Spacer(Modifier.height(8.dp))
                listOf("Tüm inek kayıtları", "Tohumlama ve aşı geçmişi", "Hesap bilgileri").forEach {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                        Text("•", color = RedAccent, modifier = Modifier.padding(end = 8.dp))
                        Text(it, fontSize = 13.sp, color = TextPrimary)
                    }
                }
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { showDeleteAccountConfirm = false },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                    ) { Text("İptal") }
                    Button(
                        onClick = { showDeleteAccountConfirm = false; deleteAccount() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusBasarisizBg, contentColor = RedAccent)
                    ) { Text("Sil") }
                }
            }
        }
    }

    if (showAboutDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showAboutDialog = false }) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Bg2, RoundedCornerShape(20.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .size(64.dp)
                        .background(GreenPrimary, RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) { Text("🐄", fontSize = 30.sp) }
                Spacer(Modifier.height(16.dp))
                Text("İnek Takip", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                Text("v2.0.0", fontSize = 13.sp, color = TextDim, modifier = Modifier.padding(top = 4.dp))
                Spacer(Modifier.height(12.dp))
                Text(
                    "Sürü yönetimini kolaylaştırmak için tasarlandı. Tohumlama takibi, gebelik hatırlatmaları ve aşı kayıtları.",
                    fontSize = 13.sp, color = TextMid,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(20.dp))
                OutlinedButton(
                    onClick = { showAboutDialog = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                ) { Text("Kapat") }
            }
        }
    }
}

// ── BOTTOM NAV ────────────────────────────────────────────────

@Composable
fun BottomNavBar(selectedTab: Int, onTabChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val items = listOf(
        Triple(Icons.Rounded.Home, "Ana Sayfa", 0),
        Triple(Icons.Rounded.List, "İnekler", 1),
        Triple(Icons.Rounded.Notifications, "Uyarılar", 2),
        Triple(Icons.Rounded.Person, "Profil", 3),
    )
    Column(modifier.fillMaxWidth()) {
        HorizontalDivider(color = BorderColor, thickness = 1.dp)
        Row(
            Modifier
                .fillMaxWidth()
                .background(Bg1)
                .padding(top = 10.dp, bottom = 24.dp)
        ) {
            items.forEach { (icon, label, index) ->
                val active = selectedTab == index
                Column(
                    Modifier.weight(1f).clickable { onTabChange(index) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(icon, contentDescription = label, tint = if (active) GreenPrimary else TextDim, modifier = Modifier.size(22.dp))
                    Text(label, fontSize = 10.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        color = if (active) GreenPrimary else TextDim)
                }
            }
        }
    }
}
