/*
 * Periodical list activity
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
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;

import de.arnowelzel.android.periodical.PeriodicalDatabase.DayEntry;

/**
 * Activity to handle the "List, details" command
 */
public class ListDetailsActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
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

        // Set up database and string array for the list
        dbMain = new PeriodicalDatabase(context);
        dbMain.loadRawDataWithDetails();

        ArrayList<DayEntry> dayList = new ArrayList<>();
        Iterator<DayEntry> dayIterator = dbMain.dayEntries.iterator();
        DayEntry day;
        while (dayIterator.hasNext()) {
            day = dayIterator.next();
            dayList.add(0, day);
        }

        // Set custom view
        setContentView(R.layout.activity_list_details);

        // Activate "back button" in Action Bar
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);

		ListView listView = findViewById(R.id.listview_details);
        listView.setAdapter(new DayEntryAdapter(this, dayList, getPackageName(), getResources()));
		listView.setOnItemClickListener(this);
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
        int listsize = dbMain.dayEntries.size();
        if (position >= 0 && position < listsize) {
            DayEntry selectedEntry = dbMain.dayEntries.get(listsize - position - 1);

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
