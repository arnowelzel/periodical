/*
 * Periodical list activity
 * Copyright (C) 2012-2017 Arno Welzel
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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.preference.PreferenceManager;
import android.widget.ListView;

import java.util.Calendar;
import java.util.Iterator;

import de.arnowelzel.android.periodical.PeriodicalDatabase.DayEntry;

/**
 * Activity to handle the "List" command
 */
public class ListActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    /**
     * Database for calendar data
     */
    private PeriodicalDatabase dbMain;

    /**
     * Called when activity starts
     */
    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        final Context context = getApplicationContext();
        assert context != null;
        super.onCreate(savedInstanceState);

        int maximumcyclelength;

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            maximumcyclelength = Integer.parseInt(preferences.getString("maximum_cycle_length", "183"));
        } catch (NumberFormatException e) {
            maximumcyclelength = 183;
        }

        // Set up database and string array for the list
        dbMain = new PeriodicalDatabase(context);
        dbMain.loadRawData();

        String[] entries = new String[dbMain.dayEntries.size()];
        java.text.DateFormat dateFormat = android.text.format.DateFormat
                .getDateFormat(context);
        Iterator<DayEntry> dayIterator = dbMain.dayEntries.iterator();
        int pos = 0;
        DayEntry dayPrevious = null;
        DayEntry day = null;
        boolean isFirst = true;
        while (dayIterator.hasNext()) {
            if (isFirst) {
                isFirst = false;
            } else {
                dayPrevious = day;
            }
            day = dayIterator.next();

            entries[pos] = dateFormat.format(day.date.getTime());
            switch (day.type) {
                case DayEntry.PERIOD_START:
                    entries[pos] = entries[pos] + " \u2014 " + getString(R.string.event_periodstart);
                    if (dayPrevious != null) {
                        // If we have a previous day, then update the previous
                        // days length description
                        Integer length = day.date.diffDayPeriods(dayPrevious.date);
                        if(length <= maximumcyclelength) {
                            entries[pos - 1] += "\n"
                                    + String.format(
                                    getString(R.string.event_periodlength),
                                    length.toString());
                        } else {
                            entries[pos - 1] +=
                                    String.format("\n%s", getString(R.string.event_ignored));
                        }
                    }
                    break;
            }
            pos++;
        }
        // If we have at least one entry, update the last days length
        // description to "first entry"
        if (pos > 0) {
            entries[pos - 1] += "\n" + getString(R.string.event_periodfirst);
        }


        // Set custom view
        setContentView(R.layout.activity_list);

        ListView listView = findViewById(R.id.listview);
        listView.setAdapter(new ArrayAdapter<>(this, R.layout.listitem,
                entries));
        listView.setOnItemClickListener(this);

        // Activate "back button" in Action Bar if possible
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    /**
     * Called when the activity is destroyed
     */
    @Override
    protected void onDestroy() {
        // Close database
        dbMain.close();

        super.onDestroy();
    }

    /**
     *  Handler for ICS "home" button
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
     * Handler for opening a list item which will return to the main view
     *
     * @param adapterView
     * The ListView where the click happened
     *
     * @param v
     * The view that was clicked within the ListView
     *
     * @param position
     * The position of the view in the list
     *
     * @param id
     * The row id of the item that was clicked
     */
    @Override
    public void onItemClick(AdapterView<?> adapterView, View v, int position, long id) {
        // Determine date of clicked item
        if (dbMain != null && position >= 0
                && position < dbMain.dayEntries.size()) {
            DayEntry selectedEntry = dbMain.dayEntries.get(position);

            Integer month = selectedEntry.date.get(Calendar.MONTH);
            Integer year = selectedEntry.date.get(Calendar.YEAR);

            Intent intent = getIntent();
            intent.putExtra("month", month.toString());
            intent.putExtra("year", year.toString());

            setResult(RESULT_OK, intent);
            finish();
        }
    }
}
