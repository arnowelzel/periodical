/*
 * Periodical database class
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

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Environment;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import static java.lang.String.*;

/**
 * Database of the app
 */
@SuppressLint("DefaultLocale")
class PeriodicalDatabase {
    public final Integer DEFAULT_PERIOD_LENGTH = 4;
    public final Integer DEFAULT_LUTEAL_LENGTH = 14;
    public final Integer DEFAULT_CYCLE_LENGTH = 183;
    public final Integer DEFAULT_START_OF_WEEK = 0;
    public final Boolean DEFAULT_DIRECT_DETAILS = false;
    public final Boolean DEFAULT_SHOW_CYCLE = true;

    /**
     * Helper to create or open database
     */
    private class PeriodicalDataOpenHelper extends SQLiteOpenHelper {
        /**
         * File name for the database
         */
        final static String DATABASE_NAME = "main.db";
        /**
         * Version of the database
         */
        final static int DATABASE_VERSION = 4;

        /**
         * Create a new database for the app
         *
         * @param context Application context
         */
        PeriodicalDataOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        /**
         * Create tables if needed
         *
         * @param db The database
         */
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.beginTransaction();
            db.execSQL("create table data (" +
                    "eventtype integer(3), " +
                    "eventdate varchar(8), " +
                    "eventcvx integer(3), " +
                    "eventtemp real, " +
                    "intensity integer(3)" +
                    ");");
            db.execSQL("create table options (" +
                    "name varchar(100), " +
                    "value varchar(500)" +
                    ");");
            db.execSQL("create table notes (" +
                    "eventdate varchar(8), " +
                    "content text" +
                    ");");
            db.execSQL("create table symptoms (" +
                    "eventdate varchar(8), " +
                    "symptom integer(3)" +
                    ");");
            db.setTransactionSuccessful();
            db.endTransaction();
        }

        /**
         * Execute schema updates if needed
         *
         * @param db         The database
         * @param oldVersion The old version which is being updated
         * @param newVersion The new version to update to
         */
        @SuppressLint("DefaultLocale")
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion < 2 && newVersion >= 2) {
                // Version 2 introduces additional data columns
                db.beginTransaction();
                db.execSQL("alter table data add column eventcvx integer(3)");
                db.execSQL("alter table data add column eventtemp real");
                db.setTransactionSuccessful();
                db.endTransaction();
            }

            if (oldVersion < 3 && newVersion >= 3) {
                // Version 3 introduces options
                db.beginTransaction();
                db.execSQL("create table options (" +
                        "name varchar(100), " +
                        "value varchar(500)" +
                        ");");
                db.setTransactionSuccessful();
                db.endTransaction();
            }

            if (oldVersion < 4 && newVersion >= 4) {
                // Version 4 introduces details and stores periods in a different way
                db.beginTransaction();
                db.execSQL("create table notes (" +
                        "eventdate varchar(8), " +
                        "content text" +
                        ");");
                db.execSQL("create table symptoms (" +
                        "eventdate varchar(8), " +
                        "symptom integer(3)" +
                        ");");

                // We don't need a primary ID column any longer but add intensity as property
                db.execSQL("alter table data add column intensity integer(3)");
                db.execSQL("alter table data rename to data_old;");
                db.execSQL("create table data (" +
                        "eventtype integer(3), " +
                        "eventdate varchar(8), " +
                        "eventcvx integer(3), " +
                        "eventtemp real, " +
                        "intensity integer(3) " +
                        ");");
                db.execSQL("insert into data (eventtype, eventdate) " +
                        "select eventtype, eventdate from data_old;");
                db.execSQL("drop table data_old;");

                // Create records for existing confirmed period entries
                // based on the global period length setting
                PreferenceUtils preferences = new PreferenceUtils(context);
                int periodlength;
                periodlength = preferences.getInt("period_length", DEFAULT_PERIOD_LENGTH);

                String statement;

                // Workaround for a bug introduced in release 0.35 which stored the
                // maximum cycle as "period length", so it is not usable at all :-(
                String option = "maximum_cycle_length";
                SharedPreferences.Editor editor = preferences.edit();
                editor.putInt(option, DEFAULT_CYCLE_LENGTH);
                editor.apply();
                statement = "delete from options where name = ?";
                db.execSQL(statement, new String[]{option});
                statement = "insert into options (name, value) values (?, ?)";
                db.execSQL(statement, new String[]{option, DEFAULT_CYCLE_LENGTH.toString()});

                // Fill database with additional entries for the period days
                statement = "select eventtype, eventdate from data order by eventdate desc";
                Cursor result = db.rawQuery(statement, null);
                while (result.moveToNext()) {
                    int eventtype = result.getInt(0);
                    String dbdate = result.getString(1);
                    assert dbdate != null;
                    int eventyear = Integer.parseInt(dbdate.substring(0, 4), 10);
                    int eventmonth = Integer.parseInt(dbdate.substring(4, 6), 10);
                    int eventday = Integer.parseInt(dbdate.substring(6, 8), 10);
                    GregorianCalendar eventdate = new GregorianCalendar(eventyear,
                            eventmonth - 1, eventday);

                    // Add default intensity for existing period
                    int intensity = 2;
                    statement = format(
                            "update data set intensity=%d where eventdate='%s'",
                            intensity,
                            format(Locale.getDefault(), "%04d%02d%02d",
                                    eventdate.get(GregorianCalendar.YEAR),
                                    eventdate.get(GregorianCalendar.MONTH) + 1,
                                    eventdate.get(GregorianCalendar.DAY_OF_MONTH)));
                    db.execSQL(statement);

                    // Add additional entries for each day of the period
                    if (eventtype == DayEntry.PERIOD_START) {
                        eventtype = DayEntry.PERIOD_CONFIRMED;

                        // Start second day with higher intensity which will be reduced every day
                        intensity = 4;

                        for (int day = 2; day <= periodlength; day++) {
                            eventdate.add(GregorianCalendar.DATE, 1);

                            statement = format(
                                    "insert into data (eventdate, eventtype, intensity) values ('%s', %d, %d)",
                                    format(Locale.getDefault(), "%04d%02d%02d",
                                            eventdate.get(GregorianCalendar.YEAR),
                                            eventdate.get(GregorianCalendar.MONTH) + 1,
                                            eventdate.get(GregorianCalendar.DAY_OF_MONTH)),
                                    eventtype,
                                    intensity);
                            db.execSQL(statement);

                            if (intensity > 1) intensity--;
                        }
                    }
                }
                result.close();

                db.setTransactionSuccessful();
                db.endTransaction();
            }
        }
    }

    /* Reference to database */
    private SQLiteDatabase db;

    /**
     * Local helper to manage calculated calendar entries
     */
    public static class DayEntry {
        final static int EMPTY = 0;
        final static int PERIOD_START = 1;
        final static int PERIOD_CONFIRMED = 2;
        final static int PERIOD_PREDICTED = 3;
        final static int FERTILITY_PREDICTED = 4;
        final static int OVULATION_PREDICTED = 5;
        final static int FERTILITY_FUTURE = 6;
        final static int OVULATION_FUTURE = 7;
        final static int INFERTILE_PREDICTED = 8;
        final static int INFERTILE_FUTURE = 9;
        int type;
        final GregorianCalendarExt date;
        int dayofcycle;
        int intensity;
        String notes;
        List<Integer> symptoms;

        /**
         * Construct a new day entry with parameters
         *
         * @param type       Entry type (DayEntry.EMPTY, DayEntry.PERIOD_START, DayEntry.PERIOD_CONFIRMED, ...)
         * @param date       Entry date
         * @param dayofcycle Day within current cycle (beginning with 1)
         * @param intensity  Intensity of the period (1-4)
         */
        DayEntry(int type, GregorianCalendar date, int dayofcycle, int intensity) {
            this.type = type;
            this.date = new GregorianCalendarExt();
            this.date.setTime(date.getTime());
            this.dayofcycle = dayofcycle;
            this.intensity = intensity;
            this.notes = "";
            this.symptoms = new ArrayList<>();
        }

        /**
         * Construct a new day entry
         */
        DayEntry() {
            this.type = EMPTY;
            this.date = new GregorianCalendarExt();
            this.dayofcycle = 0;
            this.intensity = 1;
            this.notes = "";
            this.symptoms = new ArrayList<>();
        }
    }

    /**
     * Calculated day entries
     */
    final Vector<DayEntry> dayEntries;
    /**
     * Calculated average cycle length
     */
    int cycleAverage;
    /**
     * Calculated longest cycle length
     */
    int cycleLongest;
    /**
     * Calculated shortest cycle length
     */
    int cycleShortest;

    /**
     * Private reference to application context
     */
    private final Context context;

    /**
     * Constructor, will try to create/open a writable database
     *
     * @param context Application context
     */
    PeriodicalDatabase(Context context) {
        this.context = context;
        open();

        dayEntries = new Vector<>();
    }

    /**
     * Open the database
     */
    @SuppressLint("Recycle")
    private void open() {
        PeriodicalDataOpenHelper dataOpenHelper;
        dataOpenHelper = new PeriodicalDataOpenHelper(context);
        db = dataOpenHelper.getWritableDatabase();
        assert db != null;
    }

    /**
     * Close the database
     */
    void close() {
        if (db != null)
            db.close();
    }

    /**
     * Add a period entry for a specific day to the database
     *
     * @param date Date of the entry
     */
    @SuppressLint("DefaultLocale")
    void addPeriod(GregorianCalendar date) {
        String statement;

        GregorianCalendar dateLocal = new GregorianCalendar();
        dateLocal.setTime(date.getTime());
        dateLocal.add(GregorianCalendar.DATE, -1);
        int type = getEntryType(dateLocal);
        if (type == DayEntry.PERIOD_START || type == DayEntry.PERIOD_CONFIRMED) {
            // The day before was a confirmed day of the period, then add the current day
            String datestring = format(Locale.getDefault(), "%04d%02d%02d",
                    date.get(GregorianCalendar.YEAR),
                    date.get(GregorianCalendar.MONTH) + 1,
                    date.get(GregorianCalendar.DAY_OF_MONTH));

            type = DayEntry.PERIOD_CONFIRMED;
            db.beginTransaction();
            statement = format(
                    "delete from data where eventdate = '%s'",
                    datestring);
            db.execSQL(statement);
            statement = format(
                    "insert into data (eventdate, eventtype, intensity) values ('%s', %d, 1)",
                    datestring,
                    type);
            db.execSQL(statement);
            db.setTransactionSuccessful();
            db.endTransaction();
        } else {
            // Probably start a new period
            String dateString = format(Locale.getDefault(), "%04d%02d%02d",
                    date.get(GregorianCalendar.YEAR),
                    date.get(GregorianCalendar.MONTH) + 1,
                    date.get(GregorianCalendar.DAY_OF_MONTH));
            dateLocal.setTime(date.getTime());
            dateLocal.add(GregorianCalendar.DATE, 1);
            type = getEntryType(dateLocal);
            if (type == DayEntry.PERIOD_START) {
                // The next day is already marked as new period then move the period start
                // to this day
                db.beginTransaction();

                // First insert a new start
                statement = format(
                        "delete from data where eventdate = '%s'",
                        dateString);
                db.execSQL(statement);
                statement = format(
                        "insert into data (eventdate, eventtype, intensity) values ('%s', %d, 2)",
                        dateString,
                        type);
                db.execSQL(statement);

                // Update old start to be a confirmed day
                statement = format(
                        "update data set eventtype=%d where eventdate = '%s'",
                        DayEntry.PERIOD_CONFIRMED,
                        format(Locale.getDefault(), "%04d%02d%02d",
                                dateLocal.get(GregorianCalendar.YEAR),
                                dateLocal.get(GregorianCalendar.MONTH) + 1,
                                dateLocal.get(GregorianCalendar.DAY_OF_MONTH)));
                db.execSQL(statement);

                db.setTransactionSuccessful();
                db.endTransaction();
            } else {
                // This day is a regular new period
                int periodLength;

                PreferenceUtils preferences = new PreferenceUtils(context);
                periodLength = preferences.getInt("period_length", DEFAULT_PERIOD_LENGTH);

                type = DayEntry.PERIOD_START;
                dateLocal.setTime(date.getTime());
                int intensity = 2;

                db.beginTransaction();
                for (int day = 0; day < periodLength; day++) {
                    String datestringlocal = format(Locale.getDefault(), "%04d%02d%02d",
                            dateLocal.get(GregorianCalendar.YEAR),
                            dateLocal.get(GregorianCalendar.MONTH) + 1,
                            dateLocal.get(GregorianCalendar.DAY_OF_MONTH));

                    statement = format(
                            "insert into data (eventdate, eventtype, intensity) values ('%s', %d, %d)",
                            datestringlocal,
                            type,
                            intensity);
                    db.execSQL(statement);

                    type = DayEntry.PERIOD_CONFIRMED;

                    // Second day gets a higher intensity, the following ones decrease it every day
                    if (day == 0) intensity = 4;
                    else {
                        if (intensity > 1) intensity--;
                    }
                    dateLocal.add(GregorianCalendar.DATE, 1);
                }
                db.setTransactionSuccessful();
                db.endTransaction();
            }
        }
    }

    /**
     * Remove an entry for a specific day from the database
     *
     * @param date Date of the entry
     */
    void removePeriod(GregorianCalendar date) {
        String statement;

        GregorianCalendar dateLocal = new GregorianCalendar();
        dateLocal.setTime(date.getTime());

        db.beginTransaction();

        // Remove period entry for the selected and all following days
        while (true) {
            int type = getEntryType(dateLocal);
            if (type == DayEntry.PERIOD_START || type == DayEntry.PERIOD_CONFIRMED) {
                statement = format(
                        "delete from data where eventdate = '%s'",
                        format(Locale.getDefault(), "%04d%02d%02d",
                                dateLocal.get(GregorianCalendar.YEAR),
                                dateLocal.get(GregorianCalendar.MONTH) + 1,
                                dateLocal.get(GregorianCalendar.DAY_OF_MONTH)));
                db.execSQL(statement);
                dateLocal.add(GregorianCalendar.DATE, 1);
            } else {
                break;
            }
        }

        db.setTransactionSuccessful();
        db.endTransaction();
    }

    /**
     * Update the calculation based on the entries in the database
     */
    @SuppressWarnings("ConstantConditions")
    @SuppressLint("DefaultLocale")
    void loadCalculatedData() {
        DayEntry entry = null;
        DayEntry entryPrevious = null;
        DayEntry entryPreviousStart = null;
        boolean isFirst = true;
        int count = 0;
        int countlimit = 1;
        this.cycleAverage = 0;
        this.cycleLongest = 28;
        this.cycleShortest = 28;
        int ovulationday = 0;
        Cursor result;
        int periodlength;
        int luteallength;
        int maximumcyclelength;
        int dayofcycle = 1;

        // Get default values from preferences
        PreferenceUtils preferences = new PreferenceUtils(context);
        periodlength = preferences.getInt("period_length", DEFAULT_PERIOD_LENGTH);
        luteallength = preferences.getInt("luteal_length", DEFAULT_LUTEAL_LENGTH);
        maximumcyclelength = preferences.getInt("maximum_cycle_length", DEFAULT_CYCLE_LENGTH);

        // Just a safety measure: limit maximum cycle lengths to the allowed minimum value
        if (maximumcyclelength < 60) maximumcyclelength = 60;

        // Clean up existing data
        dayEntries.removeAllElements();

        // Determine minimum entry count for
        // shortest/longest period calculation
        result = db.rawQuery(format("select count(*) from data where eventtype = %d", DayEntry.PERIOD_START), null);
        if (result.moveToNext()) {
            countlimit = result.getInt(0);
            countlimit -= 13;
            if (countlimit < 1) countlimit = 1;
        }
        result.close();

        // Get all period related entries from the database to fill the calendar
        result = db.rawQuery(
                format("select eventdate, eventtype, intensity from data " +
                                "where " +
                                "eventtype in(%d, %d) order by eventdate",
                        DayEntry.PERIOD_START, DayEntry.PERIOD_CONFIRMED),
                null);
        while (result.moveToNext()) {
            String dbdate = result.getString(0);
            int eventtype = result.getInt(1);
            int intensity = result.getInt(2);
            int eventyear = Integer.parseInt(dbdate.substring(0, 4), 10);
            int eventmonth = Integer.parseInt(dbdate.substring(4, 6), 10);
            int eventday = Integer.parseInt(dbdate.substring(6, 8), 10);
            GregorianCalendar eventdate = new GregorianCalendar(eventyear,
                    eventmonth - 1, eventday);

            switch (eventtype) {
                case DayEntry.PERIOD_START:
                    if (isFirst) {
                        // First event at all - just create an initial start entry
                        dayofcycle = 1;
                        entryPrevious = new DayEntry(eventtype, eventdate, 1, intensity);
                        entryPreviousStart = entryPrevious;
                        this.dayEntries.add(entryPrevious);
                        isFirst = false;
                    } else {
                        // Create new day entry
                        entry = new DayEntry(eventtype, eventdate, 1, intensity);
                        int length = entryPreviousStart.date.diffDayPeriods(entry.date);

                        // Add calculated values from the last date to this day, if the period has not
                        // unusual lengths (e.g. after a longer pause because of pregnancy etc.)
                        if (length <= maximumcyclelength) {
                            count++;

                            // Update values which are used to calculate the fertility
                            // window for the last 12 entries
                            if (count == countlimit) {
                                // If we have at least one period the shortest and
                                // and longest value is automatically the current length
                                this.cycleShortest = length;
                                this.cycleLongest = length;
                            } else if (count > countlimit) {
                                // We have more than two values, then update
                                // longest/shortest
                                // values
                                if (length < this.cycleShortest)
                                    this.cycleShortest = length;
                                if (length > this.cycleLongest)
                                    this.cycleLongest = length;
                            }

                            // Update average sum
                            this.cycleAverage += length;

                            // Calculate a predicted ovulation date
                            int average = this.cycleAverage;
                            if (count > 0) average /= count;
                            ovulationday = length - luteallength;

                            // Calculate days from the last event until now
                            GregorianCalendar datePrevious = new GregorianCalendar();
                            datePrevious.setTime(entryPrevious.date.getTime());
                            for (int day = dayofcycle; day < length; day++) {
                                datePrevious.add(GregorianCalendar.DATE, 1);
                                dayofcycle++;

                                int type;

                                if (day == ovulationday) {
                                    // Day of ovulation
                                    type = DayEntry.OVULATION_PREDICTED;
                                } else if (day >= this.cycleShortest - luteallength - 4
                                        && day <= this.cycleLongest - luteallength + 3) {
                                    // Fertile days
                                    type = DayEntry.FERTILITY_PREDICTED;
                                } else {
                                    // Infertile days
                                    type = DayEntry.INFERTILE_PREDICTED;
                                }

                                DayEntry entryCalculated = new DayEntry(type, datePrevious, dayofcycle, 1);
                                dayEntries.add(entryCalculated);
                            }
                        }

                        // Finally add the entry
                        dayofcycle = 1;
                        entryPrevious = entry;
                        entryPreviousStart = entry;
                        this.dayEntries.add(entry);
                    }
                    break;

                case DayEntry.PERIOD_CONFIRMED:
                    dayofcycle++;
                    entry = new DayEntry(eventtype, eventdate, dayofcycle, intensity);
                    this.dayEntries.add(entry);
                    entryPrevious = entry;
                    break;
            }
        }
        result.close();

        // Calculate global average and prediction if possible
        if (count > 0) {
            this.cycleAverage /= count;

            GregorianCalendar datePredicted = new GregorianCalendar();
            datePredicted.setTime(entry.date.getTime());

            dayofcycle++;
            for (int cycles = 0; cycles < 3; cycles++) {
                for (int day = (cycles == 0 ? dayofcycle : 1); day <= cycleAverage; day++) {
                    datePredicted.add(GregorianCalendar.DATE, 1);

                    int type;

                    if (day <= periodlength) {
                        // Predicted days of period
                        type = DayEntry.PERIOD_PREDICTED;
                    } else if (day == ovulationday) {
                        // Day of ovulation
                        type = DayEntry.OVULATION_FUTURE;
                    } else if (day >= this.cycleShortest - luteallength - 4
                            && day <= this.cycleLongest - luteallength + 3) {
                        // Fertile days
                        type = DayEntry.FERTILITY_FUTURE;
                    } else {
                        // Infertile days
                        type = DayEntry.INFERTILE_FUTURE;
                    }

                    DayEntry entryCalculated = new DayEntry(type, datePredicted, dayofcycle, 1);
                    dayEntries.add(entryCalculated);

                    dayofcycle++;
                }
                dayofcycle = 1;
            }
        }

        // Fill details for each day
        int index = 0;
        DayEntry entryTarget = null;
        result = db.rawQuery("select notes.eventdate, content, symptom from " +
                        "notes " +
                        "left outer join symptoms on notes.eventdate=symptoms.eventdate " +
                        "order by notes.eventdate",
                null);
        while (result.moveToNext()) {
            String dbdate = result.getString(0);
            assert dbdate != null;
            int eventyear = Integer.parseInt(dbdate.substring(0, 4), 10);
            int eventmonth = Integer.parseInt(dbdate.substring(4, 6), 10);
            int eventday = Integer.parseInt(dbdate.substring(6, 8), 10);
            GregorianCalendar eventdate = new GregorianCalendar(eventyear, eventmonth - 1, eventday);
            String notes = result.getString(1);
            if (notes == null) notes = "";
            int symptom = result.getInt(2);

            if (dayEntries.size() == 0) {
                // If we don't have any entries yet, create an empty entry for the details
                entryTarget = new DayEntry(DayEntry.EMPTY, eventdate, 0, 0);
                dayEntries.add(entryTarget);
                index = 0;
            } else {
                // We have at least ony entry, but it may be in the future
                if (dayEntries.size() > index) {
                    entry = dayEntries.get(index);
                    if (entry.date.getTimeInMillis() > eventdate.getTimeInMillis()) {
                        // The existing entry is in the future, so create a new blank entry before
                        // this day to hold the details
                        entryTarget = new DayEntry(DayEntry.EMPTY, eventdate, 0, 0);
                        dayEntries.insertElementAt(entryTarget, index);
                    } else {
                        // Skip existing entries, until a matching one is found
                        do {
                            entry = dayEntries.get(index);
                            index++;
                        } while (entry.date.getTimeInMillis() < eventdate.getTimeInMillis() && index < dayEntries.size());
                        index--;
                        if (entry.date.equals(eventdate)) {
                            entryTarget = entry;
                        } else {
                            // No matching entry found, so add a new one for this day
                            entryTarget = new DayEntry(DayEntry.EMPTY, eventdate, 0, 0);
                            dayEntries.add(entryTarget);
                        }
                    }
                }

                // Add symptom to the current entry
                if (null != entryTarget) {
                    if (symptom != 0) {
                        entryTarget.symptoms.add(symptom);
                    }
                    entryTarget.notes = notes;
                }
            }
        }
        result.close();

        System.gc();
    }


    /**
     * Load data for statistics and overview without calculating anything.
     */
    void loadRawData() {
        DayEntry entry;

        // Clean up existing data
        dayEntries.removeAllElements();

        // Get all entries from the database
        String statement = "select eventtype, eventdate from data where eventtype=" +
                String.format("%d", DayEntry.PERIOD_START) +
                " order by eventdate desc";
        Cursor result = db.rawQuery(statement, null);
        while (result.moveToNext()) {
            String dbdate = result.getString(1);
            assert dbdate != null;
            int eventyear = Integer.parseInt(dbdate.substring(0, 4), 10);
            int eventmonth = Integer.parseInt(dbdate.substring(4, 6), 10);
            int eventday = Integer.parseInt(dbdate.substring(6, 8), 10);
            GregorianCalendar eventdate = new GregorianCalendar(eventyear,
                    eventmonth - 1, eventday);

            // Create new day entry
            entry = new DayEntry(DayEntry.PERIOD_START, eventdate, 1, 0);
            dayEntries.add(entry);
        }
        result.close();

        System.gc();
    }

    /**
     * Load data and details without calculating anything.
     */
    @SuppressLint("DefaultLocale")
    void loadRawDataWithDetails() {
        // Clean up existing data
        dayEntries.removeAllElements();

        // Get all entries with details from the database
        String statement = "select data.eventdate, eventtype, intensity, content, symptom from " +
                "data " +
                "left outer join notes on data.eventdate=notes.eventdate " +
                "left outer join symptoms on data.eventdate=symptoms.eventdate " +
                "order by data.eventdate";
        Cursor result = db.rawQuery(statement, null);
        DayEntry entry = null;
        String dbdate = "";
        List<Integer> symptoms = new ArrayList<>();
        int dayofcycle = 1;

        while (result.moveToNext()) {
            // New day?
            if (!dbdate.equals(result.getString(0))) {
                // Store pending entry if it is not a total empty day
                if (entry != null) {
                    entry.dayofcycle = dayofcycle;
                    entry.symptoms = symptoms;
                    if (entry.type != DayEntry.EMPTY || !entry.notes.isEmpty() || entry.symptoms.size() > 0) {
                        dayEntries.add(entry);
                    }
                }

                dbdate = result.getString(0);
                assert dbdate != null;
                int eventtype = result.getInt(1);
                int eventyear = Integer.parseInt(dbdate.substring(0, 4), 10);
                int eventmonth = Integer.parseInt(dbdate.substring(4, 6), 10);
                int eventday = Integer.parseInt(dbdate.substring(6, 8), 10);
                GregorianCalendar eventdate = new GregorianCalendar(eventyear, eventmonth - 1, eventday);
                int intensity = result.getInt(2);
                String notes = result.getString(3);
                if (notes == null) notes = "";

                entry = new DayEntry();
                entry.type = eventtype;
                entry.date.setTime(eventdate.getTime());
                entry.intensity = intensity > 0 ? intensity : 1;
                entry.notes = notes;

                symptoms = new ArrayList<>();

                if (result.getInt(4) != 0) {
                    symptoms.add(result.getInt(4));
                }

                if (eventtype == DayEntry.PERIOD_START) dayofcycle = 1;
                else dayofcycle++;
            } else {
                symptoms.add(result.getInt(4));
            }
        }
        result.close();

        if (entry != null) {
            entry.symptoms = symptoms;
            entry.dayofcycle = dayofcycle;
            if (entry.type != DayEntry.EMPTY || !entry.notes.isEmpty() || entry.symptoms.size() > 0) {
                dayEntries.add(entry);
            }
        }

        System.gc();
    }

    /**
     * Get entry type for a specific day
     *
     * @param date Date of the entry
     */
    @SuppressWarnings("WrongConstant")
    int getEntryType(GregorianCalendar date) {
        for (DayEntry entry : dayEntries) {
            // If entry was found, then return type
            if (entry.date.equals(date)) {
                return entry.type;
            }
        }

        // Fall back if month was not found, then return "empty" as type
        return 0;
    }

    /**
     * Get entry for a specific day
     *
     * @param year  Year including century
     * @param month Month (1-12)
     * @param day   Day of the month (1-31)
     */
    @SuppressWarnings("WrongConstant")
    private DayEntry getEntry(int year, int month, int day) {
        for (DayEntry entry : dayEntries) {
            // If entry was found, then return entry
            if (entry.date.get(GregorianCalendar.YEAR) == year
                    && entry.date.get(GregorianCalendar.MONTH) == month - 1
                    && entry.date.get(GregorianCalendar.DATE) == day) {
                return entry;
            }
        }

        // No entry was found
        return null;
    }

    /**
     * Get entry for a specific day
     *
     * @param date Date of the entry
     */
    DayEntry getEntry(GregorianCalendar date) {
        for (DayEntry entry : dayEntries) {
            // If entry was found, then return entry
            if (entry.date.equals(date)) {
                return entry;
            }
        }

        // No entry was found
        return null;
    }

    /**
     * Get a specific day including all details
     *
     * @param year  Year including century
     * @param month Month (1-12)
     * @param day   Day of the month (1-31)
     */
    DayEntry getEntryWithDetails(int year, int month, int day) {
        DayEntry entry = getEntry(year, month, day);

        if (entry == null) {
            entry = new DayEntry();

            // Set chosen date
            GregorianCalendar date = new GregorianCalendar(year, month - 1, day);
            entry.date.setTime(date.getTime());
        }

        String statementNotes = format(
                "select content from notes where eventdate = '%04d%02d%02d'",
                year, month, day);
        Cursor resultNotes = db.rawQuery(statementNotes, null);

        if (resultNotes.moveToNext()) {
            entry.notes = resultNotes.getString(0);
        }
        resultNotes.close();

        String statementSymptoms = format(
                "select symptom from symptoms where eventdate = '%04d%02d%02d'",
                year, month, day);
        Cursor resultSymptoms = db.rawQuery(statementSymptoms, null);

        List<Integer> symptoms = new ArrayList<>();
        while (resultSymptoms.moveToNext()) {
            symptoms.add(resultSymptoms.getInt(0));
        }
        entry.symptoms = symptoms;

        resultSymptoms.close();

        return entry;
    }

    /**
     * Store details for a specific day
     *
     * @param entry The details to be stored
     */
    @SuppressLint("DefaultLocale")
    void addEntryDetails(DayEntry entry) {
        String statement;
        String dateString = format(Locale.getDefault(), "%04d%02d%02d",
                entry.date.get(GregorianCalendar.YEAR),
                entry.date.get(GregorianCalendar.MONTH) + 1,
                entry.date.get(GregorianCalendar.DAY_OF_MONTH));

        db.beginTransaction();

        // Delete existing details, if any
        statement = format(
                "delete from notes where eventdate = '%s'",
                dateString);
        db.execSQL(statement);

        statement = format(
                "delete from symptoms where eventdate = '%s'",
                dateString);
        db.execSQL(statement);

        // If there is no calendar entry for this day yet, then add one first
        boolean addNew = false;
        statement = format(
                "select eventtype from data where eventdate='%s'",
                dateString);
        Cursor result = db.rawQuery(statement, null);
        if (!result.moveToNext()) addNew = true;
        result.close();
        if (addNew) {
            statement = format(
                    "insert into data (eventdate, eventtype) values ('%s', %d)",
                    dateString,
                    DayEntry.EMPTY);
            db.execSQL(statement);
        }

        // Store new details
        statement = format(
                "update data set intensity = %d where eventdate='%s'",
                entry.intensity,
                dateString);
        db.execSQL(statement);

        statement = format(
                "insert into notes (eventdate, content) values ('%s', ?)",
                dateString);
        db.execSQL(statement, new String[]{entry.notes});

        int count = 0;
        while (count < entry.symptoms.size()) {
            statement = format(
                    "insert into symptoms (eventdate, symptom) values ('%s', %d)",
                    dateString,
                    entry.symptoms.get(count));
            db.execSQL(statement);
            count++;
        }

        db.setTransactionSuccessful();
        db.endTransaction();
    }

    /**
     * Get a named option from the options table
     *
     * @param name         Name of the option to retrieve
     * @param defaultvalue Default value to be used if the option is not stored yet
     */
    private String getOption(String name, String defaultvalue) {
        String value = defaultvalue;

        String statement = "select value from options where name = ?";
        Cursor result = db.rawQuery(statement, new String[]{name});
        if (result.moveToNext()) {
            value = result.getString(0);
        }
        result.close();

        return value;
    }

    private int getOption(String name, int defaultvalue) {
        int value = defaultvalue;

        String statement = "select value from options where name = ?";
        Cursor result = db.rawQuery(statement, new String[]{name});
        if (result.moveToNext()) {
            value = result.getInt(0);
        }
        result.close();

        return value;
    }

    private boolean getOption(String name, boolean defaultvalue) {
        boolean value = defaultvalue;

        String statement = "select value from options where name = ?";
        Cursor result = db.rawQuery(statement, new String[]{name});
        if (result.moveToNext()) {
            value = result.getString(0).equals("1");
        }
        result.close();

        return value;
    }

    /**
     * Set a named option to be stored in the options table
     *
     * @param name  Name of the option to store
     * @param value Value of the option to store
     */
    public void setOption(String name, String value) {
        String statement;

        db.beginTransaction();

        // Delete existing value
        statement = "delete from options where name = ?";
        db.execSQL(statement, new String[]{name});

        // Save option
        statement = "insert into options (name, value) values (?, ?)";
        db.execSQL(statement, new String[]{name, value});

        db.setTransactionSuccessful();
        db.endTransaction();
    }

    public void setOption(String name, Integer value) {
        String statement;
        String valueStr;

        db.beginTransaction();

        // Delete existing value
        statement = "delete from options where name = ?";
        db.execSQL(statement, new String[]{name});

        // Save option
        valueStr = value.toString();
        statement = "insert into options (name, value) values (?, ?)";
        db.execSQL(statement, new String[]{name, valueStr});

        db.setTransactionSuccessful();
        db.endTransaction();
    }

    public void setOption(String name, boolean value) {
        String statement;

        db.beginTransaction();

        // Delete existing value
        statement = "delete from options where name = ?";
        db.execSQL(statement, new String[]{name});

        // Save option
        statement = "insert into options (name, value) values (?, ?)";
        db.execSQL(statement, new String[]{name, value ? "1" : "0"});

        db.setTransactionSuccessful();
        db.endTransaction();
    }

    /**
     * Backup database to a given URI
     */
    boolean backupToUri(Context context, Uri uri) {
        boolean result = false;

        // Close the database
        db.close();

        // Check if uri is accessible
        DocumentFile directory = DocumentFile.fromTreeUri(context, uri);
        if (!directory.isDirectory()) {
            return false;
        }

        // First we need to create a directory where the backup will be stored
        String destinationDirectoryName= context.getPackageName();
        DocumentFile destinationDirectory = directory.findFile(destinationDirectoryName);
        if (null == destinationDirectory || !destinationDirectory.isDirectory()) {
            destinationDirectory = directory.createDirectory(destinationDirectoryName);
        }

        // Backup database file
        File sourceFile = new File(db.getPath());
        String destinationFileName = sourceFile.getName();
        DocumentFile destinationFile = destinationDirectory.findFile(destinationFileName);
        if (null != destinationFile) {
            destinationFile.delete();
        }
        destinationFile = destinationDirectory.createFile("application/octet-stream", destinationFileName);
        try {
            OutputStream destinationStream = context.getContentResolver().openOutputStream(destinationFile.getUri());
            FileInputStream sourceStream = new FileInputStream(sourceFile);
            int byteRead = 0;
            byte[] buffer = new byte[8192];
            while ((byteRead = sourceStream.read(buffer, 0, 8192)) != -1) {
                destinationStream.write(buffer, 0, byteRead);
            }
            sourceStream.close();
            destinationStream.close();
            result = true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Backup journal if required and the previous backup was successful
        sourceFile = new File(db.getPath() + "-journal");
        if (result && sourceFile.   exists()) {
            result = false;
            destinationFileName = sourceFile.getName();
            destinationFile = destinationDirectory.findFile(destinationFileName);
            if (null != destinationFile) {
                destinationFile.delete();
            }
            destinationFile = destinationDirectory.createFile("application/octet-stream", destinationFileName);
            try {
                OutputStream outputStream = context.getContentResolver().openOutputStream(destinationFile.getUri());
                FileInputStream sourceStream = new FileInputStream(sourceFile);
                int byteRead = 0;
                byte[] buffer = new byte[8192];
                while ((byteRead = sourceStream.read(buffer, 0, 8192)) != -1) {
                    outputStream.write(buffer, 0, byteRead);
                }
                sourceStream.close();
                outputStream.close();
                result = true;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Open the database again
        open();

        return result;
    }

    /**
     * Restore database from a given URI
     */
    boolean restoreFromUri(Context context, Uri uri) {
        boolean result = false;

        // Close the database
        db.close();

        // Check if uri exists
        DocumentFile directory = DocumentFile.fromTreeUri(context, uri);
        if (!directory.isDirectory()) {
            return false;
        }

        // Check if subfolder with backup exists
        String sourceDirectoryName= context.getPackageName();
        DocumentFile sourceDirectory = directory.findFile(sourceDirectoryName);
        if (null == sourceDirectory || !sourceDirectory.isDirectory()) {
            return false;
        }

        // Restore database file
        File destinationFile = new File(db.getPath());
        String destinationFileName = destinationFile.getName();
        DocumentFile sourceFile = sourceDirectory.findFile(destinationFileName);
        if (null == sourceFile) {
            return false;
        }
        try {
            InputStream sourceStream = context.getContentResolver().openInputStream(sourceFile.getUri());
            FileOutputStream destinationStream = new FileOutputStream(destinationFile);
            int byteRead = 0;
            byte[] buffer = new byte[8192];
            while ((byteRead = sourceStream.read(buffer, 0, 8192)) != -1) {
                destinationStream.write(buffer, 0, byteRead);
            }
            sourceStream.close();
            destinationStream.close();
            result = true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Restore journal if required
        destinationFile = new File(db.getPath() + "-journal");
        destinationFileName = destinationFile.getName();
        sourceFile = sourceDirectory.findFile(destinationFileName);
        if (null != sourceFile) {
            result = false;
            try {
                InputStream sourceStream = context.getContentResolver().openInputStream(sourceFile.getUri());
                FileOutputStream destinationStream = new FileOutputStream(destinationFile);
                int byteRead = 0;
                byte[] buffer = new byte[8192];
                while ((byteRead = sourceStream.read(buffer, 0, 8192)) != -1) {
                    destinationStream.write(buffer, 0, byteRead);
                }
                sourceStream.close();
                destinationStream.close();
                result = true;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Open the database again
        open();

        return result;
    }

    /**
     * Restore application preferences from the database
     *
     * <br><br><i>(Just a hack for now - in the future we might want to get rid of shared preferences)</i>
     */
    void restorePreferences() {
        Integer period_length = getOption("period_length", DEFAULT_PERIOD_LENGTH);
        Integer luteal_length = getOption("luteal_length", DEFAULT_LUTEAL_LENGTH);
        Integer startofweek = getOption("startofweek", DEFAULT_START_OF_WEEK);
        if (startofweek != DEFAULT_START_OF_WEEK && startofweek != 1)
            startofweek = DEFAULT_START_OF_WEEK;
        Integer maximum_cycle_length = getOption("maximum_cycle_length", DEFAULT_CYCLE_LENGTH);
        boolean direct_details = getOption("direct_details", DEFAULT_DIRECT_DETAILS);
        boolean show_cycle = getOption("show_cycle", DEFAULT_SHOW_CYCLE);
        String backup_uri = getOption( "backup_uri", null);

        PreferenceUtils preferences = new PreferenceUtils(context);
        SharedPreferences.Editor editor = preferences.edit();

        // Make sure, there are no existing values which may cause problems
        editor.remove("period_length");
        editor.remove("luteal_length");
        editor.remove("startofweek");
        editor.remove("maximum_cycle_length");
        editor.remove("direct_details");
        editor.remove("show_cycle");
        editor.remove("backup_uri");

        // Store values
        editor.putString("period_length", period_length.toString());
        editor.putString("luteal_length", luteal_length.toString());
        editor.putString("startofweek", startofweek.toString());
        editor.putString("maximum_cycle_length", maximum_cycle_length.toString());
        editor.putBoolean("direct_details", direct_details);
        editor.putBoolean("show_cycle", show_cycle);
        editor.putString("backup_uri", backup_uri);

        editor.apply();
    }
}
