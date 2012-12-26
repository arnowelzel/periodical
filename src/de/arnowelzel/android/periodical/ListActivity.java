package de.arnowelzel.android.periodical;

import java.util.Iterator;

import de.arnowelzel.android.periodical.PeriodicalDatabase.DayEntry;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

public class ListActivity extends android.app.ListActivity {
	/* Database for calendar data */
	private PeriodicalDatabase dbMain;

	/* Called when activity starts */
	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set up database and string array for the list
		dbMain = new PeriodicalDatabase(getApplicationContext());
		dbMain.loadRawData(false);

		String[] entries = new String[dbMain.dayEntries.size()];
		java.text.DateFormat dateFormat = android.text.format.DateFormat
				.getDateFormat(getApplicationContext());
		Iterator<DayEntry> dayIterator = dbMain.dayEntries.iterator();
		int pos = 0;
		DayEntry dayPrevious = null;
		DayEntry day = null;
		boolean isFirst = true;
		while (dayIterator.hasNext()) {
			if (isFirst) {
				isFirst = false;
			} else {
				dayPrevious = day;
			}
			day = dayIterator.next();

			entries[pos] = dateFormat.format(day.date.getTime());
			switch (day.type) {
			case DayEntry.PERIOD_START:
				entries[pos] = entries[pos] + " ("
						+ getString(R.string.event_periodstart) + ")";
				if (dayPrevious != null) {
					// If we have a previous day, then update the previous
					// days length description
					Integer length = day.date.diffDayPeriods(dayPrevious.date);
					entries[pos - 1] += "\n"
							+ String.format(
									getString(R.string.event_periodlength),
									length.toString());
				}
				break;
			}
			pos++;
		}
		// If we have at least one entry, update the last days length
		// description to "first entry"
		if (pos > 0) {
			entries[pos - 1] += "\n" + getString(R.string.event_periodfirst);
		}
		dbMain.close();

		// Set up view
		setListAdapter(new ArrayAdapter<String>(this, R.layout.listitem,
				entries));

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
}
