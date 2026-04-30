package com.xmm.videodownloader

import android.content.Context
import android.content.SharedPreferences

object ThemeManager {

    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_COLOR = "selected_color"

    data class ThemeColor(val name: String, val colorResId: Int, val colorHex: String)

    val themeColors = listOf(
        ThemeColor("Blue", R.color.theme_blue, "#1976D2"),
        ThemeColor("Purple", R.color.theme_purple, "#7B1FA2"),
        ThemeColor("Green", R.color.theme_green, "#388E3C"),
        ThemeColor("Orange", R.color.theme_orange, "#F57C00"),
        ThemeColor("Red", R.color.theme_red, "#D32F2F"),
        ThemeColor("Teal", R.color.theme_teal, "#00796B"),
        ThemeColor("Pink", R.color.theme_pink, "#C2185B"),
        ThemeColor("Indigo", R.color.theme_indigo, "#303F9F")
    )

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getSelectedIndex(context: Context): Int {
        return prefs(context).getInt(KEY_COLOR, 0)
    }

    fun setSelectedIndex(context: Context, index: Int) {
        prefs(context).edit().putInt(KEY_COLOR, index).apply()
    }

    fun getSelectedColor(context: Context): ThemeColor {
        val idx = getSelectedIndex(context)
        return themeColors.getOrElse(idx) { themeColors[0] }
    }
}
