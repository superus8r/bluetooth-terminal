package com.mpandg.mpandgbluetooth.activity

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import androidx.preference.ListPreference
import android.preference.PreferenceActivity
import androidx.preference.PreferenceManager
import android.view.MenuItem
import androidx.appcompat.app.AppCompatDelegate
import com.mpandg.mpandgbluetooth.R

class SettingsActivity : PreferenceActivity(), OnSharedPreferenceChangeListener {
    private var mDelegate: AppCompatDelegate? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.settings_activity)
        delegate.supportActionBar!!.setHomeButtonEnabled(true)
        delegate.supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.registerOnSharedPreferenceChangeListener(this)
        setPreferenceTitle(getString(R.string.pref_commands_mode))
        setPreferenceTitle(getString(R.string.pref_commands_ending))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        return when (id) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, value: String) {
        setPreferenceTitle(value)
    }

    /**
     * Set the list header.
     */
    private fun setPreferenceTitle(TAG: String) {
        val preference = findPreference(TAG) ?: return
        if (preference is ListPreference) {
            if (preference.entry == null) return
            val title = preference.entry.toString()
            preference.setTitle(title)
        }
    }

    private val delegate: AppCompatDelegate
        get() {
            if (mDelegate == null) {
                mDelegate = AppCompatDelegate.create(this, null)
            }
            return mDelegate!!
        }
}