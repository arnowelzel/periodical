/**
 * Periodical Database helper
 */

package de.arnowelzel.android.periodical;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.Vector;

public class PeriodicalDatabase {

	/* Helper to create or open database */
	private class PeriodicalDataOpenHelper extends SQLiteOpenHelper {
		final static String DATABASE_NAME = "main.db";
		final static int DATABASE_VERSION = 1;
		final static String DATA_TABLE_NAME = "data";
		final static String DATA_TABLE_CREATE = "CREATE TABLE "
				+ DATA_TABLE_NAME + " (_id integer primary key autoincrement,"
				+ "   eventtype integer(3)," + "   eventdate varchar(8))" + ";";

		PeriodicalDataOpenHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATA_TABLE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO Auto-generated method stub
		}
	}

	/* Reference to database */
	private SQLiteDatabase db;

	/* Local helper to manage calculated calendar entries */
	public class DayEntry {
		final static int PERIOD_START = 1;
		final static int PERIOD_CONFIRMED = 2;
		final static int PERIOD_PREDICTED = 3;
		final static int FERTILITY_PREDICTED = 4;
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
		PeriodicalDataOpenHelper dataOpenHelper;
		dataOpenHelper = new PeriodicalDataOpenHelper(context);
		db = dataOpenHelper.getWritableDatabase();

		dayEntries = new Vector<DayEntry>();
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
		db.execSQL(statement);
	}

	/* Remove an entry for a specific day into the database */
	void remove(int year, int month, int day) {
		String statement;

		statement = String.format("delete from data where eventdate='%s'",
				String.format("%04d%02d%02d", year, month, day));
		db.execSQL(statement);
	}

	/* Update the calculation based on the entries in the database */
	void loadCalculatedData() {
		DayEntry entry = null;
		DayEntry entryPrevious = null;
		boolean isFirst = true;
		int count = 0;
		this.average = 0;
		this.longest = 28;
		this.shortest = 28;

		// Clean up existing data
		dayEntries.removeAllElements();

		// Get all entries from the database
		Cursor result = db.rawQuery(
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

				// Update longest/shortest values, which are used to
				// calculate the fertility window
				if (length < this.shortest)
					this.shortest = length;
				if (length > this.longest)
					this.longest = length;

				// Calculate days from the last event until now
				GregorianCalendar datePrevious = new GregorianCalendar();
				datePrevious.setTime(entryPrevious.date.getTime());
				for (int day = 2; day <= length; day++) {
					datePrevious.add(GregorianCalendar.DATE, 1);

					if (day <= 4) {
						// First days of period
						DayEntry entryCalculated = new DayEntry(2, datePrevious);
						dayEntries.add(entryCalculated);
					} else if (day >= this.shortest - 18
							&& day <= this.longest - 11) {
						// Fertile days
						DayEntry entryCalculated = new DayEntry(4, datePrevious);
						dayEntries.add(entryCalculated);
					}
				}

				// Finally add current day
				entryPrevious = new DayEntry(entry.type, entry.date);
				this.dayEntries.add(entry);

				// Update global average sum
				this.average += length;
			}
		}
		
		// Calculate global average and prediction if possible
		if (count > 0) {
			this.average /= count;

			GregorianCalendar datePredicted = new GregorianCalendar();
			datePredicted.setTime(entry.date.getTime());

			for (int cycles = 0; cycles < 3; cycles++) {
				for (int day = (cycles == 0 ? 2 : 1); day <= average; day++) {
					datePredicted.add(GregorianCalendar.DATE, 1);

					if (day <= 4) {
						// Predicted days of period
						DayEntry entryCalculated = new DayEntry(
								(cycles == 0 ? 2 : 3), datePredicted);
						dayEntries.add(entryCalculated);
					} else if (day >= this.shortest - 18
							&& day <= this.longest - 11) {
						// Fertile days
						DayEntry entryCalculated = new DayEntry(4,
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
		if(!sortAscending) statement +=" desc";
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

	/*
	 * Helper to backup the database on the SD card if possible or restore it
	 * from there
	 */
	public void backup(Context context) {
		backupRestore(context, true);
	}

	public void restore(Context context) {
		backupRestore(context, false);
	}
	
	public void backupRestore(Context context, boolean backup) {
		// Check if SD card is mounted
		if (Environment.getExternalStorageState().equals(
				android.os.Environment.MEDIA_UNMOUNTED))
			return;

		File sourceFile = null;
		File destDir = null;
		File destFile = null;

		// Get source of DB and path for external storage
		if (backup) {
			sourceFile = new File(db.getPath());
			destDir = new File(Environment.getExternalStorageDirectory()
					.getAbsolutePath() + "/" + context.getPackageName());
			destFile = new File(destDir
					.getAbsolutePath()
					+ "/" + sourceFile.getName());
		} else {
			destFile = new File(db.getPath());
			sourceFile = new File(Environment.getExternalStorageDirectory()
					.getAbsolutePath() + "/" + context.getPackageName()
					+ "/" + destFile.getName());
		}

		// Before we can copy anything, close the DB
		db.close();

		boolean ok = true;

		// Check, if destination exists and delete first
		if (destFile.exists())
			ok = destFile.delete();

		// If everything is ok, then copy source to destination
		if (ok) {
			if(backup && destDir != null) destDir.mkdirs();
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
				}
			}
		}

		// Open the DB again
		PeriodicalDataOpenHelper dataOpenHelper;
		dataOpenHelper = new PeriodicalDataOpenHelper(context);
		db = dataOpenHelper.getWritableDatabase();
	}
}
