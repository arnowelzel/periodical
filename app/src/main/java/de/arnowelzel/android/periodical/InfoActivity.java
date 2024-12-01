/*
 * Periodical "info" activity
 * Copyright (C) 2012-2024 Arno Welzel
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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class InfoActivity extends AppCompatActivity {
    /**
     * Database for calendar data
     */
    private PeriodicalDatabase dbMain;

    /* Launchers for activities with result */
    private ActivityResultLauncher<Intent> pickDateResultLauncher;

    /**
     * Called when the activity starts
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        final Context context = getApplicationContext();
        assert context != null;
        super.onCreate(savedInstanceState);

        // Set up view
        setContentView(R.layout.info);

        // Calculate data
        dbMain = new PeriodicalDatabase(context);
        dbMain.loadCalculatedData();

        TextView viewCountEntries = findViewById(R.id.infoCountEntries);
        viewCountEntries.setText(String.format("%d", dbMain.cycleCount));

        TextView viewAverage = findViewById(R.id.infoDurationAverage);
        viewAverage.setText(String.format("%d", dbMain.cycleAverage));

        TextView viewShortest = findViewById(R.id.infoDurationShortest);
        viewShortest.setText(String.format("%d", dbMain.cycleShortest));

        TextView viewLongest = findViewById(R.id.infoDurationLongest);
        viewLongest.setText(String.format("%d", dbMain.cycleLongest));

        // Register activity result launchers
        pickDateResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        handleActivityResultPickDate(result.getData());
                    }
                });

        // Set up main toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);
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

    /**
     * Handler for "List" menu action
     */
    public void showList(View v) {
        pickDateResultLauncher.launch(new Intent(InfoActivity.this, ListActivity.class));
    }

    /**
     * Handler for "List, details" menu action
     */
    public void showListDetails(View v) {
        pickDateResultLauncher.launch(
                new Intent(InfoActivity.this, ListDetailsActivity.class)
        );
    }

    /**
     * Handler for date selection in list views
     */
    protected void handleActivityResultPickDate(Intent data) {
        // Forward the result to the calling view
        Bundle extras = data.getExtras();
        Intent intent = getIntent();
        intent.putExtras(extras);
        setResult(RESULT_OK, intent);
        finish();
    }
}
