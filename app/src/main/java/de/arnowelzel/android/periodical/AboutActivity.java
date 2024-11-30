/*
 * Periodical "about" activity
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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Activity to handle the "About" command
 */
public class AboutActivity extends AppCompatActivity {

    /**
     * Called when the activity starts
     */
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up view
        setContentView(R.layout.webview);

        // Set up main toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);

        WebView view = findViewById(R.id.webView);
        view.getSettings().setJavaScriptEnabled(true);
        view.setWebViewClient(
                new WebViewClient() {
                    // Update version and year after loading the document
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        PreferenceUtils preferences = new PreferenceUtils(getApplicationContext());
                        String backupUriString = preferences.getString("backup_uri", "");
                        if (backupUriString.equals("")) {
                            backupUriString = "<em>(" + getString(R.string.backup_noruiyet) + ")</em>";
                        }

                        super.onPageFinished(view, url);
                        view.loadUrl("javascript:replace('version', '" + BuildConfig.VERSION_NAME + "')");
                        view.loadUrl("javascript:replace('year', '" + BuildConfig.VERSION_YEAR + "')");
                        view.loadUrl("javascript:replace('backupfolder','" + backupUriString + "')");
                        view.loadUrl("javascript:replace('translation','Albert Kannemeyer, Sébastien Gravier, Primokorn, Valerio Bozzolan, Ingrid Spangler, " +
                                "Wjatscheslaw Stoljarski, Pander, Laura Arjona Reina, Naofumi Fukue, Tomasz Terka, " +
                                "Nikoletta Karasmani, Yaron Shahrabani, Inbar Gover, Turan Guliyeva, Enara Larraitz, " +
                                "Rza Sharifi, Mevlüt Erdem Güven, Stanislav Zinchenko')");
                    }

                    // Handle URLs always external links
                    @SuppressWarnings("deprecation")
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                        return true;
                    }
                });
        view.loadUrl("file:///android_asset/" + getString(R.string.asset_about));
    }

    /**
     * Handler for ICS "home" button
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Home icon in action bar clicked, then close activity
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
