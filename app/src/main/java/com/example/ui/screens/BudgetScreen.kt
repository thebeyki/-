package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AddBudgetDialog
import com.example.ui.components.BudgetCard
import com.example.ui.theme.ColorExpense
import com.example.ui.theme.ColorIncome
import com.example.ui.theme.ColorWarning
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.ShamsiDateHelper

@Composable
fun BudgetScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val budgetProgressList by viewModel.budgetProgressList.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()
    val selectedShamsi by viewModel.selectedShamsiDate.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    val totalBudgetLimit = budgetProgressList.sumOf { it.monthlyLimit }
    val totalBudgetSpent = budgetProgressList.sumOf { it.spentAmount }
    val totalPercentage = if (totalBudgetLimit > 0) {
        (totalBudgetSpent.toFloat() / totalBudgetLimit.toFloat()) * 100f
    } else 0f

    val hasExceededBudget = budgetProgressList.any { it.isExceeded }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("budget_screen_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Card: Total Budget Overview
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("total_budget_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF1E3A8A), // Deep blue
                                        Color(0xFF0F172A)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "مجموع بودجه‌بندی ماهانه",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "${selectedShamsi.monthName} ${ShamsiDateHelper.toPersianDigits(selectedShamsi.year.toString())}",
                                        color = Color.White,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${ShamsiDateHelper.toPersianDigits(String.format("%.0f", totalPercentage))}٪ مصرف",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Overall Progress Bar
                            val animatedTotalProgress by animateFloatAsState(
                                targetValue = (totalPercentage / 100f).coerceIn(0f, 1f),
                                animationSpec = tween(600),
                                label = "total_budget_prog"
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(animatedTotalProgress)
                                        .height(12.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (totalPercentage >= 100f) ColorExpense
                                            else if (totalPercentage >= 75f) ColorWarning
                                            else Color(0xFF34D399)
                                        )
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = "کل سقف بودجه", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                    Text(
                                        text = ShamsiDateHelper.formatAmount(totalBudgetLimit, "تومان"),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "مجموع مصرف شده", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                    Text(
                                        text = ShamsiDateHelper.formatAmount(totalBudgetSpent, "تومان"),
                                        color = if (totalPercentage >= 100f) Color(0xFFFFB4AB) else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Exceeded alert banner if any category is over budget
            if (hasExceededBudget) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = ColorExpense.copy(alpha = 0.12f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = null,
                                tint = ColorExpense,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "توجه: در یک یا چند دسته‌بندی از سقف بودجه تعیین شده عبور کرده‌اید!",
                                color = ColorExpense,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "بودجه‌بندی دسته‌بندی‌ها",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_open_add_budget")
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "افزودن بودجه", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Budget list or Empty State
            if (budgetProgressList.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PieChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "هنوز برای هیچ دسته‌ای بودجه تعیین نکرده‌اید",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "با تعیین سقف بودجه، هزینه‌های خود را کنترل کرده و از هشدارهای هوشمند استفاده کنید.",
                                color = MaterialTheme.colorScheme.outline,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(
                    items = budgetProgressList,
                    key = { it.budgetId }
                ) { b ->
                    BudgetCard(
                        budgetProgress = b,
                        onDelete = { budgetId ->
                            val budget = viewModel.allBudgets.value.firstOrNull { it.id == budgetId }
                            if (budget != null) viewModel.deleteBudget(budget)
                        }
                    )
                }
            }
        }

        // Dialog for setting budget
        if (showAddDialog) {
            AddBudgetDialog(
                categories = allCategories,
                onDismiss = { showAddDialog = false },
                onSave = { catId, catName, limit ->
                    viewModel.setBudget(catId, catName, limit)
                }
            )
        }
    }
}
