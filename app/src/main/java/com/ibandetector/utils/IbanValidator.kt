package com.ibandetector.utils

import android.content.Context
import com.ibandetector.R
import java.math.BigInteger

data class IbanValidationResult(
    val isValid: Boolean,
    val country: String,
    val countryName: String,
    val length: Int,
    val expectedLength: Int,
    val errorMessage: String? = null
)

object IbanValidator {
    
    // خريطة الدول المدعومة مع أطوال الإيبان المتوقعة
    private val supportedCountries = mapOf(
        "IQ" to 23, // العراق
        "SA" to 24, // السعودية
        "AE" to 23, // الإمارات
        "KW" to 30, // الكويت
        "BH" to 22, // البحرين
        "QA" to 29, // قطر
        "OM" to 23, // عمان
        "JO" to 30, // الأردن
        "EG" to 29, // مصر
        "LB" to 28  // لبنان
    )
    
    /**
     * التحقق من صحة رقم الإيبان
     */
    fun validateIban(iban: String, context: Context): IbanValidationResult {
        // إزالة المسافات وتحويل إلى أحرف كبيرة
        val cleanIban = iban.replace(" ", "").uppercase()
        
        // التحقق من أن الإيبان ليس فارغاً
        if (cleanIban.isEmpty()) {
            return IbanValidationResult(
                isValid = false,
                country = "",
                countryName = "",
                length = 0,
                expectedLength = 0,
                errorMessage = context.getString(R.string.error_empty_iban)
            )
        }
        
        // التحقق من الحد الأدنى للطول
        if (cleanIban.length < 4) {
            return IbanValidationResult(
                isValid = false,
                country = "",
                countryName = "",
                length = cleanIban.length,
                expectedLength = 0,
                errorMessage = context.getString(R.string.error_invalid_length)
            )
        }
        
        // استخراج رمز الدولة
        val countryCode = cleanIban.substring(0, 2)
        val expectedLength = supportedCountries[countryCode]
        
        // التحقق من أن الدولة مدعومة
        if (expectedLength == null) {
            return IbanValidationResult(
                isValid = false,
                country = countryCode,
                countryName = context.getString(R.string.country_unknown),
                length = cleanIban.length,
                expectedLength = 0,
                errorMessage = context.getString(R.string.error_invalid_country)
            )
        }
        
        // التحقق من الطول الصحيح
        if (cleanIban.length != expectedLength) {
            return IbanValidationResult(
                isValid = false,
                country = countryCode,
                countryName = getCountryName(countryCode, context),
                length = cleanIban.length,
                expectedLength = expectedLength,
                errorMessage = context.getString(R.string.error_invalid_length)
            )
        }
        
        // التحقق من التنسيق الأساسي (حرفان ثم رقمان ثم حروف/أرقام)
        if (!cleanIban.matches(Regex("^[A-Z]{2}[0-9]{2}[A-Z0-9]+$"))) {
            return IbanValidationResult(
                isValid = false,
                country = countryCode,
                countryName = getCountryName(countryCode, context),
                length = cleanIban.length,
                expectedLength = expectedLength,
                errorMessage = context.getString(R.string.error_invalid_format)
            )
        }
        
        // التحقق من رقم التحقق (checksum) باستخدام معيار mod-97
        val isChecksumValid = validateChecksum(cleanIban)
        
        return if (isChecksumValid) {
            IbanValidationResult(
                isValid = true,
                country = countryCode,
                countryName = getCountryName(countryCode, context),
                length = cleanIban.length,
                expectedLength = expectedLength
            )
        } else {
            IbanValidationResult(
                isValid = false,
                country = countryCode,
                countryName = getCountryName(countryCode, context),
                length = cleanIban.length,
                expectedLength = expectedLength,
                errorMessage = context.getString(R.string.error_invalid_checksum)
            )
        }
    }
    
    /**
     * التحقق من رقم التحقق باستخدام خوارزمية mod-97
     */
    private fun validateChecksum(iban: String): Boolean {
        return try {
            // نقل أول 4 أحرف إلى النهاية
            val rearranged = iban.substring(4) + iban.substring(0, 4)
            
            // تحويل الحروف إلى أرقام (A=10, B=11, ..., Z=35)
            val numericString = rearranged.map { char ->
                if (char.isDigit()) char.toString()
                else (char.code - 'A'.code + 10).toString()
            }.joinToString("")
            
            // حساب mod 97
            val remainder = BigInteger(numericString).mod(BigInteger("97"))
            
            // يجب أن يكون الباقي 1
            remainder == BigInteger.ONE
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * الحصول على اسم الدولة بالعربية
     */
    fun getCountryName(countryCode: String, context: Context): String {
        return when (countryCode) {
            "IQ" -> context.getString(R.string.country_iq)
            "SA" -> context.getString(R.string.country_sa)
            "AE" -> context.getString(R.string.country_ae)
            "KW" -> context.getString(R.string.country_kw)
            "BH" -> context.getString(R.string.country_bh)
            "QA" -> context.getString(R.string.country_qa)
            "OM" -> context.getString(R.string.country_om)
            "JO" -> context.getString(R.string.country_jo)
            "EG" -> context.getString(R.string.country_eg)
            "LB" -> context.getString(R.string.country_lb)
            else -> context.getString(R.string.country_unknown)
        }
    }
    
    /**
     * تنسيق الإيبان مع مسافات
     */
    fun formatIban(iban: String): String {
        val clean = iban.replace(" ", "").uppercase()
        return clean.chunked(4).joinToString(" ")
    }
    
    /**
     * استخراج الإيبان من النص (للكاميرا)
     */
    fun extractIbanFromText(text: String): List<String> {
        val ibans = mutableListOf<String>()
        
        // البحث عن أنماط الإيبان في النص
        val countryCodesPattern = supportedCountries.keys.joinToString("|")
        val pattern = Regex("($countryCodesPattern)[0-9]{2}[A-Z0-9\\s]{15,34}", RegexOption.IGNORE_CASE)
        
        val matches = pattern.findAll(text.uppercase().replace("O", "0").replace("I", "1"))
        
        for (match in matches) {
            val candidate = match.value.replace(" ", "")
            val countryCode = candidate.substring(0, 2)
            val expectedLength = supportedCountries[countryCode]
            
            if (expectedLength != null && candidate.length == expectedLength) {
                ibans.add(candidate)
            }
        }
        
        return ibans.distinct()
    }
}
