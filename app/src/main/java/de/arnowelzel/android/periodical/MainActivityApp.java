/*
 * Periodical main activity
 * Copyright (C) 2012-2023 Arno Welzel
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
import android.app.Activity;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import com.google.android.material.navigation.NavigationView;
import com.yariksoffice.lingver.Lingver;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentResultListener;

import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
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
public class MainActivityApp extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private final int[] calButtonIds = {R.id.cal01, R.id.cal02, R.id.cal03,
            R.id.cal04, R.id.cal05, R.id.cal06, R.id.cal07, R.id.cal08,
            R.id.cal09, R.id.cal10, R.id.cal11, R.id.cal12, R.id.cal13,
            R.id.cal14, R.id.cal15, R.id.cal16, R.id.cal17, R.id.cal18,
            R.id.cal19, R.id.cal20, R.id.cal21, R.id.cal22, R.id.cal23,
            R.id.cal24, R.id.cal25, R.id.cal26, R.id.cal27, R.id.cal28,
            R.id.cal29, R.id.cal30, R.id.cal31, R.id.cal32, R.id.cal33,
            R.id.cal34, R.id.cal35, R.id.cal36, R.id.cal37, R.id.cal38,
            R.id.cal39, R.id.cal40, R.id.cal41, R.id.cal42};
    private final int[] calButtonIds_2 = {R.id.cal01_2, R.id.cal02_2, R.id.cal03_2,
            R.id.cal04_2, R.id.cal05_2, R.id.cal06_2, R.id.cal07_2,
            R.id.cal08_2, R.id.cal09_2, R.id.cal10_2, R.id.cal11_2,
            R.id.cal12_2, R.id.cal13_2, R.id.cal14_2, R.id.cal15_2,
            R.id.cal16_2, R.id.cal17_2, R.id.cal18_2, R.id.cal19_2,
            R.id.cal20_2, R.id.cal21_2, R.id.cal22_2, R.id.cal23_2,
            R.id.cal24_2, R.id.cal25_2, R.id.cal26_2, R.id.cal27_2,
            R.id.cal28_2, R.id.cal29_2, R.id.cal30_2, R.id.cal31_2,
            R.id.cal32_2, R.id.cal33_2, R.id.cal34_2, R.id.cal35_2,
            R.id.cal36_2, R.id.cal37_2, R.id.cal38_2, R.id.cal39_2,
            R.id.cal40_2, R.id.cal41_2, R.id.cal42_2};

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

    /* Status of the main navigation drawer */
    private boolean navigationDrawerActive = false;

    /* Last active local in this view */
    private String currentLocale = "";

    /* Flag for Webview fix */
    private boolean webviewFixRequired = true;

    /* Launchers for activities with result */
    private ActivityResultLauncher<Intent> pickDateResultLauncher;
    private ActivityResultLauncher<Intent> setOptionsResultLauncher;
    private ActivityResultLauncher<Intent> detailsResultLauncher;
    private ActivityResultLauncher<Intent> setOptionsResultStorageAccessSelectBackup;
    private ActivityResultLauncher<Intent> setOptionsResultStorageAccessSelectRestore;

    // For unknown reasons, the very first creation of a [WebView] (either programmatically
    // or via inflation) resets an application locale to the system default.
    // More on that: https://issuetracker.google.com/issues/37113860
    //
    // The workaround is to create a webview once, destroy it and set the locale again
    private void webviewFix() {
        if (webviewFixRequired) {
            webviewFixRequired = false;

            Context context = getApplicationContext();
            WebView webView = new WebView(context);
            webView.destroy();
            Lingver lingver = Lingver.getInstance();
            lingver.setLocale(context, lingver.getLocale());
        }
    }

    /**
     * Called when activity starts
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        super.onCreate(savedInstanceState);
        final Context context = getApplicationContext();
        assert context != null;

        // Get current locale
        PreferenceUtils preferenceUtils = new PreferenceUtils(this);
        currentLocale = preferenceUtils.getString("locale", "system");

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
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset)
            {
                navigationDrawerActive = true;
            }

            @Override
            public void onDrawerOpened(@NonNull View drawerView)
            {
                navigationDrawerActive = true;
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView)
            {
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

        // Register activity result launchers
        pickDateResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        handleActivityResultPickDate(result.getData());
                    }
                });
        setOptionsResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        handleActivityResultOptions(result.getData());
                    }
                });
        detailsResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        handleActivityResultDetails(result.getData());
                    }
                });
        setOptionsResultStorageAccessSelectBackup = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        handleActivityResultStorageAccessSelectBackup(result.getData());
                    }
                });
        setOptionsResultStorageAccessSelectRestore = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        handleActivityResultStorageAccessSelectRestore(result.getData());
                    }
                });

        // Fragment listener for dialogs
        getSupportFragmentManager().setFragmentResultListener(
            BackupConfirmationDialogFragment.RESULT_BUNDLE_KEY,
            this,
            new FragmentResultListener() {
                @Override
                public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
                    handleBackupConfirmation(
                            bundle.getInt(BackupConfirmationDialogFragment.RESULT_CHOICE)
                    );
                }
        });
        getSupportFragmentManager().setFragmentResultListener(
                BackupSelectLocationDialogFragment.RESULT_BUNDLE_KEY,
                this,
                new FragmentResultListener() {
                    @Override
                    public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
                        handleBackupSelectLocation(
                                bundle.getInt(BackupSelectLocationDialogFragment.RESULT_CHOICE)
                        );
                    }
                });
        getSupportFragmentManager().setFragmentResultListener(
                RestoreConfirmationDialogFragment.RESULT_BUNDLE_KEY,
                this,
                new FragmentResultListener() {
                    @Override
                    public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
                        handleRestoreConfirmation(
                                bundle.getInt(RestoreConfirmationDialogFragment.RESULT_CHOICE)
                        );
                    }
                });
        getSupportFragmentManager().setFragmentResultListener(
                RestoreSelectLocationDialogFragment.RESULT_BUNDLE_KEY,
                this,
                new FragmentResultListener() {
                    @Override
                    public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
                        handleRestoreSelectLocation(
                                bundle.getInt(RestoreSelectLocationDialogFragment.RESULT_CHOICE)
                        );
                    }
                });
        getSupportFragmentManager().setFragmentResultListener(
                EditCalendarEntryDialogFragment.RESULT_BUNDLE_KEY,
                this,
                new FragmentResultListener() {
                    @Override
                    public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
                        handleEditCalendarEntry(
                                bundle.getInt(EditCalendarEntryDialogFragment.RESULT_TYPE),
                                bundle.getInt(EditCalendarEntryDialogFragment.RESULT_CHOICE),
                                bundle.getInt(EditCalendarEntryDialogFragment.RESULT_YEAR),
                                bundle.getInt(EditCalendarEntryDialogFragment.RESULT_MONTH),
                                bundle.getInt(EditCalendarEntryDialogFragment.RESULT_DAY)
                        );
                    }
                });
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
     * @param outState Bundle to place the saved state
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
     * Close navigation drawer when pressing "back"
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
     * Handle navigation item selection
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
                finishAndRemoveTask();
                return true;
        }

        return true;
    }

    /**
     * Handler for "Help" menu action
     */
    private void showHelp() {
        webviewFix();
        startActivity(
                new Intent(MainActivityApp.this, HelpActivity.class));
    }

    /**
     * Handler for "About" menu action
     */
    private void showAbout() {
        webviewFix();
        startActivity(
                new Intent(MainActivityApp.this, AboutActivity.class));
    }

    /**
     * Handler for "List" menu action
     */
    private void showList() {
        pickDateResultLauncher.launch(new Intent(MainActivityApp.this, ListActivity.class));
    }

    /**
     * Handler for "List, details" menu action
     */
    private void showListDetails() {
        pickDateResultLauncher.launch(
                new Intent(MainActivityApp.this, ListDetailsActivity.class)
        );
    }

    /**
     * Handler for "Options" menu action
     */
    private void showOptions() {
        setOptionsResultLauncher.launch(
                new Intent(MainActivityApp.this, PreferenceActivity.class)
        );
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
        int startOfWeek = preferences.getInt("startofweek", 0);
        int column = 0;
        int dayIndex = startOfWeek;
        int dayLabels[] = {
                R.string.main_calday_su,
                R.string.main_calday_mo,
                R.string.main_calday_tu,
                R.string.main_calday_we,
                R.string.main_calday_th,
                R.string.main_calday_fr,
                R.string.main_calday_sa
        };
        int dayIds[][] = {
                { R.id.daylabel0_0, R.id.daylabel1_0},
                { R.id.daylabel0_1, R.id.daylabel1_1},
                { R.id.daylabel0_2, R.id.daylabel1_2},
                { R.id.daylabel0_3, R.id.daylabel1_3},
                { R.id.daylabel0_4, R.id.daylabel1_4},
                { R.id.daylabel0_5, R.id.daylabel1_5},
                { R.id.daylabel0_6, R.id.daylabel1_6}
        };
        while (column<7) {
            TextView view1 = findViewById(dayIds[column][0]);
            TextView view2 = findViewById(dayIds[column][1]);
            view1.setText(dayLabels[dayIndex]);
            view2.setText(dayLabels[dayIndex]);
            column++;
            dayIndex++;
            if (dayIndex>6) {
                dayIndex = 0;
            }
        }

        // Show day of cycle?
        boolean show_cycle = preferences.getBoolean("show_cycle", true);

        // Create calendar object for current month
        GregorianCalendar cal = new GregorianCalendar(yearCurrent, monthCurrent - 1, 1);

        // Output current year/month
        TextView displayDate = findViewById(R.id.displaydate);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM yyyy");
        displayDate.setText(String.format("%s", dateFormat.format(cal.getTime())));
        displayDate.setContentDescription(String.format("%s", dateFormat.format(cal.getTime())));

        // Calculate first week day of month
        firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int daysCount = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Adjust first day of week to selected start day of week
        firstDayOfWeek -= startOfWeek;
        if (firstDayOfWeek <= 0) firstDayOfWeek += 7;

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
                cell.setIntercourse(false);
                cell.setNotes(false);

                if (entry != null) {
                    cell.setType(entry.type);
                    cell.setDayofcycle(show_cycle ? entry.dayofcycle : 0);
                    cell.setIntensity(entry.intensity);

                    for (int s : entry.symptoms) {
                        if (s == 1) cell.setIntercourse(true);
                        else cell.setNotes(true);
                    }

                    if (!entry.notes.isEmpty()) cell.setNotes(true);
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
     * Handler for "show info" button in main view
     */
    @SuppressWarnings({"UnusedParameters", "SameParameterValue"})
    public void showInfo(View v) {
        pickDateResultLauncher.launch(
                new Intent(MainActivityApp.this, InfoActivity.class)
        );
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
     * Handler for "backup" menu action
     */
    private void doBackup() {
        final Context context = getApplicationContext();
        assert context != null;

        Uri uriBackup = getUriBackup(context);
        if (uriBackup != null) {
            // The backup location is already selected, just use this
            new BackupConfirmationDialogFragment().show(
                    getSupportFragmentManager(), BackupConfirmationDialogFragment.TAG
            );
        } else {
            // There is no backup location stored yet, ask the user to select one
            new BackupSelectLocationDialogFragment().show(
                    getSupportFragmentManager(), BackupSelectLocationDialogFragment.TAG
            );
        }
    }

    /**
     * Handler for backup confirmation dialog
     */
    private void handleBackupConfirmation(int choice) {
        final Context context = getApplicationContext();
        assert context != null;

        Uri uriBackup = getUriBackup(context);

        switch(choice) {
            case BackupConfirmationDialogFragment.CHOICE_OK:
                boolean ok = dbMain.backupToUri(context, uriBackup);
                String text;
                if (ok) {
                    text = getResources().getString(R.string.backup_finished);
                } else {
                    text = getResources().getString(R.string.backup_failed);
                }
                Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
                toast.show();

                // When the backup failed, ask for a new backup location
                if (!ok) {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                    setOptionsResultStorageAccessSelectBackup.launch(intent);
                }
                break;

            case BackupConfirmationDialogFragment.CHOICE_NEW_LOCATION:
                dbMain.setOption("backup_uri", "");
                dbMain.restorePreferences();
                doBackup();
                break;
        }
    }

    /**
     * Handler for backup select location dialog
     */
    private void handleBackupSelectLocation(int choice) {
        final Context context = getApplicationContext();
        assert context != null;

        switch (choice) {
            case BackupSelectLocationDialogFragment.CHOICE_OK:
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                setOptionsResultStorageAccessSelectBackup.launch(intent);
                break;
            case BackupSelectLocationDialogFragment.CHOICE_HELP:
                showHelp();
                break;
        }
    }

    /**
     * Handler for "restore" menu action
     */
    private void doRestore() {
        final Context context = getApplicationContext();
        assert context != null;

        Uri uriBackup = getUriBackup(context);
        if (uriBackup != null) {
            // The backup location is already selected, just use this
            new RestoreConfirmationDialogFragment().show(
                    getSupportFragmentManager(), RestoreConfirmationDialogFragment.TAG
            );
        } else {
            // There is no backup location stored yet, ask the user to select one
            new RestoreSelectLocationDialogFragment().show(
                    getSupportFragmentManager(), RestoreSelectLocationDialogFragment.TAG
            );
        }
    }

    /**
     * Handler for restore confirmation dialog
     */
    private void handleRestoreConfirmation(int choice) {
        final Context context = getApplicationContext();
        assert context != null;

        Uri uriBackup = getUriBackup(context);

        switch(choice) {
            case RestoreConfirmationDialogFragment.CHOICE_OK:
                boolean ok = dbMain.restoreFromUri(context, uriBackup);
                dbMain.loadCalculatedData();
                calendarUpdate();
                String text;
                if (ok) {
                    text = getResources().getString(R.string.restore_finished);
                } else {
                    text = getResources().getString(R.string.restore_failed);
                }
                Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
                toast.show();

                // When the restore failed, ask for a new backup location
                if (!ok) {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                    setOptionsResultStorageAccessSelectRestore.launch(intent);
                }
                break;

            case RestoreConfirmationDialogFragment.CHOICE_NEW_LOCATION:
                dbMain.setOption("backup_uri", "");
                dbMain.restorePreferences();
                doRestore();
                break;
        }
    }

    /**
     * Handler for restore select location dialog
     */
    private void handleRestoreSelectLocation(int choice) {
        final Context context = getApplicationContext();
        assert context != null;

        switch (choice) {
            case RestoreSelectLocationDialogFragment.CHOICE_OK:
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                setOptionsResultStorageAccessSelectRestore.launch(intent);
                break;
            case RestoreSelectLocationDialogFragment.CHOICE_HELP:
                showHelp();
                break;
        }
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

        if (preferences.getBoolean("direct_details", false)) {
            showDetailsActivity(yearCurrent, monthCurrent, day);
        } else {
            // Set or remove entry with confirmation

            final GregorianCalendar date = new GregorianCalendar(yearCurrent, monthCurrent - 1, day);
            int type = dbMain.getEntryType(date);
            if (type != PERIOD_START && type != PERIOD_CONFIRMED) {
                EditCalendarEntryDialogFragment.newInstance(EditCalendarEntryDialogFragment.TYPE_ADD, yearCurrent, monthCurrent, day).show(
                        getSupportFragmentManager(), EditCalendarEntryDialogFragment.TAG
                );
            } else {
                if (type == PERIOD_START) {
                    EditCalendarEntryDialogFragment.newInstance(EditCalendarEntryDialogFragment.TYPE_REMOVE_PERIOD, yearCurrent, monthCurrent, day).show(
                            getSupportFragmentManager(), EditCalendarEntryDialogFragment.TAG
                    );
                } else {
                    EditCalendarEntryDialogFragment.newInstance(EditCalendarEntryDialogFragment.TYPE_REMOVE, yearCurrent, monthCurrent, day).show(
                            getSupportFragmentManager(), EditCalendarEntryDialogFragment.TAG
                    );
                }
            }
        }
    }

    /**
     * Handle calender edit dialog result
     */
    private void handleEditCalendarEntry(int type, int choice, int year, int month, int day) {
        final GregorianCalendar date = new GregorianCalendar(yearCurrent, monthCurrent - 1, day);

        if (choice == EditCalendarEntryDialogFragment.CHOICE_DETAILS) {
            showDetailsActivity(year, month, day);

            return;
        }

        if (choice == EditCalendarEntryDialogFragment.CHOICE_OK) {
            if (type == EditCalendarEntryDialogFragment.TYPE_ADD) {
                dbMain.addPeriod(date);
                databaseChanged();

                return;
            }

            if (type == EditCalendarEntryDialogFragment.TYPE_REMOVE || type == EditCalendarEntryDialogFragment.TYPE_REMOVE_PERIOD) {
                dbMain.removePeriod(date);
                databaseChanged();

                return;
            }
        }
    }

    /**
     * Helper to show the details activity for a specific day
     */
    private void showDetailsActivity(int year, int month, int day) {
        Intent details = new Intent(MainActivityApp.this, DetailsActivity.class);
        details.putExtra("year", year);
        details.putExtra("month", month);
        details.putExtra("day", day);
        detailsResultLauncher.launch(details);
    }

    /**
     * Helper to handle changes in the database
     */
    private void databaseChanged() {
        // Update calculated values
        dbMain.loadCalculatedData();
        calendarUpdate();

        // Notify backup agent about the change and mark DB as clean
        BackupManager bm = new BackupManager(this);
        bm.dataChanged();
    }

    private Uri getUriBackup(Context context) {
        PreferenceUtils preferences = new PreferenceUtils(context);
        String backupUriString = preferences.getString("backup_uri", "");

        if (backupUriString.equals("")) {
            return null;
        }

        return Uri.parse(backupUriString);
    }

    /**
     * Handler for date selection in list views
     */
    protected void handleActivityResultPickDate(Intent data) {
        Bundle extras = data.getExtras();
        if (extras != null) {
            monthCurrent = Integer.parseInt(extras.getString("month")) + 1;
            yearCurrent = Integer.parseInt(extras.getString("year"));
            calendarUpdate();
        }
    }

    /**
     * Handler for activity result - options changed
     */
    protected void handleActivityResultOptions(Intent data) {
        PreferenceUtils preferenceUtils = new PreferenceUtils(this);
        String locale = preferenceUtils.getString("locale", "system");
        if(currentLocale.equals(locale)) {
            databaseChanged();
            calendarUpdate();
        } else {
            currentLocale = locale;
            this.recreate();
        }
    }

    /**
     * Handler for activity result - details closed
     */
    protected void handleActivityResultDetails(Intent data) {
        dbMain.loadCalculatedData();
        calendarUpdate();
    }

    /**
     * Handler for activity result - details closed
     */
    protected void handleActivityResultStorageAccessSelectBackup(Intent data) {
        if (data != null) {
            Uri storageUri = data.getData();
            getContentResolver().takePersistableUriPermission(
                    storageUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION |
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            dbMain.setOption("backup_uri", storageUri.toString());
            dbMain.restorePreferences();
            doBackup();
        }
    }

    /**
     * Handler for activity result - details closed
     */
    protected void handleActivityResultStorageAccessSelectRestore(Intent data) {
        if (data != null) {
            Uri storageUri = data.getData();
            getContentResolver().takePersistableUriPermission(
                    storageUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION |
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            dbMain.setOption("backup_uri", storageUri.toString());
            dbMain.restorePreferences();
            doRestore();
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
        if (!navigationDrawerActive) {
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

