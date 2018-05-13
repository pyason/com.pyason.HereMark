package com.pyason.heremark;

import android.Manifest;
import android.app.Activity;
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
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.List;


public class MainActivity extends AppCompatActivity {

    protected GeoDataClient mGeoDataClient;
    protected PlaceDetectionClient mPlaceDetectionClient;
    protected FusedLocationProviderClient mFusedLocationProviderClient;
    protected LocationRequest mLocationRequest;
    protected LocationCallback mLocationCallback;
    private MainList listFragment;
    private FragmentManager mFragmentManager;

    protected Location currentLocation, selectedLocation;
    protected Place selectedPlace;
    protected double maxDistance = 1000;

    private final String FRAGMENT_TAG = "main_list_tag";
    private final String G_PLACE_SERVICE_ERROR = "g_places_service_error";
    private final String LOC_SETTING_ERROR = "location_setting_error";
    private final String PERMISSIONS = "permissions";
    private static final int REQUEST_LOCATION = 0;
    protected static final int REQUEST_CHECK_SETTINGS = 1;
    protected final int PLACE_PICKER_REQUEST = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGeoDataClient = Places.getGeoDataClient(this);
        mPlaceDetectionClient = Places.getPlaceDetectionClient(this);

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(60000);
        mLocationRequest.setFastestInterval(10000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                List<Location> locationList = locationResult.getLocations();
                if (locationList.size()>0) {
                    //Last location is the latest
                    currentLocation = locationList.get(locationList.size() - 1);
                }
            }
        };

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

        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkLocationSettings(MainActivity.this);

                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

                    Log.i(PERMISSIONS, "Location Permission not granted, request for permission.");
                    // Should we show an explanation?
                    PermissionUtil.requestForPermissions(MainActivity.this, listFragment, REQUEST_LOCATION);

                }
                else if (checkAirPlaneMode(getApplicationContext())) {
                    Snackbar.make(findViewById(R.id.coordinator_layout), R.string.airplane_mode_on, Snackbar.LENGTH_LONG).show();
                }
                else if (!checkNetwork()) {
                    Snackbar.make(findViewById(R.id.coordinator_layout), R.string.network_unavailable, Snackbar.LENGTH_LONG).show();
                }
                else {
                    PlacePicker.IntentBuilder placeBuilder = new PlacePicker.IntentBuilder();
                    mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);

                    try{
                        startActivityForResult(placeBuilder.build(MainActivity.this), PLACE_PICKER_REQUEST);
                    }
                    catch (GooglePlayServicesNotAvailableException e) {
                        Log.e(G_PLACE_SERVICE_ERROR, "Google places service not availabe...");
                        Snackbar.make(findViewById(R.id.coordinator_layout), R.string.service_not_available, Snackbar.LENGTH_LONG).show();
                    }
                    catch (GooglePlayServicesRepairableException er) {
                        Log.e(G_PLACE_SERVICE_ERROR, "Google places service may not be installed or up to date...");
                        Snackbar.make(findViewById(R.id.coordinator_layout), R.string.install_update_play_service, Snackbar.LENGTH_LONG).show();
                        //TODO: show installation dialog to user
                    }
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

    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mFusedLocationProviderClient != null) {
            mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            Log.i(PERMISSIONS, "Location Permission not granted, request for permission.");
            // Should we show an explanation?
            PermissionUtil.requestForPermissions(MainActivity.this, listFragment, REQUEST_LOCATION);

        }
        mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
    }

    protected void showLocationSettings() {
        AlertDialog.Builder settingDialog = new AlertDialog.Builder(MainActivity.this);

        settingDialog.setTitle(R.string.enable_location_title);
        settingDialog.setMessage(R.string.enable_location);

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
                            Log.e(LOC_SETTING_ERROR, "Location settings resolution callback failed.");
                        }
                        break;

                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        Log.e(LOC_SETTING_ERROR, "Location setting change is unavailable for unknown error.");
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                String displayNameAddress;
                selectedPlace = PlacePicker.getPlace(this, data);

                if (currentLocation != null && selectedPlace != null) {

                    selectedLocation = new Location("selectedLocation");
                    selectedLocation.setLongitude(selectedPlace.getLatLng().longitude);
                    selectedLocation.setLatitude(selectedPlace.getLatLng().latitude);

                    if (selectedLocation.distanceTo(currentLocation) > maxDistance) {
                        Snackbar.make(findViewById(R.id.coordinator_layout), R.string.select_nearby, Snackbar.LENGTH_LONG).show();
                    } else {
                        displayNameAddress = new StringBuilder().append(selectedPlace.getName()).append(" - ").append(selectedPlace.getAddress()).toString();
                        listFragment.list.add(displayNameAddress);
                        listFragment.mAdapter.notifyDataSetChanged();
                    }
                }
            }
        }
    }
}
