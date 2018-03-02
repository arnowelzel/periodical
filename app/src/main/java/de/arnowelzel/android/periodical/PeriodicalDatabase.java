/*
 * Periodical database class
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
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.preference.PreferenceManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

/**
 * Database of the app
 */
class PeriodicalDatabase {

    /**
     * Helper to create or open database
     */
    private class PeriodicalDataOpenHelper extends SQLiteOpenHelper {
        /** File name for the database */
        final static String DATABASE_NAME = "main.db";
        /** Version of the database */
        final static int DATABASE_VERSION = 4;

        /**
         * Create a new database for the app
         *
         * @param context
         * Application context
         */
        PeriodicalDataOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        /**
         * Create tables if needed
         *
         * @param db
         * The database
         */
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.beginTransaction();
            db.execSQL("create table data (" +
                    "eventtype integer(3), " +
                    "eventdate varchar(8), " +
                    "eventcvx integer(3), " +
                    "eventtemp real " +
                    ");");
            db.execSQL("create table options (" +
                    "name varchar(100), " +
                    "value varchar(500)" +
                    ");");
            db.execSQL("create table notes (" +
                    "eventdate varchar(8), " +
                    "intensity integer(3), " +
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
         * @param db
         * The database
         *
         * @param oldVersion
         * The old version which is being updated
         *
         * @param newVersion
         * The new version to update to
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
            } else if (oldVersion < 3 && newVersion >= 3) {
                // Version 3 introduces options
                db.beginTransaction();
                db.execSQL("create table options (" +
                        "name varchar(100), " +
                        "value varchar(500)" +
                        ");");
                db.setTransactionSuccessful();
                db.endTransaction();
            } else if (oldVersion < 4 && newVersion >= 4) {
                // Version 4 introduces details and stores periods in a different way
                db.execSQL("create table notes (" +
                        "eventdate varchar(8), " +
                        "intensity integer(3), " +
                        "content text" +
                        ");");
                db.execSQL("create table symptoms (" +
                        "eventdate varchar(8), " +
                        "symptom integer(3)" +
                        ");");

                // We don't need a primary ID column
                db.execSQL("alter table data rename to data_old;");
                db.execSQL("create table data (" +
                        "eventtype integer(3), " +
                        "eventdate varchar(8), " +
                        "eventcvx integer(3), " +
                        "eventtemp real " +
                        ");");
                db.execSQL("insert into data (eventtype, eventdate) " +
                        "select eventtype, eventdate from data_old;");
                db.execSQL("drop table data_old;");

                // Create records for existing confirmed period entries
                // based on the global period length setting
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                int periodlength;
                try {
                    periodlength = Integer.parseInt(preferences.getString("period_length", "4"));
                } catch (NumberFormatException e) {
                    periodlength = 4;
                }

                String statement = "select eventtype, eventdate from data order by eventdate desc";
                Cursor result = db.rawQuery(statement, null);
                DayEntry entry;
                while (result.moveToNext()) {
                    int eventtype = result.getInt(0);
                    String dbdate = result.getString(1);
                    assert dbdate != null;
                    int eventyear = Integer.parseInt(dbdate.substring(0, 4), 10);
                    int eventmonth = Integer.parseInt(dbdate.substring(4, 6), 10);
                    int eventday = Integer.parseInt(dbdate.substring(6, 8), 10);
                    GregorianCalendar eventdate = new GregorianCalendar(eventyear,
                            eventmonth - 1, eventday);

                    // Add additional entries for each day of the period
                    if(eventtype == DayEntry.PERIOD_START) {
                        eventtype = DayEntry.PERIOD_CONFIRMED;

                        for(int day = 2; day <= periodlength; day++) {
                            eventdate.add(GregorianCalendar.DATE, 1);

                            statement = String.format(
                                    "insert into data (eventtype, eventdate) values (%d, '%s')",
                                    eventtype,
                                    String.format(Locale.getDefault(), "%04d%02d%02d",
                                            eventdate.get(GregorianCalendar.YEAR),
                                            eventdate.get(GregorianCalendar.MONTH) + 1,
                                            eventdate.get(GregorianCalendar.DAY_OF_MONTH)));
                            db.beginTransaction();
                            db.execSQL(statement);
                            db.setTransactionSuccessful();
                            db.endTransaction();
                        }
                    }
                }
                result.close();
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
        final int dayofcycle;
        int intensity;
        String notes;
        List<Integer> symptoms;

        /**
         * Construct a new day entry
         *
         * @param type
         * Entry type (DayEntry.EMPTY, DayEntry.PERIOD_START, DayEntry.PERIOD_CONFIRMED, ...)
         *
         * @param date
         * Entry date
         *
         * @param dayofcycle
         * Day within current cycle (beginning with 1)
         */
        DayEntry(int type, GregorianCalendar date, int dayofcycle) {
            this.type = type;
            this.date = new GregorianCalendarExt();
            this.date.setTime(date.getTime());
            this.dayofcycle = dayofcycle;
            this.intensity = 0;
            this.notes = "";
            this.symptoms = new ArrayList<Integer>();
        }

        /**
         * Construct a new day entry
         */
        DayEntry() {
            this.type = EMPTY;
            this.date = new GregorianCalendarExt();
            this.dayofcycle = 0;
            this.intensity = 0;
            this.notes = "";
            this.symptoms = new ArrayList<Integer>();
        }
    }

    /** Calculated day entries */
    final Vector<DayEntry> dayEntries;
    /** Calculated average cycle length */
    int cycleAverage;
    /** Calculated longest cycle length */
    int cycleLongest;
    /** Calculated shortest cycle length */
    int cycleShortest;

    /** Private reference to application context */
    private Context context;

    /**
     * Constructor, will try to create/open a writable database
     *
     * @param context
     * Application context
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
     * Store an entry for a specific day into the database
     *
     * @param date
     * Date of the entry
     */
    @SuppressLint("DefaultLocale")
    void addPeriod(GregorianCalendar date) {
        String statement;

        GregorianCalendar dateLocal = new GregorianCalendar();
        dateLocal.setTime(date.getTime());
        dateLocal.add(GregorianCalendar.DATE, -1);
        int type = getEntryType(dateLocal);
        if(type == DayEntry.PERIOD_START || type == DayEntry.PERIOD_CONFIRMED) {
            // Continue existing period to the future
            type = DayEntry.PERIOD_CONFIRMED;
            db.beginTransaction();
            statement = String.format(
                    "insert into data (eventtype, eventdate) values (%d, '%s')",
                    type,
                    String.format(Locale.getDefault(), "%04d%02d%02d",
                            date.get(GregorianCalendar.YEAR),
                            date.get(GregorianCalendar.MONTH) +1,
                            date.get(GregorianCalendar.DAY_OF_MONTH)));
            db.execSQL(statement);
            db.setTransactionSuccessful();
            db.endTransaction();
        } else {
            dateLocal.add(GregorianCalendar.DATE, +2);
            type = getEntryType(dateLocal);
            if(type == DayEntry.PERIOD_START) {
                // Continue existing period to the past
                db.beginTransaction();

                statement = String.format(
                        "insert into data (eventtype, eventdate) values (%d, '%s')",
                        type,
                        String.format(Locale.getDefault(), "%04d%02d%02d",
                                date.get(GregorianCalendar.YEAR),
                                date.get(GregorianCalendar.MONTH) +1,
                                date.get(GregorianCalendar.DAY_OF_MONTH)));
                db.execSQL(statement);

                statement = String.format(
                        "update data set eventtype=%d where eventdate = '%s'",
                        DayEntry.PERIOD_CONFIRMED,
                        String.format(Locale.getDefault(), "%04d%02d%02d",
                                dateLocal.get(GregorianCalendar.YEAR),
                                dateLocal.get(GregorianCalendar.MONTH) +1,
                                dateLocal.get(GregorianCalendar.DAY_OF_MONTH)));
                db.execSQL(statement);

                db.setTransactionSuccessful();
                db.endTransaction();
            } else {
                // This day is a regular new period
                int periodlength;

                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                try {
                    periodlength = Integer.parseInt(preferences.getString("period_length", "4"));
                } catch (NumberFormatException e) {
                    periodlength = 4;
                }

                type = DayEntry.PERIOD_START;
                dateLocal.setTime(date.getTime());

                db.beginTransaction();
                for(int day = 0; day < periodlength; day++) {
                    statement = String.format(
                            "insert into data (eventtype, eventdate) values (%d, '%s')",
                            type,
                            String.format(Locale.getDefault(), "%04d%02d%02d",
                                    dateLocal.get(GregorianCalendar.YEAR),
                                    dateLocal.get(GregorianCalendar.MONTH) +1,
                                    dateLocal.get(GregorianCalendar.DAY_OF_MONTH)));
                    db.execSQL(statement);

                    type = DayEntry.PERIOD_CONFIRMED;
                    dateLocal.add(GregorianCalendar.DATE, 1);
                }
                db.setTransactionSuccessful();
                db.endTransaction();
            }
        }
        date.add(GregorianCalendar.DATE, 1);

        // Add the selected date
    }

    /**
     * Remove an entry for a specific day into the database
     *
     * @param date
     * Date of the entry
     */
    void removePeriod(GregorianCalendar date) {
        String statement;

        GregorianCalendar dateLocal = new GregorianCalendar();
        dateLocal.setTime(date.getTime());

        db.beginTransaction();

        // Remove selected day and all following entries
        while(true) {
            int type = getEntryType(dateLocal);
            if(type == DayEntry.PERIOD_START || type == DayEntry.PERIOD_CONFIRMED) {
                statement = String.format("delete from data where eventdate='%s'",
                        String.format(Locale.getDefault(), "%04d%02d%02d",
                                dateLocal.get(GregorianCalendar.YEAR),
                                dateLocal.get(GregorianCalendar.MONTH) +1,
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
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            periodlength = Integer.parseInt(preferences.getString("period_length", "4"));
        } catch (NumberFormatException e) {
            periodlength = 4;
        }
        try {
            luteallength = Integer.parseInt(preferences.getString("luteal_length", "14"));
        } catch (NumberFormatException e) {
            luteallength = 14;
        }
        try {
            maximumcyclelength = Integer.parseInt(preferences.getString("maximum_cycle_length", "183"));
        } catch (NumberFormatException e) {
            maximumcyclelength = 183;
        }

        // Clean up existing data
        dayEntries.removeAllElements();

        // Determine minimum entry count for
        // shortest/longest period calculation
        result = db.rawQuery(String.format("select count(*) from data where eventtype = %d", DayEntry.PERIOD_START), null);
        if (result.moveToNext()) {
            countlimit = result.getInt(0);
            countlimit -= 13;
            if (countlimit < 1) countlimit = 1;
        }
        result.close();

        // Get all entries from the database
        result = db.rawQuery(
                "select eventtype, eventdate from data order by eventdate",
                null);
        while (result.moveToNext()) {
            int eventtype = result.getInt(0);
            String dbdate = result.getString(1);
            int eventyear = Integer.parseInt(dbdate.substring(0, 4), 10);
            int eventmonth = Integer.parseInt(dbdate.substring(4, 6), 10);
            int eventday = Integer.parseInt(dbdate.substring(6, 8), 10);
            GregorianCalendar eventdate = new GregorianCalendar(eventyear,
                    eventmonth - 1, eventday);

            switch(eventtype)
            {
            case DayEntry.PERIOD_START:
                if (isFirst) {
                    // First event at all - just create an initial start entry
                    dayofcycle = 1;
                    entryPrevious = new DayEntry(eventtype, eventdate, 1);
                    entryPreviousStart = entryPrevious;
                    this.dayEntries.add(entryPrevious);
                    isFirst = false;
                }
                else {
                    // Create new day entry
                    entry = new DayEntry(eventtype, eventdate, 1);
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
                        ovulationday = average - luteallength;

                        // Calculate days from the last event until now
                        GregorianCalendar datePrevious = new GregorianCalendar();
                        datePrevious.setTime(entryPrevious.date.getTime());
                        for (int day = dayofcycle; day < length; day++) {
                            datePrevious.add(GregorianCalendar.DATE, 1);

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

                            DayEntry entryCalculated = new DayEntry(type, datePrevious, dayofcycle);
                            dayEntries.add(entryCalculated);
                            dayofcycle++;
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
                entry = new DayEntry(eventtype, eventdate, dayofcycle);
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

                    DayEntry entryCalculated = new DayEntry(type, datePredicted, dayofcycle);
                    dayEntries.add(entryCalculated);

                    dayofcycle++;
                }
                dayofcycle = 1;
            }
        }

        System.gc();
    }

    /**
     * Load data without calculating anything.
     */
    @SuppressLint("DefaultLocale")
    void loadRawDataWithDetails() {
        DayEntry entry;

        // Clean up existing data
        dayEntries.removeAllElements();

        // Get all entries from the database
        /*
        Complete data:

        select data.eventdate, eventtype, intensity, content, symptom
        from
        data
        left outer join notes on data.eventdate=notes.eventdate
        left outer join symptoms on data.eventdate=symptoms.eventdate
        order by data.eventdate desc
        */


        String statement = String.format(
                "select eventtype, eventdate from data where eventtype=%d order by eventdate desc",
                DayEntry.PERIOD_START);
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

            // Create new day entry
            entry = new DayEntry(eventtype, eventdate, 0);
            dayEntries.add(entry);
        }
        result.close();

        System.gc();
    }

    /**
     * Load extended data for list activity
     */


    /**
     * Get entry type for a specific day
     *
     * @param date
     * Date of the entry
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
     * @param year
     * Year including century
     *
     * @param month
     * Month (1-12)
     *
     * @param day
     * Day of the month (1-31)
     */
    @SuppressWarnings("WrongConstant")
    DayEntry getEntry(int year, int month, int day) {
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
     * @param date
     * Date of the entry
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
     * @param year
     * Year including century
     *
     * @param month
     * Month (1-12)
     *
     * @param day
     * Day of the month (1-31)
     */
    DayEntry getEntryEntryWithDetails(int year, int month, int day) {
        DayEntry entry = getEntry(year, month, day);

        if(entry == null) {
            entry = new DayEntry();
        }

        String statementNotes = String.format(
                "select intensity, content from notes where eventdate = '%04d%02d%02d'",
                year, month, day);
        Cursor resultNotes = db.rawQuery(statementNotes, null);

        String statementSymptoms = String.format(
                "select symptom from symptoms where eventdate = '%04d%02d%02d'",
                year, month, day);
        Cursor resultSymptoms = db.rawQuery(statementSymptoms, null);

        if (resultNotes.moveToNext()) {
            GregorianCalendar date = new GregorianCalendar(year, month - 1, day);
            List<Integer> symptoms = new ArrayList<Integer>();

            while(resultSymptoms.moveToNext()) {
                symptoms.add(resultSymptoms.getInt(0) + 1);
            }

            entry.intensity = resultNotes.getInt(0);
            entry.notes = resultNotes.getString(1);
            entry.symptoms = symptoms;
        }

        resultNotes.close();
        resultSymptoms.close();

        return entry;
    }

    /**
     * Store details for a specific day
     *
     * @param entry
     * The details to be stored
     */
    @SuppressLint("DefaultLocale")
    void addEntryDetails(DayEntry entry) {
        String statement;

        db.beginTransaction();


        statement = String.format(
                "delete from notes where eventdate = '%04d%02d%02d'",
                entry.date.get(GregorianCalendar.YEAR),
                entry.date.get(GregorianCalendar.MONTH) +1,
                entry.date.get(GregorianCalendar.DAY_OF_MONTH));
        db.execSQL(statement);

        statement = String.format(
                "delete from symptoms where eventdate = '%04d%02d%02d'",
                entry.date.get(GregorianCalendar.YEAR),
                entry.date.get(GregorianCalendar.MONTH) +1,
                entry.date.get(GregorianCalendar.DAY_OF_MONTH));
        db.execSQL(statement);

        statement = String.format(
                "insert into notes (eventdate, intensity, content) values ('%04d%02d%02d', %d, ?)",
                entry.date.get(GregorianCalendar.YEAR),
                entry.date.get(GregorianCalendar.MONTH) +1,
                entry.date.get(GregorianCalendar.DAY_OF_MONTH),
                entry.intensity);
        db.execSQL(statement, new String[]{ entry.notes });

        int count=0;
        while (count < entry.symptoms.size()) {
            statement = String.format(
                    "insert into symptoms (eventdate, symptom) values ('%04d%02d%02d', %d)",
                    entry.date.get(GregorianCalendar.YEAR),
                    entry.date.get(GregorianCalendar.MONTH) +1,
                    entry.date.get(GregorianCalendar.DAY_OF_MONTH),
                    entry.symptoms.get(count) - 1);
            db.execSQL(statement);
            count++;
        }

        db.setTransactionSuccessful();
        db.endTransaction();
    }

    /**
     * Delete details for a specific day
     *
     * @param year
     * Year including century
     *
     * @param month
     * Month (1-12)
     *
     * @param day
     * Day of the month (1-31)
     */
    void removeEntryDetails(int year, int month, int day) {
        String statement;

        db.beginTransaction();

        statement = String.format(
                "delete from notes where eventdate = '%04d%02d%02d'",
                year, month, day);
        db.execSQL(statement);

        statement = String.format(
                "delete from symptoms where eventdate = '%04d%02d%02d'",
                year, month, day);
        db.execSQL(statement);

        db.setTransactionSuccessful();
        db.endTransaction();
    }

    /**
     * Get a named option from the options table
     *
     * @param name
     * Name of the option to retrieve
     *
     * @param defaultvalue
     * Default value to be used if the option is not stored yet
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
     * @param name
     * Name of the option to store
     *
     * @param value
     * Value of the option to store
     */
    private void setOption(String name, String value) {
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

    private void setOption(String name, boolean value) {
        String statement;

        db.beginTransaction();

        // Delete existing value
        statement = "delete from options where name = ?";
        db.execSQL(statement, new String[]{name});

        // Save option
        statement = "insert into options (name, value) values (?, ?)";
        db.execSQL(statement, new String[]{name, value?"1":"0"});

        db.setTransactionSuccessful();
        db.endTransaction();
    }
    /**
     * Backup the database
     */
    boolean backup() {
        return backupRestore(true);
    }

    /**
     * Restore the database
     */
    boolean restore() {
        return backupRestore(false);
    }

    /**
     * Helper to handle backup and restore operations
     *
     * @param backup
     * true for doing a backup, false for a restore
     */

    private boolean backupRestore(boolean backup) {
        boolean ok = true;

        // Check if SD card is mounted
        if (Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_UNMOUNTED))
            return false;

        File sourceFile;
        File sourceFileJournal;
        File destDir;
        File destFile;
        File destFileJournal;

        // Get source of DB and path for external storage
        if (backup) {
            sourceFile = new File(db.getPath());
            sourceFileJournal = new File(db.getPath()+"-journal");
            destDir = new File(Environment.getExternalStorageDirectory()
                    .getAbsolutePath() + "/" + context.getPackageName());
            destFile = new File(destDir.getAbsolutePath() + "/"
                    + sourceFile.getName());
            destFileJournal = new File(destFile.getPath() + "-journal");
        } else {
            destDir = null; // If we restore, the directory is already there
            destFile = new File(db.getPath());
            destFileJournal = new File(db.getPath()+"-journal");
            sourceFile = new File(Environment.getExternalStorageDirectory()
                    .getAbsolutePath()
                    + "/"
                    + context.getPackageName()
                    + "/"
                    + destFile.getName());
            sourceFileJournal = new File(sourceFile.getPath() + "-journal");
        }

        // Before we can copy anything, close the DB
        db.close();

        // Check, if destination files exists and delete first
        if(destFile.exists()) ok = destFile.delete();
        if(destFileJournal.exists() && ok) ok = destFileJournal.delete();

        // If everything is ok, then copy source to destination
        if (ok) {
            if (backup) {
                //noinspection ResultOfMethodCallIgnored
                destDir.mkdirs();
            }

            try {
                FileUtils.copyFile(new FileInputStream(sourceFile),
                        new FileOutputStream(destFile));
                // Try to copy journal only if it exists
                if(sourceFileJournal.exists())
                    FileUtils.copyFile(new FileInputStream(sourceFileJournal),
                            new FileOutputStream(destFileJournal));
            } catch (IOException e) {
                ok = false;
                e.printStackTrace();
            }
        }

        // Open the DB again
        open();

        return ok;
    }

    /**
     * Save application preferences to the database
     *
     * <br><br><i>(Just a hack for now - in the future we might want to get rid of shared preferences)</i>
     */
    void savePreferences() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        setOption("period_length", preferences.getString("period_length", "4"));
        setOption("startofweek", preferences.getString("startofweek", "0"));
        setOption("maximum_cycle_length", preferences.getString("period_length", "183"));
        setOption("direct_details", preferences.getBoolean("pref_direct_details", false));
        setOption("show_cycle", preferences.getBoolean("show_cycle", true));
    }

    /**
     * Restore application preferences from the database
     *
     * <br><br><i>(Just a hack for now - in the future we might want to get rid of shared preferences)</i>
     */
    void restorePreferences() {
        String period_length = getOption("period_length", "4");
        String startofweek = getOption("startofweek", "0");
        String maximum_cycle_length = getOption("maximum_cycle_length", "183");
        boolean direct_details = getOption("direct_details", false);
        boolean show_cycle = getOption("show_cycle", true);
                
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("period_length", period_length);
        editor.putString("startofweek", startofweek);
        editor.putString("maximum_cycle_length", maximum_cycle_length);
        editor.putBoolean("direct_details", direct_details);
        editor.putBoolean("show_cycle", show_cycle);
        editor.apply();
    }
}
