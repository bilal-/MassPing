package dev.bilalahmad.massping.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Database(
    entities = [MessageHistory::class],
    version = 1,
    exportSchema = false
)
abstract class MassPingDatabase : RoomDatabase() {

    abstract fun messageHistoryDao(): MessageHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: MassPingDatabase? = null

        fun getDatabase(context: Context): MassPingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MassPingDatabase::class.java,
                    "massping_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}