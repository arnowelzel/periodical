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

import java.util.List;

import static java.lang.String.*;

/**
 * Custom adapter to populate calendar entry list items
 */
class DayEntryAdapter extends ArrayAdapter<PeriodicalDatabase.DayEntry> {
    private final Context context;
    private final List<PeriodicalDatabase.DayEntry> entryList;
    private final String packageName;
    private final Resources resources;

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
     * Group in which this view is inserted
     *
     * @return
     * View to be used for the item
     */
    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if(listItem == null)
            listItem = LayoutInflater.from(context).inflate(R.layout.listdetailsitem,parent,false);

        PeriodicalDatabase.DayEntry currentEntry = entryList.get(position);

        String textSymptoms = "";
        int num = 1;
        while(true) {
            String resName = format("label_details_ev%d",num);
            int resId = resources.getIdentifier(resName, "string", packageName);
            if(resId != 0) {
                if(currentEntry.symptoms.contains(num)) {
                    if(!textSymptoms.isEmpty()) textSymptoms += "\n";
                    textSymptoms += "\u2022 " + resources.getString(resId);
                }
                num++;
            } else {
                break;
            }
        }

        java.text.DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(context);

        TextView view;

        view = listItem.findViewById(R.id.item_date);
        switch(currentEntry.type) {
            case PeriodicalDatabase.DayEntry.PERIOD_START:
                view.setText(
                        dateFormat.format(currentEntry.date.getTime()) + " \u2014 " +
                        resources.getString(R.string.event_periodstart));
                break;
            case PeriodicalDatabase.DayEntry.PERIOD_CONFIRMED:
                view.setText(
                        dateFormat.format(currentEntry.date.getTime()) + " \u2014 " +
                                format(
                                        resources.getString(R.string.label_period_day),
                                        currentEntry.dayofcycle));
                break;
            default:
                view.setText(dateFormat.format(currentEntry.date.getTime()));
                break;
        }

        view = listItem.findViewById(R.id.item_intensity);
        if(currentEntry.type == PeriodicalDatabase.DayEntry.PERIOD_START ||
                currentEntry.type == PeriodicalDatabase.DayEntry.PERIOD_CONFIRMED) {
            String intensity = "?";
            switch(currentEntry.intensity)
            {
                case 1: intensity = resources.getString(R.string.label_details_intensity1);break;
                case 2: intensity = resources.getString(R.string.label_details_intensity2);break;
                case 3: intensity = resources.getString(R.string.label_details_intensity3);break;
                case 4: intensity = resources.getString(R.string.label_details_intensity4);break;
            }
            view.setText(intensity);
        } else {
            view.setText("\u2014");
        }

        view = listItem.findViewById(R.id.item_notes);
        if(currentEntry.notes.isEmpty()) view.setText("\u2014");
        else view.setText(currentEntry.notes);

        view = listItem.findViewById(R.id.item_symptom);
        if(textSymptoms.isEmpty()) view.setText("\u2014");
        else view.setText(textSymptoms);

        return listItem;
    }
}
