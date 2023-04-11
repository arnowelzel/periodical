/*
 * Periodical main application
 * Copyright (C) 2012-2023 Arno Welzel
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

import android.app.Application;
import android.content.SharedPreferences;

import com.yariksoffice.lingver.Lingver;

public class AppPeriodical extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        PreferenceUtils preferenceUtils = new PreferenceUtils(this);
        String locale = preferenceUtils.getString("locale", "");

        if (locale.isEmpty()) {
            locale = "system";
            SharedPreferences.Editor editor = preferenceUtils.edit();
            editor.putString("locale", "system");
            editor.apply();
        }

        Lingver.init(this, "en");
        if (locale.equals("system")) {
            Lingver.getInstance().setFollowSystemLocale(this);
        } else {
            Lingver.getInstance().setLocale(this, locale);
        }
    }
}
