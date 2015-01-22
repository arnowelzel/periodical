/**
 * Extended calendar class to provide correct date difference calculation
 * Copyright (C) 2012-2015 Arno Welzel
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

public class GregorianCalendarExt extends GregorianCalendar {
    private static final long serialVersionUID = 7320228290550179309L;
    
    /* Calculate the difference between this calendar date and a given date in days */
    public int diffDayPeriods(Calendar date) {
        long endL = date.getTimeInMillis() + date.getTimeZone().getOffset( date.getTimeInMillis() );
        long startL = this.getTimeInMillis() + getTimeZone().getOffset( getTimeInMillis() );
        return (int) ((endL-startL) / (1000*60*60*24));         
    }
}
