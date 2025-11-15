package com.ibandetector.data

import androidx.lifecycle.LiveData

class IbanRepository(private val ibanDao: IbanDao) {
    
    val allIbans: LiveData<List<IbanEntity>> = ibanDao.getAllIbans()
    
    suspend fun insertIban(iban: IbanEntity): Long {
        return ibanDao.insertIban(iban)
    }
    
    suspend fun deleteIban(iban: IbanEntity) {
        ibanDao.deleteIban(iban)
    }
    
    suspend fun deleteAllIbans() {
        ibanDao.deleteAllIbans()
    }
    
    suspend fun getIbanByNumber(ibanNumber: String): IbanEntity? {
        return ibanDao.getIbanByNumber(ibanNumber)
    }
    
    suspend fun getCount(): Int {
        return ibanDao.getCount()
    }
    
    fun searchIbans(query: String): LiveData<List<IbanEntity>> {
        return ibanDao.searchIbans(query)
    }
}
