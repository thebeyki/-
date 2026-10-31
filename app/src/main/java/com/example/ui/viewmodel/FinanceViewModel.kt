package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.model.BudgetEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.TransactionEntity
import com.example.data.repository.FinanceRepository
import com.example.util.BiometricLockHelper
import com.example.util.ShamsiDate
import com.example.util.ShamsiDateHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategorySpending(
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String,
    val colorHex: Long,
    val totalAmount: Long,
    val percentage: Float
)

data class BudgetProgress(
    val budgetId: Long,
    val categoryId: Long,
    val categoryName: String,
    val monthlyLimit: Long,
    val spentAmount: Long,
    val percentage: Float,
    val remainingAmount: Long,
    val isExceeded: Boolean
)

data class DaySpending(
    val day: Int,
    val amount: Long
)

data class MonthTrend(
    val monthName: String,
    val year: Int,
    val month: Int,
    val income: Long,
    val expense: Long
)

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = FinanceRepository(
        database.transactionDao(),
        database.categoryDao(),
        database.budgetDao()
    )
    val securityHelper = BiometricLockHelper(application)

    // Current Selected Month state for dashboard and charts
    private val _selectedShamsiDate = MutableStateFlow(ShamsiDateHelper.currentShamsiDate())
    val selectedShamsiDate: StateFlow<ShamsiDate> = _selectedShamsiDate.asStateFlow()

    // Filter states
    val filterType = MutableStateFlow("ALL") // "ALL", "EXPENSE", "INCOME"
    val filterCategoryId = MutableStateFlow<Long?>(null)
    val searchQuery = MutableStateFlow("")

    // Security state
    private val _isLocked = MutableStateFlow(securityHelper.isLockEnabled)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBudgets: StateFlow<List<BudgetEntity>> = repository.allBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered transactions for the current selected month
    val currentMonthTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        _selectedShamsiDate
    ) { txList, shamsi ->
        val (start, end) = ShamsiDateHelper.getMonthTimestampRange(shamsi.year, shamsi.month)
        txList.filter { it.timestamp in start..end }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Transactions displayed in dashboard with search and category filters
    val filteredDashboardTransactions: StateFlow<List<TransactionEntity>> = combine(
        currentMonthTransactions,
        filterType,
        filterCategoryId,
        searchQuery
    ) { txList, type, catId, query ->
        txList.filter { tx ->
            val matchesType = when (type) {
                "EXPENSE" -> tx.type == "EXPENSE"
                "INCOME" -> tx.type == "INCOME"
                else -> true
            }
            val matchesCat = (catId == null || tx.categoryId == catId)
            val matchesQuery = query.isBlank() ||
                    tx.title.contains(query, ignoreCase = true) ||
                    tx.categoryName.contains(query, ignoreCase = true) ||
                    tx.note.contains(query, ignoreCase = true)

            matchesType && matchesCat && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Monthly totals
    val monthlyIncome: StateFlow<Long> = currentMonthTransactions.combine(allTransactions) { monthTx, _ ->
        monthTx.filter { it.type == "INCOME" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val monthlyExpense: StateFlow<Long> = currentMonthTransactions.combine(allTransactions) { monthTx, _ ->
        monthTx.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val totalOverallBalance: StateFlow<Long> = allTransactions.combine(allTransactions) { list, _ ->
        val income = list.filter { it.type == "INCOME" }.sumOf { it.amount }
        val expense = list.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        income - expense
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Category spending breakdown for charts
    val categorySpendings: StateFlow<List<CategorySpending>> = combine(
        currentMonthTransactions,
        allCategories
    ) { txList, _ ->
        val expenses = txList.filter { it.type == "EXPENSE" }
        val totalExp = expenses.sumOf { it.amount }.toFloat()

        if (totalExp <= 0) {
            emptyList()
        } else {
            expenses.groupBy { it.categoryId }.map { (catId, group) ->
                val first = group.first()
                val sum = group.sumOf { it.amount }
                CategorySpending(
                    categoryId = catId,
                    categoryName = first.categoryName,
                    categoryIcon = first.categoryIcon,
                    colorHex = first.categoryColor,
                    totalAmount = sum,
                    percentage = (sum / totalExp) * 100f
                )
            }.sortedByDescending { it.totalAmount }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Budget progress
    val budgetProgressList: StateFlow<List<BudgetProgress>> = combine(
        allBudgets,
        currentMonthTransactions
    ) { budgets, monthTx ->
        val expenseByCat = monthTx.filter { it.type == "EXPENSE" }
            .groupBy { it.categoryId }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        budgets.map { b ->
            val spent = expenseByCat[b.categoryId] ?: 0L
            val pct = if (b.monthlyLimit > 0) (spent.toFloat() / b.monthlyLimit.toFloat()) * 100f else 0f
            BudgetProgress(
                budgetId = b.id,
                categoryId = b.categoryId,
                categoryName = b.categoryName,
                monthlyLimit = b.monthlyLimit,
                spentAmount = spent,
                percentage = pct,
                remainingAmount = (b.monthlyLimit - spent).coerceAtLeast(0L),
                isExceeded = spent > b.monthlyLimit
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Daily spending data for chart
    val dailySpendings: StateFlow<List<DaySpending>> = combine(
        currentMonthTransactions,
        _selectedShamsiDate
    ) { monthTx, shamsi ->
        val daysInMonth = if (shamsi.month <= 6) 31 else if (shamsi.month <= 11) 30 else 29
        val expenses = monthTx.filter { it.type == "EXPENSE" }

        val map = (1..daysInMonth).associateWith { 0L }.toMutableMap()
        expenses.forEach { tx ->
            val txShamsi = ShamsiDateHelper.fromTimestamp(tx.timestamp)
            if (txShamsi.month == shamsi.month && txShamsi.year == shamsi.year) {
                map[txShamsi.day] = (map[txShamsi.day] ?: 0L) + tx.amount
            }
        }
        map.map { DaySpending(it.key, it.value) }.sortedBy { it.day }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Past 6 months trends
    val monthlyTrends: StateFlow<List<MonthTrend>> = combine(
        allTransactions,
        _selectedShamsiDate
    ) { txList, curShamsi ->
        val trends = mutableListOf<MonthTrend>()
        var y = curShamsi.year
        var m = curShamsi.month

        for (i in 0 until 6) {
            val (start, end) = ShamsiDateHelper.getMonthTimestampRange(y, m)
            val monthItems = txList.filter { it.timestamp in start..end }
            val inc = monthItems.filter { it.type == "INCOME" }.sumOf { it.amount }
            val exp = monthItems.filter { it.type == "EXPENSE" }.sumOf { it.amount }

            trends.add(
                MonthTrend(
                    monthName = ShamsiDateHelper.getMonthName(m),
                    year = y,
                    month = m,
                    income = inc,
                    expense = exp
                )
            )

            m--
            if (m < 1) {
                m = 12
                y--
            }
        }
        trends.reversed()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun nextMonth() {
        val cur = _selectedShamsiDate.value
        val (newYear, newMonth) = if (cur.month == 12) Pair(cur.year + 1, 1) else Pair(cur.year, cur.month + 1)
        _selectedShamsiDate.value = ShamsiDate(newYear, newMonth, 1)
    }

    fun previousMonth() {
        val cur = _selectedShamsiDate.value
        val (newYear, newMonth) = if (cur.month == 1) Pair(cur.year - 1, 12) else Pair(cur.year, cur.month - 1)
        _selectedShamsiDate.value = ShamsiDate(newYear, newMonth, 1)
    }

    fun resetToCurrentMonth() {
        _selectedShamsiDate.value = ShamsiDateHelper.currentShamsiDate()
    }

    // CRUD Transactions
    fun addTransaction(
        title: String,
        amount: Long,
        type: String,
        category: CategoryEntity,
        timestamp: Long,
        paymentMethod: String,
        note: String
    ) {
        viewModelScope.launch {
            val shamsiDate = ShamsiDateHelper.fromTimestamp(timestamp).format(false)
            val tx = TransactionEntity(
                title = title,
                amount = amount,
                type = type,
                categoryId = category.id,
                categoryName = category.name,
                categoryIcon = category.iconName,
                categoryColor = category.colorHex,
                timestamp = timestamp,
                shamsiDate = shamsiDate,
                paymentMethod = paymentMethod,
                note = note
            )
            repository.insertTransaction(tx)
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun deleteTransactionById(id: Long) {
        viewModelScope.launch {
            repository.deleteTransactionById(id)
        }
    }

    // Category CRUD
    fun addCategory(name: String, type: String, iconName: String, colorHex: Long) {
        viewModelScope.launch {
            val cat = CategoryEntity(
                name = name,
                type = type,
                iconName = iconName,
                colorHex = colorHex
            )
            repository.insertCategory(cat)
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    // Budget CRUD
    fun setBudget(categoryId: Long, categoryName: String, monthlyLimit: Long) {
        viewModelScope.launch {
            val budget = BudgetEntity(
                categoryId = categoryId,
                categoryName = categoryName,
                monthlyLimit = monthlyLimit
            )
            repository.insertOrUpdateBudget(budget)
        }
    }

    fun deleteBudget(budget: BudgetEntity) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
        }
    }

    // Security Actions
    fun unlockApp() {
        _isLocked.value = false
    }

    fun lockApp() {
        if (securityHelper.isLockEnabled) {
            _isLocked.value = true
        }
    }

    fun verifyPin(enteredPin: String): Boolean {
        return enteredPin == securityHelper.pinCode
    }

    fun enableSecurity(pin: String, allowBiometrics: Boolean) {
        securityHelper.pinCode = pin
        securityHelper.isBiometricAllowed = allowBiometrics
        securityHelper.isLockEnabled = true
        _isLocked.value = false
    }

    fun disableSecurity() {
        securityHelper.isLockEnabled = false
        _isLocked.value = false
    }
}
