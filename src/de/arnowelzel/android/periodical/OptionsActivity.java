/**
 * Periodical options activity 
 * Copyright (C) 2012-2014 Arno Welzel
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
 * Periodical Activity
 */

package de.arnowelzel.android.periodical;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioButton;

/**
 * Created by Arno on 2014-05-19.
 */
public class OptionsActivity extends Activity {
    /* Database for calendar data */
    private PeriodicalDatabase dbMain;
    
    /* Controls interfaces */
    RadioButton method_knausogino;
    RadioButton method_standarddays;

    /* Called when activity starts */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set up database
        dbMain = new PeriodicalDatabase(getApplicationContext());
        
        // Set up view
        setContentView(R.layout.page_options);

        method_knausogino = (RadioButton) findViewById(R.id.method_knausogino);
        method_standarddays = (RadioButton) findViewById(R.id.method_standarddays);
        
        // Get current options
        int method = 0;
        try {
            method = Integer.parseInt(dbMain.getOption("CalculationMethod"));
        } catch(NumberFormatException e) {
        }
        switch(method) {
            case 1:
                method_standarddays.setChecked(true);
                break;
            default:
                method_knausogino.setChecked(true);
                break;
        }

        // Activate "back button" in Action Bar if possible
        if (android.os.Build.VERSION.SDK_INT >= 11) {
            ActionBar actionBar = getActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /* Called to save the current instance state */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
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

    /* Handler for "Cancel" button to leave without saving the options */
    public void onOptionsCancelClick(View view) {
        Intent intent = this.getIntent();
        setResult(RESULT_CANCELED, intent);
        finish();
    }
    
    /* Handler for "OK" button to save the options */
    public void onOptionsOkClick(View view) {
        // Save options and return result to main view
        if (dbMain != null ) {
            Integer method = 0;

            if(method_standarddays.isChecked()) method = 1;
            dbMain.setOption("CalculationMethod", method.toString());

            Intent intent = this.getIntent();
            setResult(RESULT_OK, intent);
            finish();
        }
    }
}