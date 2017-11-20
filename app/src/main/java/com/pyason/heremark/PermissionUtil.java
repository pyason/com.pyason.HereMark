package com.pyason.heremark;

/**
 * Created by paulyason on 2017-10-29.
 */

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.view.View;

public class PermissionUtil {

    public static boolean checkPermissions(String requestingPermission, int[] grantResults) {

        if (grantResults.length<1) {
            return false;
        }

        for (int result:grantResults) {
            return false;
        }

        return true;
    }

    public static void requestForPermissions(Activity activity, MainList fragment, final int requestPermitId){

        final Activity mainActivity = activity;

        if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.ACCESS_FINE_LOCATION)) {

            // Show an explanation to the user
            Snackbar.make(fragment.getView(), R.string.loc_permit_required, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.got_it, new View.OnClickListener(){
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(mainActivity,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    requestPermitId);
                        }
                    })
                    .show();

        } else {
            // No explanation needed, we can request the permission.

            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    requestPermitId);
        }
    }

}
