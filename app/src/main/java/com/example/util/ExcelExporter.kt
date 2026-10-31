package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.TransactionEntity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelExporter {

    private const val UTF8_BOM = "\uFEFF"

    /**
     * Generates CSV string with UTF-8 BOM for perfect Excel compatibility with Persian letters
     */
    fun generateCsvContent(transactions: List<TransactionEntity>, currencyUnit: String = "تومان"): String {
        val sb = StringBuilder()
        sb.append(UTF8_BOM)

        // Headers
        sb.append("ردیف,عنوان تراکنش,نوع,دسته‌بندی,مبلغ ($currencyUnit),تاریخ شمسی,زمان ثبت,روش پرداخت,توضیحات\n")

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        var totalIncome = 0L
        var totalExpense = 0L

        transactions.forEachIndexed { index, tx ->
            val typeStr = if (tx.type == "INCOME") "درآمد" else "هزینه"
            if (tx.type == "INCOME") totalIncome += tx.amount else totalExpense += tx.amount

            val timeStr = timeFormat.format(Date(tx.timestamp))

            val escapedTitle = escapeCsv(tx.title)
            val escapedCategory = escapeCsv(tx.categoryName)
            val escapedMethod = escapeCsv(tx.paymentMethod)
            val escapedNote = escapeCsv(tx.note)

            sb.append("${index + 1},$escapedTitle,$typeStr,$escapedCategory,${tx.amount},${tx.shamsiDate},$timeStr,$escapedMethod,$escapedNote\n")
        }

        // Summary row
        sb.append("\n")
        sb.append(",,,خلاصه گزارش,,,,,\n")
        sb.append(",,,جمع کل درآمدها,$totalIncome,,,,\n")
        sb.append(",,,جمع کل هزینه‌ها,$totalExpense,,,,\n")
        sb.append(",,,تراز مالی (مانده),${totalIncome - totalExpense},,,,\n")

        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        var result = value.replace("\"", "\"\"")
        if (result.contains(",") || result.contains("\n") || result.contains("\"")) {
            result = "\"$result\""
        }
        return result
    }

    /**
     * Exports transactions to a temporary file in cache and returns a content URI for sharing
     */
    fun exportAndShare(context: Context, transactions: List<TransactionEntity>, currencyUnit: String = "تومان"): Intent {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }

        val shamsiNow = ShamsiDateHelper.currentShamsiDate()
        val filename = "Hesabdar_Report_${shamsiNow.year}_${shamsiNow.month}_${shamsiNow.day}.csv"
        val file = File(exportDir, filename)

        val csvContent = generateCsvContent(transactions, currencyUnit)
        FileOutputStream(file).use { out ->
            out.write(csvContent.toByteArray(Charsets.UTF_8))
        }

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "گزارش مالی حسابدار - ${shamsiNow.format(false)}")
            putExtra(Intent.EXTRA_TEXT, "خروجی اکسل تراکنش‌ها از اپلیکیشن حسابدار")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        return Intent.createChooser(shareIntent, "اشتراک‌گذاری گزارش اکسل")
    }

    /**
     * Writes CSV bytes directly to an OutputStream (e.g. from SAF CreateDocument)
     */
    fun writeToStream(outputStream: OutputStream, transactions: List<TransactionEntity>, currencyUnit: String = "تومان") {
        val csvContent = generateCsvContent(transactions, currencyUnit)
        outputStream.use { out ->
            out.write(csvContent.toByteArray(Charsets.UTF_8))
        }
    }
}
