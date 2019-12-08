package com.mpandg.mpandgbluetooth

import android.content.Context
import androidx.preference.PreferenceManager
import android.text.InputFilter
import android.text.Spanned
import android.util.Log

object Utils {
    /**
     * A general method to output debug messages in the log
     */
    fun log(message: String?) {
        if (BuildConfig.DEBUG) {
            if (message != null) Log.i(Const.TAG, message)
        }
    }

    /**
     * Convert hex to string
     */
    fun printHex(hex: String): String {
        val sb = StringBuilder()
        val len = hex.length
        try {
            var i = 0
            while (i < len) {
                sb.append("0x").append(hex.substring(i, i + 2)).append(" ")
                i += 2
            }
        } catch (e: NumberFormatException) {
            log("printHex NumberFormatException: " + e.message)
        } catch (e: StringIndexOutOfBoundsException) {
            log("printHex StringIndexOutOfBoundsException: " + e.message)
        }
        return sb.toString()
    }

    /**
     * get preferences.
     */
    fun getPreference(context: Context?, item: String?): String? {
        val settings = PreferenceManager.getDefaultSharedPreferences(context)
        return settings.getString(item, Const.TAG)
    }

    /**
     * getting a boolean flag from the settings.
     */
    fun getBooleanPreference(context: Context?, tag: String?): Boolean {
        val settings = PreferenceManager.getDefaultSharedPreferences(context)
        return settings.getBoolean(tag, true)
    }

    class InputFilterHex : InputFilter {
        override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int): CharSequence? {
            for (i in start until end) {
                if (!Character.isDigit(source[i])
                        && source[i] != 'A' && source[i] != 'D' && source[i] != 'B' && source[i] != 'E' && source[i] != 'C' && source[i] != 'F') {
                    return ""
                }
            }
            return null
        }
    }
}