package com.gitlab.mudlej.MjPdfReader

import android.app.AlertDialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.*
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.gitlab.mudlej.MjPdfReader.data.Preferences

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
        setUpSwitches();
    }

    private fun setUpSwitches() {
        // ----------------- First Section -------------------

        // Configure and add Anti Aliasing Switch
        val qualitySwitch = SwitchPreferenceCompat(context)
        qualitySwitch.title = getString(R.string.quality)
        qualitySwitch.setDefaultValue(Preferences.highQualityDefault)
        qualitySwitch.key = Preferences.highQualityKey
        qualitySwitch.isIconSpaceReserved = false

        // Configure and add Anti Aliasing Switch
        val aliasSwitch = SwitchPreferenceCompat(context)
        aliasSwitch.title = getString(R.string.alias)
        aliasSwitch.setDefaultValue(Preferences.antiAliasingDefault)
        aliasSwitch.key = Preferences.antiAliasingKey
        aliasSwitch.isIconSpaceReserved = false

        // Configure and add Keep Screen On Switch
        val screenOnSwitch = SwitchPreferenceCompat(context)
        screenOnSwitch.title = getString(R.string.keep_screen_on)
        screenOnSwitch.setDefaultValue(Preferences.screenOnDefault)
        screenOnSwitch.key = Preferences.screenOnKey
        screenOnSwitch.isIconSpaceReserved = false

        // add the switches to the first section
        val firstSection: PreferenceCategory? = findPreference("firstSection")
        firstSection?.isIconSpaceReserved = false
        firstSection?.addPreference(qualitySwitch)
        firstSection?.addPreference(aliasSwitch)
        firstSection?.addPreference(screenOnSwitch)


        // ----------------- Second Section ------------------

        // Configure and add Keep Screen On Switch
        val horizontalScrollSwitch = SwitchPreferenceCompat(context)
        horizontalScrollSwitch.title = getString(R.string.scroll)
        horizontalScrollSwitch.setDefaultValue(Preferences.horizontalScrollDefault)
        horizontalScrollSwitch.key = Preferences.horizontalScrollKey
        horizontalScrollSwitch.isIconSpaceReserved = false

        // Configure and add Page Snap Switch
        val pageSnapSwitch = SwitchPreferenceCompat(context)
        pageSnapSwitch.title = getString(R.string.snap)
        pageSnapSwitch.setDefaultValue(Preferences.pageSnapDefault)
        pageSnapSwitch.key = Preferences.pageSnapKey
        pageSnapSwitch.summary = getString(R.string.snap_summary)
        pageSnapSwitch.isIconSpaceReserved = false

        // Configure and add Page Snap Switch
        val pageFlingSwitch = SwitchPreferenceCompat(context)
        pageFlingSwitch.title = getString(R.string.fling)
        pageFlingSwitch.setDefaultValue(Preferences.pageFlingDefault)
        pageFlingSwitch.key = Preferences.pageFlingKey
        pageFlingSwitch.summary = getString(R.string.fling_summary)
        pageFlingSwitch.isIconSpaceReserved = false

        // add the switches to the second section
        val secondSection: PreferenceCategory? = findPreference("secondSection")
        secondSection?.isIconSpaceReserved = false
        secondSection?.addPreference(horizontalScrollSwitch)
        secondSection?.addPreference(pageSnapSwitch)
        secondSection?.addPreference(pageFlingSwitch)


        // ----------------- Third Section ------------------

        // Configure and add Keep Screen On Switch
        val appDarkThemeSwitch = SwitchPreferenceCompat(context)
        appDarkThemeSwitch.title = getString(R.string.dark_theme_for_app)
        appDarkThemeSwitch.setDefaultValue(Preferences.appFollowSystemThemeDefault)
        appDarkThemeSwitch.key = Preferences.appFollowSystemTheme
        appDarkThemeSwitch.summary = getString(R.string.app_dark_theme_summary)
        appDarkThemeSwitch.isIconSpaceReserved = false

        // set a caution dialog to show for this option
        appDarkThemeSwitch.setOnPreferenceClickListener {
            // don't show the dialog when turning it off
            if (!appDarkThemeSwitch.isChecked) {
                AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO)
                return@setOnPreferenceClickListener true
            }

            AlertDialog.Builder(context)
                .setTitle(getString(R.string.caution))
                .setMessage(getString(R.string.app_dark_dialog_message))
                .setPositiveButton("Ok") { dialog, _ ->
                    dialog.dismiss()
                    setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM)
                }
                .setNegativeButton("Cancel") {_, _ -> appDarkThemeSwitch.isChecked = false }
                .create().show()
            return@setOnPreferenceClickListener true
        }

        // add the switches to the third section
        val thirdSection: PreferenceCategory? = findPreference("thirdSection")
        thirdSection?.isIconSpaceReserved = false
        thirdSection?.addPreference(appDarkThemeSwitch)

    }
}