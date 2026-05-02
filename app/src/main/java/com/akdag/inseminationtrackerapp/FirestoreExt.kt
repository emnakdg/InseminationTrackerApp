package com.akdag.inseminationtrackerapp

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

@Suppress("UNCHECKED_CAST")
fun DocumentSnapshot.toCowData(): CowData {
    val insRaw = get("insemination_records") as? List<Map<String, Any>> ?: emptyList()
    val insRecords = insRaw.map { m ->
        InseminationRecord(
            date = m["date"] as? Timestamp,
            status = m["status"] as? String ?: ""
        )
    }
    val vacRaw = get("vaccinations") as? List<Map<String, Any>> ?: emptyList()
    val vacRecords = vacRaw.map { m ->
        VaccinationRecord(
            vaccineName = m["vaccine_name"] as? String ?: "",
            date = m["date"] as? Timestamp
        )
    }
    val earTag = getString("ear_tag") ?: ""
    return CowData(
        id = id,
        earTag = earTag,
        name = getString("name").takeIf { !it.isNullOrEmpty() } ?: earTag,
        isPregnant = getBoolean("is_pregnant") ?: false,
        dryingOffDate = getTimestamp("drying_off_date"),
        inseminationRecords = insRecords,
        vaccinations = vacRecords
    )
}

fun formatDate(date: Date?): String {
    if (date == null) return "—"
    val cal = Calendar.getInstance().apply { time = date }
    return "%02d.%02d.%04d".format(
        cal.get(Calendar.DAY_OF_MONTH),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.YEAR)
    )
}

fun daysUntil(date: Date): Int {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    return ((date.time - today) / (1000L * 60 * 60 * 24)).toInt()
}

fun addDays(date: Date, days: Int): Date = Date(date.time + days.toLong() * 24 * 60 * 60 * 1000)

fun CowData.latestStatus(): String? {
    if (isPregnant) return "Başarılı"
    val pending = inseminationRecords.find { it.status == "Tohumlama Yapıldı" }
    if (pending != null) return "Tohumlama Yapıldı"
    return inseminationRecords.firstOrNull()?.status
}

fun scheduleDryOffNotification(context: Context, earTag: String, inseminationDate: Date) {
    val dryOffDate = addDays(inseminationDate, 195)
    val delayMs = dryOffDate.time - System.currentTimeMillis()
    val builder = OneTimeWorkRequestBuilder<CowBirthReminderWorker>()
        .setInputData(workDataOf("earTag" to earTag))
        .addTag("reminder_$earTag")
    if (delayMs > 0) builder.setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
    WorkManager.getInstance(context).enqueueUniqueWork(
        "drying_off_$earTag", ExistingWorkPolicy.REPLACE, builder.build()
    )
}
