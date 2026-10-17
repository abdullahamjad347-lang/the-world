package com.example.data.model

data class PredefinedStaff(
    val name: String,
    val isCoordinator: Boolean = false,
    val coordinatorLabel: String = if (isCoordinator) "کوآرڈینیٹر" else ""
)

object PredefinedSchedules {
    val MONDAY = listOf(
        PredefinedStaff("ذیشان ایوب", isCoordinator = true),
        PredefinedStaff("عامر حسن"),
        PredefinedStaff("حافظ ظہیر"),
        PredefinedStaff("حافظ عمیر"),
        PredefinedStaff("عبدالغفار"),
        PredefinedStaff("امجد سلیم"),
        PredefinedStaff("عمیر رؤف"),
        PredefinedStaff("فیصل ندیم")
    )

    val TUESDAY = listOf(
        PredefinedStaff("حاجی ارشاد", isCoordinator = true),
        PredefinedStaff("ناصر شکیل"),
        PredefinedStaff("منظور احمد"),
        PredefinedStaff("محمود احمد"),
        PredefinedStaff("محمد انور"),
        PredefinedStaff("محمد قاسم")
    )

    val WEDNESDAY = listOf(
        PredefinedStaff("عامر سہیل", isCoordinator = true),
        PredefinedStaff("عمران ظفر"),
        PredefinedStaff("عامر حسن"),
        PredefinedStaff("ناصر عباس"),
        PredefinedStaff("عثمان عمر"),
        PredefinedStaff("فضل محمود"),
        PredefinedStaff("فیصل ندیم"),
        PredefinedStaff("عرفان")
    )

    val THURSDAY = listOf(
        PredefinedStaff("افتخار حسین", isCoordinator = true),
        PredefinedStaff("محمد اشرف"),
        PredefinedStaff("امجد سلیم"),
        PredefinedStaff("حافظ ظہیر"),
        PredefinedStaff("عمر نصیر"),
        PredefinedStaff("محسن عارف")
    )

    val FRIDAY = listOf(
        PredefinedStaff("شاہد ندیم", isCoordinator = true),
        PredefinedStaff("محمد انور"),
        PredefinedStaff("عمر رؤف"),
        PredefinedStaff("عامر سہیل"),
        PredefinedStaff("غلام محمد"),
        PredefinedStaff("فضل محمود"),
        PredefinedStaff("الطاف اللہ رکھا"),
        PredefinedStaff("ناصر عباس"),
        PredefinedStaff("انعام اللہ"),
        PredefinedStaff("عبدالغفار"),
        PredefinedStaff("مقبول احمد"),
        PredefinedStaff("عابد"),
        PredefinedStaff("فیصل ندیم")
    )

    val SATURDAY = listOf(
        PredefinedStaff("محمد زین", isCoordinator = true),
        PredefinedStaff("محمد احسان"),
        PredefinedStaff("محمد شیراز"),
        PredefinedStaff("قیصر ندیم"),
        PredefinedStaff("نسیم یعقوب"),
        PredefinedStaff("مقصود احمد"),
        PredefinedStaff("محسن عارف"),
        PredefinedStaff("ذیشان ایوب")
    )

    val SUNDAY = listOf(
        PredefinedStaff("محمد احسان", isCoordinator = true),
        PredefinedStaff("محمد اشرف"),
        PredefinedStaff("نسیم یعقوب"),
        PredefinedStaff("محمد سلیم"),
        PredefinedStaff("محمد زین"),
        PredefinedStaff("آصف صادق"),
        PredefinedStaff("سعد نسیم")
    )

    fun getStaffForDay(dayOfWeek: Int): List<PredefinedStaff> {
        return when (dayOfWeek) {
            1 -> MONDAY
            2 -> TUESDAY
            3 -> WEDNESDAY
            4 -> THURSDAY
            5 -> FRIDAY
            6 -> SATURDAY
            7 -> SUNDAY
            else -> MONDAY
        }
    }

    fun getDayNameUrdu(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            1 -> "پیر"
            2 -> "منگل"
            3 -> "بدھ"
            4 -> "جمعرات"
            5 -> "جمعہ"
            6 -> "ہفتہ"
            7 -> "اتوار"
            else -> "پیر"
        }
    }
}
