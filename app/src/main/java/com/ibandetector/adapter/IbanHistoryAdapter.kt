package com.ibandetector.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ibandetector.data.IbanEntity
import com.ibandetector.databinding.ItemIbanHistoryBinding

class IbanHistoryAdapter(
    private val onCopyClick: (IbanEntity) -> Unit,
    private val onDeleteClick: (IbanEntity) -> Unit
) : ListAdapter<IbanEntity, IbanHistoryAdapter.IbanViewHolder>(IbanDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IbanViewHolder {
        val binding = ItemIbanHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return IbanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IbanViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class IbanViewHolder(
        private val binding: ItemIbanHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(iban: IbanEntity) {
            binding.countryFlag.text = iban.country
            binding.countryName.text = iban.countryName
            binding.ibanNumber.text = iban.getFormattedIban()
            binding.timestamp.text = iban.getFormattedDate()

            binding.copyButton.setOnClickListener {
                onCopyClick(iban)
            }

            binding.deleteButton.setOnClickListener {
                onDeleteClick(iban)
            }
        }
    }

    private class IbanDiffCallback : DiffUtil.ItemCallback<IbanEntity>() {
        override fun areItemsTheSame(oldItem: IbanEntity, newItem: IbanEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: IbanEntity, newItem: IbanEntity): Boolean {
            return oldItem == newItem
        }
    }
}
