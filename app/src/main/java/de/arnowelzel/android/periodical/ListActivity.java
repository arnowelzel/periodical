/**
 * Periodical list activity 
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

import java.util.Calendar;
import java.util.Iterator;

import android.content.Context;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import de.arnowelzel.android.periodical.PeriodicalDatabase.DayEntry;

public class ListActivity extends android.app.ListActivity {
    /* Database for calendar data */
    private PeriodicalDatabase dbMain;

    /* Called when activity starts */
    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        final Context context = getApplicationContext();
        assert context != null;
        super.onCreate(savedInstanceState);

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
                entries[pos] = entries[pos] + " ("
                        + getString(R.string.event_periodstart) + ")";
                if (dayPrevious != null) {
                    // If we have a previous day, then update the previous
                    // days length description
                    Integer length = day.date.diffDayPeriods(dayPrevious.date);
                    entries[pos - 1] += "\n"
                            + String.format(
                                    getString(R.string.event_periodlength),
                                    length.toString());
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

        setListAdapter(new ArrayAdapter<>(this, R.layout.listitem,
                entries));

        // Set custom view
        setContentView(R.layout.listview);

        // Activate "back button" in Action Bar if possible
        if (android.os.Build.VERSION.SDK_INT >= 11) {
            ActionBar actionBar = getActionBar();
            assert actionBar != null;
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /* Called when the activity is destroyed */
    @Override
    protected void onDestroy() {
        // Close database
        dbMain.close();

        super.onDestroy();
    }

    /* Handler for ICS "home" button */
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

    /* Handler for selecting a list item */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
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
