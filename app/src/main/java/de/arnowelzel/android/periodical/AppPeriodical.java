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
