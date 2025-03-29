/*
 * Periodical database helper
 * Copyright (C) 2012-2025 Arno Welzel
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

import android.database.Cursor;
import androidx.arch.core.util.Function;

public class DatabaseHelper {
    /**
     * Helper to iterate over cursor
     *
     * @param cursor Cursor to use for the iteration
     * @param function Function to be called for each iteration
     */
    public static void iterateOverCursor(Cursor cursor, Function<Cursor,Void> function){
        if (cursor .moveToFirst()) {
            while (!cursor.isAfterLast()) {
                function.apply(cursor);
                cursor.moveToNext();
            }
        }
        cursor.close();
    }
}
