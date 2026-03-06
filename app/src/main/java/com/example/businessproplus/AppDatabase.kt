package com.example.businessproplus
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Order::class, Party::class, Item::class, MissedItem::class, User::class, Category::class, UserActivity::class, CalculationHistory::class], version = 31, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun orderDao(): OrderDao
    abstract fun partyDao(): PartyDao
    abstract fun itemDao(): ItemDao
    abstract fun missedItemDao(): MissedItemDao
    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun userActivityDao(): UserActivityDao
    abstract fun calculationHistoryDao(): CalculationHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private fun addColumnSafely(db: SupportSQLiteDatabase, tableName: String, columnName: String, columnDef: String) {
            try {
                db.execSQL("ALTER TABLE $tableName ADD COLUMN $columnName $columnDef")
            } catch (e: Exception) {
                // Column likely exists
            }
        }

        private val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnSafely(db, "orders_table", "deliveredOn", "TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE TABLE IF NOT EXISTS `user_activity_table` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `userId` INTEGER NOT NULL, `userName` TEXT NOT NULL, `action` TEXT NOT NULL, `timestamp` TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `calculation_history_table` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `voltage` TEXT NOT NULL, `ampere` TEXT NOT NULL, `ohms` TEXT NOT NULL, `watts` TEXT NOT NULL, `timestamp` TEXT NOT NULL)")
            }
        }

        private val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnSafely(db, "items_table", "photoPath", "TEXT")
                addColumnSafely(db, "items_table", "videoPath", "TEXT")
            }
        }

        private val MIGRATION_23_30 = object : Migration(23, 30) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnSafely(db, "users_table", "lastLogin", "TEXT")
            }
        }

        private val MIGRATION_30_31 = object : Migration(30, 31) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnSafely(db, "orders_table", "isPaid", "INTEGER NOT NULL DEFAULT 0")
                addColumnSafely(db, "orders_table", "dueAmount", "REAL NOT NULL DEFAULT 0.0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "business_pro_database"
                )
                    .addMigrations(MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_30, MIGRATION_30_31)
                    .fallbackToDestructiveMigration(dropAllTables = false) // 🛡️ CRITICAL FIX: Prevent freeze/crash if migration paths are missing
                    .addCallback(DatabaseCallback(context))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            if (database.userDao().getUserCount() == 0) {
                                database.userDao().insertUser(User(username = "Master Admin", pinCode = "1234", role = "Admin"))
                            }
                        } catch (e: Exception) {}
                    }
                }
            }
        }
    }
}