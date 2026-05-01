package com.akdag.inseminationtrackerapp

import com.google.firebase.Timestamp

data class InseminationRecord(
    val date: Timestamp? = null,
    val status: String = ""
)

data class VaccinationRecord(
    val vaccineName: String = "",
    val date: Timestamp? = null
)

data class CowData(
    val id: String = "",
    val earTag: String = "",
    val name: String = "",
    val isPregnant: Boolean = false,
    val dryingOffDate: Timestamp? = null,
    val inseminationRecords: List<InseminationRecord> = emptyList(),
    val vaccinations: List<VaccinationRecord> = emptyList()
)

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val farmName: String = "",
    val email: String = ""
)
