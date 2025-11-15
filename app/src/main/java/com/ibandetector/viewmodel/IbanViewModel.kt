package com.ibandetector.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.ibandetector.data.IbanDatabase
import com.ibandetector.data.IbanEntity
import com.ibandetector.data.IbanRepository
import kotlinx.coroutines.launch

class IbanViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: IbanRepository
    val allIbans: LiveData<List<IbanEntity>>
    
    init {
        val ibanDao = IbanDatabase.getDatabase(application).ibanDao()
        repository = IbanRepository(ibanDao)
        allIbans = repository.allIbans
    }
    
    fun insertIban(iban: IbanEntity) = viewModelScope.launch {
        repository.insertIban(iban)
    }
    
    fun deleteIban(iban: IbanEntity) = viewModelScope.launch {
        repository.deleteIban(iban)
    }
    
    fun deleteAllIbans() = viewModelScope.launch {
        repository.deleteAllIbans()
    }
    
    suspend fun getIbanByNumber(ibanNumber: String): IbanEntity? {
        return repository.getIbanByNumber(ibanNumber)
    }
    
    suspend fun getCount(): Int {
        return repository.getCount()
    }
    
    fun searchIbans(query: String): LiveData<List<IbanEntity>> {
        return repository.searchIbans(query)
    }
}
