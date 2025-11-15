package com.ibandetector.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.*

@Entity(tableName = "iban_history")
data class IbanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ibanNumber: String,
    val country: String,
    val countryName: String,
    val isValid: Boolean,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("ar"))
        return sdf.format(Date(timestamp))
    }
    
    fun getFormattedIban(): String {
        return ibanNumber.chunked(4).joinToString(" ")
    }
}
