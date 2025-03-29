/*
 * Periodical settings fragment
 * Copyright (C) 2012-2025 Arno Welzel
 *
 * This code is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.arnowelzel.android.periodical;

import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;

import com.yariksoffice.lingver.Lingver;

public class SettingsFragment extends PreferenceFragmentCompat {
    private PeriodicalDatabase dbMain;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        final Context context = requireActivity().getApplicationContext();
        assert context != null;

        // We store preferences in the database
        dbMain = new PeriodicalDatabase(context);

        // Build preference entries
        addPreferencesFromResource(R.xml.preferences);
        PreferenceUtils preferenceUtils = new PreferenceUtils(getPreferenceManager().getSharedPreferences());

        // Set up change listeners and initialize summaries

        ListPreference settingsLanguage = findPreference("locale");
        assert settingsLanguage != null;
        settingsLanguage.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                String locale = newValue.toString();
                if (locale.equals("system")) {
                    Lingver.getInstance().setFollowSystemLocale(requireActivity());
                } else {
                    Lingver.getInstance().setLocale(requireActivity(), locale);
                }
                requireActivity().recreate();
                return true;
            }
        });
        updatePrefSummary(settingsLanguage);

        EditTextPreference settingsPeriodLength = findPreference("period_length");
        assert settingsPeriodLength != null;
        settingsPeriodLength.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                return validateAndStoreNumericalSetting(
                        preference, newValue,
                        1, 14,
                        R.string.invalid_period_length
                );
            }
        });
        updatePrefSummary(settingsPeriodLength);

        EditTextPreference settingsLutealLength = findPreference("luteal_length");
        assert settingsLutealLength != null;
        settingsLutealLength.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                return validateAndStoreNumericalSetting(
                        preference, newValue,
                        1, null,
                        R.string.invalid_luteal_length
                );
            }
        });
        updatePrefSummary(settingsLutealLength);

        ListPreference settingsStartOfWeek = findPreference("startofweek");
        assert settingsStartOfWeek != null;
        settingsStartOfWeek.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                return storeSetting(preference, newValue);
            }
        });
        updatePrefSummary(settingsStartOfWeek);

        EditTextPreference settingsMaximumCycleLength = findPreference("maximum_cycle_length");
        assert settingsMaximumCycleLength != null;
        settingsMaximumCycleLength.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                return validateAndStoreNumericalSetting(
                        preference, newValue,
                        60, null,
                        R.string.invalid_maximum_cycle_length
                );
            }
        });
        updatePrefSummary(settingsMaximumCycleLength);

        CheckBoxPreference settingsDirectDetails = findPreference("direct_details");
        assert settingsDirectDetails != null;
        settingsDirectDetails.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                return storeSetting(preference, newValue);
            }
        });

        CheckBoxPreference settingsShowCycle = findPreference("show_cycle");
        assert settingsShowCycle != null;
        settingsShowCycle.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                return storeSetting(preference, newValue);
            }
        });
    }

    /**
     * Update summary text for a preference using the stored value
     */
    private static void updatePrefSummary(Preference p) {
        if (p instanceof ListPreference) {
            ListPreference listPref = (ListPreference) p;
            p.setSummary(listPref.getEntry());
        }
        if (p instanceof EditTextPreference) {
            EditTextPreference editTextPref = (EditTextPreference) p;
            p.setSummary(editTextPref.getText());
        }
    }

    /**
     * Update summary text for a preference using a new value
     */
    private static void updatePrefSummaryNew(Preference p, Object newValue) {
        String value = newValue.toString();
        if (p instanceof ListPreference) {
            int index = Integer.parseInt(value);
            ListPreference listPref = (ListPreference) p;
            p.setSummary(listPref.getEntries()[index]);
        }
        if (p instanceof EditTextPreference) {
            EditTextPreference editTextPref = (EditTextPreference) p;
            p.setSummary(newValue.toString());
        }
    }

    /**
     * Validate a setting and store it to the database
     */
    private boolean validateAndStoreNumericalSetting(
            Preference preference,
            Object newValue,
            Integer minValue,
            Integer maxValue,
            @StringRes int idErrorMessage
    ) {
        int value;
        try {
            value = Integer.parseInt(newValue.toString());
        } catch (NumberFormatException e) {
            value = 0;
        }

        if ((minValue != null && value < minValue) || (maxValue != null && value > maxValue)) {
            Toast.makeText(getContext(),
                    getResources().getString(idErrorMessage),
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        String key = preference.getKey();
        dbMain.setOption(key, value);

        updatePrefSummaryNew(preference, newValue);

        return true;
    }

    /**
     * Store a numerical setting to the database
     */
    private boolean storeSetting(
            Preference preference,
            Object newValue
    ) {
        if(newValue instanceof Integer || newValue instanceof String) {
            int value;
            try {
                value = Integer.parseInt(newValue.toString());
            } catch (NumberFormatException e) {
                value = 0;
            }

            String key = preference.getKey();
            dbMain.setOption(key, value);
        }

        if (newValue instanceof Boolean) {
            int value = 0;
            if ((Boolean) newValue) {
                value = 1;
            }
            String key = preference.getKey();
            dbMain.setOption(key, value);
        }

        updatePrefSummaryNew(preference, newValue);

        return true;
    }
}