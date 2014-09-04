/**
 * Periodical database class
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
 */

package de.arnowelzel.android.periodical;

import android.content.ContentValues;
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
import java.sql.SQLDataException;
import java.sql.SQLException;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.Vector;

public class PeriodicalDatabase {

    /* Helper to create or open database */
    private class PeriodicalDataOpenHelper extends SQLiteOpenHelper {
        final static String DATABASE_NAME = "main.db";
        final static int DATABASE_VERSION = 3;

        PeriodicalDataOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("create table data (" +
                    "_id integer primary key autoincrement, " +
                    "eventtype integer(3), " +
                    "eventdate varchar(8), " +
                    "eventcvx integer(3), " +
                    "eventtemp real " +
                    ");");
            /* Due to a bug in release 0.16 this was missing - see open() for the workaround */   
            db.execSQL("create table options (" +
                    "name varchar(100), " +
                    "value varchar(500)" +
                    ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion < 2 && newVersion >= 2) {
                // Version 2 introduces additional data columns
                db.execSQL("alter table data add column eventcvx integer(3)");
                db.execSQL("alter table data add column eventtemp real");
            } else if (oldVersion < 3 && newVersion >= 3) {
                // Version 3 introduces options
                db.execSQL("create table options (" +
                        "name varchar(100), " +
                        "value varchar(500)" +
                        ");");
            }
        }
    }

    /* Reference to database */
    private SQLiteDatabase db;

    /* Dirty flag to signal changes for the backup manager */
    private boolean isDirty;

    /* Local helper to manage calculated calendar entries */
    public class DayEntry {
        final static int PERIOD_START = 1;
        final static int PERIOD_CONFIRMED = 2;
        final static int PERIOD_PREDICTED = 3;
        final static int FERTILITY_PREDICTED = 4;
        final static int OVULATION_PREDICTED = 5;
        final static int FERTILITY_PREDICTED_FUTURE = 6;
        final static int OVULATION_PREDICTED_FUTURE = 7;
        int type;
        GregorianCalendarExt date;

        public DayEntry(int type, GregorianCalendar date) {
            this.type = type;
            this.date = new GregorianCalendarExt();
            this.date.setTime(date.getTime());
        }
    }

    /* Calculated data */
    Vector<DayEntry> dayEntries;
    int average;
    int longest;
    int shortest;

    /* Constructor, will try to create/open a writable database */
    PeriodicalDatabase(Context context) {
        open(context);

        dayEntries = new Vector<DayEntry>();
    }

    /* Open the database */
    void open(Context context) {
        PeriodicalDataOpenHelper dataOpenHelper;
        dataOpenHelper = new PeriodicalDataOpenHelper(context);
        db = dataOpenHelper.getWritableDatabase();
        isDirty = false;
        
        // Workaround for a bug introduced in release 0.16:
        // 1) Creating a new database did not create the "options" table
        // 2) Opening a restored database may result in an old version missing the "options" table

        // Check if table "options" exist
        Cursor result;
        boolean options_missing = false;
        result = db.rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = 'options'", null);
        if (!result.moveToNext()) {
            options_missing = true;
        }
        if (options_missing) {
            db.execSQL("create table options (" +
                    "name varchar(100), " +
                    "value varchar(500)" +
                    ");");
        }
    }

    /* Close the database */
    void close() {
        if (db != null)
            db.close();
    }

    /* Store an entry for a specific day into the database */
    void add(int year, int month, int day) {
        String statement;

        statement = String.format(
                "insert into data (eventtype, eventdate) values (1, '%s')",
                String.format("%04d%02d%02d", year, month, day));
        db.beginTransaction();
        db.execSQL(statement);
        db.setTransactionSuccessful();
        db.endTransaction();

        isDirty = true;
    }

    /* Remove an entry for a specific day into the database */
    void remove(int year, int month, int day) {
        String statement;

        statement = String.format("delete from data where eventdate='%s'",
                String.format("%04d%02d%02d", year, month, day));
        db.beginTransaction();
        db.execSQL(statement);
        db.setTransactionSuccessful();
        db.endTransaction();

        isDirty = true;
    }

    /* Update the calculation based on the entries in the database */
    void loadCalculatedData(Context context) {
        DayEntry entry = null;
        DayEntry entryPrevious = null;
        boolean isFirst = true;
        int count = 0;
        int countlimit = 1;
        this.average = 0;
        this.longest = 28;
        this.shortest = 28;
        int ovulationday = 0;
        Cursor result;
        int periodlength = 4;

        // Get default values from preferences
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            periodlength = Integer.parseInt(preferences.getString("period_length", "4"));
        } catch (NumberFormatException e) {
            periodlength = 4;
        }

        // Clean up existing data
        dayEntries.removeAllElements();

        // Determine minimum entry count for
        // shortest/longest period calculation
        result = db.rawQuery("select count(*) from data", null);
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

            if (isFirst) {
                isFirst = false;

                // First event at all - just create an initial start entry
                entryPrevious = new DayEntry(eventtype, eventdate);

                this.dayEntries.add(entryPrevious);
            } else {
                count++;

                // Create new day entry
                entry = new DayEntry(eventtype, eventdate);
                int length = entryPrevious.date.diffDayPeriods(entry.date);

                // Update values which are used to calculate the fertility
                // window for the last 12 entries
                if (count == countlimit) {
                    // If we have at least one period the shortest and
                    // and longest value is automatically the current length
                    this.shortest = length;
                    this.longest = length;
                } else if (count > countlimit) {
                    // We have more than two values, then update
                    // longest/shortest
                    // values
                    if (length < this.shortest)
                        this.shortest = length;
                    if (length > this.longest)
                        this.longest = length;
                }

                // Update average sum
                this.average += length;


                // Calculate a predicted ovulation date
                int average = this.average;
                if (count > 0) average /= count;
                ovulationday = average - 14;

                // Calculate days from the last event until now
                GregorianCalendar datePrevious = new GregorianCalendar();
                datePrevious.setTime(entryPrevious.date.getTime());
                for (int day = 2; day <= length; day++) {
                    datePrevious.add(GregorianCalendar.DATE, 1);

                    if (day <= periodlength) {
                        // First days of period
                        DayEntry entryCalculated = new DayEntry(DayEntry.PERIOD_CONFIRMED, datePrevious);
                        dayEntries.add(entryCalculated);
                    } else if (day == ovulationday) {
                        // Day of ovulation
                        DayEntry entryCalculated = new DayEntry(DayEntry.OVULATION_PREDICTED, datePrevious);
                        dayEntries.add(entryCalculated);
                    } else if (day >= this.shortest - 18
                            && day <= this.longest - 11) {
                        // Fertile days
                        DayEntry entryCalculated = new DayEntry(DayEntry.FERTILITY_PREDICTED, datePrevious);
                        dayEntries.add(entryCalculated);
                    }
                }

                // Finally add current day
                entryPrevious = new DayEntry(entry.type, entry.date);
                this.dayEntries.add(entry);
            }
        }
        result.close();

        // Calculate global average and prediction if possible
        if (count > 0) {
            this.average /= count;

            GregorianCalendar datePredicted = new GregorianCalendar();
            datePredicted.setTime(entry.date.getTime());

            for (int cycles = 0; cycles < 3; cycles++) {
                for (int day = (cycles == 0 ? 2 : 1); day <= average; day++) {
                    datePredicted.add(GregorianCalendar.DATE, 1);

                    if (day <= periodlength) {
                        // Predicted days of period
                        DayEntry entryCalculated = new DayEntry(
                                (cycles == 0 ? DayEntry.PERIOD_CONFIRMED : DayEntry.PERIOD_PREDICTED),
                                datePredicted);
                        dayEntries.add(entryCalculated);
                    } else if (day == ovulationday) {
                        // Day of ovulation
                        DayEntry entryCalculated = new DayEntry(
                                cycles == 0 ? DayEntry.OVULATION_PREDICTED : DayEntry.OVULATION_PREDICTED_FUTURE,
                                datePredicted);
                        dayEntries.add(entryCalculated);
                    } else if (day >= this.shortest - 18
                            && day <= this.longest - 11) {
                        // Fertile days
                        DayEntry entryCalculated = new DayEntry(
                                cycles == 0 ? DayEntry.FERTILITY_PREDICTED : DayEntry.FERTILITY_PREDICTED_FUTURE,
                                datePredicted);
                        dayEntries.add(entryCalculated);
                    }
                }
            }
        }

        System.gc();
    }

    /* Load data without calculating anything */
    void loadRawData(boolean sortAscending) {
        DayEntry entry;

        // Clean up existing data
        dayEntries.removeAllElements();

        // Get all entries from the database
        String statement = "select eventtype, eventdate from data order by eventdate";
        if (!sortAscending)
            statement += " desc";
        Cursor result = db.rawQuery(statement, null);
        while (result.moveToNext()) {
            int eventtype = result.getInt(0);
            String dbdate = result.getString(1);
            int eventyear = Integer.parseInt(dbdate.substring(0, 4), 10);
            int eventmonth = Integer.parseInt(dbdate.substring(4, 6), 10);
            int eventday = Integer.parseInt(dbdate.substring(6, 8), 10);
            GregorianCalendar eventdate = new GregorianCalendar(eventyear,
                    eventmonth - 1, eventday);

            // Create new day entry
            entry = new DayEntry(eventtype, eventdate);
            dayEntries.add(entry);
        }
        result.close();

        System.gc();
    }

    /* Get entry type from cache for a specific day in month */
    int getEntry(int year, int month, int day) {
        for (int dayPos = 0; dayPos < this.dayEntries.size(); dayPos++) {
            DayEntry entry = dayEntries.get(dayPos);

            // If entry was found, then return type
            if (entry.date.get(GregorianCalendar.YEAR) == year
                    && entry.date.get(GregorianCalendar.MONTH) == month - 1
                    && entry.date.get(GregorianCalendar.DATE) == day) {
                return entry.type;
            }
        }

        // Fall back if month was not found, then return "empty" as type
        return 0;
    }

    /* Get a named option from the options table */
    public String getOption(String name, String defaultvalue) {
        String value = defaultvalue;

        String statement = "select value from options where name = ?";
        Cursor result = db.rawQuery(statement, new String[]{name});
        if (result.moveToNext()) {
            value = result.getString(0);
        }
        result.close();

        return value;
    }

    /* Set a named option to be stored in the options table */
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

    /*
     * Helper to backup the database on the SD card if possible or restore it
     * from there
     */
    public boolean backup(Context context) {
        return backupRestore(context, true);
    }

    public boolean restore(Context context) {
        return backupRestore(context, false);
    }

    public boolean backupRestore(Context context, boolean backup) {
        // Check if SD card is mounted
        if (Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_UNMOUNTED))
            return false;

        File sourceFile = null;
        File destDir = null;
        File destFile = null;

        // Get source of DB and path for external storage
        if (backup) {
            sourceFile = new File(db.getPath());
            destDir = new File(Environment.getExternalStorageDirectory()
                    .getAbsolutePath() + "/" + context.getPackageName());
            destFile = new File(destDir.getAbsolutePath() + "/"
                    + sourceFile.getName());
        } else {
            destFile = new File(db.getPath());
            sourceFile = new File(Environment.getExternalStorageDirectory()
                    .getAbsolutePath()
                    + "/"
                    + context.getPackageName()
                    + "/"
                    + destFile.getName());
        }

        // Before we can copy anything, close the DB
        db.close();

        boolean ok = true;

        // Check, if destination exists and delete first
        if (destFile.exists())
            ok = destFile.delete();

        // If everything is ok, then copy source to destination
        if (ok) {
            if (backup && destDir != null)
                destDir.mkdirs();
            FileInputStream in = null;
            FileOutputStream out = null;
            try {
                in = new FileInputStream(sourceFile);
                out = new FileOutputStream(destFile);
            } catch (IOException e) {
                ok = false;
                e.printStackTrace();
            }

            if (ok) {
                byte[] buffer = new byte[4096];
                int bytesRead;

                try {
                    while ((bytesRead = in.read(buffer)) != -1)
                        out.write(buffer, 0, bytesRead);

                    if (in != null)
                        in.close();
                    if (out != null)
                        out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    ok = false;
                }
            }
        }

        // Open the DB again
        open(context);

        return ok;
    }

    /*
     * Save application preferences to the database
     * (Just a hack for now - in the future we might want to get rid of shared preferences)
     */
    void savePreferences(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        setOption("period_length", preferences.getString("period_length", "4"));
        setOption("startofweek", preferences.getString("startofweek", "0"));
    }

    /*
     * Restore application preferences from the database
     * (Just a hack for now - in the future we might want to get rid of shared preferences)
     */
    void restorePreferences(Context context) {
        String period_length = getOption("period_length", "4");
        String startofweek = getOption("startofweek", "0");
                
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("period_length", period_length);
        editor.putString("startofweek", startofweek);
        editor.commit();
    }

    /*
     * Get the path of the currently used database file, needed for external
     * backups
     */
    String getPath() {
        return db.getPath();
    }

    /*
     * Getter/setter for dirty flag
     */
    boolean getDirty() {
        return this.isDirty;
    }

    void setDirty(boolean isDirty) {
        this.isDirty = isDirty;
    }
}
