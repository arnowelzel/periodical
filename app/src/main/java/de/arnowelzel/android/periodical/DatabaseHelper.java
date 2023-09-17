package de.arnowelzel.android.periodical;

import android.database.Cursor;
import androidx.arch.core.util.Function;

public class DatabaseHelper {
    /**
     * Helper to iterate over cursor
     *
     * @param cursor
     * @param function
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
