package com.example.gardenofeatenproject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.core.app.ActivityCompat;

import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;
import android.location.Location;


import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.*;




import retrofit2.Call;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
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
import com.yelp.fusion.client.models.Business;
import com.yelp.fusion.client.models.SearchResponse;


import com.google.firebase.database.*;


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

    /* ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        Preconditions:
            Program compiles
        Postconditions:
            The map and all required functions will be initialized
       vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv  */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getCurrentLocation();
        buildGoogleApiClient();
        mGoogleApiClient.connect();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        //Have to use strictmode or the app won't compile, its an error with location logging
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        //signing into the google API with the key provided
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        findViewById(R.id.sign_in_button).setOnClickListener((view) -> { signIn(); });
        findViewById(R.id.visitedButton).setOnClickListener((view) -> { visitedPlace(); });
        findViewById(R.id.buttonPush).setOnClickListener((view) -> { updateDatabase(userFirebase); });

        mTextView = findViewById(R.id.mTextId);
        mPlace = findViewById(R.id.mPlace);
        //also need to request permissions to access fine location or the app will crash on trying
        //get location
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        updateRestaurants();

        mAuth = FirebaseAuth.getInstance();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }
    /* ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        Preconditions:
            The sign in button is pressed
        Postconditions:
            The user is signed in and the visitedPlaces arraylist is cleared on the first
            signin, and the markers are updated with a seperate function
       vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv  */
    private void signIn() {
        //have to use intents to log into the google client
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
        if(!signedIn)
            visitedPlaces.clear();
        signedIn = true;
        //signedin is used to clear whatever places the user has visited before logging in,
        //so that the places in the database already will overwrite anything that was visited
        //before connecting
        markerUpdate();
    }
    /* ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        Preconditions:
            The button visit is pressed and a place is selected
        Postconditions:
            The arraylist is checked to see if it already contains the currently selected place,
            and if so, does nothing. Also, displays current places visited and updates the map when
            pressed
       vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv  */
    private void visitedPlace() {
        if (!visitedPlaces.contains(currentPlace)) {
            visitedPlaces.add(currentPlace);
            mPlace.setText(visitedPlaces.toString());
        }
        markerUpdate();
    }

    /* ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        Preconditions:
            The user has pressed the sign in button
        Postconditions:
            The system tries to log in the user to google using the data provided, and authenticates
             the user with the firebase database
       vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv  */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed
                Log.w( "Google sign in failed", e);
            }
        }
    }

    /* ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    Preconditions:
        The user has been signed in with google
    Postconditions:
        The user is registered in the firebase database, and will be used to push data to in the
        future, as well as logged to monitor changes to the database.
   vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv  */
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


    /* ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        Preconditions:
            The user has been signed in to google
            The user has been registered in the database
        Postconditions:
            The database is updated to also contain the places that have been visited locally,
            and also show what account the data is being uploaded to.
            If the database is not already created for the unique user, the database will be
            created.
       vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv  */
    private void updateDatabase(FirebaseUser user) {

        if (user != null) {
                String name = user.getDisplayName();
                String email = user.getEmail();


            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference myRef = database.getReference("RegisteredUsers");
            //you can't enter data into firebase that contains periods, so I replace
            //the periods in the email addresses with commas
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
    /* ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        Preconditions:
        A user has logged into the database
        Postconditions:
        The local arraylist of the visited places is overwritten if this is the first time
        connecting to the firebase this session
        Any data input into the arraylist before this is called is overwritten
       vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv  */
    private void loginUpdate(FirebaseUser user) {
        if (user != null) {
            String email = user.getEmail();
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            final String emailFinal = email.replaceAll("\\.", ",");
            //manually find the path for the user to access their data
            DatabaseReference myRef2 = database.getReference("RegisteredUsers/users/" + emailFinal);


            myRef2.addChildEventListener(new ChildEventListener() {
                // Retrieve new posts as they are added to Firebase
                // but for our purposes, only the first new post is required and will be used
                @Override
                public void onChildAdded(DataSnapshot snapshot, String previousChildKey) {
                    if(firstCall) {
                        GenericTypeIndicator<ArrayList<String>> t = new GenericTypeIndicator<ArrayList<String>>() {
                        };
                        ArrayList<String> allData = (ArrayList) snapshot.getValue(t);
                        System.out.println("Title: " + allData);
                        visitedPlaces = allData;
                        firstCall = false;
                        markerUpdate();
                    }

                }
                //These next four overrides must be contained to compile, they contain nothing
                //because they require no functionality for this project
                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {

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


    /* ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        Preconditions:
            The mouse is clicked on the sign in button
        Postconditions:
            Called signin()
       vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv  */
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                signIn();
                break;
            // ...
        }
    }

    /* ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        Preconditions:
            The program is started
        Postconditions:
            If the map is not connected, connect it
       vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv  */
    @Override
    public void onStart(){
        super.onStart();
        if(this.mGoogleApiClient != null){
            this.mGoogleApiClient.connect();
        }

    }

    /* ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    Preconditions:
        The program is started
    Postconditions:
        Connect to the Yelp API using the key
   vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv  */
    public MapsActivity() throws IOException {
        yelpFusionApiFactory = new YelpFusionApiFactory();
        yelpFusionApi = yelpFusionApiFactory.createAPI(apiKey);



    }

    /* ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    Preconditions:
        OnStart connects the api client
    Postconditions:
        The map is initialized and built
   vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv  */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

    }

    /* ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        Preconditions:
            The device connecting to this has location providing enabled
        Postconditions:
            The current latitude and longitude is returned and the frequency of how often
            this function will be called is set
       vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv  */
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

    /* ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    Preconditions:
        The map is initialized, connected and built
    Postconditions:
        The specifications of the map are set and the visual component of the map is created
   vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv  */
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


/* ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    Preconditions:
        The program has connected to the Yelp database
    Postconditions:
        The nearest restaurants will be found and displayed
   vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv  */

    public void updateRestaurants(){
        Map<String, String> params = new HashMap<>();
        //params are what will be searched for in the database,
        //term specifies that restaurants will be found
        //and latitude and longitude specify where they will look
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
    /* ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    Preconditions:
        The current location has been found by the google api
    Postconditions:
    The latlngs are found for all nearby businesses (set at 15) and the names are also found
    for the businesses at those coordinates
    vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv  */

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
            }

    }


    /* ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        Preconditions:
            latlngs have been found
            names for businesses have been found
            The map has been created
        Postconditions:
            The names of the businesses are marked at each of their locations
            and if the place has been visited, an orange cube is placed instead
       vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv  */
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

    /* ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    Preconditions:
        Location is found
        Map is set up
    Postconditions:
        The marers are updated and the latitudes and longitudes of nearby businesses are logged
   vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv  */
    public void assembleMap(){
        populateLatLng(mLat, mLong);
        markerUpdate();
    }
    /* ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        Preconditions:
            The location changes
        Postconditions:
            The current location is set to the new location
       vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv  */
    @Override
    public void onLocationChanged(Location location) {
        Log.i("latlng", " " + location.getLongitude());
        mCurrentLocation = location;
    }
    /* ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        Preconditions:
            Map has been set up
            Markers have been placed
        Postconditions:
            When a marker is clicked, they now show their current name above their marker
       vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv  */
    @Override
    public boolean onMarkerClick(final Marker marker) {
        currentPlace = marker.getTitle();
        return false;
    }
    /* ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        Preconditions:
            Connection fails to the google API
        Postconditions:
            The failure is logged
       vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv  */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i("LocationFragment", "Connection failed: ConnectionResult.getErrorCode() " + result.getErrorCode());
    }

    @Override/* ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    Preconditions:
        Connection to the google API is suspended (the user swaps apps)
    Postconditions:
        The swap is logged and the program tries to connect again
   vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv  */
    public void onConnectionSuspended(int cause) {
        Log.i("LocationFragment", "Connection suspended");
        mGoogleApiClient.connect();
    }
    /* ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        Preconditions:
            The app is closed
        Postconditions:
            The app disconnects itself to cleanly close itself
       vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv  */
    @Override
    public void onDestroy() {
        Log.i("Destroyed", "Connection destroyed");
        mGoogleApiClient.disconnect();
        super.onDestroy();
    }

    /* ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    Preconditions:
        The Google API is connected to the app
    Postconditions:
        The location begins logging itself to the client
   vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv  */
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