package com.ibandetector.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface IbanDao {
    
    @Query("SELECT * FROM iban_history ORDER BY timestamp DESC")
    fun getAllIbans(): LiveData<List<IbanEntity>>
    
    @Query("SELECT * FROM iban_history WHERE ibanNumber LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchIbans(query: String): LiveData<List<IbanEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIban(iban: IbanEntity): Long
    
    @Delete
    suspend fun deleteIban(iban: IbanEntity)
    
    @Query("DELETE FROM iban_history")
    suspend fun deleteAllIbans()
    
    @Query("SELECT * FROM iban_history WHERE ibanNumber = :ibanNumber LIMIT 1")
    suspend fun getIbanByNumber(ibanNumber: String): IbanEntity?
    
    @Query("SELECT COUNT(*) FROM iban_history")
    suspend fun getCount(): Int
}
