package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.BudgetDao
import com.example.data.dao.CategoryDao
import com.example.data.dao.TransactionDao
import com.example.data.model.BudgetEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.TransactionEntity
import com.example.util.ShamsiDateHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        BudgetEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hesabdar_local_database.db"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }

            private suspend fun populateInitialData(database: AppDatabase) {
                val categoryDao = database.categoryDao()
                val transactionDao = database.transactionDao()
                val budgetDao = database.budgetDao()

                // Default Expense Categories
                val expenseCategories = listOf(
                    CategoryEntity(id = 1, name = "خوراک و رستوران", type = "EXPENSE", iconName = "Restaurant", colorHex = 0xFFF97316, isDefault = true),
                    CategoryEntity(id = 2, name = "مسکن و اجاره", type = "EXPENSE", iconName = "Home", colorHex = 0xFF3B82F6, isDefault = true),
                    CategoryEntity(id = 3, name = "حمل و نقل", type = "EXPENSE", iconName = "DirectionsCar", colorHex = 0xFFEAB308, isDefault = true),
                    CategoryEntity(id = 4, name = "قبوض و شارژ", type = "EXPENSE", iconName = "Receipt", colorHex = 0xFF6366F1, isDefault = true),
                    CategoryEntity(id = 5, name = "خرید و پوشاک", type = "EXPENSE", iconName = "ShoppingBag", colorHex = 0xFFEC4899, isDefault = true),
                    CategoryEntity(id = 6, name = "سلامت و درمان", type = "EXPENSE", iconName = "LocalHospital", colorHex = 0xFFEF4444, isDefault = true),
                    CategoryEntity(id = 7, name = "آموزش و کتاب", type = "EXPENSE", iconName = "School", colorHex = 0xFF8B5CF6, isDefault = true),
                    CategoryEntity(id = 8, name = "تفریح و سرگرمی", type = "EXPENSE", iconName = "SportsEsports", colorHex = 0xFF14B8A6, isDefault = true),
                    CategoryEntity(id = 9, name = "سایر هزینه‌ها", type = "EXPENSE", iconName = "MoreHoriz", colorHex = 0xFF64748B, isDefault = true)
                )

                // Default Income Categories
                val incomeCategories = listOf(
                    CategoryEntity(id = 10, name = "حقوق و دستمزد", type = "INCOME", iconName = "Work", colorHex = 0xFF10B981, isDefault = true),
                    CategoryEntity(id = 11, name = "کسب و کار", type = "INCOME", iconName = "Store", colorHex = 0xFF059669, isDefault = true),
                    CategoryEntity(id = 12, name = "سرمایه‌گذاری و سود", type = "INCOME", iconName = "TrendingUp", colorHex = 0xFF0D9488, isDefault = true),
                    CategoryEntity(id = 13, name = "پاداش و کمیسیون", type = "INCOME", iconName = "CardGiftcard", colorHex = 0xFF22C55E, isDefault = true),
                    CategoryEntity(id = 14, name = "سایر درآمدها", type = "INCOME", iconName = "AccountBalanceWallet", colorHex = 0xFF3B82F6, isDefault = true)
                )

                categoryDao.insertAll(expenseCategories)
                categoryDao.insertAll(incomeCategories)

                // Initial realistic budgets
                val initialBudgets = listOf(
                    BudgetEntity(categoryId = 1, categoryName = "خوراک و رستوران", monthlyLimit = 6000000),
                    BudgetEntity(categoryId = 3, categoryName = "حمل و نقل", monthlyLimit = 2500000),
                    BudgetEntity(categoryId = 5, categoryName = "خرید و پوشاک", monthlyLimit = 4000000),
                    BudgetEntity(categoryId = 4, categoryName = "قبوض و شارژ", monthlyLimit = 1500000)
                )
                budgetDao.insertAll(initialBudgets)

                // Sample starter transactions in current month
                val now = System.currentTimeMillis()
                val dayMillis = 86400000L
                val currentShamsi = ShamsiDateHelper.fromTimestamp(now)

                val sampleTransactions = listOf(
                    TransactionEntity(
                        title = "حقوق ماهانه",
                        amount = 28000000,
                        type = "INCOME",
                        categoryId = 10,
                        categoryName = "حقوق و دستمزد",
                        categoryIcon = "Work",
                        categoryColor = 0xFF10B981,
                        timestamp = now - (dayMillis * 4),
                        shamsiDate = ShamsiDateHelper.fromTimestamp(now - (dayMillis * 4)).format(false),
                        paymentMethod = "انتقال بانکی",
                        note = "واریز حقوق آبان ماه"
                    ),
                    TransactionEntity(
                        title = "خرید ماهانه سوپرمارکت",
                        amount = 3200000,
                        type = "EXPENSE",
                        categoryId = 1,
                        categoryName = "خوراک و رستوران",
                        categoryIcon = "Restaurant",
                        categoryColor = 0xFFF97316,
                        timestamp = now - (dayMillis * 3),
                        shamsiDate = ShamsiDateHelper.fromTimestamp(now - (dayMillis * 3)).format(false),
                        paymentMethod = "کارت بانکی",
                        note = "خرید مواد غذایی، برنج و روغن"
                    ),
                    TransactionEntity(
                        title = "بنزین و سرویس خودرو",
                        amount = 850000,
                        type = "EXPENSE",
                        categoryId = 3,
                        categoryName = "حمل و نقل",
                        categoryIcon = "DirectionsCar",
                        categoryColor = 0xFFEAB308,
                        timestamp = now - (dayMillis * 2),
                        shamsiDate = ShamsiDateHelper.fromTimestamp(now - (dayMillis * 2)).format(false),
                        paymentMethod = "کارت بانکی",
                        note = "بنزین و تعویض روغن"
                    ),
                    TransactionEntity(
                        title = "شارژ ساختمان و اینترنت",
                        amount = 750000,
                        type = "EXPENSE",
                        categoryId = 4,
                        categoryName = "قبوض و شارژ",
                        categoryIcon = "Receipt",
                        categoryColor = 0xFF6366F1,
                        timestamp = now - dayMillis,
                        shamsiDate = ShamsiDateHelper.fromTimestamp(now - dayMillis).format(false),
                        paymentMethod = "کارت به کارت",
                        note = "شارژ واحد و بسته فیبر نوری"
                    ),
                    TransactionEntity(
                        title = "خرید کفش ورزشی",
                        amount = 1950000,
                        type = "EXPENSE",
                        categoryId = 5,
                        categoryName = "خرید و پوشاک",
                        categoryIcon = "ShoppingBag",
                        categoryColor = 0xFFEC4899,
                        timestamp = now,
                        shamsiDate = currentShamsi.format(false),
                        paymentMethod = "کارت بانکی",
                        note = "خرید کفش پیاده‌روی"
                    ),
                    TransactionEntity(
                        title = "پروژه جانبی طراحی",
                        amount = 6500000,
                        type = "INCOME",
                        categoryId = 11,
                        categoryName = "کسب و کار",
                        categoryIcon = "Store",
                        categoryColor = 0xFF059669,
                        timestamp = now - (dayMillis * 5),
                        shamsiDate = ShamsiDateHelper.fromTimestamp(now - (dayMillis * 5)).format(false),
                        paymentMethod = "پایا",
                        note = "طراحی لوگو و بنر تبلیغاتی"
                    )
                )

                transactionDao.insertAll(sampleTransactions)
            }
        }
    }
}
