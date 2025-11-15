package com.ibandetector

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ibandetector.adapter.IbanHistoryAdapter
import com.ibandetector.data.IbanEntity
import com.ibandetector.databinding.ActivityHistoryBinding
import com.ibandetector.viewmodel.IbanViewModel

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var viewModel: IbanViewModel
    private lateinit var adapter: IbanHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[IbanViewModel::class.java]

        setupToolbar()
        setupRecyclerView()
        setupSearch()
        setupObservers()
        setupListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = IbanHistoryAdapter(
            onCopyClick = { iban ->
                copyToClipboard(iban.ibanNumber)
            },
            onDeleteClick = { iban ->
                showDeleteConfirmation(iban)
            }
        )

        binding.historyRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.historyRecyclerView.adapter = adapter
    }

    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                if (query.isEmpty()) {
                    observeAllIbans()
                } else {
                    observeSearchResults(query)
                }
            }
        })
    }

    private fun setupObservers() {
        observeAllIbans()
    }

    private fun observeAllIbans() {
        viewModel.allIbans.observe(this) { ibans ->
            updateList(ibans)
        }
    }

    private fun observeSearchResults(query: String) {
        viewModel.searchIbans(query).observe(this) { ibans ->
            updateList(ibans)
        }
    }

    private fun updateList(ibans: List<IbanEntity>) {
        if (ibans.isEmpty()) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.historyRecyclerView.visibility = View.GONE
            binding.deleteAllFab.hide()
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.historyRecyclerView.visibility = View.VISIBLE
            binding.deleteAllFab.show()
        }
        adapter.submitList(ibans)
    }

    private fun setupListeners() {
        binding.deleteAllFab.setOnClickListener {
            showDeleteAllConfirmation()
        }
    }

    private fun copyToClipboard(iban: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("IBAN", iban)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
    }

    private fun showDeleteConfirmation(iban: IbanEntity) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.delete))
            .setMessage(getString(R.string.delete_confirmation))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                viewModel.deleteIban(iban)
                Toast.makeText(this, getString(R.string.delete), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showDeleteAllConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.delete_all))
            .setMessage("هل تريد حذف جميع الإيبانات المحفوظة?")
            .setPositiveButton(getString(R.string.delete_all)) { _, _ ->
                viewModel.deleteAllIbans()
                Toast.makeText(this, getString(R.string.delete_all), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
}
