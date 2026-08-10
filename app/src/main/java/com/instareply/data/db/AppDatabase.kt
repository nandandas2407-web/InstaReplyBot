package com.instareply.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.instareply.data.model.Contact
import com.instareply.data.model.ReplyLog
import com.instareply.data.model.Rule

@Database(
    entities = [Rule::class, Contact::class, ReplyLog::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ruleDao(): RuleDao
    abstract fun contactDao(): ContactDao
    abstract fun replyLogDao(): ReplyLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "instareply_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
