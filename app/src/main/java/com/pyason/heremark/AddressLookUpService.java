package com.pyason.heremark;

import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.location.LocationListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.IllegalFormatFlagsException;
import java.util.List;
import java.util.Locale;

/**
 * Created by paulyason on 2015-07-09.
 */
public class AddressLookUpService extends IntentService {

    private Location location;
    private ResultReceiver receiver;
    private Geocoder geocoder;
    public static final String TAG = "AddressLookUpService";

    public AddressLookUpService() {
        super("AddressLookUpService");
    }


    @Override
    protected void onHandleIntent (Intent intent) {
        geocoder = new Geocoder(this, Locale.getDefault());
        String errorMsg = "";

        location = intent.getParcelableExtra(Constants.LOCATION_DATA_EXTRA);

        receiver  = intent.getParcelableExtra(Constants.RECEIVER);

        if (location == null) return;

        List<Address> addressList = null;

        try{
            addressList = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
        }
        catch (IOException ioException) {
            errorMsg = getString(R.string.service_not_available);
            Log.e(TAG, errorMsg, ioException);
        }
        catch (IllegalArgumentException illegalArgumentException) {
            errorMsg = getString(R.string.invalid_lat_long);
            Log.e(TAG, errorMsg + ". " +
                    "Latitude = " + location.getLatitude() +
                    ", Longitude = " +
                    location.getLongitude(), illegalArgumentException);
        }

        if (addressList == null || addressList.size() == 0) {
            if (errorMsg.isEmpty()) {
                errorMsg = getString(R.string.no_address_found);
                Log.e(TAG, errorMsg);
            }

            if (location == null) {
                if (errorMsg.isEmpty()) {
                    errorMsg = "Location is null";
                    Log.e(TAG, errorMsg);
                }
            }

            deliverResultToReceiver(Constants.FAIL, errorMsg);
        }
        else {
            ArrayList<String> addressFragments = new ArrayList<String>();

            /*for (Address address : addressList) {
                for (int i=0; i<address.getMaxAddressLineIndex(); i++) {
                    addressFragments.add(address.getAddressLine(i));
                }
            }*/

            Address address = addressList.get(0);

            for (int i=0; i<=address.getMaxAddressLineIndex(); i++) {
                addressFragments.add(address.getAddressLine(i));
            }

            Log.i(TAG, getString(R.string.address_found) + address.getAddressLine(0));

            String addrsssss = TextUtils.join(System.getProperty("line.separator"), addressFragments);
            deliverResultToReceiver(Constants.SUCCESS, TextUtils.join(System.getProperty("line.separator"), addressFragments));
        }
    }

    public final class Constants {
        public static final int SUCCESS = 0;
        public static final int FAIL = 1;
        public static final String PACKAGE_NAME = "com.pyason.heremark";
        public static final String RECEIVER = PACKAGE_NAME + ".RECEIVER";
        public static final String RESULT_DATA_KEY = PACKAGE_NAME + ".RESULT_DATA_KEY";
        public static final String LOCATION_DATA_EXTRA = PACKAGE_NAME + ".LOCATION_DATA_EXTRA";
    }

    private void deliverResultToReceiver(int resultCode, String message) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.RESULT_DATA_KEY, message);
        receiver.send(resultCode, bundle);
    }

}
