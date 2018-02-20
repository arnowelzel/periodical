/*
 * Extended calendar class to provide correct date difference calculation
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

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Helper to deal with calendar dates
 */
class GregorianCalendarExt extends GregorianCalendar {
    /**
     *  Calculate the difference between this calendar date and a given date in days
     *
     *  @param date
     *  The date to which the difference should be calculated
     *
     *  @return
     *  The number of days between the calendar date and the given date
     */
    int diffDayPeriods(Calendar date) {
        long endL = date.getTimeInMillis() + date.getTimeZone().getOffset( date.getTimeInMillis() );
        long startL = this.getTimeInMillis() + getTimeZone().getOffset( getTimeInMillis() );
        return (int) ((endL-startL) / (1000*60*60*24));         
    }
}
