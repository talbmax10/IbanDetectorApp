package com.ibandetector.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [IbanEntity::class], version = 1, exportSchema = false)
abstract class IbanDatabase : RoomDatabase() {
    
    abstract fun ibanDao(): IbanDao
    
    companion object {
        @Volatile
        private var INSTANCE: IbanDatabase? = null
        
        fun getDatabase(context: Context): IbanDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    IbanDatabase::class.java,
                    "iban_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
