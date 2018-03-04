/*
 * Custom adapter for calendar entry list view
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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom adapter to populate calendar entry list items
 */
public class DayEntryAdapter extends ArrayAdapter<PeriodicalDatabase.DayEntry> {
    private Context context;
    private List<PeriodicalDatabase.DayEntry> entryList = new ArrayList<>();
    private String packageName;
    private Resources resources;

    /**
     * Constructor
     *
     * @param context
     * Application content
     *
     * @param list
     * List with all calendar entries including details
     *
     * @param packageName
     * Application package from getPackageName()
     *
     * @param resources
     * Global resources from getResources()
     */
    public DayEntryAdapter(Context context, List<PeriodicalDatabase.DayEntry> list, String packageName, Resources resources) {
        super(context, 0, list);

        this.context = context;
        this.packageName = packageName;
        this.resources = resources;
        entryList = list;
    }

    /**
     * Constructs a single item view
     *
     * @param position
     * Position of the item in the list
     *
     * @param convertView
     * Existing view to use (if null, a new one will be created)
     *
     * @param parent
     *
     * @return
     */
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if(listItem == null)
            listItem = LayoutInflater.from(context).inflate(R.layout.listitem,parent,false);

        PeriodicalDatabase.DayEntry currentEntry = entryList.get(position);

        Integer length = 0;

        if(position > 0) length = currentEntry.date.diffDayPeriods(entryList.get(position -1).date);

        String textSymptoms = "";
        int num = 1;
        while(true) {
            @SuppressLint("DefaultLocale") String resName = String.format("label_details_ev%d",num);
            int resId = resources.getIdentifier(resName, "string", packageName);
            if(resId != 0) {
                if(currentEntry.symptoms.contains(new Integer(num))) {
                    if(!textSymptoms.isEmpty()) textSymptoms += "\n";
                    textSymptoms += resources.getString(resId);
                }
                num++;
            } else {
                break;
            }
        }

        java.text.DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(context);

        TextView view;

        view = (TextView) listItem.findViewById(R.id.item_date);
        switch(currentEntry.type) {
            case PeriodicalDatabase.DayEntry.PERIOD_START:
                view.setText(
                        dateFormat.format(currentEntry.date.getTime()) + " " +
                        resources.getString(R.string.dash) + " " +
                        resources.getString(R.string.event_periodstart));
                break;
            case PeriodicalDatabase.DayEntry.PERIOD_CONFIRMED:
                view.setText(
                        dateFormat.format(currentEntry.date.getTime()) + " " +
                        resources.getString(R.string.dash) + " " +
                        resources.getString(R.string.label_details_period));
                break;
            default:
                view.setText(dateFormat.format(currentEntry.date.getTime()));
                break;
        }

        view = (TextView) listItem.findViewById(R.id.item_intensity);
        if(currentEntry.type == PeriodicalDatabase.DayEntry.PERIOD_START ||
                currentEntry.type == PeriodicalDatabase.DayEntry.PERIOD_CONFIRMED) {
            view.setText(String.format("%d", currentEntry.intensity));
        } else {
            view.setText(R.string.dash);
        }

        view = (TextView) listItem.findViewById(R.id.item_notes);
        if(currentEntry.notes.isEmpty()) view.setText(R.string.dash);
        else view.setText(currentEntry.notes);

        view = (TextView) listItem.findViewById(R.id.item_symptom);
        if(textSymptoms.isEmpty()) view.setText(R.string.dash);
        else view.setText(textSymptoms);

        return listItem;
    }
}
