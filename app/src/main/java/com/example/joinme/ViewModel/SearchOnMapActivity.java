package com.example.joinme.ViewModel;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.joinme.Model.Category;
import com.example.joinme.Model.Contact;
import com.example.joinme.Model.api.RetrofitClient;
import com.example.joinme.R;
//import com.example.joinme.databinding.ActivityMainPageBinding;
//import com.example.joinme.databinding.ActivityMapsBinding;
import com.example.joinme.databinding.ActivityMapsBinding;
import com.example.joinme.databinding.ActivitySearchOnMapBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.maps.GoogleMap.OnCameraIdleListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import org.checkerframework.checker.nullness.qual.NonNull;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.GeoPoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.os.Bundle;

public class SearchOnMapActivity extends AppCompatActivity implements OnMapReadyCallback, OnMapClickListener {
    private GoogleMap mMap;
    private static final String TAG = MapsActivity.class.getSimpleName();
    private  boolean locationPermissionGranted;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location lastKnownLocation;
    // The entry point to the Fused Location Provider.
    private FusedLocationProviderClient fusedLocationProviderClient;
    // A default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    private final LatLng defaultLocation = new LatLng(-33.8523341, 151.2106085);
    private static final int DEFAULT_ZOOM = 15;
    private ActivitySearchOnMapBinding binding;
    androidx.constraintlayout.widget.ConstraintLayout parent;
    // creating a variable
    // for search view.
    SearchView searchView;
    String[] addressArr;
    String[] groupsIdArr;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //The LayoutInflater takes an XML file as input and builds the View objects from it.
        binding = ActivitySearchOnMapBinding.inflate(getLayoutInflater());
        //Set the activity content to an explicit view. This view is placed directly into the activity's view hierarchy
        setContentView(binding.getRoot());
        parent = findViewById(R.id.searchMapPage);
        // initializing our search view.
        searchView = findViewById(R.id.searchMapView);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.Searchmap);
        mapFragment.getMapAsync(this);
//        mapFragment.getMapAsync(this);
        // adding on query listener for our search view.
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // on below line we are getting the
                // location name from search view.
                String location = searchView.getQuery().toString();

                // below line is to create a list of address
                // where we will store the list of all address.
                List<Address> addressList = null;

                // checking if the entered location is null or not.
                if (location != null || location.equals("")) {
                    // on below line we are creating and initializing a geo coder.
                    Geocoder geocoder = new Geocoder(SearchOnMapActivity.this);
                    try {
                        // on below line we are getting location from the
                        // location name and adding that location to address list.
                        addressList = geocoder.getFromLocationName(location, 1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // on below line we are getting the location
                    // from our list a first position.
                    Address address = addressList.get(0);

                    // on below line we are creating a variable for our location
                    // where we will add our locations latitude and longitude.
                    LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());

                    // below line is to animate camera to that position.
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });


    }

    public LatLng getLocationFromAddress(Context context,String strAddress) {

        Geocoder coder = new Geocoder(context);
        List<Address> address;
        LatLng p1 = null;

        try {
            address = coder.getFromLocationName(strAddress, 5);
            if (address == null) {
                return null;
            }
            Address location = address.get(0);
            location.getLatitude();
            location.getLongitude();

            p1 = new LatLng(location.getLatitude(), location.getLongitude() );

        } catch (Exception ex) {

            ex.printStackTrace();
        }

        return p1;
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     *
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        UiSettings setting = this.mMap.getUiSettings();
        setting.setZoomControlsEnabled(true);
        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();
        // Get the current location of the device and set the position of the map.
        getDeviceLocation();
        Intent intent = getIntent();
        String title = intent.getStringExtra("Title");
        Call<ArrayList<Contact>> call = RetrofitClient.getInstance().getAPI().getGroups(title);
        call.enqueue(new Callback<ArrayList<Contact>>() { //todo: this not work. the array dont update in time. need to check if is pass to the details page when click on the marker and after add join button in details page.
            @Override
            public void onResponse(Call<ArrayList<Contact>> call, Response<ArrayList<Contact>> response) {
                addressArr = new String[response.body().size()];
                groupsIdArr = new String[response.body().size()];
                for(int i = 0; i < response.body().size(); i++){
                    addressArr[i] = response.body().get(i).getAddress();
                    groupsIdArr[i] = response.body().get(i).getId();
                    //  String address = response.body().get(i).getAddress();
//                    LatLng pos = getLocationFromAddress(getApplicationContext(),address);
//                    Marker marker = mMap.addMarker(new MarkerOptions().position(pos));
//                    marker.setTag(response.body().get(i).getId());
//                    mMap.setOnMarkerClickListener(this);

                }
            }

            @Override
            public void onFailure(Call<ArrayList<Contact>> call, Throwable t) {
                Log.d("Fail", t.getMessage());
            }
        });
        for(int i=0; i<addressArr.length; i++){
            LatLng pos = getLocationFromAddress(getApplicationContext(),addressArr[i]);
            Marker marker = mMap.addMarker(new MarkerOptions().position(pos));
            marker.setTag(groupsIdArr[i]);
//            mMap.setOnMarkerClickListener(this);
        }
        // at last we calling our map fragment to update.
//        mapFragment.getMapAsync(this);
        this.mMap.setOnMapClickListener(this);
    }
    public boolean onMarkerClick(final Marker marker) {

        String gid = marker.getTag().toString();
        Intent intent = new Intent(SearchOnMapActivity.this, GroupDetailsActivity.class);
        intent.putExtra("ID", gid);
        intent.putExtra("from", "map");
        startActivity(intent);

        // Return false to indicate that we have not consumed the event and that we wish
        // for the default behavior to occur (which is for the camera to move such that the
        // marker is centered and for the marker's info window to open, if it has one).
        return false;
    }
    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        locationPermissionGranted = false;
        if (requestCode
                == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = true;
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
        updateLocationUI();
    }

    /**
     * Updates the map's UI settings based on whether the user has granted location permission.
     */
    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (locationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                lastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (locationPermissionGranted) {
                Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            lastKnownLocation = task.getResult();
                            if (lastKnownLocation != null) {
                                LatLng last = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        new LatLng(lastKnownLocation.getLatitude(),
                                                lastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                            }
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            mMap.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage(), e);
        }
    }

    @Override
    public void onMapClick(LatLng point) {
//        Marker marker = mMap.addMarker(new MarkerOptions()
//                .position(point).draggable(true)
//                .title("my new group"));
//        LatLng pos = marker.getPosition();
//        Geocoder geocoder;
//        List<Address> addresses;
//        geocoder = new Geocoder(this, Locale.getDefault());
//
//        try {
//            addresses = geocoder.getFromLocation(pos.latitude, pos.longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
//            String address = addresses.get(0).getAddressLine(0);
//            String city = addresses.get(0).getLocality();
//            onButtonShowPopupWindowClick(address, city, marker);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }


    }

    public void onButtonShowPopupWindowClick(String address, String city, Marker marker) {
        // inflate the layout of the popup window
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        //Inflate a new view hierarchy from the specified xml resource.
        View popupView = inflater.inflate(R.layout.choose_location_popup, null);

        //confirm the deletion of the user
        TextView tvMsg = popupView.findViewById(R.id.msgTxt);
        Button yesBtn = popupView.findViewById(R.id.yesBtn);
        Button noBtn = popupView.findViewById(R.id.noBtn);
        tvMsg.setText("you choose the location " + address);

        // create the popup window
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        boolean focusable = true; // lets taps outside the popup also dismiss it

        //This class represents a popup window that can be used to display an arbitrary view.
        //The popup window is a floating container that appears on top of the current activity.
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);

        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window token
        popupWindow.showAtLocation(parent, Gravity.CENTER, 0, 0);
        yesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /**
                 * If we click Yes in the window, the user enters the collection of the blocked users
                 */
                popupWindow.dismiss();
                Log.d(TAG, "yes");
                Intent intent = new Intent(SearchOnMapActivity.this, OpenGroupActivity.class);
                intent.putExtra("Address", address);
                intent.putExtra("City", city);
                startActivity(intent);
            }
        });

        noBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /**
                 * If we click in the window no, the window will close
                 */
                Log.d(TAG, "no");
                popupWindow.dismiss();
                marker.remove();
            }
        });

    }
}