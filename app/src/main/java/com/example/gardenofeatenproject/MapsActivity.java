package com.example.gardenofeatenproject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.core.app.ActivityCompat;

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


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    final static String tag = "MapsActivity";
    private GoogleMap mMap;

    protected Location mCurrentLocation;
    public static GoogleApiClient mGoogleApiClient;

    private TextView mTextView;
    private Button mButton;
    private Marker mMarker1;
    private Marker mMarker2;
    private Marker mMarker3;
    private SeekBar mVisibleBusinesses;
    private MarkerOptions options = new MarkerOptions();
    private ArrayList<String> names = new ArrayList<>();
    private ArrayList<LatLng> latlngs = new ArrayList<>();
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;
    private SignInButton mSignInButton;
    private FirebaseAuth mAuth;
    private ImageView mProfilePic;
    private FirebaseDatabase database;
    private DatabaseReference mDatabase;
    private static final String USERS = "users";
    private User currentUser;
    private List<String> visitedPlaces;
    private DatabaseReference usersRef;

    public double mLat;
    public double mLong;


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

        mTextView = findViewById(R.id.mTextId);
        //mProfilePic = (ImageView)findViewById(R.id.profilePic);
        mButton = findViewById(R.id.mainButton);
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        updateRestaraunts();


        //database = FirebaseDatabase.getInstance();
        //mDatabase = FirebaseDatabase.getInstance().getReference().child("users");
        mAuth = FirebaseAuth.getInstance();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        seekBar();

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("message");
        DatabaseReference usersRef;
        //myRef.setValue("Hello, World!");





        class returnRestaraunt extends AsyncTask<String, String, String> {
            TextView txt;

            @Override
            protected void onPreExecute() {
                // Set the variable txtView here, after setContentView on the dialog
                // has been called! use dialog.findViewById().
                txt = findViewById(R.id.mTextId);
            }
            @Override
            protected String doInBackground(String... strings) {

                Call<Business> call = yelpFusionApi.getBusiness("restaurant");
                call.enqueue(callback);
                Response<Business> response = null;
                try {
                    response = call.execute();

                }catch(IOException e){
                    e.printStackTrace();
                }

                Business business = response.body();

                String businessName = business.getName();  // "JapaCurry Truck"
                Double rating = business.getRating();
                runOnUiThread(new Runnable() {
                                  @Override
                                  public void run() {

                                      Toast.makeText(getApplicationContext(),"Hello Javatpoint",Toast.LENGTH_SHORT).show();

                                      txt.setText("HI");
                                  }
                              });

                return null;


            }


        }





    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);

    }

    public void seekBar(){
        mVisibleBusinesses = findViewById((R.id.simpleSeekBar));
        //mTextView.setText("Visible Businesses: " + mVisibleBusinesses.getProgress() + "/" + mVisibleBusinesses.getMax());



        mVisibleBusinesses.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progressChangedValue;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChangedValue = progress;

            }
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                mTextView.setText("Visible Businesses: " + String.valueOf(progressChangedValue) + "/" + mVisibleBusinesses.getMax());
            }

        });
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
        // [START_EXCLUDE silent]
        // [END_EXCLUDE]

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            updateUI(null);
                        }

                        // [START_EXCLUDE]
                        // [END_EXCLUDE]
                    }
                });
    }

    private void updateUI(FirebaseUser user) {

        if (user != null) {
                String name = user.getDisplayName();
                String email = user.getEmail();


            //userEnter.put(email, "ea");
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference myRef = database.getReference("user");
            visitedPlaces = new ArrayList<>();
            visitedPlaces.add ("3");
            User a = new User("v", visitedPlaces);
            //myRef.setValue(a);
            email = email.replaceAll("\\.", ",");
            email = "dum";
            //String key = myRef.push().getKey();

            //myRef.child(email).setValue(a);
            Map<String, Object> userEnter = new HashMap<>();
            userEnter.put(email + "/data", a);
            DatabaseReference usersRef = myRef.child("users");

            usersRef.updateChildren(userEnter);




            //final FirebaseDatabase database = FirebaseDatabase.getInstance();
            //mDatabase = FirebaseDatabase.getInstance().getReference("users").push();
            //mDatabase.child("email").setValue("w");
           /* DatabaseReference ref = database.getReference("server/saving-data/fireblog");
            //DatabaseReference postsRef = ref.child("posts");
            mDatabase = ref.child("posts");
            DatabaseReference mNewDatabase = mDatabase.push();
            User a = new User("w", visitedPlaces);
            mNewDatabase.setValue(a);
*/
//visitedNodes[0] = "w";
            //visitedNodes[0] = "r";
           // currentUser = new User(email, visitedNodes);
            //List nodeConvert = new ArrayList<String>(Arrays.asList(visitedNodes));


            //mTextView.setText(photoUrl.toString())

                mTextView.setText("Logged in as" + name);
                //mDatabase.child("users").child(keyid).setValue(currentUser);
                 // mDatabase.child("users").child(user.getUid()).setValue(currentUser); //adding user info to database
            //mDatabase.push().setValue(currentUser);


            // Check if user's email is verified
                boolean emailVerified = user.isEmailVerified();

                // The user's ID, unique to the Firebase project. Do NOT use this value to
                // authenticate with your backend server, if you have one. Use
                // FirebaseUser.getIdToken() instead.
                String uid = user.getUid();


        } else {

        }
    }
    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            // Signed in successfully, show authenticated UI.
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w("failedconnect" , "signInResult:failed code=" + e.getStatusCode());
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
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
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
    Callback<Business> callback = new Callback<Business>() {
        @Override
        public void onResponse(Call<Business> call, Response<Business> response) {
            Business business = response.body();
            // Update UI text with the Business object.
        }
        @Override
        public void onFailure(Call<Business> call, Throwable t) {
            // HTTP error happened, do something to handle it.
        }
    };

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
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
                            //mTextView.setText(Double.toString(longitude) + " + " + Double.toString(latitude));

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
        Double rating = 0.0;
        Double distance = 0.0;
        try {
            searchResponse = call.execute().body();
            int totalNumberOfResult = searchResponse.getTotal();  // 3
            ArrayList<Business> businesses = searchResponse.getBusinesses();
            businessName = businesses.get(0).getCoordinates().toString();  // "JapaCurry Truck"
            rating = businesses.get(0).getRating();

            distance = businesses.get(0).getDistance();// 4.0

        } catch(Exception ex)
        { }


        //mTextView.setText("Name: " + businessName + "\n Rating: " + rating + "\n Distance: " + distance);
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
            }

    }

        public double returnLat(int businessNum, double baseLatitude, double baseLongitude){
            double latitude = 0;
            Map<String, String> params = new HashMap<>();
            params.put("latitude", Double.toString(baseLatitude));
            params.put("longitude", Double.toString(baseLongitude));
            Call<SearchResponse> call = yelpFusionApi.getBusinessSearch(params);
            SearchResponse searchResponse;

            try {
                searchResponse = call.execute().body();
                int totalNumberOfResult = searchResponse.getTotal();  // 3
                ArrayList<Business> businesses = searchResponse.getBusinesses();
                latitude = businesses.get(businessNum).getCoordinates().getLatitude();
            } catch(Exception ex)
            { }

            return latitude;
        }
        public double returnLong(int businessNum, double baseLatitude, double baseLongitude){
            double longitude = 0;

            Map<String, String> params = new HashMap<>();
            params.put("latitude", Double.toString(baseLatitude));
            params.put("longitude", Double.toString(baseLongitude));
            Call<SearchResponse> call = yelpFusionApi.getBusinessSearch(params);
            SearchResponse searchResponse;

            try {
                searchResponse = call.execute().body();
                int totalNumberOfResult = searchResponse.getTotal();  // 3
                ArrayList<Business> businesses = searchResponse.getBusinesses();
                longitude = businesses.get(businessNum).getCoordinates().getLongitude();
            } catch(Exception ex)
            { }
            //mTextView.setText(Double.toString(longitude));

            return longitude;
        }
        public String returnName(int businessNum, double baseLatitude, double baseLongitude){
            String businessName = "error";

            Map<String, String> params = new HashMap<>();
            params.put("latitude", Double.toString(baseLatitude));
        params.put("longitude", Double.toString(baseLongitude));
        Call<SearchResponse> call = yelpFusionApi.getBusinessSearch(params);
        SearchResponse searchResponse;

        try {
            searchResponse = call.execute().body();
            ArrayList<Business> businesses = searchResponse.getBusinesses();
            businessName = businesses.get(businessNum).getName();
        } catch(Exception ex)
        { }

        return businessName;
    }
    public void markerUpdate(String names[], double[] latitude, double[] longitude){
        int counter = 0;
        for (LatLng point : latlngs) {
            options.position(point);
            options.icon(BitmapDescriptorFactory.fromResource(R.drawable.rsz_cube));
            counter++;
            //options.title(names[counter]);
            mMap.addMarker(options);
        }
        counter = 0;
        LatLng location1 = new LatLng(latitude[0], longitude[0]);
        /*
        LatLng location2 = new LatLng(latitude[1], longitude[1]);
        LatLng location3 = new LatLng(latitude[2], longitude[2]);
        //mTextView.setText(Double.toString(latitude[1]) + " + " + Double.toString(longitude[1    ]));

        mMarker1 = mMap.addMarker(new MarkerOptions()
                .position(location1)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.rsz_cube))
                .title(names[0]));

        mMarker2 = mMap.addMarker(new MarkerOptions()
                .position(location2)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.rsz_cube))
                .title(names[1]));

        mMarker3 = mMap.addMarker(new MarkerOptions()
                .position(location3)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.rsz_cube))
                .title(names[2])); */

        mMap.moveCamera(CameraUpdateFactory.newLatLng(location1));

    }
    public void assembleMap(){
        double currentLat = mLat;
        double currentLong = mLong;
        populateLatLng(mLat, mLong);
        double[] longitudes = new double[3];
        double[] latitudes = new double[3];
        String[] names = new String[3];
        for(int i = 0; i  <= 2; i++){
            longitudes[i] = returnLong(i, currentLat, currentLong);
            latitudes[i] = returnLat(i, currentLat, currentLong);
            names[i] = returnName(i, currentLat, currentLong);
        }
        markerUpdate(names, latitudes, longitudes);
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i("latlng", " " + location.getLongitude());

        mCurrentLocation = location;
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
                        //location = inputLocation;
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