/*
 * Custom adapter for calendar entry list view
 * Copyright (C) 2012-2020 Arno Welzel
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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

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
     * @param context     Application content
     * @param list        List with all calendar entries including details
     * @param packageName Application package from getPackageName()
     * @param resources   Global resources from getResources()
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
     * @param position    Position of the item in the list
     * @param convertView Existing view to use (if null, a new one will be created)
     * @param parent      Group in which this view is inserted
     * @return View to be used for the item
     */
    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if (listItem == null)
            listItem = LayoutInflater.from(context).inflate(R.layout.listdetailsitem, parent, false);

        PeriodicalDatabase.DayEntry currentEntry = entryList.get(position);

        String textEvents = "";
        String textMood = "";
        String textSymptoms = "";

        // Elements 0-1 are events, 2-6 moods, 7-22 are symptoms
        int eventIds[] = {
                1,  // Intercourse
                18, // Contraceptive pill
                20, // Tired
                21, // Energized
                22, // Sad
                14, // Grumpiness
                23, // Edgy
                19, // Spotting
                9,  // Intense bleeding
                2,  // Cramps
                17, // Headache/migraine
                3,  // Back pain
                4,  // Middle pain left
                5,  // Middle pain right
                6,  // Breast pain/dragging pain
                7,  // Thrush/candida
                8,  // Discharge
                10, // Temperature fluctuations
                11, // Pimples
                12, // Bloating
                13, // Fainting
                15, // Nausea
                16, // Cravings
        };
        int num = 0;
        for (int eventId : eventIds) {
            String resName = format(Locale.ENGLISH,"label_details_ev%d", eventId);
            int resId = resources.getIdentifier(resName, "string", packageName);
            if (resId != 0) {
                if (currentEntry.symptoms.contains(eventId)) {
                    if (num < 2) {
                        if (!textEvents.isEmpty()) textEvents += "\n";
                        textEvents += "\u2022 " + resources.getString(resId);
                    } else if(num > 1 && num < 7) {
                        if (!textMood.isEmpty()) textMood += "\n";
                        textMood += "\u2022 " + resources.getString(resId);
                    } else {
                        if (!textSymptoms.isEmpty()) textSymptoms += "\n";
                        textSymptoms += "\u2022 " + resources.getString(resId);
                    }
                }
            }
            num++;
        }

        java.text.DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(context);

        TextView viewLabel;
        TextView view;

        view = listItem.findViewById(R.id.item_date);
        switch (currentEntry.type) {
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

        View blockIntensity = listItem.findViewById(R.id.block_intensity);
        viewLabel = listItem.findViewById(R.id.label_item_intensity);
        view = listItem.findViewById(R.id.item_intensity);
        if (currentEntry.type == PeriodicalDatabase.DayEntry.PERIOD_START ||
                currentEntry.type == PeriodicalDatabase.DayEntry.PERIOD_CONFIRMED) {
            String intensity = "?";
            switch (currentEntry.intensity) {
                case 1:
                    intensity = resources.getString(R.string.label_details_intensity1);
                    break;
                case 2:
                    intensity = resources.getString(R.string.label_details_intensity2);
                    break;
                case 3:
                    intensity = resources.getString(R.string.label_details_intensity3);
                    break;
                case 4:
                    intensity = resources.getString(R.string.label_details_intensity4);
                    break;
            }
            view.setText(intensity);
        } else {
            blockIntensity.setVisibility(View.GONE);
            viewLabel.setVisibility(View.GONE);
            view.setVisibility(View.GONE);
            view.setText("\u2014");
        }

        viewLabel = listItem.findViewById(R.id.label_item_notes);
        view = listItem.findViewById(R.id.item_notes);
        if (currentEntry.notes.isEmpty()) {
            viewLabel.setVisibility(View.GONE);
            view.setVisibility(View.GONE);
            view.setText("\u2014");
        }
        else {
            view.setText(currentEntry.notes);
        }

        viewLabel = listItem.findViewById(R.id.label_item_events);
        view = listItem.findViewById(R.id.item_event);
        if (textEvents.isEmpty()) {
            viewLabel.setVisibility(View.GONE);
            view.setVisibility(View.GONE);
            view.setText("\u2014");
        } else {
            view.setText(textEvents);
        }

        viewLabel = listItem.findViewById(R.id.label_item_mood);
        view = listItem.findViewById(R.id.item_mood);
        if (textMood.isEmpty()) {
            viewLabel.setVisibility(View.GONE);
            view.setVisibility(View.GONE);
            view.setText("\u2014");
        } else {
            view.setText(textMood);
        }

        viewLabel = listItem.findViewById(R.id.label_item_symptoms);
        view = listItem.findViewById(R.id.item_symptom);
        if (textSymptoms.isEmpty()) {
            viewLabel.setVisibility(View.GONE);
            view.setVisibility(View.GONE);
            view.setText("\u2014");
        } else {
            view.setText(textSymptoms);
        }

        return listItem;
    }
}
