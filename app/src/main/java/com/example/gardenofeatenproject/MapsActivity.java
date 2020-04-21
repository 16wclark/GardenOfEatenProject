package com.example.gardenofeatenproject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.core.app.ActivityCompat;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.os.StrictMode;
import android.Manifest;
import android.location.LocationManager;
import android.location.Location;


import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.*;




import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationListener;



import com.yelp.fusion.client.connection.YelpFusionApi;
import com.yelp.fusion.client.connection.YelpFusionApiFactory;
import com.yelp.fusion.client.models.AutoComplete;
import com.yelp.fusion.client.models.Business;
import com.yelp.fusion.client.models.Reviews;
import com.yelp.fusion.client.models.SearchResponse;


import com.google.firebase.database.*;


import android.content.pm.PackageManager;

import org.json.JSONObject;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, GoogleMap.OnMarkerClickListener  {

    final static String tag = "MapsActivity";
    private GoogleMap mMap;

    protected Location mCurrentLocation;
    public static GoogleApiClient mGoogleApiClient;

    private TextView mTextView;
    private TextView mPlace;
    private MarkerOptions options = new MarkerOptions();
    private ArrayList<String> names = new ArrayList<>();
    private ArrayList<String> nameID = new ArrayList<>();
    private ArrayList<LatLng> latlngs = new ArrayList<>();
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;
    private FirebaseAuth mAuth;
    private List<String> visitedPlaces = new ArrayList<>();
    private String currentPlace;
    private boolean signedIn;
    public double mLat;
    public double mLong;
    public FirebaseUser userFirebase;
    public boolean firstCall = true;



    YelpFusionApiFactory yelpFusionApiFactory;
    YelpFusionApi yelpFusionApi;
    String apiKey = "k8fR4cYB2UhpwApEQxh55MAo1c3AlRxL78u0OeGgQf3EI1CKZp4FTY4K3Wahng0Yvhje6NXTmUW5LTJqu2mPNP6e2vSSpYeHYJJwExgtOmo9K6Q60wA4UUKFAzzMXXYx";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getCurrentLocation();
        buildGoogleApiClient();
        mGoogleApiClient.connect();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        findViewById(R.id.sign_in_button).setOnClickListener((view) -> { signIn(); });
        findViewById(R.id.visitedButton).setOnClickListener((view) -> { visitedPlace(); });
        findViewById(R.id.buttonPush).setOnClickListener((view) -> { updateUI(userFirebase); });

        mTextView = findViewById(R.id.mTextId);
        mPlace = findViewById(R.id.mPlace);
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        updateRestaraunts();


        mAuth = FirebaseAuth.getInstance();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    private void signIn() {


        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
        if(!signedIn)
            visitedPlaces.clear();
        signedIn = true;
        markerUpdate();
    }

    private void visitedPlace() {
        if (!visitedPlaces.contains(currentPlace)) {
            visitedPlaces.add(currentPlace);
            mPlace.setText(visitedPlaces.toString());
        }
        markerUpdate();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w( "Google sign in failed", e);
                // ...
            }
        }
    }
    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            userFirebase = mAuth.getCurrentUser();
                            loginUpdate(userFirebase);
                        } else {
                        }
                    }
                });
    }



    private void updateUI(FirebaseUser user) {

        if (user != null) {
                String name = user.getDisplayName();
                String email = user.getEmail();


            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference myRef = database.getReference("RegisteredUsers");
            final String emailFinal = email.replaceAll("\\.", ",");
            Map<String, Object> userEnter = new HashMap<>();
            User a = new User(visitedPlaces);
            userEnter.put(emailFinal, a);
            DatabaseReference usersRef = myRef.child("users");
            a.setVisitedPlaces(visitedPlaces);
            usersRef.updateChildren(userEnter);
                mTextView.setText("Logged in as" + name);
        } else {

        }
    }

    private void loginUpdate(FirebaseUser user) {
        if (user != null) {
            String email = user.getEmail();
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            final String emailFinal = email.replaceAll("\\.", ",");
            DatabaseReference myRef2 = database.getReference("RegisteredUsers/users/" + emailFinal);


            myRef2.addChildEventListener(new ChildEventListener() {
                // Retrieve new posts as they are added to Firebase
                @Override
                public void onChildAdded(DataSnapshot snapshot, String previousChildKey) {
                    if(firstCall) {
                        GenericTypeIndicator<ArrayList<String>> t = new GenericTypeIndicator<ArrayList<String>>() {
                        };
                        ArrayList<String> allData = (ArrayList) snapshot.getValue(t);
                        System.out.println("Titl1e: " + allData);
                        visitedPlaces = allData;
                        firstCall = false;
                        markerUpdate();
                    }

                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {

                    // ...
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
        }
    }



    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                signIn();
                break;
            // ...
        }
    }


    @Override
    public void onStart(){
        super.onStart();
        if(this.mGoogleApiClient != null){
            this.mGoogleApiClient.connect();
        }

    }



    public MapsActivity() throws IOException {
        yelpFusionApiFactory = new YelpFusionApiFactory();
        yelpFusionApi = yelpFusionApiFactory.createAPI(apiKey);



    }
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

    }


    private void getCurrentLocation(){
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationServices.getFusedLocationProviderClient(MapsActivity.this)
                .requestLocationUpdates(locationRequest, new LocationCallback(){
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        super.onLocationResult(locationResult);
                        LocationServices.getFusedLocationProviderClient(MapsActivity.this)
                                .removeLocationUpdates(this);
                        if (locationResult != null && locationResult.getLocations().size() > 0) {
                            int latestLocationIndex = locationResult.getLocations().size() - 1;
                            double latitude =
                                    locationResult.getLocations().get(latestLocationIndex).getLatitude();
                            mLat = latitude;
                            double longitude =
                                    locationResult.getLocations().get(latestLocationIndex).getLongitude();
                            mLong = longitude;
                        }
                    }
                }, Looper.getMainLooper());
    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        getCurrentLocation();
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);
        mMap.setMinZoomPreference(14.0f);
        mMap.setMyLocationEnabled(true);
        LatLng currentLoc = new LatLng(mLat, mLong);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLoc, 3));
        mMap.setOnMarkerClickListener(this);
        assembleMap();
    }




    public void updateRestaraunts(){
        Map<String, String> params = new HashMap<>();
        params.put("term", "restaurants");
        params.put("latitude", Double.toString(mLat));
        params.put("longitude", Double.toString(mLong));
        Call<SearchResponse> call = yelpFusionApi.getBusinessSearch(params);
        SearchResponse searchResponse;
        String businessName = "empty";
        try {
            searchResponse = call.execute().body();
            ArrayList<Business> businesses = searchResponse.getBusinesses();
        } catch(Exception ex)
        { }

    }
    public void populateLatLng(double currentLat, double currentLong) {
        latlngs.clear();
        ArrayList<Business> businesses = null;
        Map<String, String> params = new HashMap<>();
        params.put("latitude", Double.toString(currentLat));
        params.put("longitude", Double.toString(currentLong));
        Call<SearchResponse> call = yelpFusionApi.getBusinessSearch(params);
        SearchResponse searchResponse;
        try {
            searchResponse = call.execute().body();
            businesses = searchResponse.getBusinesses();
        } catch (Exception ex) {
        }


            for (int i = 0; i < 15; i++) {
                try {
                    latlngs.add(new LatLng(businesses.get(i).getCoordinates().getLatitude(), businesses.get(i).getCoordinates().getLongitude()));
                } catch (Exception ex) {
                }
                try {
                    names.add(businesses.get(i).getName());
                } catch (Exception ex) {
                }
                try {
                    nameID.add(businesses.get(i).getUrl());
                } catch (Exception ex) {
                }
            }

    }



    public void markerUpdate(){
        int counter = 0;
        mMap.clear();
        for (LatLng point : latlngs) {
            options.position(point);
            if(visitedPlaces.contains(names.get(counter)))
                options.icon(BitmapDescriptorFactory.fromResource(R.drawable.rsz_colorchange));
            else
                options.icon(BitmapDescriptorFactory.fromResource(R.drawable.rsz_cube));
            options.title(names.get(counter));
            //options.title(names[counter]);
            mMap.addMarker(options);
            counter++;
        }
        counter = 0;


        mMap.moveCamera(CameraUpdateFactory.newLatLng(latlngs.get(1)));

    }
    public void assembleMap(){
        populateLatLng(mLat, mLong);
        markerUpdate();
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i("latlng", " " + location.getLongitude());

        mCurrentLocation = location;
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {

        // Retrieve the data from the marker.
        Integer clickCount = (Integer) marker.getTag();
        currentPlace = marker.getTitle();


        // Check if a click count was set, then display the click count.
        if (clickCount != null) {
            clickCount = clickCount + 1;
            marker.setTag(clickCount);
            Toast.makeText(this,
                    marker.getTitle() +
                            " has been clicked " + clickCount + " times.",
                    Toast.LENGTH_SHORT).show();
        }

        return false;
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i("LocationFragment", "Connection failed: ConnectionResult.getErrorCode() " + result.getErrorCode());
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i("LocationFragment", "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onDestroy() {
        Log.i("Destroyed", "Connection destroyed");
        mGoogleApiClient.disconnect();
        super.onDestroy();
    }
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i("onconnected", "GoogleApiClient connected!");
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location inputLocation) {
                        if (inputLocation != null) {
                        }
                    }
                });
        Log.i("location", " Location: " + mCurrentLocation); //may return **null** because, I can't guarantee location has been changed immmediately
    }
}
/* This code uses examples from
https://github.com/benawad/Munchy
https://github.com/Yelp/yelp-android
https://github.com/ranga543/yelp-fusion-android
https://github.com/googlemaps/android-samples/tree/master/tutorials/StyledMap
https://github.com/sean-park/yelp-fusion (didn't use code, but used as a reference)
https://stackoverflow.com/questions/28954421/android-cannot-resolve-method-requestlocationupdates-fusedlocationproviderapi
https://stackoverflow.com/questions/48633859/fusedlocationapi-is-deprecated?noredirect=1&lq=1
https://stackoverflow.com/questions/15071199/android-mock-location-dont-work-with-google-maps
https://stackoverflow.com/questions/30569854/adding-multiple-markers-in-google-maps-api-v2-android
 */



//TODO

//Firebase
//Login page


//Firebase login
//Location visiting
//Location logging on account
//Make restaraunts go away
//Tokens


//Personal TODO

//Add name support
//MORE CLEANING (useless functions, use latlng instead of ints everywhere)
//Sync bar to visible squares (may be more than I can chew, maybe rebuild the map after?)
//Token to firebase



// Account Login
// Save to firebase
// Retrive on new session
//Save and display places visited
//Use array
//Testing


//Check into restaraunts
//save to firebase
// indicate where you have been