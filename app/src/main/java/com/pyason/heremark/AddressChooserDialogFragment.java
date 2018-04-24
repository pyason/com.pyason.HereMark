package com.pyason.heremark;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class AddressChooserDialogFragment extends DialogFragment {

    private char[] listItems;

    @Override
    public Dialog onCreateDialog(Bundle savedInstancestate) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.select_location);
        AlertDialog dialog = builder.create();

        return dialog;
    }
}
