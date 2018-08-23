/*
 * Periodical main activity 
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

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import static de.arnowelzel.android.periodical.PeriodicalDatabase.DayEntry.PERIOD_CONFIRMED;
import static de.arnowelzel.android.periodical.PeriodicalDatabase.DayEntry.PERIOD_START;

/**
 * The main activity of the app
 */
public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private final int[] calButtonIds = { R.id.cal01, R.id.cal02, R.id.cal03,
            R.id.cal04, R.id.cal05, R.id.cal06, R.id.cal07, R.id.cal08,
            R.id.cal09, R.id.cal10, R.id.cal11, R.id.cal12, R.id.cal13,
            R.id.cal14, R.id.cal15, R.id.cal16, R.id.cal17, R.id.cal18,
            R.id.cal19, R.id.cal20, R.id.cal21, R.id.cal22, R.id.cal23,
            R.id.cal24, R.id.cal25, R.id.cal26, R.id.cal27, R.id.cal28,
            R.id.cal29, R.id.cal30, R.id.cal31, R.id.cal32, R.id.cal33,
            R.id.cal34, R.id.cal35, R.id.cal36, R.id.cal37, R.id.cal38,
            R.id.cal39, R.id.cal40, R.id.cal41, R.id.cal42 };
    private final int[] calButtonIds_2 = { R.id.cal01_2, R.id.cal02_2, R.id.cal03_2,
            R.id.cal04_2, R.id.cal05_2, R.id.cal06_2, R.id.cal07_2,
            R.id.cal08_2, R.id.cal09_2, R.id.cal10_2, R.id.cal11_2,
            R.id.cal12_2, R.id.cal13_2, R.id.cal14_2, R.id.cal15_2,
            R.id.cal16_2, R.id.cal17_2, R.id.cal18_2, R.id.cal19_2,
            R.id.cal20_2, R.id.cal21_2, R.id.cal22_2, R.id.cal23_2,
            R.id.cal24_2, R.id.cal25_2, R.id.cal26_2, R.id.cal27_2,
            R.id.cal28_2, R.id.cal29_2, R.id.cal30_2, R.id.cal31_2,
            R.id.cal32_2, R.id.cal33_2, R.id.cal34_2, R.id.cal35_2,
            R.id.cal36_2, R.id.cal37_2, R.id.cal38_2, R.id.cal39_2,
            R.id.cal40_2, R.id.cal41_2, R.id.cal42_2 };

    private final String STATE_MONTH = "month";
    private final String STATE_YEAR = "year";

    private static final int PERMISSION_CONFIRM_BACKUP = 1;
    private static final int PERMISSION_CONFIRM_RESTORE = 2;

    private GestureDetector gestureDetector;

    private int viewCurrent = R.id.calendar;

    private int monthCurrent, yearCurrent;

    /* First day of the week (0 = sunday) */
    private int firstDayOfWeek;

    private PeriodicalDatabase dbMain;

    /* Request codes for other activities */
    private static final int PICK_DATE = 1;       // Detail list: Date selected in detail list
    private static final int SET_OPTIONS = 2;     // Preferences: Options changed
    private static final int HELP_CLOSED = 3;     // Help: closed
    private static final int ABOUT_CLOSED = 4;    // About: closed
    private static final int DETAILS_CLOSED = 5;  // Details: closed

    /* Status of the main navigartion drawer */
    private boolean navigationDrawerActive = false;

    /**
     * Called when activity starts
     */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Context context = getApplicationContext();
        assert context != null;

        // setContentView(R.layout.main);

        // Setup main view with navigation drawer
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        // Listener to detect when the navigation drawer is opening, so we
        // avoid the main view to handle the swipe of the navigation drawer
        drawer.addDrawerListener(new DrawerLayout.DrawerListener() {
                 @Override
                 public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
                     navigationDrawerActive = true;
                 }

                 @Override
                 public void onDrawerOpened(@NonNull View drawerView) {
                     navigationDrawerActive = true;
                 }

                 @Override
                 public void onDrawerClosed(@NonNull View drawerView) {
                     navigationDrawerActive = false;
                 }

                 @Override
                 public void onDrawerStateChanged(int newState) {
                 }
             });

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Setup gesture handling
        gestureDetector = new GestureDetector(context, new CalendarGestureDetector());
        @SuppressWarnings("unused")
        View.OnTouchListener gestureListener = new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        gestureDetector.onTouchEvent(event);
                        break;
                    case MotionEvent.ACTION_UP:
                        v.performClick();
                        break;
                    default:
                        break;
                }
                return true;
            }
        };

        // Setup database
        dbMain = new PeriodicalDatabase(context);

        // Restore preferences from database to make sure, we got the correct datatypes
        dbMain.restorePreferences();

        // If savedInstanceState exists, restore the last
        // instance state, otherwise use current month as start value
        if (savedInstanceState == null) {
            initMonth();
        } else {
            monthCurrent = savedInstanceState.getInt(STATE_MONTH);
            yearCurrent = savedInstanceState.getInt(STATE_YEAR);
        }

        // Update calculated values
        dbMain.loadCalculatedData();
    }

    /**
     * Called when the activity starts interacting with the user
     */
    @Override
    protected void onResume() {
        super.onResume();

        // Update calendar view
        calendarUpdate();
    }

    /**
     * Called to save the current instance state
     *
     * @param outState
     * Bundle to place the saved state
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(STATE_MONTH, monthCurrent);
        outState.putInt(STATE_YEAR, yearCurrent);
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
     * Close draw when pressing "back"
     */
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Called when the user selects an item in the navigation drawr
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        switch (item.getItemId()) {
            case R.id.list:
                showList();
                return true;

            case R.id.listdetails:
                showListDetails();
                return true;

            case R.id.help:
                showHelp();
                return true;

            case R.id.about:
                showAbout();
                return true;

            case R.id.copy:
                doBackup();
                return true;

            case R.id.restore:
                doRestore();
                return true;

            case R.id.options:
                showOptions();
                return true;

            case R.id.exit:
                finish();
                return true;
        }

        return true;
    }

    /**
     * Handler for "Help" menu action
     */
    private void showHelp() {
        startActivityForResult(
                new Intent(MainActivity.this, HelpActivity.class), HELP_CLOSED);
    }

    /**
     * Handler for "About" menu action
     */
    private void showAbout() {
        startActivityForResult(
                new Intent(MainActivity.this, AboutActivity.class), ABOUT_CLOSED);
    }

    /**
     * Handler for "List" menu action
     */
    private void showList() {
        startActivityForResult(
                new Intent(MainActivity.this, ListActivity.class), PICK_DATE);
    }

    /**
     * Handler for "List, details" menu action
     */
    private void showListDetails() {
        startActivityForResult(
                new Intent(MainActivity.this, ListDetailsActivity.class), PICK_DATE);
    }

    /**
     * Handler for "Options" menu action
     */
    private void showOptions() {
        startActivityForResult(
                new Intent(MainActivity.this, PreferenceActivity.class), SET_OPTIONS);
    }

    /**
     * Update calendar data and view
     */
    @SuppressWarnings("WrongConstant")
    @SuppressLint("DefaultLocale")
    private void calendarUpdate() {
        final Context context = getApplicationContext();
        assert context != null;

        // Initialize control ids for the target view to be used
        int calendarCells[];
        if (viewCurrent == R.id.calendar) {
            calendarCells = calButtonIds;
        } else {
            calendarCells = calButtonIds_2;
        }
        
        PreferenceUtils preferences = new PreferenceUtils(context);

        // Set weekday labels depending on selected start of week
        int startofweek = preferences.getInt("startofweek", 0);
        if(startofweek == 0) {
            findViewById(R.id.rowcaldays0).setVisibility(View.VISIBLE);
            findViewById(R.id.rowcaldays0_2).setVisibility(View.VISIBLE);
            findViewById(R.id.rowcaldays1).setVisibility(View.GONE);
            findViewById(R.id.rowcaldays1_2).setVisibility(View.GONE);
        } else {
            findViewById(R.id.rowcaldays0).setVisibility(View.GONE);
            findViewById(R.id.rowcaldays0_2).setVisibility(View.GONE);
            findViewById(R.id.rowcaldays1).setVisibility(View.VISIBLE);
            findViewById(R.id.rowcaldays1_2).setVisibility(View.VISIBLE);
        }

        // Show day of cycle?
        boolean show_cycle = preferences.getBoolean("show_cycle", true);

        // Create calendar object for current month
        GregorianCalendar cal = new GregorianCalendar(yearCurrent, monthCurrent - 1, 1);

        // Output current year/month
        TextView displayDate = findViewById(R.id.displaydate);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM yyyy");
        displayDate.setText(String.format("%s\nØ%d ↓%d ↑%d",
                dateFormat.format(cal.getTime()),
                dbMain.cycleAverage, dbMain.cycleShortest,
                dbMain.cycleLongest));
        displayDate.setContentDescription(String.format("%s - %s %d - %s %d - %s %d",
                dateFormat.format(cal.getTime()),
                getResources().getString(R.string.label_average_cycle), dbMain.cycleAverage,
                getResources().getString(R.string.label_shortest_cycle), dbMain.cycleShortest,
                getResources().getString(R.string.label_longest_cycle), dbMain.cycleLongest));

        // Calculate first week day of month
        firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int daysCount = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        // If the week should start on monday, adjust the first day of the month,
        // so every day moves one position to the left and sunday gets to the end
        if(startofweek == 1) {
            firstDayOfWeek--;
            if(firstDayOfWeek == 0) firstDayOfWeek = 7;
        }
        
        GregorianCalendar calToday = new GregorianCalendar();
        int dayToday = calToday.get(GregorianCalendar.DATE);
        int monthToday = calToday.get(GregorianCalendar.MONTH) + 1;
        int yearToday = calToday.get(GregorianCalendar.YEAR);

        // Adjust calendar elements
        for (int i = 1; i <= 42; i++) {
            CalendarCell cell = findViewById(calendarCells[i - 1]);
            if (i < firstDayOfWeek || i >= firstDayOfWeek + daysCount) {
                cell.setVisibility(android.view.View.INVISIBLE);
                // TODO Display days of previous/next month as "disabled" buttons
            } else {
                // This cell is part of the current month,
                // label text is the day of the month
                int day = i - firstDayOfWeek + 1;
                cell.setText(String.format("%d", day));
                cell.setVisibility(android.view.View.VISIBLE);
                PeriodicalDatabase.DayEntry entry = dbMain.getEntry(cal);

                boolean current = false;

                if (day == dayToday && monthCurrent == monthToday && yearCurrent == yearToday) {
                    current = true;
                }
                
                // Set other button attributes
                cell.setYear(yearCurrent);
                cell.setMonth(monthCurrent);
                cell.setDay(day);
                cell.setCurrent(current);

                if(entry != null) {
                    cell.setType(entry.type);
                    cell.setDayofcycle(show_cycle ? entry.dayofcycle : 0);
                    cell.setIntensity(entry.intensity);
                } else {
                    cell.setType(PeriodicalDatabase.DayEntry.EMPTY);
                    cell.setDayofcycle(0);
                }

                // Set content description for TalkBack
                cell.updateContentDescription();

                cal.add(GregorianCalendar.DATE, 1);
            }
        }
    }

    /**
     * Handler for "previous month" button in main view
     */
    @SuppressWarnings({"UnusedParameters", "SameParameterValue"})
    public void goPrev(View v) {
        // Update calendar
        monthCurrent--;
        if (monthCurrent < 1) {
            monthCurrent = 12;
            yearCurrent--;
        }

        if (viewCurrent == R.id.calendar) {
            viewCurrent = R.id.calendar_2;
        } else {
            viewCurrent = R.id.calendar;
        }

        calendarUpdate();

        // Show slide animation from left to right
        ViewFlipper flipper = findViewById(R.id.mainwidget);
        flipper.setInAnimation(AnimationHelper.inFromLeftAnimation());
        flipper.setOutAnimation(AnimationHelper.outToRightAnimation());
        flipper.showNext();
    }

    /**
     * Handler for "next month" button in main view
     */
    @SuppressWarnings({"UnusedParameters", "SameParameterValue"})
    public void goNext(View v) {
        // Update calendar
        monthCurrent++;
        if (monthCurrent > 12) {
            monthCurrent = 1;
            yearCurrent++;
        }

        if (viewCurrent == R.id.calendar) {
            viewCurrent = R.id.calendar_2;
        } else {
            viewCurrent = R.id.calendar;
        }

        calendarUpdate();

        // Show slide animation from right to left
        ViewFlipper flipper = findViewById(R.id.mainwidget);
        flipper.setInAnimation(AnimationHelper.inFromRightAnimation());
        flipper.setOutAnimation(AnimationHelper.outToLeftAnimation());
        flipper.showPrevious();
    }

    /**
     * Handler for "current" button in main view
     */
    @SuppressWarnings({"UnusedParameters", "SameParameterValue"})
    public void goCurrent(View v) {
        initMonth();
        calendarUpdate();
    }

    /**
     * Change to current month
     */
    private void initMonth() {
        Calendar cal = new GregorianCalendar();
        monthCurrent = cal.get(Calendar.MONTH) + 1;
        yearCurrent = cal.get(Calendar.YEAR);
    }

    /**
     * Helper to request the required permissions for backups if needed
     */
    private boolean checkBackupStoragePermissions(final int requestCode) {
        final Activity activity = this;

        // Check if we have the permission to access storage
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setMessage(getResources().getString(R.string.permissions_needed));

            builder.setPositiveButton(getResources().getString(R.string.permissions_needed_ok),
                    new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(activity,
                                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    requestCode
                                    );
                        }
                    });

            builder.show();

            return false;
        } else {
            return true;
        }
    }

    /**
     * Handle permission request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,  @NonNull String[] permissions, @NonNull int[] grantResults) {
        final Context context = getApplicationContext();
        assert context != null;

        // If the requested permission was granted by the user,
        // run the operation which requested the permission
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case PERMISSION_CONFIRM_BACKUP:
                    runBackup(context);
                    break;
                case PERMISSION_CONFIRM_RESTORE:
                    runRestore(context);
                    break;
            }
        }
    }

    /**
     * Handler for "backup" menu action
     */
    private void doBackup() {
        final Context context = getApplicationContext();
        assert context != null;

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.backup_title));
        builder.setMessage(getResources().getString(R.string.backup_text));
        builder.setIcon(R.drawable.ic_warning_black_40dp);

        builder.setPositiveButton(getResources().getString(R.string.backup_ok),
                new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(checkBackupStoragePermissions(PERMISSION_CONFIRM_BACKUP)) {
                            runBackup(context);
                        }
                    }
                });

        builder.setNegativeButton(
                getResources().getString(R.string.backup_cancel),
                new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

        builder.show();
    }

    /**
     * Run database backup and show a toast for the result
     */
    private void runBackup(Context context) {
        boolean ok = dbMain.backup();

        // Show toast depending on result of operation
        String text;
        if (ok) {
            text = getResources().getString(R.string.backup_finished);
        } else {
            text = getResources().getString(R.string.backup_failed);
        }

        Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
        toast.show();
    }

    /**
     * Handler for "restore" menu action
     */
    private void doRestore() {
        final Context context = getApplicationContext();
        assert context != null;

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.restore_title));
        builder.setMessage(getResources().getString(R.string.restore_text));
        builder.setIcon(R.drawable.ic_warning_black_40dp);

        builder.setPositiveButton(
                getResources().getString(R.string.restore_ok),
                new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(checkBackupStoragePermissions(PERMISSION_CONFIRM_RESTORE)) {
                            runRestore(context);
                        }
                    }
                });

        builder.setNegativeButton(
                getResources().getString(R.string.restore_cancel),
                new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

        builder.show();
    }

    /**
     * Run database restore and show a toast for the result
     */
    private void runRestore(Context context) {
        boolean ok = dbMain.restore();

        // Show toast depending on result of operation
        String text;
        if (ok) {
            dbMain.restorePreferences();
            handleDatabaseEdit();
            text = getResources().getString(R.string.restore_finished);
        } else {
            text = getResources().getString(R.string.restore_failed);
        }
        Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
        toast.show();
    }

    /**
     * Handler for the selection of one day in the calendar
     */
    public void handleCalendarButton(View v) {
        final Context context = getApplicationContext();
        assert context != null;

        // Determine selected date
        int idButton = v.getId();
        int nButtonClicked = 0;

        int calButtonIds[];
        if (viewCurrent == R.id.calendar) {
            calButtonIds = this.calButtonIds;
        } else {
            calButtonIds = calButtonIds_2;
        }

        while (nButtonClicked < 42) {
            if (calButtonIds[nButtonClicked] == idButton)
                break;
            nButtonClicked++;
        }
        final int day = nButtonClicked - firstDayOfWeek + 2;

        // If "direct details" is set by the user, just open the details
        PreferenceUtils preferences = new PreferenceUtils(context);

        if(preferences.getBoolean("direct_details", false)) {
            showDetailsActivity(yearCurrent, monthCurrent, day);
        } else {
            // Set or remove entry with confirmation
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getResources()
                    .getString(R.string.calendaraction_title));

            final GregorianCalendar date = new GregorianCalendar(yearCurrent, monthCurrent -1 , day);
            int type = dbMain.getEntryType(date);
            if (type != PERIOD_START && type != PERIOD_CONFIRMED) {
                builder.setMessage(getResources().getString(
                        R.string.calendaraction_add));
                builder.setPositiveButton(
                        getResources().getString(R.string.calendaraction_ok),
                        new OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dbMain.addPeriod(date);
                                handleDatabaseEdit();
                            }
                        });

                builder.setNegativeButton(
                        getResources().getString(R.string.calendaraction_cancel),
                        new OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });

                builder.setNeutralButton(
                        getResources().getString(R.string.calendaraction_details),
                        new OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                showDetailsActivity(yearCurrent, monthCurrent, day);
                            }
                        });
            } else {
                if(type == PERIOD_START) builder.setMessage(getResources().getString(R.string.calendaraction_removeperiod));
                else builder.setMessage(getResources().getString(R.string.calendaraction_remove));
                builder.setPositiveButton(
                        getResources().getString(R.string.calendaraction_ok),
                        new OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dbMain.removeData(date);
                                handleDatabaseEdit();
                            }
                        });

                builder.setNegativeButton(
                        getResources().getString(R.string.calendaraction_cancel),
                        new OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });

                builder.setNeutralButton(
                        getResources().getString(R.string.calendaraction_details),
                        new OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                showDetailsActivity(yearCurrent, monthCurrent, day);
                            }
                        });
            }
            builder.show();
        }
    }

    /**
     * Helper to show the details activity for a specific day
     */
    private void showDetailsActivity(int year, int month, int day) {
        Intent details = new Intent(MainActivity.this, DetailsActivity.class);
        details.putExtra("year", year);
        details.putExtra("month", month);
        details.putExtra("day", day);
        startActivityForResult(details, DETAILS_CLOSED);
    }

    /**
     * Helper to update view after database modification
     */
    private void handleDatabaseEdit() {
        // Update calculated values
        dbMain.loadCalculatedData();
        calendarUpdate();

        // Notify backup agent about the change and mark DB as clean
        BackupManager bm = new BackupManager(this);
        bm.dataChanged();
    }

    /**
     * Handler of activity results (detail list, options)
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // Specific date in detail list selected
            case PICK_DATE:
                if (resultCode == RESULT_OK) {
                    Bundle extras = data.getExtras();
                    if (extras != null) {
                        monthCurrent = Integer.parseInt(extras.getString("month")) + 1;
                        yearCurrent = Integer.parseInt(extras.getString("year"));
                        calendarUpdate();
                    }
    
                }
                break;
            
            // Options modified
            case SET_OPTIONS:
                handleDatabaseEdit();
                calendarUpdate();
                break;

            // Details closed
            case DETAILS_CLOSED:
                dbMain.loadCalculatedData();
                calendarUpdate();
                break;
        }
    }

    /**
     * Touch dispatcher to pass events to the gesture detector to detect swipes on the UI
     */

    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        boolean result = super.dispatchTouchEvent(e);

        // Only dispatch touch event to gesture detector,
        // if the navigation drawer is not active (opening, closing etc.)
        if(!navigationDrawerActive) {
            return gestureDetector.onTouchEvent(e);
        }

        return result;
    }

    /**
     * Gesture detector to handle swipes on the UI
     */
    private class CalendarGestureDetector extends SimpleOnGestureListener {
        private static final int SWIPE_MIN_DISTANCE = 120;
        private static final int SWIPE_MAX_OFF_PATH = 250;
        private static final int SWIPE_THRESHOLD_VELOCITY = 200;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                               float velocityY) {
            try {
                // if swipe is not straight enough then ignore
                if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH) {
                    return false;
                }

                if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE
                        && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    goNext(null);
                } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
                        && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    goPrev(null);
                }
            } catch (Exception e) {
                // nothing
            }
            return false;
        }
    }
}

