/*
 * Utility class to access shared preferences
 * Copyright (C) 2012-2018 Arno Welzel
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
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Preference utilities
 */
public class PreferenceUtils {
    /** Private reference to application context */
    private final Context context;

    /** Private reference to shared preferences */
    private SharedPreferences preferences;

    /**
     * Constructor, will try to create/open a writable database
     *
     * @param context
     * Application context
     */
    PreferenceUtils(Context context) {
        this.context = context;
        this.preferences =  PreferenceManager.getDefaultSharedPreferences(context);
    }

    /**
     * Get integer preference
     *
     * @param key
     * Name of the preference
     *
     * @param defValue
     * Default value
     *
     * @return
     * The preference
     */
    public int getInt(String key, Integer defValue) {
        int result = defValue;

        try {
            result = Integer.parseInt(preferences.getString(key, defValue.toString()));
        } catch (ClassCastException e) {
        } catch (NumberFormatException e) {
        }

        return result;
    }

    /**
     * Get string preference
     *
     * @param key
     * Name of the preference
     *
     * @param defValue
     * Default value
     *
     * @return
     * The preference
     */
    public String getString(String key, String defValue) {
        String result = defValue;

        try {
            result = preferences.getString(key, defValue);
        } catch (ClassCastException e) {
            result = defValue;
        }

        return result;
    }

    /**
     * Get bool preference
     *
     * @param key
     * Name of the preference
     *
     * @param defValue
     * Default value
     *
     * @return
     * The preference
     */
    public boolean getBoolean(String key, boolean defValue) {
        boolean result = defValue;

        try {
            result = preferences.getBoolean(key, defValue);
        } catch (ClassCastException e) {
            result = defValue;
        }

        return result;
    }

    /**
     * Get an editor for the shared preferences
     *
     * @return
     * Editor for the shared preferences
     */
    public SharedPreferences.Editor edit() {
        return preferences.edit();
    }
}
