/**
 * Periodical main activity 
 * Copyright (C) 2012-2013 Arno Welzel
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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.backup.BackupManager;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import java.util.Calendar;
import java.util.GregorianCalendar;

import de.arnowelzel.android.periodical.PeriodicalDatabase.DayEntry;
import de.arnowelzel.android.periodical.R;

public class MainActivity extends Activity {

	/* Constants for dialog boxes */
	final int DLG_ABOUT = 1;
	final int DLG_HELP = 2;

	/* Array for calendar button IDs */
	final int calButtonIds[] = { R.id.cal01, R.id.cal02, R.id.cal03,
			R.id.cal04, R.id.cal05, R.id.cal06, R.id.cal07, R.id.cal08,
			R.id.cal09, R.id.cal10, R.id.cal11, R.id.cal12, R.id.cal13,
			R.id.cal14, R.id.cal15, R.id.cal16, R.id.cal17, R.id.cal18,
			R.id.cal19, R.id.cal20, R.id.cal21, R.id.cal22, R.id.cal23,
			R.id.cal24, R.id.cal25, R.id.cal26, R.id.cal27, R.id.cal28,
			R.id.cal29, R.id.cal30, R.id.cal31, R.id.cal32, R.id.cal33,
			R.id.cal34, R.id.cal35, R.id.cal36, R.id.cal37, R.id.cal38,
			R.id.cal39, R.id.cal40, R.id.cal41, R.id.cal42 };
	final int calButtonIds_2[] = { R.id.cal01_2, R.id.cal02_2, R.id.cal03_2,
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

	/* Bundle entries */
	final String STATE_MONTH = "month";
	final String STATE_YEAR = "year";

	/* Swipe handler */
	GestureDetector gestureDetector;
	View.OnTouchListener gestureListener;

	/* Current view which has to be updated */
	private int viewCurrent = R.id.calendar;

	/* Current month year */
	private int monthCurrent, yearCurrent;

	/* First day of week (0=Sunday) and number of days on current month */
	private int firstDay, daysCount;

	/* Database for calendar data */
	private PeriodicalDatabase dbMain;

	/* Called when activity starts */
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set up database
		dbMain = new PeriodicalDatabase(getApplicationContext());

		// Set up view
		setContentView(R.layout.main);

		// Set gesture handling
		gestureDetector = new GestureDetector(new CalendarGestureDetector());
		gestureListener = new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if (gestureDetector.onTouchEvent(event)) {
					return true;
				}
				return false;
			}
		};

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

	/* Called when the activity starts interacting with the user */
	@Override
	protected void onResume() {
		super.onResume();

		// Update calendar view
		calendarUpdate();
	}

	/* Called to save the current instance state */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putInt(STATE_MONTH, monthCurrent);
		outState.putInt(STATE_YEAR, yearCurrent);
	}

	/* Called when the activity is destroyed */
	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (dbMain != null)
			dbMain.close();
	}

	/* Called when the use selects the menu button */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	/* Called when a menu item was selected */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.current:
			goCurrent();
			return true;

		case R.id.list:
			showList();
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

		case R.id.exit:
			finish();
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/* Called when a dialog box is created */
	@Override
	public Dialog onCreateDialog(int id) {
		Dialog dialog;
		Button buttonOk;

		switch (id) {
		case DLG_HELP:
			dialog = new Dialog(this);
			dialog.setContentView(R.layout.dialog_help);
			dialog.setTitle(R.string.help_title);
			buttonOk = (Button) dialog.findViewById(R.id.ok);
			buttonOk.setOnClickListener(new View.OnClickListener() {

				@SuppressWarnings("deprecation")
				public void onClick(View view) {
					dismissDialog(DLG_HELP);
				}
			});
			break;
			
		case DLG_ABOUT:
			dialog = new Dialog(this);
			dialog.setContentView(R.layout.dialog_about);
			dialog.setTitle(R.string.about_title);
			buttonOk = (Button) dialog.findViewById(R.id.ok);
			buttonOk.setOnClickListener(new View.OnClickListener() {

				@SuppressWarnings("deprecation")
				public void onClick(View view) {
					dismissDialog(DLG_ABOUT);
				}
			});
			break;

		default:
			dialog = null;
			break;
		}

		return dialog;
	}

	/* Handler for "Help" menu action */
	@SuppressWarnings("deprecation")
	void showHelp() {
		showDialog(DLG_HELP);
	}
	
	/* Handler for "About" menu action */
	@SuppressWarnings("deprecation")
	void showAbout() {
		showDialog(DLG_ABOUT);
	}

	/* Handler for "List" menu action */
	void showList() {
		Intent listIntent = new Intent(MainActivity.this, ListActivity.class);
		startActivity(listIntent);
	}

	/* Update calendar data and view */
	void calendarUpdate() {
		// Initialize control ids for the target view to be used
		int calButtonIds[];
		if (this.viewCurrent == R.id.calendar) {
			calButtonIds = this.calButtonIds;
		} else {
			calButtonIds = this.calButtonIds_2;
		}

		// Output current year/month
		TextView displayDate = (TextView) findViewById(R.id.displaydate);
		displayDate.setText(String.format("%s %d\nØ%d ↓%d ↑%d", DateUtils
				.getMonthString(this.monthCurrent - 1, DateUtils.LENGTH_LONG),
				this.yearCurrent, dbMain.average, dbMain.shortest,
				dbMain.longest));

		// Calculate first week day of month
		GregorianCalendar cal = new GregorianCalendar(yearCurrent,
				monthCurrent - 1, 1);
		firstDay = cal.get(Calendar.DAY_OF_WEEK);
		daysCount = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

		GregorianCalendar calToday = new GregorianCalendar();

		// Adjust calendar elements
		for (int i = 1; i <= 42; i++) {
			Button calButton = (Button) findViewById(calButtonIds[i - 1]);
			if (i < firstDay || i >= firstDay + daysCount) {
				calButton.setVisibility(android.view.View.INVISIBLE);
				// TODO Display days of previous/next month as "disabled"
				// buttons
			} else {
				int day = i - firstDay + 1;
				calButton.setText(String.format("%d", day));
				calButton.setVisibility(android.view.View.VISIBLE);
				int type = dbMain.getEntry(yearCurrent, monthCurrent, day);
				boolean current = false;

				if (this.yearCurrent == calToday.get(Calendar.YEAR)
						&& this.monthCurrent == calToday.get(Calendar.MONTH) + 1
						&& day == calToday.get(Calendar.DAY_OF_MONTH)) {
					calButton.setTypeface(null, 1);
					current = true;

				} else {
					calButton.setTypeface(null, 0);
				}

				switch (type) {
				case DayEntry.PERIOD_START: // Start of period
					if (current) {
						calButton
								.setBackgroundDrawable(getResources()
										.getDrawable(
												R.drawable.button_calendar_periodstart_current));
					} else {
						calButton
								.setBackgroundDrawable(getResources()
										.getDrawable(
												R.drawable.button_calendar_periodstart_normal));
					}
					calButton.setTypeface(null, 0);
					calButton.setTextColor(getResources().getColor(
							R.drawable.text_calendar_start));
					break;

				case DayEntry.PERIOD_CONFIRMED: // Confirmed period day
					if (current) {
						calButton
								.setBackgroundDrawable(getResources()
										.getDrawable(
												R.drawable.button_calendar_period_current));
					} else {
						calButton.setBackgroundDrawable(getResources()
								.getDrawable(
										R.drawable.button_calendar_period_normal));
					}
					calButton.setTextColor(getResources().getColor(
							R.drawable.text_calendar));
					break;

				case DayEntry.PERIOD_PREDICTED: // Predicted period day
					if (current) {
						calButton
								.setBackgroundDrawable(getResources()
										.getDrawable(
												R.drawable.button_calendar_periodcalc_current));
					} else {
						calButton
								.setBackgroundDrawable(getResources()
										.getDrawable(
												R.drawable.button_calendar_periodcalc_normal));

					}
					calButton.setTextColor(getResources().getColor(
							R.drawable.text_calendar));
					break;

				case DayEntry.FERTILITY_PREDICTED: // Calculated fertile day
					if (current) {
						calButton
								.setBackgroundDrawable(getResources()
										.getDrawable(
												R.drawable.button_calendar_fertile_current));
					} else {
						calButton
								.setBackgroundDrawable(getResources()
										.getDrawable(
												R.drawable.button_calendar_fertile_normal));
					}

					calButton.setTextColor(getResources().getColor(
							R.drawable.text_calendar));
					break;

				default:
					if (current) {
						calButton
								.setBackgroundDrawable(getResources()
										.getDrawable(
												R.drawable.button_calendar_empty_current));
					} else {
						calButton
								.setBackgroundDrawable(getResources()
										.getDrawable(
												R.drawable.button_calendar_empty_normal));
					}
					calButton.setTextColor(getResources().getColor(
							R.drawable.text_calendar));
					break;
				}
			}
		}
	}

	/* Handler for "previous month" button in main view */
	public void goPrev(View v) {
		// Update calendar
		monthCurrent--;
		if (monthCurrent < 1) {
			monthCurrent = 12;
			yearCurrent--;
		}

		if (this.viewCurrent == R.id.calendar) {
			this.viewCurrent = R.id.calendar_2;
		} else {
			this.viewCurrent = R.id.calendar;
		}

		calendarUpdate();

		// Show slide animation from left to right
		ViewFlipper flipper = (ViewFlipper) findViewById(R.id.mainwidget);
		flipper.setInAnimation(AnimationHelper.inFromLeftAnimation());
		flipper.setOutAnimation(AnimationHelper.outToRightAnimation());
		flipper.showNext();
	}

	/* Handler for "next month" button in main view */
	public void goNext(View v) {
		// Update calendar
		monthCurrent++;
		if (monthCurrent > 12) {
			monthCurrent = 1;
			yearCurrent++;
		}

		if (this.viewCurrent == R.id.calendar) {
			this.viewCurrent = R.id.calendar_2;
		} else {
			this.viewCurrent = R.id.calendar;
		}

		calendarUpdate();

		// Show slide animation from right to left
		ViewFlipper flipper = (ViewFlipper) findViewById(R.id.mainwidget);
		flipper.setInAnimation(AnimationHelper.inFromRightAnimation());
		flipper.setOutAnimation(AnimationHelper.outToLeftAnimation());
		flipper.showPrevious();
	}

	/* Change to current month */
	private void initMonth() {
		Calendar cal = new GregorianCalendar();
		monthCurrent = cal.get(Calendar.MONTH) + 1;
		yearCurrent = cal.get(Calendar.YEAR);
	}

	/* Handler for "current month" menu action */
	private void goCurrent() {
		initMonth();
		calendarUpdate();
	}

	/* Handler for "backup" menu action */
	private void doBackup() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getResources().getString(R.string.backup_title));
		builder.setMessage(getResources().getString(R.string.backup_text));
		builder.setIcon(android.R.drawable.ic_dialog_alert);

		builder.setPositiveButton(getResources().getString(R.string.backup_ok),
				new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						boolean ok = dbMain.backup(getApplicationContext());

						// Show toast depending on result of operation
						String text;
						if (ok) {
							text = getResources().getString(
									R.string.backup_finished);
						} else {
							text = getResources().getString(
									R.string.backup_failed);
						}

						Toast toast = Toast.makeText(getApplicationContext(),
								text, Toast.LENGTH_SHORT);
						toast.show();
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

	/* Handler for "restore" menu action */
	private void doRestore() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getResources().getString(R.string.restore_title));
		builder.setMessage(getResources().getString(R.string.restore_text));
		builder.setIcon(android.R.drawable.ic_dialog_alert);

		builder.setPositiveButton(
				getResources().getString(R.string.restore_ok),
				new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						boolean ok = dbMain.restore(getApplicationContext());
						
						handleDatabaseEdit();

						// Show toast depending on result of operation
						String text;
						if (ok) {
							text = getResources().getString(
									R.string.restore_finished);
						} else {
							text = getResources().getString(
									R.string.restore_failed);
						}
						Toast toast = Toast.makeText(getApplicationContext(), text,
								Toast.LENGTH_SHORT);
						toast.show();
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

	/* Handler for the selection of one day in the calendar */
	public void handleCalendarButton(View v) {
		// Determine selected date
		int idButton = v.getId();
		int nButtonClicked = 0;

		int calButtonIds[];
		if (this.viewCurrent == R.id.calendar) {
			calButtonIds = this.calButtonIds;
		} else {
			calButtonIds = this.calButtonIds_2;
		}

		while (nButtonClicked < 42) {
			if (calButtonIds[nButtonClicked] == idButton)
				break;
			nButtonClicked++;
		}
		final int day = nButtonClicked - firstDay + 2;

		// Set or remove entry with confirmation
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getResources()
				.getString(R.string.calendaraction_title));

		if (dbMain.getEntry(yearCurrent, monthCurrent, day) != 1) {
			builder.setMessage(getResources().getString(
					R.string.calendaraction_add));
			builder.setPositiveButton(
					getResources().getString(R.string.calendaraction_ok),
					new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							dbMain.add(yearCurrent, monthCurrent, day);
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
		} else {
			builder.setMessage(getResources().getString(
					R.string.calendaraction_remove));
			builder.setPositiveButton(
					getResources().getString(R.string.calendaraction_ok),
					new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							dbMain.remove(yearCurrent, monthCurrent, day);
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
		}
		builder.show();
	}

	/* Helper to update view after database modification */
	private void handleDatabaseEdit() {
		// Update calculated values
		dbMain.loadCalculatedData();
		calendarUpdate();

		// Notify backup agent about the change and mark DB as clean
		BackupManager bm = new BackupManager(this);
		bm.dataChanged();
	}

	// Gesture detector to handle swipes

	@Override
	public boolean dispatchTouchEvent(MotionEvent e) {
		super.dispatchTouchEvent(e);
		return gestureDetector.onTouchEvent(e);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return gestureDetector.onTouchEvent(event);
	}

	class CalendarGestureDetector extends SimpleOnGestureListener {
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
