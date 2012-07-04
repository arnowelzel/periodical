/**
 * Extended calendar class to provide correct difference calculation
 */

package de.arnowelzel.android.periodical;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class GregorianCalendarExt extends GregorianCalendar {
	private static final long serialVersionUID = 7320228290550179309L;

	public int diffDayPeriods(Calendar date) {
		long endL = date.getTimeInMillis() + date.getTimeZone().getOffset( date.getTimeInMillis() );
		long startL = this.getTimeInMillis() + this.getTimeZone().getOffset( this.getTimeInMillis() );
		return (int) ((endL-startL) / (1000*60*60*24)); 		
	}
}
