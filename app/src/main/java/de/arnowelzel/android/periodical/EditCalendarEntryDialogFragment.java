/*
 * Periodical dialog fragment for calendar entry details
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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class EditCalendarEntryDialogFragment extends DialogFragment {
    public static final String TAG = "EditCalendarEntryDialog";
    public static final String RESULT_BUNDLE_KEY = "resultEditCalendarEntry";
    public static final String RESULT_CHOICE = "choice";
    public static final String RESULT_TYPE = "type";
    public static final String RESULT_YEAR = "year";
    public static final String RESULT_MONTH = "month";
    public static final String RESULT_DAY = "day";
    public static final int CHOICE_CANCEL = 0;
    public static final int CHOICE_OK = 1;
    public static final int CHOICE_DETAILS = 2;
    public static final int TYPE_ADD = 1;
    public static final int TYPE_REMOVE_PERIOD = 2;
    public static final int TYPE_REMOVE = 3;

    private int type;
    private int year;
    private int month;
    private int day;

    static EditCalendarEntryDialogFragment newInstance(int type, int year, int month, int day) {
        EditCalendarEntryDialogFragment fragment = new EditCalendarEntryDialogFragment();

        Bundle args = new Bundle();
        args.putInt(RESULT_TYPE, type);
        args.putInt(RESULT_DAY, day);
        args.putInt(RESULT_MONTH, month);
        args.putInt(RESULT_YEAR, year);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        assert getArguments() != null;
        type = getArguments().getInt(RESULT_TYPE);
        year = getArguments().getInt(RESULT_YEAR);
        month = getArguments().getInt(RESULT_MONTH);
        day = getArguments().getInt(RESULT_DAY);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        int messageId = R.string.calendaraction_add;
        switch (type) {
            case TYPE_REMOVE_PERIOD:
                messageId = R.string.calendaraction_removeperiod;
                break;
            case TYPE_REMOVE:
                messageId = R.string.calendaraction_remove;
                break;
        }
        String message = getResources().getString(messageId);

        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.calendaraction_title))
            .setMessage(message)
            .setIcon(R.drawable.ic_warning_black_40dp)
            .setPositiveButton(
                getResources().getString(R.string.backup_ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setResultChoice(CHOICE_OK);
                    }
                })
            .setNeutralButton(
                getResources().getString(R.string.calendaraction_details),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setResultChoice(CHOICE_DETAILS);
                    }
                })
            .setNegativeButton(
                getResources().getString(R.string.backup_cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setResultChoice(CHOICE_CANCEL);
                    }
                })
            .create();
    }

    private void setResultChoice(int choice)
    {
        Bundle result = new Bundle();
        result.putInt(RESULT_TYPE, this.type);
        result.putInt(RESULT_YEAR, this.year);
        result.putInt(RESULT_MONTH, this.month);
        result.putInt(RESULT_DAY, this.day);
        result.putInt(RESULT_CHOICE, choice);
        getParentFragmentManager().setFragmentResult(RESULT_BUNDLE_KEY, result);
    }
}
