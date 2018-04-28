package com.pyason.heremark;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import android.app.Activity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.text.DateFormat;
import java.util.Date;

import me.drakeet.materialdialog.MaterialDialog;


public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation, mCurrentLocation;
    protected LocationRequest mLocationRequest;
    private MainList listFragment;
    private FragmentManager mFragmentManager;
    //private boolean mRequestStatus = true;
    private MaterialDialog dialog;

    private double currentLatitude, currentLongitude;
    protected String lastUpdateTime, addressMessage = null;
    protected CharSequence[] addressMessageList = null;
    private AddressResultReceiver resultReceiver;
    public boolean mAddressRequested = false;

    private final String FRAGMENT_TAG = "main_list_tag";
    private final String SELECT_ADDR_TAG = "addr_select_tag";
    private static final int REQUEST_LOCATION = 0;
    protected static final int REQUEST_CHECK_SETTINGS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addOnConnectionFailedListener(this)
                .addConnectionCallbacks(this)
                .build();

        mFragmentManager = getFragmentManager();

        listFragment = (MainList) mFragmentManager.findFragmentByTag(FRAGMENT_TAG);

        // If the Fragment is non-null, then it is currently being
        // retained across a configuration change.
        if (listFragment == null) {
            listFragment = new MainList();
            FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
            fragmentTransaction.add(R.id.list_frame, listFragment, FRAGMENT_TAG).commit();
        }


        Toolbar toolbar = findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);

        FloatingActionButton floatingActionButton = findViewById(R.id.fab_button);

        //LayoutInflater inflater = LayoutInflater.from(getApplicationContext());

        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkLocationSettings(MainActivity.this);
                
                if (checkAirPlaneMode(getApplicationContext())) {
                    Snackbar.make(findViewById(R.id.coordinator_layout), "Air Plane Mode is ON, Please turn off to get location", Snackbar.LENGTH_LONG).show();
                }
                else if (!checkNetwork()) {
                    Snackbar.make(findViewById(R.id.coordinator_layout), "Unable to reach network, please check network settings", Snackbar.LENGTH_LONG).show();
                }
                else {
                    mAddressRequested = true;

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle(R.string.select_location)
                            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    //AddressChooserDialogFragment.this.getDialog().dismiss();
                                }
                            })
                            .setItems(addressMessageList, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    listFragment.list.add((String)addressMessageList[i]);
                                    listFragment.mAdapter.notifyDataSetChanged();
                                }
                            });
                    builder.create().show();
                }
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addOnConnectionFailedListener(this)
                    .addConnectionCallbacks(this)
                    .build();
        }
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);

    }

    @Override
    public void onResume() {
        super.onResume();
        if(mGoogleApiClient.isConnected()) {
            LocationRequest locationRequest = createLocationRequest();
            startLocationUpdates(locationRequest);
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
/*        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            Log.i("Permissions", "Location Permission not granted, request for permission.");
            // Should we show an explanation?
            PermissionUtil.requestForPermissions(this, listFragment, REQUEST_LOCATION);

        }else {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        }

        if (mLastLocation != null) {
            Log.i("Main", "Last location exists");
        }*/

        LocationRequest locationRequest = createLocationRequest();
        startLocationUpdates(locationRequest);

        if (mAddressRequested) {
            startAddressLookUp();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {


    }

    @Override
    public void onConnectionSuspended(int i) {

        // Making sure GoogleAPIClient is connected

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addOnConnectionFailedListener(this)
                    .addConnectionCallbacks(this)
                    .build();
        }
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        currentLatitude = mCurrentLocation.getLatitude();
        currentLongitude = mCurrentLocation.getLongitude();
        lastUpdateTime = DateFormat.getDateTimeInstance().format(new Date());
    }


    protected LocationRequest createLocationRequest() {

        mLocationRequest = LocationRequest.create();

        mLocationRequest.setInterval(8000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return mLocationRequest;
    }

    protected void startLocationUpdates(LocationRequest request) {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            Log.i("Permissions", "Location Permission not granted, request for permission.");
            // Should we show an explanation?
            PermissionUtil.requestForPermissions(this, listFragment, REQUEST_LOCATION);

        } else{
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, request, this);
        }
    }

    protected void startAddressLookUp() {
        Intent intent = new Intent(this,AddressLookUpService.class);
        resultReceiver = new AddressResultReceiver(new Handler());
        intent.putExtra(AddressLookUpService.Constants.RECEIVER, resultReceiver);
        intent.putExtra(AddressLookUpService.Constants.LOCATION_DATA_EXTRA, mCurrentLocation);
        startService(intent);
    }

    protected void showLocationSettings() {
        AlertDialog.Builder settingDialog = new AlertDialog.Builder(MainActivity.this);

        settingDialog.setTitle("Location is not enabled");
        settingDialog.setMessage("Location setting is not enabled, do you want to go and set it?");

        settingDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                MainActivity.this.startActivity(intent);
            }
        });

        settingDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        settingDialog.show();
    }

    public boolean checkAirPlaneMode(Context context) {
            return Settings.Global.getInt(context.getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    public boolean checkNetwork() {
       /* boolean wifiConnected = false;
        boolean mobileConnected = false;*/

        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    public void checkLocationSettings(final Activity activity) {

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        builder.setAlwaysShow(true);
        SettingsClient settingsClient = LocationServices.getSettingsClient(activity);
        Task<LocationSettingsResponse> locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build());

        locationSettingsResponseTask.addOnSuccessListener(activity, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                startAddressLookUp();
            }
        });

        locationSettingsResponseTask.addOnFailureListener(activity, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                int statusCode = ((ApiException) e).getStatusCode();

                switch (statusCode) {
                    case CommonStatusCodes.RESOLUTION_REQUIRED:
                        //Location settings does not meet requirement, request for location settings change
                        try {
                            ResolvableApiException resolvable = (ResolvableApiException) e;
                            resolvable.startResolutionForResult(activity, REQUEST_CHECK_SETTINGS);
                        }
                        catch (IntentSender.SendIntentException senderException) {
                            Log.i("LocationSetting", "Location settings resolution callback failed.");
                        }
                        break;

                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        Log.i("LocationSettings", "Location setting change is unavailable for unkonwn error.");
                }
            }
        });

/*        int locationMode = 0;
        String locationProviders;

        try {
            locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }

        return locationMode != Settings.Secure.LOCATION_MODE_OFF && locationMode != Settings.Secure.LOCATION_MODE_SENSORS_ONLY;*/
    }

    @SuppressLint("ParcelCreator")
    public class AddressResultReceiver extends ResultReceiver {
        AddressResultReceiver(Handler handler) {
            super(handler);
        }

        //Parcelable.Creator CREATOR = ResultReceiver.CREATOR;

        @Override
        protected void onReceiveResult(int resultCode, Bundle result) {
            if (resultCode == AddressLookUpService.Constants.SUCCESS) {
                //addressMessage = result.getString(AddressLookUpService.Constants.RESULT_DATA_KEY);
                addressMessageList = result.getCharSequenceArray(AddressLookUpService.Constants.RESULT_DATA_KEY);
            }
        }
    }
}
