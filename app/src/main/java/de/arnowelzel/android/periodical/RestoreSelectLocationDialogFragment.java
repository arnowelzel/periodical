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
