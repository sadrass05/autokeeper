package com.example.autobookkeeper.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.autobookkeeper.BuildConfig
import com.example.autobookkeeper.data.repository.CategoryRepository
import com.example.autobookkeeper.data.repository.ExpenseRepository
import com.example.autobookkeeper.data.repository.FinanceExpenseRepository
import com.example.autobookkeeper.data.repository.FinanceRepository
import com.example.autobookkeeper.data.FinancePrefs
import com.example.autobookkeeper.data.entity.Category
import com.example.autobookkeeper.data.entity.ExpenseRecord
import com.example.autobookkeeper.data.entity.FinanceExpense
import com.example.autobookkeeper.data.entity.FinancePosition
import com.example.autobookkeeper.network.MySqlApi
import com.example.autobookkeeper.ui.importdata.FinanceImportParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DailyExpense(val date: String, val amount: Float)
data class MonthlyExpense(val year: Int, val month: Int, val amount: Float, val label: String)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val financeRepository: FinanceRepository,
    private val financeExpenseRepository: FinanceExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val mySqlApi: MySqlApi,
    private val financePrefs: FinancePrefs
) : ViewModel() {

    private val _expenses = MutableStateFlow<List<ExpenseRecord>>(emptyList())
    val expenses: StateFlow<List<ExpenseRecord>> = _expenses.asStateFlow()

    private val _positions = MutableStateFlow<List<FinancePosition>>(emptyList())
    val positions: StateFlow<List<FinancePosition>> = _positions.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _monthlyExpense = MutableStateFlow(0.0)
    val monthlyExpense: StateFlow<Double> = _monthlyExpense.asStateFlow()

    private val _totalProfit = MutableStateFlow(0.0)
    val totalProfit: StateFlow<Double> = _totalProfit.asStateFlow()

    private val _dailyExpense = MutableStateFlow(0.0)
    val dailyExpense: StateFlow<Double> = _dailyExpense.asStateFlow()

    private val _trendData = MutableStateFlow<List<DailyExpense>>(emptyList())
    val trendData: StateFlow<List<DailyExpense>> = _trendData.asStateFlow()

    private val _monthlyStats = MutableStateFlow<List<MonthlyExpense>>(emptyList())
    val monthlyStats: StateFlow<List<MonthlyExpense>> = _monthlyStats.asStateFlow()

    val topExpenses: StateFlow<List<ExpenseRecord>> = expenses
        .map { list ->
            val cal = Calendar.getInstance()
            val currentYear = cal.get(Calendar.YEAR)
            val currentMonth = cal.get(Calendar.MONTH)
            list.filter { record ->
                val recordCal = Calendar.getInstance().apply {
                    timeInMillis = record.recordedAt
                }
                recordCal.get(Calendar.YEAR) == currentYear &&
                        recordCal.get(Calendar.MONTH) == currentMonth
            }
                .sortedByDescending { it.amount }
                .take(5)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _todayCategoryData = MutableStateFlow<List<Pair<String, Double>>>(emptyList())
    val todayCategoryData: StateFlow<List<Pair<String, Double>>> = _todayCategoryData.asStateFlow()

    private val _financeExpense = MutableStateFlow(0.0)
    val financeExpense: StateFlow<Double> = _financeExpense.asStateFlow()

    private val _financeExpenses = MutableStateFlow<List<FinanceExpense>>(emptyList())
    val financeExpenses: StateFlow<List<FinanceExpense>> = _financeExpenses.asStateFlow()

    private val _netFinanceProfit = MutableStateFlow(0.0)
    val netFinanceProfit: StateFlow<Double> = _netFinanceProfit.asStateFlow()

    private val _accumulatedProfit = MutableStateFlow(financePrefs.accumulatedProfit)
    val accumulatedProfit: StateFlow<Double> = _accumulatedProfit.asStateFlow()

    private val _financeExpenseRecords = MutableStateFlow<List<ExpenseRecord>>(emptyList())
    val financeExpenseRecords: StateFlow<List<ExpenseRecord>> = _financeExpenseRecords.asStateFlow()

    private val _deletedExpenses = MutableStateFlow<List<ExpenseRecord>>(emptyList())
    val deletedExpenses: StateFlow<List<ExpenseRecord>> = _deletedExpenses.asStateFlow()

    init {
        loadData()
        initCategories()
        calculateDailyExpense()
        loadTrendData()
        loadMonthlyStats()
        loadTodayCategoryData()

        // 仅 Pro 版加载理财数据
        if (BuildConfig.IS_PRO) {
            loadFinanceExpenses()
        }

        cleanExpiredTrash()
        loadDeletedExpenses()
    }

    private fun loadData() {
        viewModelScope.launch {
            expenseRepository.getAllExpenses().collect {
                _expenses.value = it
                calculateMonthlyExpense()
                calculateDailyExpense()
                loadTrendData()
                loadMonthlyStats()
                loadTodayCategoryData()
            }
        }

        viewModelScope.launch {
            financeRepository.getAllPositions().collect {
                _positions.value = it
                calculateTotalProfit()
            }
        }

        viewModelScope.launch {
            financeExpense.collect { calculateNetFinanceProfit() }
        }

        viewModelScope.launch {
            accumulatedProfit.collect { calculateNetFinanceProfit() }
        }

        viewModelScope.launch {
            categoryRepository.getAllCategories().collect {
                _categories.value = it
            }
        }

        viewModelScope.launch {
            financeExpenseRepository.getAllExpenses().collect {
                _financeExpenses.value = it
                calculateFinanceExpense()
            }
        }

        viewModelScope.launch {
            expenseRepository.getFinanceFlaggedExpenses().collect {
                _financeExpenseRecords.value = it.sortedByDescending { e -> e.amount }
            }
        }
    }

    private fun initCategories() {
        viewModelScope.launch {
            categoryRepository.initDefaultCategories()
        }
    }

    private fun calculateMonthlyExpense() {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val startOfMonth = calendar.timeInMillis

            calendar.add(Calendar.MONTH, 1)
            calendar.add(Calendar.MILLISECOND, -1)
            val endOfMonth = calendar.timeInMillis

            _monthlyExpense.value = expenseRepository.getTotalExpenseByMonth(startOfMonth, endOfMonth)
        }
    }

    private fun calculateDailyExpense() {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endOfDay = calendar.timeInMillis
            _dailyExpense.value = expenseRepository.getTotalExpenseByDay(startOfDay, endOfDay) ?: 0.0
        }
    }

    private fun calculateTotalProfit() {
        viewModelScope.launch {
            _totalProfit.value = financeRepository.getTotalProfit()
        }
    }

    private fun calculateFinanceExpense() {
        viewModelScope.launch {
            val newTableTotal = financeExpenseRepository.getTotalExpenses()
            val oldTableTotal = expenseRepository.getTotalFinanceFlaggedExpense()
            _financeExpense.value = newTableTotal + oldTableTotal
        }
    }

    private fun loadFinanceExpenses() {
        viewModelScope.launch {
            combine(
                financeExpenseRepository.getAllExpenses(),
                expenseRepository.getFinanceFlaggedExpenses()
            ) { newRecords, oldRecords ->
                val converted = oldRecords.map { record ->
                    FinanceExpense(
                        amount = record.amount,
                        description = record.merchant.ifEmpty { record.category },
                        fromProduct = record.platform,
                        recordedAt = record.recordedAt
                    )
                }
                (newRecords + converted).sortedByDescending { it.recordedAt }
            }.collect { _financeExpenses.value = it }
        }
    }

    private fun calculateNetFinanceProfit() {
        viewModelScope.launch {
            _netFinanceProfit.value = _accumulatedProfit.value - _financeExpense.value
        }
    }

    private fun cleanExpiredTrash() {
        viewModelScope.launch {
            val threshold = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
            expenseRepository.deleteExpired(threshold)
        }
    }

    private fun loadDeletedExpenses() {
        viewModelScope.launch {
            expenseRepository.getDeletedExpenses().collect {
                _deletedExpenses.value = it
            }
        }
    }

    fun loadTrendData() {
        viewModelScope.launch {
            val dateFormat = SimpleDateFormat("MM-dd", Locale.getDefault())
            val result = mutableListOf<DailyExpense>()

            for (i in 6 downTo 0) {
                val dayCal = Calendar.getInstance()
                dayCal.add(Calendar.DAY_OF_YEAR, -i)
                dayCal.set(Calendar.HOUR_OF_DAY, 0)
                dayCal.set(Calendar.MINUTE, 0)
                dayCal.set(Calendar.SECOND, 0)
                dayCal.set(Calendar.MILLISECOND, 0)
                val dayStart = dayCal.timeInMillis

                val endCal = dayCal.clone() as Calendar
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                endCal.set(Calendar.SECOND, 59)
                endCal.set(Calendar.MILLISECOND, 999)
                val dayEnd = endCal.timeInMillis

                val amount = expenseRepository.getExpenseForDay(dayStart, dayEnd)
                result.add(DailyExpense(
                    date = dateFormat.format(Date(dayStart)),
                    amount = amount.toFloat()
                ))
            }
            _trendData.value = result
        }
    }

    fun loadMonthlyStats() {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            val result = mutableListOf<MonthlyExpense>()
            repeat(6) { i ->
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH) + 1
                val startOfMonth = calendar.apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                calendar.add(Calendar.MONTH, 1)
                val endOfMonth = calendar.timeInMillis - 1
                val amount = expenseRepository
                    .getTotalExpenseByMonth(startOfMonth, endOfMonth).toFloat()
                val label = if (i == 0 || month == 1) "${year}\n${month}月" else "${month}月"
                result.add(0, MonthlyExpense(year, month, amount, label))
                calendar.add(Calendar.MONTH, -2)
            }
            _monthlyStats.value = result
        }
    }

    fun loadTodayCategoryData() {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val dayStart = calendar.timeInMillis
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val dayEnd = calendar.timeInMillis
            _todayCategoryData.value = expenseRepository.getTodayExpenseByCategory(dayStart, dayEnd)
        }
    }

    suspend fun updateExpenseCategory(expenseId: Long, category: String) {
        val expense = _expenses.value.find { it.id == expenseId }
        expense?.let {
            val updated = it.copy(category = category)
            expenseRepository.updateExpense(updated)
        }
    }

    suspend fun updateExpenseFinanceFlag(expenseId: Long, isFinance: Boolean) {
        val expense = _expenses.value.find { it.id == expenseId }
        expense?.let {
            val updated = it.copy(isFinanceExpense = isFinance)
            expenseRepository.updateExpense(updated)
        }
    }

    fun softDeleteExpense(id: Long) {
        viewModelScope.launch {
            expenseRepository.softDelete(id)
        }
    }

    fun restoreExpense(id: Long) {
        viewModelScope.launch {
            expenseRepository.restoreExpense(id)
        }
    }

    fun permanentlyDeleteExpense(id: Long) {
        viewModelScope.launch {
            expenseRepository.deleteExpenseById(id)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            expenseRepository.deleteAllDeleted()
        }
    }

    fun addExpense(record: ExpenseRecord) {
        viewModelScope.launch {
            expenseRepository.insertExpense(record)
        }
    }

    fun addFinancePosition(position: FinancePosition) {
        viewModelScope.launch {
            financeRepository.upsertPosition(position)
        }
    }

    fun deleteFinancePosition(id: Long) {
        viewModelScope.launch {
            financeRepository.deletePositionById(id)
        }
    }

    fun deleteFinancePositions(ids: List<Long>) {
        viewModelScope.launch {
            financeRepository.deleteByIds(ids)
        }
    }

    fun updateFinancePosition(position: FinancePosition) {
        viewModelScope.launch {
            financeRepository.updatePosition(position)
        }
    }

    fun addFinancePositions(parsedList: List<FinanceImportParser.ParsedPosition>) {
        viewModelScope.launch {
            parsedList.forEach { parsed ->
                val position = FinancePosition(
                    productName = parsed.productName,
                    platform = parsed.platform,
                    buyAmount = parsed.buyAmount,
                    currentValue = parsed.currentValue,
                    profit = parsed.profit,
                    profitRate = parsed.profitRate,
                    screenshotPath = "",
                    updatedAt = System.currentTimeMillis()
                )
                financeRepository.upsertPosition(position)
            }
        }
    }

    fun addFinancePositionsFromList(positions: List<FinancePosition>) {
        viewModelScope.launch {
            financeRepository.insertPositions(positions)
        }
    }

    fun setAccumulatedProfit(value: Double) {
        financePrefs.accumulatedProfit = value
        _accumulatedProfit.value = value
    }

    fun addFinanceExpense(expense: FinanceExpense) {
        viewModelScope.launch {
            financeExpenseRepository.insert(expense)
        }
    }

    fun deleteFinanceExpense(expense: FinanceExpense) {
        viewModelScope.launch {
            financeExpenseRepository.delete(expense)
        }
    }

    suspend fun getNetProfit(): Double {
        val totalProfit = financeRepository.getTotalProfit()
        val totalExpense = expenseRepository.getTotalNonFinanceExpense()
        return totalProfit - totalExpense
    }

    // 已废弃：改用 SyncService + HTTP REST API 方案进行局域网同步
    // 详见 SettingsScreen 中的数据同步设置区域
    suspend fun syncToMySQL() {
        mySqlApi.syncExpenses(_expenses.value)
        mySqlApi.syncFinance(_positions.value)
    }

    suspend fun getAllExpensesForExport(): List<ExpenseRecord> {
        return expenseRepository.getAllExpenses().first().filter { !it.isDeleted }
    }

    suspend fun getAllPositionsForExport(): List<FinancePosition> {
        return financeRepository.getAllPositions().first()
    }
}
