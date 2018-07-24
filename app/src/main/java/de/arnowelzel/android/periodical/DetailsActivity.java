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
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatCheckBox;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.MultiAutoCompleteTextView;
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
public class DetailsActivity extends AppCompatActivity implements View.OnClickListener, TextWatcher {
    private PeriodicalDatabase dbMain;
    private PeriodicalDatabase.DayEntry entry;
    private RadioButton buttonPeriodIntensity1;
    private RadioButton buttonPeriodIntensity2;
    private RadioButton buttonPeriodIntensity3;
    private RadioButton buttonPeriodIntensity4;
    private int maxSymptomNum;

    /**
     *  Called when the activity starts
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        final Context context = getApplicationContext();
        assert context != null;
        super.onCreate(savedInstanceState);

        // Set up view
        setContentView(R.layout.activity_details);

        // Activate "back button" in Action Bar
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);

        // Get details
        Intent intent = getIntent();
        int year = intent.getIntExtra("year", 1970);
        int month = intent.getIntExtra("month", 1);
        int day = intent.getIntExtra("day", 1);

        dbMain = new PeriodicalDatabase(context);
        dbMain.loadCalculatedData();

        entry = dbMain.getEntryWithDetails(year, month, day);
        if(entry == null) {
            entry = new PeriodicalDatabase.DayEntry(EMPTY, new GregorianCalendar(year, month - 1, day), 0, 1);
        }

        // Set header using the entry date
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.LONG);
        ((TextView)findViewById(R.id.labelDetailsHeader)).setText(
                String.format("%s", dateFormat.format(entry.date.getTime())));

        // Set period status
        RadioButton buttonPeriodYes = findViewById(R.id.periodYes);
        RadioButton buttonPeriodNo = findViewById(R.id.periodNo);
        boolean intensityEnabled = false;

        switch(entry.type) {
            case PERIOD_START:
            case PERIOD_CONFIRMED:
                buttonPeriodYes.setChecked(true);
                intensityEnabled = true;
                break;
            default:
                buttonPeriodNo.setChecked(true);
                break;
        }

        buttonPeriodYes.setOnClickListener(this);
        buttonPeriodNo.setOnClickListener(this);

        // Set period intensity
        buttonPeriodIntensity1 = findViewById(R.id.periodIntensity1);
        buttonPeriodIntensity2 = findViewById(R.id.periodIntensity2);
        buttonPeriodIntensity3 = findViewById(R.id.periodIntensity3);
        buttonPeriodIntensity4 = findViewById(R.id.periodIntensity4);

        switch(entry.intensity) {
            case 1: buttonPeriodIntensity1.setChecked(true);break;
            case 2: buttonPeriodIntensity2.setChecked(true);break;
            case 3: buttonPeriodIntensity3.setChecked(true);break;
            case 4: buttonPeriodIntensity4.setChecked(true);break;
        }

        buttonPeriodIntensity1.setEnabled(intensityEnabled);
        buttonPeriodIntensity2.setEnabled(intensityEnabled);
        buttonPeriodIntensity3.setEnabled(intensityEnabled);
        buttonPeriodIntensity4.setEnabled(intensityEnabled);
        buttonPeriodIntensity1.setOnClickListener(this);
        buttonPeriodIntensity2.setOnClickListener(this);
        buttonPeriodIntensity3.setOnClickListener(this);
        buttonPeriodIntensity4.setOnClickListener(this);

        // Transfer notes
        MultiAutoCompleteTextView editNotes = findViewById(R.id.editNotes);
        editNotes.setText(entry.notes);
        editNotes.addTextChangedListener(this);

        // Build list of events/symptoms
        LinearLayout groupEvents = findViewById(R.id.groupEvents);
        String packageName = getPackageName();
        Resources resources = getResources();
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        int marginLeft = (int)(12 * Resources.getSystem().getDisplayMetrics().density);
        int marginRight = (int)(12 * Resources.getSystem().getDisplayMetrics().density);
        layoutParams.setMargins(marginLeft,0, marginRight, 0);

        int num = 1;
        while(true) {
            @SuppressLint("DefaultLocale") String resName = String.format("label_details_ev%d",num);
            int resId = resources.getIdentifier(resName, "string", packageName);
            if(resId != 0) {
                AppCompatCheckBox option = new AppCompatCheckBox(this);
                option.setLayoutParams(layoutParams);
                option.setTextSize(18);
                option.setText(resId);
                option.setId(resId);
                if(entry.symptoms.contains(num)) option.setChecked(true);
                option.setOnClickListener(this);
                groupEvents.addView(option);
                num++;
            } else {
                break;
            }
        }
        // Store highest possible symptom index for the onClick handler
        maxSymptomNum = num;
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

    /**
     * Listener for clicks on the radio buttons and checkboxes
     */
    public void onClick(View v) {
        int id = v.getId();
        switch(id) {
            case R.id.periodYes:
                dbMain.addPeriod(entry.date);
                buttonPeriodIntensity1.setEnabled(true);
                buttonPeriodIntensity2.setEnabled(true);
                buttonPeriodIntensity3.setEnabled(true);
                buttonPeriodIntensity4.setEnabled(true);
                break;
            case R.id.periodNo:
                dbMain.removeData(entry.date);
                buttonPeriodIntensity1.setEnabled(false);
                buttonPeriodIntensity2.setEnabled(false);
                buttonPeriodIntensity3.setEnabled(false);
                buttonPeriodIntensity4.setEnabled(false);
                break;
            case R.id.periodIntensity1:
                entry.intensity = 1;
                dbMain.addEntryDetails(entry);
                break;
            case R.id.periodIntensity2:
                entry.intensity = 2;
                dbMain.addEntryDetails(entry);
                break;
            case R.id.periodIntensity3:
                entry.intensity = 3;
                dbMain.addEntryDetails(entry);
                break;
            case R.id.periodIntensity4:
                entry.intensity = 4;
                dbMain.addEntryDetails(entry);
                break;
            default:
                String packageName = getPackageName();
                int resId = getResources().getIdentifier("label_details_ev1", "string", packageName);
                int symptom = id - resId;
                if(symptom >= 0 && symptom < maxSymptomNum) {
                    entry.symptoms.clear();
                    int num = 1;
                    while(true) {
                        @SuppressLint("DefaultLocale") String resName = String.format("label_details_ev%d",num);
                        resId = getResources().getIdentifier(resName, "string", packageName);
                        if(resId != 0) {
                            CheckBox option = findViewById(resId);
                            if(option.isChecked()) entry.symptoms.add(num);
                            num++;
                        } else {
                            break;
                        }
                    }
                }
                dbMain.addEntryDetails(entry);
                databaseChanged();
                break;
        }
    }

    /**
     * Handler for text changes in edit fields
     */
    @Override
    public void beforeTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable editable) {
        entry.notes = ((MultiAutoCompleteTextView)findViewById(R.id.editNotes)).getText().toString();
        dbMain.addEntryDetails(entry);
        databaseChanged();
    }

    /**
     * Helper to notify backup agent about database changes
     */
    private void databaseChanged() {
        BackupManager bm = new BackupManager(this);
        bm.dataChanged();
    }
}
