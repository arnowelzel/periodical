/*
 * Periodical "help" activity
 * Copyright (C) 2012-2020 Arno Welzel
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
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatCheckBox;
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

    /**
     * Called when the activity starts
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

        // Set header using the entry date
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.LONG);
        ((TextView) findViewById(R.id.labelDetailsHeader)).setText(
                String.format("%s", dateFormat.format(entry.date.getTime())));

        // Set period status
        RadioButton buttonPeriodYes = findViewById(R.id.periodYes);
        RadioButton buttonPeriodNo = findViewById(R.id.periodNo);
        boolean intensityEnabled = false;

        switch (entry.type) {
            case PERIOD_START:
            case PERIOD_CONFIRMED:
                buttonPeriodYes.setChecked(true);
                intensityEnabled = true;
                break;
            default:
                buttonPeriodNo.setChecked(true);
                // Default intensity for new period days
                entry.intensity = 2;
                break;
        }

        buttonPeriodYes.setOnClickListener(this);
        buttonPeriodNo.setOnClickListener(this);

        // Set period intensity
        buttonPeriodIntensity1 = findViewById(R.id.periodIntensity1);
        buttonPeriodIntensity2 = findViewById(R.id.periodIntensity2);
        buttonPeriodIntensity3 = findViewById(R.id.periodIntensity3);
        buttonPeriodIntensity4 = findViewById(R.id.periodIntensity4);

        switch (entry.intensity) {
            case 1:
                buttonPeriodIntensity1.setChecked(true);
                break;
            case 2:
                buttonPeriodIntensity2.setChecked(true);
                break;
            case 3:
                buttonPeriodIntensity3.setChecked(true);
                break;
            case 4:
                buttonPeriodIntensity4.setChecked(true);
                break;
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

        // Build list of events and symptoms
        LinearLayout groupEvents = findViewById(R.id.groupEvents);
        LinearLayout groupMood = findViewById(R.id.groupMood);
        LinearLayout groupSymptoms = findViewById(R.id.groupSymptoms);
        String packageName = getPackageName();
        Resources resources = getResources();
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        int marginLeft = (int) (12 * Resources.getSystem().getDisplayMetrics().density);
        int marginRight = (int) (12 * Resources.getSystem().getDisplayMetrics().density);
        layoutParams.setMargins(marginLeft, 0, marginRight, 0);

        // Elements 0-1 are events, 2-6 moods, 7-22 are symptoms
        int eventIds[] = {
                1,  // Intercourse
                18, // Contraceptive pill
                20, // Tired
                21, // Energized
                22, // Sad
                14, // Grumpiness
                23, // Edgy
                19, // Spotting
                9,  // Intense bleeding
                2,  // Cramps
                17, // Headache/migraine
                3,  // Back pain
                4,  // Middle pain left
                5,  // Middle pain right
                6,  // Breast pain/dragging pain
                7,  // Thrush/candida
                8,  // Discharge
                10, // Temperature fluctuations
                11, // Pimples
                12, // Bloating
                13, // Fainting
                15, // Nausea
                16, // Cravings
        };
        int num = 0;
        for (int eventId : eventIds) {
            @SuppressLint("DefaultLocale") String resName = String.format("label_details_ev%d", eventId);
            int resId = resources.getIdentifier(resName, "string", packageName);
            if (resId != 0) {
                AppCompatCheckBox option = new AppCompatCheckBox(this);
                option.setLayoutParams(layoutParams);
                option.setTextSize(18);
                option.setText(resId);
                option.setId(resId);
                if (entry.symptoms.contains(eventId)) option.setChecked(true);
                option.setOnClickListener(this);
                if (num < 2) {
                    groupEvents.addView(option);
                } else if(num > 1 && num < 7) {
                    groupMood.addView(option);
                } else {
                    groupSymptoms.addView(option);
                }
            }
            num++;
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

    /**
     * Listener for clicks on the radio buttons and checkboxes
     */
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.periodYes:
                dbMain.addPeriod(entry.date);
                databaseChanged();
                buttonPeriodIntensity1.setEnabled(true);
                buttonPeriodIntensity2.setEnabled(true);
                buttonPeriodIntensity3.setEnabled(true);
                buttonPeriodIntensity4.setEnabled(true);
                break;
            case R.id.periodNo:
                dbMain.removePeriod(entry.date);
                databaseChanged();
                buttonPeriodIntensity1.setEnabled(false);
                buttonPeriodIntensity2.setEnabled(false);
                buttonPeriodIntensity3.setEnabled(false);
                buttonPeriodIntensity4.setEnabled(false);
                break;
            case R.id.periodIntensity1:
                entry.intensity = 1;
                dbMain.addEntryDetails(entry);
                databaseChanged();
                break;
            case R.id.periodIntensity2:
                entry.intensity = 2;
                dbMain.addEntryDetails(entry);
                databaseChanged();
                break;
            case R.id.periodIntensity3:
                entry.intensity = 3;
                dbMain.addEntryDetails(entry);
                databaseChanged();
                break;
            case R.id.periodIntensity4:
                entry.intensity = 4;
                dbMain.addEntryDetails(entry);
                databaseChanged();
                break;
            default:
                String packageName = getPackageName();
                int resId;
                entry.symptoms.clear();
                int num = 1;
                while (num < 22) {
                    @SuppressLint("DefaultLocale") String resName = String.format("label_details_ev%d", num);
                    resId = getResources().getIdentifier(resName, "string", packageName);
                    if (resId != 0) {
                        CheckBox option = findViewById(resId);
                        if (option.isChecked()) entry.symptoms.add(num);
                    }
                    num++;
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
        entry.notes = ((MultiAutoCompleteTextView) findViewById(R.id.editNotes)).getText().toString();
        dbMain.addEntryDetails(entry);
        databaseChanged();
    }

    /**
     * Called when the activity is destroyed
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (dbMain != null)
            dbMain.close();
    }

    /**
     * Helper to handle changes in the database
     */
    private void databaseChanged() {
        dbMain.loadCalculatedData();

        BackupManager bm = new BackupManager(this);
        bm.dataChanged();
    }
}
