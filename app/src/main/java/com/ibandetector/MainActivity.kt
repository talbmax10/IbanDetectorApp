package com.ibandetector

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.ibandetector.data.IbanEntity
import com.ibandetector.databinding.ActivityMainBinding
import com.ibandetector.utils.IbanValidator
import com.ibandetector.viewmodel.IbanViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: IbanViewModel
    private var lastValidationResult: Pair<String, Boolean>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[IbanViewModel::class.java]

        setupViews()
        setupListeners()
        handleIntentData()
    }

    private fun setupViews() {
        // إعداد الرسوم المتحركة
        val slideUp = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)
        binding.mainCard.startAnimation(slideUp)
    }

    private fun setupListeners() {
        // زر التحقق
        binding.verifyButton.setOnClickListener {
            val iban = binding.ibanEditText.text.toString()
            if (iban.isNotEmpty()) {
                verifyIban(iban)
            } else {
                showError(getString(R.string.error_empty_iban))
            }
        }

        // زر المسح
        binding.clearButton.setOnClickListener {
            clearAll()
        }

        // زر النسخ
        binding.copyButton.setOnClickListener {
            copyToClipboard()
        }

        // زر الحفظ
        binding.saveButton.setOnClickListener {
            saveIban()
        }

        // زر الكاميرا
        binding.cameraScanButton.setOnClickListener {
            startActivity(Intent(this, CameraScanActivity::class.java))
        }

        // زر السجل
        binding.historyButton.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        // مراقب تغيير النص
        binding.ibanEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty()) {
                    hideValidationResult()
                }
            }
        })
    }

    private fun verifyIban(iban: String) {
        val result = IbanValidator.validateIban(iban, this)
        
        lastValidationResult = Pair(iban.replace(" ", "").uppercase(), result.isValid)
        
        showValidationResult(result)
    }

    private fun showValidationResult(result: com.ibandetector.utils.IbanValidationResult) {
        binding.validationResult.visibility = View.VISIBLE
        binding.actionButtonsLayout.visibility = if (result.isValid) View.VISIBLE else View.GONE

        // تنسيق الإيبان وعرضه
        val formattedIban = IbanValidator.formatIban(binding.ibanEditText.text.toString())
        binding.ibanEditText.setText(formattedIban)
        binding.ibanEditText.setSelection(formattedIban.length)

        if (result.isValid) {
            binding.validationStatusText.text = getString(R.string.iban_valid)
            binding.validationStatusText.setTextColor(getColor(R.color.success))
            binding.validationResult.setBackgroundResource(R.drawable.result_background)
            
            binding.countryText.text = "${getString(R.string.country_label)} ${result.countryName}"
            binding.lengthText.text = "${getString(R.string.length_label)} ${result.length}"
            
            // رسوم متحركة
            val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
            binding.validationResult.startAnimation(fadeIn)
            
        } else {
            binding.validationStatusText.text = getString(R.string.iban_invalid)
            binding.validationStatusText.setTextColor(getColor(R.color.error))
            
            var errorText = result.errorMessage ?: getString(R.string.error_invalid_format)
            
            if (result.country.isNotEmpty()) {
                errorText += "\n${getString(R.string.country_label)} ${result.countryName}"
            }
            
            if (result.length > 0) {
                errorText += "\n${getString(R.string.length_label)} ${result.length}"
                if (result.expectedLength > 0) {
                    errorText += " (المتوقع: ${result.expectedLength})"
                }
            }
            
            binding.countryText.text = errorText
            binding.lengthText.visibility = View.GONE
        }
    }

    private fun hideValidationResult() {
        binding.validationResult.visibility = View.GONE
        binding.actionButtonsLayout.visibility = View.GONE
        lastValidationResult = null
    }

    private fun clearAll() {
        binding.ibanEditText.text?.clear()
        hideValidationResult()
    }

    private fun copyToClipboard() {
        val iban = binding.ibanEditText.text.toString()
        if (iban.isNotEmpty()) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("IBAN", iban)
            clipboard.setPrimaryClip(clip)
            
            Toast.makeText(this, getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveIban() {
        val result = lastValidationResult
        if (result != null && result.second) {
            lifecycleScope.launch {
                val existingIban = viewModel.getIbanByNumber(result.first)
                if (existingIban != null) {
                    showSnackbar(getString(R.string.already_saved))
                } else {
                    val validationResult = IbanValidator.validateIban(result.first, this@MainActivity)
                    val entity = IbanEntity(
                        ibanNumber = result.first,
                        country = validationResult.country,
                        countryName = validationResult.countryName,
                        isValid = true
                    )
                    viewModel.insertIban(entity)
                    showSnackbar(getString(R.string.saved_to_history))
                }
            }
        }
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
            .setBackgroundTint(getColor(R.color.error))
            .setTextColor(getColor(R.color.white))
            .show()
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
            .setBackgroundTint(getColor(R.color.success))
            .setTextColor(getColor(R.color.white))
            .show()
    }

    private fun handleIntentData() {
        val ibanFromIntent = intent.getStringExtra("IBAN")
        if (!ibanFromIntent.isNullOrEmpty()) {
            binding.ibanEditText.setText(ibanFromIntent)
            verifyIban(ibanFromIntent)
        }
    }

    override fun onResume() {
        super.onResume()
        handleIntentData()
    }
}
