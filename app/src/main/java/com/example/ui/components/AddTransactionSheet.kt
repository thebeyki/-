package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryEntity
import com.example.ui.theme.ColorExpense
import com.example.ui.theme.ColorIncome
import com.example.util.ShamsiDateHelper

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionSheet(
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        amount: Long,
        type: String,
        category: CategoryEntity,
        timestamp: Long,
        paymentMethod: String,
        note: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var type by remember { mutableStateOf("EXPENSE") } // "EXPENSE" or "INCOME"
    var amountText by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf("کارت بانکی") }
    var dayOffset by remember { mutableStateOf(0) } // 0 = today, 1 = yesterday

    val filteredCategories = categories.filter { it.type == type }
    var selectedCategory by remember(type, categories) {
        mutableStateOf(filteredCategories.firstOrNull())
    }

    val paymentMethods = listOf("کارت بانکی", "نقدی", "کارت به کارت", "پایا / ساتنا", "چک")
    val quickAmounts = listOf(100000L, 500000L, 1000000L, 2000000L, 5000000L)

    val parsedAmount = amountText.toLongOrNull() ?: 0L
    val isFormValid = parsedAmount > 0 && title.isNotBlank() && selectedCategory != null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (type == "EXPENSE") "ثبت هزینه جدید" else "ثبت درآمد جدید",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "بستن")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Income / Expense Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
            ) {
                // Expense tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (type == "EXPENSE") ColorExpense else Color.Transparent)
                        .clickable {
                            type = "EXPENSE"
                            selectedCategory = categories.firstOrNull { it.type == "EXPENSE" }
                        }
                        .padding(vertical = 10.dp)
                        .testTag("tab_expense"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "هزینه (-)",
                        fontWeight = FontWeight.Bold,
                        color = if (type == "EXPENSE") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Income tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (type == "INCOME") ColorIncome else Color.Transparent)
                        .clickable {
                            type = "INCOME"
                            selectedCategory = categories.firstOrNull { it.type == "INCOME" }
                        }
                        .padding(vertical = 10.dp)
                        .testTag("tab_income"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "درآمد (+)",
                        fontWeight = FontWeight.Bold,
                        color = if (type == "INCOME") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Amount Input with preview
            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    if (input.all { it.isDigit() } && input.length <= 13) {
                        amountText = input
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_tx_amount"),
                label = { Text("مبلغ به تومان") },
                placeholder = { Text("مثلاً ۲۵۰,۰۰۰") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                supportingText = {
                    if (parsedAmount > 0) {
                        Text(
                            text = ShamsiDateHelper.formatAmount(parsedAmount, "تومان"),
                            fontWeight = FontWeight.Bold,
                            color = if (type == "EXPENSE") ColorExpense else ColorIncome
                        )
                    }
                }
            )

            // Quick Amount Increment Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickAmounts) { amt ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clickable {
                                val current = amountText.toLongOrNull() ?: 0L
                                amountText = (current + amt).toString()
                            }
                    ) {
                        Text(
                            text = "+ ${ShamsiDateHelper.formatAmount(amt, "تومان")}",
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Title Input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_tx_title"),
                label = { Text("عنوان / طرف حساب") },
                placeholder = { Text(if (type == "EXPENSE") "مثلاً خرید سوپرمارکت یا رستوران" else "مثلاً حقوق یا سود سهام") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Category Selector
            Text(
                text = "انتخاب دسته‌بندی:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filteredCategories.forEach { cat ->
                    val isSelected = selectedCategory?.id == cat.id
                    val color = Color(cat.colorHex)
                    val icon = CategoryIconHelper.getIcon(cat.iconName)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(
                                width = if (isSelected) 1.5.dp else 0.dp,
                                color = if (isSelected) color else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("chip_cat_${cat.id}")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(color, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = cat.name,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Date options: Today / Yesterday
            Text(
                text = "تاریخ تراکنش:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val todayShamsi = ShamsiDateHelper.currentShamsiDate()
                FilterChip(
                    selected = dayOffset == 0,
                    onClick = { dayOffset = 0 },
                    label = { Text("امروز (${todayShamsi.format()})") }
                )
                FilterChip(
                    selected = dayOffset == 1,
                    onClick = { dayOffset = 1 },
                    label = { Text("دیروز") }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Payment Method Selector
            Text(
                text = "روش پرداخت / دریافت:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(paymentMethods) { method ->
                    FilterChip(
                        selected = selectedPaymentMethod == method,
                        onClick = { selectedPaymentMethod = method },
                        label = { Text(method, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Note Input
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("توضیحات و یادداشت (اختیاری)") },
                singleLine = false,
                maxLines = 2,
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Submit Button
            Button(
                onClick = {
                    if (isFormValid) {
                        val timestamp = System.currentTimeMillis() - (dayOffset * 86400000L)
                        onSave(
                            title.trim(),
                            parsedAmount,
                            type,
                            selectedCategory!!,
                            timestamp,
                            selectedPaymentMethod,
                            note.trim()
                        )
                        onDismiss()
                    }
                },
                enabled = isFormValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_save_transaction"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (type == "EXPENSE") ColorExpense else ColorIncome
                )
            ) {
                Icon(imageVector = Icons.Filled.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ثبت نهایی تراکنش",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
