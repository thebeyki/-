package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIconHelper {

    val AVAILABLE_ICONS = listOf(
        "Restaurant" to Icons.Filled.Restaurant,
        "Home" to Icons.Filled.Home,
        "DirectionsCar" to Icons.Filled.DirectionsCar,
        "ShoppingBag" to Icons.Filled.ShoppingBag,
        "Receipt" to Icons.Filled.Receipt,
        "LocalHospital" to Icons.Filled.LocalHospital,
        "School" to Icons.Filled.School,
        "SportsEsports" to Icons.Filled.SportsEsports,
        "Work" to Icons.Filled.Work,
        "Store" to Icons.Filled.Store,
        "TrendingUp" to Icons.Filled.TrendingUp,
        "CardGiftcard" to Icons.Filled.CardGiftcard,
        "AccountBalanceWallet" to Icons.Filled.AccountBalanceWallet,
        "LocalCafe" to Icons.Filled.LocalCafe,
        "ShoppingCart" to Icons.Filled.ShoppingCart,
        "Flight" to Icons.Filled.Flight,
        "Pets" to Icons.Filled.Pets,
        "AttachMoney" to Icons.Filled.AttachMoney,
        "AccountBalance" to Icons.Filled.AccountBalance,
        "MoreHoriz" to Icons.Filled.MoreHoriz
    )

    val PRESET_COLORS = listOf(
        0xFFF97316, // Orange
        0xFF3B82F6, // Blue
        0xFFEAB308, // Yellow
        0xFF6366F1, // Indigo
        0xFFEC4899, // Pink
        0xFFEF4444, // Red
        0xFF8B5CF6, // Purple
        0xFF14B8A6, // Teal
        0xFF10B981, // Emerald
        0xFF06B6D4, // Cyan
        0xFF84CC16, // Lime
        0xFF64748B  // Slate
    )

    fun getIcon(iconName: String): ImageVector {
        return AVAILABLE_ICONS.firstOrNull { it.first.equals(iconName, ignoreCase = true) }?.second
            ?: Icons.Filled.Category
    }
}
