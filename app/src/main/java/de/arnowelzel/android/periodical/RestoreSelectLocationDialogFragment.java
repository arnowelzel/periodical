/*
 * Periodical dialog fragment for restore location selection
 * Copyright (C) 2012-2024 Arno Welzel
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

public class RestoreSelectLocationDialogFragment extends DialogFragment {
    public static final String TAG = "RestoreSelectLocationDialog";
    public static final String RESULT_BUNDLE_KEY = "resultRestoreSelectLocation";
    public static final String RESULT_CHOICE = "choice";
    public static final int CHOICE_CANCEL = 0;
    public static final int CHOICE_OK = 1;
    public static final int CHOICE_HELP = 2;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.restore_title))
            .setMessage(getResources().getString(R.string.restore_selectfolder))
            .setIcon(R.drawable.ic_warning_black_40dp)
            .setPositiveButton(
                getResources().getString(R.string.backup_ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Bundle result = new Bundle();
                        result.putInt(RESULT_CHOICE, CHOICE_OK);
                        getParentFragmentManager().setFragmentResult(RESULT_BUNDLE_KEY, result);
                    }
                })
            .setNeutralButton(
                    getResources().getString(R.string.backup_help),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Bundle result = new Bundle();
                            result.putInt(RESULT_CHOICE, CHOICE_HELP);
                            getParentFragmentManager().setFragmentResult(RESULT_BUNDLE_KEY, result);
                        }
                    })
            .setNegativeButton(
                getResources().getString(R.string.backup_cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Bundle result = new Bundle();
                        result.putInt(RESULT_CHOICE, CHOICE_CANCEL);
                        getParentFragmentManager().setFragmentResult(RESULT_BUNDLE_KEY, result);
                    }
                })
            .create();
    }
}
