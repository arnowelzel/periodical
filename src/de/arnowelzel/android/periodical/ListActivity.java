package de.arnowelzel.android.periodical;

import java.util.Iterator;

import de.arnowelzel.android.periodical.PeriodicalDatabase.DayEntry;

import android.os.Bundle;
import android.widget.ArrayAdapter;

public class ListActivity extends android.app.ListActivity {
	/* Database for calendar data */
	private PeriodicalDatabase dbMain;
	
	/* Called when activity starts */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set up database and string array for the list
		dbMain = new PeriodicalDatabase(getApplicationContext());
		dbMain.loadRawData(false);
		
		String[] entries=new String[dbMain.dayEntries.size()];
		java.text.DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(getApplicationContext());		
		Iterator<DayEntry> dayIterator = dbMain.dayEntries.iterator();
		int pos=0;
		while(dayIterator.hasNext()) {
			DayEntry day = dayIterator.next();
			String description = dateFormat.format(day.date.getTime());
			switch(day.type) {
			case DayEntry.PERIOD_START:
				description = description + " (" + getString(R.string.event_periodstart) + ")";
				break;			
			}
			entries[pos] = description;
			pos++;
		}
		
		dbMain.close();
		
		// Set up view
		setListAdapter(new ArrayAdapter<String>(this, R.layout.listitem, entries));
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
}
