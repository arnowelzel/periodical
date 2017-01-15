/**
 * Periodical "about" activity 
 * Copyright (C) 2012-2015 Arno Welzel
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
import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Activity to handle the "About" command
 */
public class AboutActivity extends Activity {

    /**
     * Called when the activity starts
     */
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up view
        setContentView(R.layout.webview);

        WebView view = (WebView) findViewById(R.id.webView);
        view.getSettings().setJavaScriptEnabled(true);
        view.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                view.loadUrl("javascript:replace('version', '"+BuildConfig.VERSION_NAME+"')");
                view.loadUrl("javascript:replace('year', '"+BuildConfig.VERSION_YEAR+"')");
            }
        });
        view.loadUrl("file:///android_asset/"+getString(R.string.asset_about));

        // Activate "back button" in Action Bar if possible
        if (android.os.Build.VERSION.SDK_INT >= 11) {
            ActionBar actionBar = getActionBar();
            assert actionBar != null;
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
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