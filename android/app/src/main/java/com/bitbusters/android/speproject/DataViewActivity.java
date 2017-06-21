package com.bitbusters.android.speproject;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;

import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.VisibleRegion;
import com.google.maps.android.clustering.ClusterManager;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import org.osgeo.proj4j.BasicCoordinateTransform;
import org.osgeo.proj4j.CRSFactory;
import org.osgeo.proj4j.CoordinateReferenceSystem;
import org.osgeo.proj4j.CoordinateTransform;
import org.osgeo.proj4j.ProjCoordinate;
import org.osgeo.proj4j.io.Proj4FileReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class DataViewActivity extends FragmentActivity implements OnTaskCompleted, OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String BITMAP_TAG = "BITMAP";
    private static final String TAG = "DATA_VIEW_ACTIVITY";
    private static final int REQUEST_LOCATION = 1;
    private static final int REQUEST_CAMERA = 2;
    private GoogleMap mMap;
    private ProgressBar mProgressSpinner;
    private FloatingActionButton mCameraButton;
    private ImageButton mMenuButton;
    private FloatingActionButton mGpsButton;

    //variables used for displaying current location
    private GoogleApiClient mGoogleApiClient;
    private Location mLocation;
    private boolean connected;
    private boolean inPhotoDataView;
    private boolean hasMapCameraChanged;
    private Double mPhotoDataViewLatitudeOffset;
    private Marker currentLocationMarker;
    private FragmentManager mFragmentManager;
    private SPDataFragment mSPDataFragment;
    private PhotoDataFragment mPhotoDataFragment;
    private List<SamplingPoint> mSamplePoints = new ArrayList<>();
    private Circle mRadiusCircle;
    private List<GalleryItem> photoMarkers = new ArrayList<>();
    private Boolean imageLocationsDownloaded;
    private ClusterManager<SamplingPoint> mSampleClusterManager;
    private ClusterManager<GalleryItem> mPictureClusterManager;
    private MultiListener mMultiListener = new MultiListener();
    private Drawer mDrawer;

    private SamplingPoint selectedSamplingPoint;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_view);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mProgressSpinner = (ProgressBar) findViewById(R.id.progress_spinner);

        // Initialises the drawer menu
        setupDrawer();

        // The action performed when the menu button is pressed.
        mMenuButton = (ImageButton) findViewById(R.id.hamburger_button);
        mMenuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show the Drawer
                mDrawer.openDrawer();
            }
        });

        // The action performed when the camera button is pressed.
        mCameraButton = (FloatingActionButton) findViewById(R.id.cam_button);
        mCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(DataViewActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    startCameraIntent(v);
                } else {
                    ActivityCompat.requestPermissions(DataViewActivity.this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
                }
            }
        });

        mGpsButton = (FloatingActionButton) this.findViewById(R.id.gps_button);

        // Create an instance of GoogleAPIClient -> Required for the GPS Location
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        mFragmentManager = getSupportFragmentManager();
        // Sampling points are first to be populated onMapReady for that reason inPhotoDataView is set to true
        inPhotoDataView = true;
        mPhotoDataViewLatitudeOffset = 0.0;
        hasMapCameraChanged = false;
    }

    public void setupDrawer() {

        AccountHeader header = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.drawable.drawer_back)
                .addProfiles(
                        new ProfileDrawerItem().withName("myRivers").withIcon(R.drawable.icon_green_blue)
                )
                .withTextColor(Color.BLACK)
                .withSelectionListEnabledForSingleProfile(false)
                .build();

        //if you want to update the items at a later time it is recommended to keep it in a variable
        final PrimaryDrawerItem drawerSamplingPoints = new PrimaryDrawerItem()
                .withIdentifier(1)
                .withName(R.string.drawer_sampling_point)
                .withSelectedColor(0x0d4caf)
                .withSelectedTextColor(Color.WHITE)
                .withTextColor(Color.WHITE)
                .withIcon(R.drawable.marker_white);

        final PrimaryDrawerItem drawerImages = new PrimaryDrawerItem()
                .withIdentifier(2)
                .withName(R.string.drawer_images)
                .withSelectedColor(0x0d4caf)
                .withSelectedTextColor(Color.WHITE)
                .withTextColor(Color.WHITE)
                .withIcon(R.drawable.photo_icon);

        final SecondaryDrawerItem drawerInfo = new SecondaryDrawerItem()
                .withIdentifier(3)
                .withName(R.string.drawer_info)
                .withSelectedColor(0x0d4caf)
                .withSelectedTextColor(Color.WHITE)
                .withTextColor(Color.WHITE)
                .withIcon(R.drawable.info_white);

        //create the drawer and remember the `Drawer` result object
        mDrawer = new DrawerBuilder()
                .withActivity(this)
                .withAccountHeader(header)
                .withSliderBackgroundColor(Color.DKGRAY)
                .addDrawerItems(
                        drawerSamplingPoints,
                        drawerImages,
                        new DividerDrawerItem(),
                        drawerInfo
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        mDrawer.closeDrawer();
//                        Log.i(TAG, "identifier: " + Long.toString(drawerItem.getIdentifier()));
                        switch ((int) drawerItem.getIdentifier()){
                            case 1:
                                if(inPhotoDataView) {
                                    closePhotoView();
                                    openSamplingPointView();
                                }
                                break;
                            case 2:
                                if(!inPhotoDataView) {
                                    closeSamplingPointView();
                                    openPhotoView();
                                }
                                break;
                            case 3:
                                showInfo(view);
                                break;
                            default:
                                break;
                        }
                        return true;
                    }
                })
                .build();
    }

    public void openSamplingPointView() {
        // reset the center of the screen
        updateMapCameraPosition();
        // if there was a change in camera wait for it to be idle and then load sampling points
        if(hasMapCameraChanged) {
            mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
                @Override
                public void onCameraIdle() {
                    loadSamplingPoints();
                    mMap.setOnCameraIdleListener(null);
                }
            });
        } else {
            loadSamplingPoints();
        }
    }

    public void closeSamplingPointView() {
        mRadiusCircle.setVisible(false);
        mSampleClusterManager.clearItems();
        mSampleClusterManager.cluster();
    }

    public void openPhotoView() {
        Fragment fragment = mFragmentManager.findFragmentById(R.id.fragment_container);
        if (fragment == null) {
            setHomeButtonsPhotoView();

            fragment = new PhotoDataFragment();
            mPhotoDataFragment = (PhotoDataFragment) fragment;

            showPhotoMarkersInView();

            mFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_bottom, 0, 0, R.anim.slide_out_bottom)
                    .add(R.id.fragment_container, fragment)
                    .addToBackStack(null).commit();
        }
        inPhotoDataView = true;
        mPhotoDataViewLatitudeOffset = -0.05;
        updateMapCameraPosition();
    }

    public void closePhotoView() {
        mFragmentManager.popBackStack();
        mPictureClusterManager.clearItems();
        mPictureClusterManager.cluster();
        resetHomeButtonsPhotoView();
        inPhotoDataView = false;
        if(mPhotoDataViewLatitudeOffset != 0.0){
            hasMapCameraChanged = true;
        }
        mPhotoDataViewLatitudeOffset = 0.0;
    }

    public void loadSamplingPoints() {
        if (haveNetworkConnection()) {
            mProgressSpinner.setVisibility(View.VISIBLE);
            mSampleClusterManager.clearItems();
            LatLng camCentre = mMap.getCameraPosition().target;
            String[] location = {String.valueOf(camCentre.latitude), String.valueOf(camCentre.longitude)};
            new SamplingPointsAPI(DataViewActivity.this).execute(location);

            // Add a radius circle around sample point query area.
            if (mRadiusCircle != null) {
                mRadiusCircle.remove();
            }
            mRadiusCircle = mMap.addCircle(new CircleOptions()
                    .center(camCentre)
                    .radius(14142) // i.e. hypotenuse of 10km x 10km triangle.
                    .strokeColor(0x661854E1)
                    .fillColor(0x221854E1));
        } else {
            Toast.makeText(getApplicationContext(), "Sample point retrieval needs internet connection", Toast.LENGTH_LONG).show();
        }
    }

    public void showInfo(View v) {
        // Hide the floating action buttons.
        hideHomeButtons();
        // Initiate the info fragment.
        InfoFragment fragment = new InfoFragment();
        mFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_top, 0, 0, R.anim.slide_out_top)
                .add(R.id.fragment_container, fragment)
                .addToBackStack(null).commit();
    }

    public void startCameraIntent(View v) {
        if (haveNetworkConnection() && haveGPSOn(v.getContext())) {
            Intent photoCommentActivityIntent = new Intent(v.getContext(), PhotoCommentActivity.class);
            startActivity(photoCommentActivityIntent);
        } else {
            Toast.makeText(v.getContext(), "Uploading image needs internet connection and gps", Toast.LENGTH_LONG).show();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (connected) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
            connected = false;
        }
    }


    // On sample point click.
    public void setUpSampleManager() {
        mSampleClusterManager.setRenderer(new SamplingPointRenderer(this, mMap, mSampleClusterManager));

        mSampleClusterManager.setOnClusterItemClickListener(new ClusterManager.OnClusterItemClickListener<SamplingPoint>() {
            @Override
            public boolean onClusterItemClick(SamplingPoint point) {
                if (point.getTitle().equals("Sample_Point")) {
                    selectedSamplingPoint = point;
                    Fragment fragment = mFragmentManager.findFragmentById(R.id.fragment_container);
                    if (fragment == null) {
                        hideHomeButtons();
                        // Hide the radius circle.
                        mRadiusCircle.setVisible(false);
                        LatLng markerPos = new LatLng(point.getLatitude() + 0.05f, point.getLongitude());
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(markerPos, 11.0f));

                        mSampleClusterManager.clearItems();
                        mSampleClusterManager.addItem(point);
                        mSampleClusterManager.cluster();

                        fragment = new SPDataFragment();
                        mSPDataFragment = (SPDataFragment) fragment;

                        mFragmentManager.beginTransaction()
                                .setCustomAnimations(R.anim.slide_in_top, 0, 0, R.anim.slide_out_top)
                                .add(R.id.fragment_container, fragment)
                                .addToBackStack(null).commit();

                    }
                }

                return true;
            }
        });


    }

    // On Picture point click.
    public void setUpPictureManager() {
        mPictureClusterManager.setRenderer(new PicturePointRenderer(this, mMap, mPictureClusterManager));
        mPictureClusterManager.setOnClusterItemClickListener(new ClusterManager.OnClusterItemClickListener<GalleryItem>() {
            @Override
            public boolean onClusterItemClick(GalleryItem point) {

                if (point.getTitle().equals("Picture_Point")) {
                    PhotoViewFragment fragment = new PhotoViewFragment();
                    fragment.setGalleryItem(mPhotoDataFragment.getGalleryItem(point.getId()));
                    mFragmentManager.beginTransaction()
                            .setCustomAnimations(R.anim.slide_in_left, 0, 0, R.anim.slide_out_left)
                            .add(R.id.fragment_container, fragment)
                            .addToBackStack(null).commit();
                }
                return true;
            }
        });
    }


    // Shows all photo markers currently on screen.
    public void showPhotoMarkersInView() {

        mProgressSpinner.setVisibility(View.VISIBLE);
        VisibleRegion screen = mMap.getProjection().getVisibleRegion();
        LatLng topLeft = screen.farLeft;
        LatLng bottomRight = screen.nearRight;

        Log.i(TAG,"Coordinates:");
        Log.i(TAG,String.valueOf(screen.farLeft));
        Log.i(TAG,String.valueOf(screen.farRight));
        Log.i(TAG,String.valueOf(screen.nearRight));
        Log.i(TAG,String.valueOf(screen.nearLeft));

//        String osgb36 = "+proj=tmerc +lat_0=49 +lon_0=-2 +k=0.9996012717 +x_0=400000 +y_0=-100000 +ellps=airy +towgs84=446.448,-125.157,542.060,0.1502,0.2470,0.8421,-20.4894 +units=m +no_defs";
//        String wgs84 = "+proj=longlat +ellps=WGS84 +datum=WGS84 +no_defs ";
//
//        CRSFactory crsFactory = new CRSFactory();
//
//        CoordinateReferenceSystem sourceSRS = crsFactory.createFromParameters("EPSG:27700", osgb36);
//        CoordinateReferenceSystem targetSRS = crsFactory.createFromParameters("EPSG:4326", wgs84);
//
//        CoordinateTransform transformation = new BasicCoordinateTransform(sourceSRS, targetSRS);
//
//        ProjCoordinate result = new ProjCoordinate();
//        ProjCoordinate input = new ProjCoordinate(354270, 167981);
//        transformation.transform(input, result);

//        Log.i(TAG, "ST5427067981 in LatLong: " + result.toShortString());
        NGRtoWGS84Converter ngrToWGS84Converter = new NGRtoWGS84Converter();
        LatLng position = ngrToWGS84Converter.convert("ST5427067981");

        Log.i(TAG, "NGR: ST5427067981");
        Log.i(TAG, "LatLng: " + position.toString());


        String[] points = new String[4];
        points[0] = String.valueOf(topLeft.latitude);
        points[1] = String.valueOf(topLeft.longitude);
        points[2] = String.valueOf(bottomRight.latitude);
        points[3] = String.valueOf(bottomRight.longitude);
        new ThumbnailsDownloader(this, mPhotoDataFragment).execute(points);

    }

    public void repopulateSamplePoints(ClusterManager<SamplingPoint> mSampleClusterManager) {
        for (SamplingPoint sp : mSamplePoints) {
            mSampleClusterManager.addItem(sp);
        }
        mSampleClusterManager.cluster();
    }


    // Manipulates the map once available when created.
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setMapToolbarEnabled(false);
        try {
            //This customises the google maps using the json file
            boolean success = mMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, R.raw.style_json));
            if (!success) {
                Log.e(TAG, "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Can't find style.", e);
        }

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(55.036837, -3.625488), 5.0f));

        setUpMultiManager();

        // Zooms in on current location
        currentLocation(findViewById(R.id.map));

        // When zoom finished it populates the map with sampling points
        mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                closePhotoView();
                openSamplingPointView();
                mMap.setOnCameraIdleListener(null);
            }
        });

    }

    public void setUpMultiManager() {
        mPictureClusterManager = new ClusterManager<>(this, mMap);
        mSampleClusterManager = new ClusterManager<>(this, mMap);
        setUpSampleManager();
        setUpPictureManager();
        mMultiListener.addOC(mSampleClusterManager);
        mMultiListener.addOC(mPictureClusterManager);
        mMultiListener.addOM(mSampleClusterManager);
        mMultiListener.addOM(mPictureClusterManager);
        mMap.setOnMarkerClickListener(mMultiListener);
        mMap.setOnCameraIdleListener(mMultiListener);
    }

    @Override
    public void onTaskCompleted(List<SamplingPoint> result) {
        //do something after fetching sampling points
        mSamplePoints = result;
        for (SamplingPoint r : result) {
            mSampleClusterManager.addItem(r);
        }
        mSampleClusterManager.cluster();

        /*
        for (SamplingPoint r:result){
            System.out.println(r.getId() + " " + r.getLatitude() + " " + r.getLongitude() + " " + r.getEasting() + " " + r.getNorthing() + " ");
        }
        */
    }

    //Method called when connection established with Google Play Service Location API
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            connected = true;
            displayLocation();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        }
    }

    public void displayLocation() {
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (mLocation == null) {
                Log.e(TAG, "mLocation was null");
            }
            if (mLocation != null) {
                setLocationMarker(mLocation.getLatitude(), mLocation.getLongitude());
                updateMapCameraPosition();
            }
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, createLocationRequest(), this);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        }
    }

    public void updateMapCameraPosition() {
        if (mMap != null && mLocation != null) {
            CameraPosition newCameraPosition = new CameraPosition.Builder().zoom(11)
                    .target(new LatLng(mLocation.getLatitude() + mPhotoDataViewLatitudeOffset, mLocation.getLongitude())).build();
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(newCameraPosition));
        }
    }

    public LocationRequest createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return mLocationRequest;

    }

    public void setLocationMarker(double latitude, double longitude){
        if (currentLocationMarker != null) {
            currentLocationMarker.remove();
        }
        currentLocationMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(latitude,longitude)).icon(BitmapDescriptorFactory.fromResource(R.drawable.target_icon)));
        currentLocationMarker.setTag("Current Location");
    }

    @Override
    public void onLocationChanged(Location location) {
        setLocationMarker(location.getLatitude(), location.getLongitude());
    }

    //Method called when location button is pressed
    public void currentLocation(View v){
        if(haveGPSOn(v.getContext())){
            if(!connected){
                mGoogleApiClient.connect();

            }else if(currentLocationMarker != null){
                displayLocation();
            }

        }else{
            Toast.makeText(v.getContext(), "GPS required", Toast.LENGTH_LONG).show();
        }
    }

    //Requesting permission for location information at runtime. Need for devices running Android 6 upwards
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_LOCATION) {
            if(grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // We can now safely use the API we requested access to
                Log.i(TAG,"Location request allowed");
                connected = true;
                displayLocation();
            } else {
                connected = false;
            }
        }else if(requestCode == REQUEST_CAMERA){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Log.i(TAG,"Photo granted");
                mCameraButton.callOnClick();
            }
        }
    }

    //Called when user is temporarily in a disconnected state.
    @Override
    public void onConnectionSuspended(int i) {
        connected = false;
        Log.i(TAG, "Connection Suspended");
        mGoogleApiClient.connect();
    }

    //Called when there is an error connecting the client to the service
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "Connection Failed");
        Toast.makeText(this,"Location Connection Failed", Toast.LENGTH_SHORT).show();
        connected = false;
        mGoogleApiClient.connect();
    }

    @Override
    public void onBackPressed() {

        Fragment fragment = mFragmentManager.findFragmentById(R.id.fragment_container);

        if (fragment instanceof SPDataFragment) {
            mFragmentManager.popBackStack();
            mRadiusCircle.setVisible(true);
            mPictureClusterManager.clearItems();
            mPictureClusterManager.cluster();
            mSampleClusterManager.clearItems();
            repopulateSamplePoints(mSampleClusterManager);
            mProgressSpinner.setVisibility(View.INVISIBLE);

            // Re-show the buttons.
            showHomeButtons();
        }
        else if (fragment instanceof PhotoDataFragment) {
            openSamplingPointView();
        }
        else if (fragment instanceof PhotoViewFragment) {
            mFragmentManager.popBackStack();
        }
        else if (fragment instanceof InfoFragment) {
            mFragmentManager.popBackStack();
            showHomeButtons();
        }
        // Else do normal back button stuff.
        else {
            super.onBackPressed();
        }
    }

    public void showHomeButtons() {
        mGpsButton.show();
        mCameraButton.show();
        mMenuButton.setVisibility(View.VISIBLE);
        mDrawer.getDrawerLayout().setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
    }

    public void hideHomeButtons() {
        mGpsButton.hide();
        mCameraButton.hide();
        mMenuButton.setVisibility(View.INVISIBLE);
        mDrawer.getDrawerLayout().setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    public void setHomeButtonsPhotoView() {
        // Initialise camera button layout
        mCameraButton = (FloatingActionButton) this.findViewById(R.id.cam_button);
        FrameLayout.LayoutParams mCameraButtonLayoutParams =
                (FrameLayout.LayoutParams) mCameraButton.getLayoutParams();

        // Initialise gps button layout
        mGpsButton = (FloatingActionButton) this.findViewById(R.id.gps_button);
        FrameLayout.LayoutParams mGpsButtonLayoutParams =
                (FrameLayout.LayoutParams) mGpsButton.getLayoutParams();

        // Set the new positions
        mCameraButtonLayoutParams.gravity = Gravity.TOP | Gravity.END;
        mGpsButtonLayoutParams.gravity = Gravity.TOP | Gravity.END;

        //Apply the changes
        mCameraButton.setLayoutParams(mCameraButtonLayoutParams);
        mGpsButton.setLayoutParams(mGpsButtonLayoutParams);
    }

    public void resetHomeButtonsPhotoView() {
        // Initialise camera button layout
        mCameraButton = (FloatingActionButton) this.findViewById(R.id.cam_button);
        FrameLayout.LayoutParams mCameraButtonLayoutParams =
                (FrameLayout.LayoutParams) mCameraButton.getLayoutParams();

        // Initialise gps button layout
        mGpsButton = (FloatingActionButton) this.findViewById(R.id.gps_button);
        FrameLayout.LayoutParams mGpsButtonLayoutParams =
                (FrameLayout.LayoutParams) mGpsButton.getLayoutParams();

        // Set the new positions
        mCameraButtonLayoutParams.gravity = Gravity.BOTTOM | Gravity.END;
        mGpsButtonLayoutParams.gravity = Gravity.BOTTOM | Gravity.END;

        //Apply the changes
        mCameraButton.setLayoutParams(mCameraButtonLayoutParams);
        mGpsButton.setLayoutParams(mGpsButtonLayoutParams);
    }

    public List<GalleryItem> getPhotoMarkers() {
        return photoMarkers;
    }

    public void setPhotoMarkers(List<GalleryItem> photoMarkers) {
        this.photoMarkers = photoMarkers;
    }

    public ClusterManager<GalleryItem> getPictureClusterManager() {
        return mPictureClusterManager;
    }

    public void setPictureClusterManager(ClusterManager<GalleryItem> pictureClusterManager) {
        mPictureClusterManager = pictureClusterManager;
    }

    public Boolean getImageLocationsDownloaded() {
        return imageLocationsDownloaded;
    }

    public void setImageLocationsDownloaded(Boolean imageLocationsDownloaded) {
        this.imageLocationsDownloaded = imageLocationsDownloaded;
    }

    public SamplingPoint getSelectedSamplingPoint(){
        return selectedSamplingPoint;
    }

    public ProgressBar getProgressSpinner() {
        return mProgressSpinner;
    }

    public boolean haveNetworkConnection() {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null) { // connected to the internet
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                // connected to wifi
                haveConnectedWifi = true;
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                // connected to the mobile provider's data plan
                haveConnectedMobile = true;
            }
        }

        return haveConnectedWifi || haveConnectedMobile;

    }

    public boolean haveGPSOn(Context context){
        LocationManager lm = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

}
