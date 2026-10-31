package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Long, // Stored in Toman or Rial
    val type: String, // "EXPENSE" or "INCOME"
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: Long, // 0xFF... color ARGB
    val timestamp: Long = System.currentTimeMillis(),
    val shamsiDate: String, // e.g. "1403/08/10"
    val paymentMethod: String = "کارت بانکی",
    val note: String = ""
)
