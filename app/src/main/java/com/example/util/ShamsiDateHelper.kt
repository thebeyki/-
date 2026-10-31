package com.example.util

import java.text.DecimalFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ShamsiDate(
    val year: Int,
    val month: Int, // 1 to 12
    val day: Int
) {
    val monthName: String
        get() = ShamsiDateHelper.getMonthName(month)

    fun format(toPersianDigits: Boolean = true): String {
        val y = year.toString()
        val m = month.toString().padStart(2, '0')
        val d = day.toString().padStart(2, '0')
        val str = "$y/$m/$d"
        return if (toPersianDigits) ShamsiDateHelper.toPersianDigits(str) else str
    }

    val monthKey: String
        get() = "$year-${month.toString().padStart(2, '0')}"
}

object ShamsiDateHelper {

    val PERSIAN_MONTHS = listOf(
        "فروردین", "اردیبهشت", "خرداد",
        "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر",
        "دی", "بهمن", "اسفند"
    )

    fun getMonthName(month: Int): String {
        return if (month in 1..12) PERSIAN_MONTHS[month - 1] else ""
    }

    /**
     * Converts a millisecond timestamp to ShamsiDate
     */
    fun fromTimestamp(millis: Long): ShamsiDate {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        val gy = cal.get(Calendar.YEAR)
        val gm = cal.get(Calendar.MONTH) + 1
        val gd = cal.get(Calendar.DAY_OF_MONTH)
        return gregorianToShamsi(gy, gm, gd)
    }

    fun currentShamsiDate(): ShamsiDate {
        return fromTimestamp(System.currentTimeMillis())
    }

    /**
     * Converts Gregorian Year, Month (1-12), Day to Shamsi Date
     */
    fun gregorianToShamsi(gy: Int, gm: Int, gd: Int): ShamsiDate {
        val gDaysInMonth = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val jDaysInMonth = intArrayOf(0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

        var gy2 = gy
        if (isGregorianLeapYear(gy2)) {
            gDaysInMonth[2] = 29
        }

        var totalDays = 0
        for (i in 1 until gm) {
            totalDays += gDaysInMonth[i]
        }
        totalDays += gd

        var jy: Int
        var jm: Int
        var jd: Int

        val marchDayDiff = if (isGregorianLeapYear(gy2)) 79 else 80

        if (totalDays > marchDayDiff) {
            val daysAfterMarch = totalDays - marchDayDiff
            jy = gy2 - 621
            if (isShamsiLeapYear(jy)) {
                jDaysInMonth[12] = 30
            }
            var tempDays = daysAfterMarch
            var m = 1
            while (m <= 12 && tempDays > jDaysInMonth[m]) {
                tempDays -= jDaysInMonth[m]
                m++
            }
            jm = m
            jd = tempDays
        } else {
            val daysBeforeMarch = totalDays + (if (isGregorianLeapYear(gy2 - 1)) 11 else 10)
            jy = gy2 - 622
            if (isShamsiLeapYear(jy)) {
                jDaysInMonth[12] = 30
            }
            var tempDays = daysBeforeMarch
            var m = 1
            while (m <= 12 && tempDays > jDaysInMonth[m]) {
                tempDays -= jDaysInMonth[m]
                m++
            }
            jm = m
            jd = tempDays
        }

        return ShamsiDate(jy, jm, jd)
    }

    fun isGregorianLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }

    fun isShamsiLeapYear(year: Int): Boolean {
        val rem = year % 33
        return rem == 1 || rem == 5 || rem == 9 || rem == 13 || rem == 17 || rem == 22 || rem == 26 || rem == 30
    }

    fun toPersianDigits(input: String): String {
        val persianChars = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val sb = StringBuilder()
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(persianChars[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun formatAmount(amount: Long, unit: String = "تومان", toPersian: Boolean = true): String {
        val formatter = DecimalFormat("#,###")
        val formatted = formatter.format(amount)
        val withUnit = "$formatted $unit"
        return if (toPersian) toPersianDigits(withUnit) else withUnit
    }

    fun formatNumberOnly(amount: Long, toPersian: Boolean = true): String {
        val formatter = DecimalFormat("#,###")
        val formatted = formatter.format(amount)
        return if (toPersian) toPersianDigits(formatted) else formatted
    }

    /**
     * Get start and end timestamp in millis for a specific Shamsi year and month
     */
    fun getMonthTimestampRange(year: Int, month: Int): Pair<Long, Long> {
        val daysInMonth = when {
            month in 1..6 -> 31
            month in 7..11 -> 30
            isShamsiLeapYear(year) -> 30
            else -> 29
        }

        // Approximate gregorian start
        val startCal = shamsiToGregorianCalendar(year, month, 1, 0, 0, 0)
        val endCal = shamsiToGregorianCalendar(year, month, daysInMonth, 23, 59, 59)
        return Pair(startCal.timeInMillis, endCal.timeInMillis)
    }

    fun shamsiToGregorianCalendar(jy: Int, jm: Int, jd: Int, hour: Int = 0, min: Int = 0, sec: Int = 0): Calendar {
        // Compute days since Shamsi epoch
        var days = (jy - 979) * 365 + (jy - 979) / 33 * 8 + (jy - 979) % 33 * 97 / 400
        for (i in 1 until jm) {
            days += if (i <= 6) 31 else 30
        }
        days += jd - 1

        var gDays = days + 79
        var gy = 1600 + 400 * (gDays / 146097)
        gDays %= 146097

        var leap = true
        if (gDays >= 36525) {
            gDays--
            gy += 100 * (gDays / 36524)
            gDays %= 36524
            if (gDays >= 365) {
                gDays++
            } else {
                leap = false
            }
        }

        gy += 4 * (gDays / 1461)
        gDays %= 1461

        if (gDays >= 366) {
            leap = false
            gDays--
            gy += gDays / 365
            gDays %= 365
        }

        val gDaysInMonth = intArrayOf(
            31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31
        )

        var gm = 0
        while (gm < 12 && gDays >= gDaysInMonth[gm]) {
            gDays -= gDaysInMonth[gm]
            gm++
        }
        val gd = gDays + 1

        val cal = Calendar.getInstance()
        cal.set(gy, gm, gd, hour, min, sec)
        cal.set(Calendar.MILLISECOND, 0)
        return cal
    }
}
