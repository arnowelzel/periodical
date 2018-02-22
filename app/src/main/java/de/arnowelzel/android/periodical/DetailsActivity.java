/*
 * Periodical "help" activity 
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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.GregorianCalendar;

import static de.arnowelzel.android.periodical.PeriodicalDatabase.DayEntry.EMPTY;
import static de.arnowelzel.android.periodical.PeriodicalDatabase.DayEntry.PERIOD_CONFIRMED;
import static de.arnowelzel.android.periodical.PeriodicalDatabase.DayEntry.PERIOD_START;

/**
 * Activity to handle the "Help" command
 */
public class DetailsActivity extends AppCompatActivity {
    /**
     *  Called when the activity starts
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        PeriodicalDatabase dbMain;
        PeriodicalDatabase.DayEntry entry;

        final Context context = getApplicationContext();
        assert context != null;
        super.onCreate(savedInstanceState);

        // Set up view
        setContentView(R.layout.detailsview);

        // Activate "back button" in Action Bar if possible
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);

        // Get details
        dbMain = new PeriodicalDatabase(context);
        dbMain.loadCalculatedData();

        Intent intent = getIntent();
        int year = intent.getIntExtra("year", 1970);
        int month = intent.getIntExtra("month", 1);
        int day = intent.getIntExtra("day", 1);
        entry = dbMain.getEntry(year, month, day);
        if(entry == null) {
            entry = new PeriodicalDatabase.DayEntry(EMPTY, new GregorianCalendar(year, month, day), 0);
        }

        // Set header using the entry date
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.LONG);
        ((TextView)findViewById(R.id.labelDetailsHeader)).setText(
                String.format("%s", dateFormat.format(entry.date.getTime())));

        // Set period status
        switch(entry.type) {
            case PERIOD_START:
            case PERIOD_CONFIRMED:
                ((RadioButton)findViewById(R.id.periodYes)).setChecked(true);
                break;
            default:
                ((RadioButton)findViewById(R.id.periodNo)).setChecked(true);
                break;
        }

        // Set period intensity
        ((RadioButton)findViewById(R.id.periodIntensity1)).setChecked(true);

        // Build list of events/symptoms
        LinearLayout groupEvents = (LinearLayout)findViewById(R.id.groupEvents);
        String packageName = getPackageName();
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        DisplayMetrics displaymetrics = new DisplayMetrics();
        int marginLeft = (int)(12 * Resources.getSystem().getDisplayMetrics().density);
        int marginRight = (int)(12 * Resources.getSystem().getDisplayMetrics().density);
        layoutParams.setMargins(marginLeft,0, marginRight, 0);

        int num = 1;
        while(true) {
            @SuppressLint("DefaultLocale") String resName = String.format("label_details_ev%d",num);
            int resId = getResources().getIdentifier(resName, "string", packageName);
            if(resId != 0) {
                CheckBox option = new CheckBox(this);
                option.setLayoutParams(layoutParams);
                option.setText(resId);
                option.setId(resId);
                option.setTextSize(18);
                groupEvents.addView(option);
                num++;
            } else {
                break;
            }
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
                return true;
        }
    }
}
