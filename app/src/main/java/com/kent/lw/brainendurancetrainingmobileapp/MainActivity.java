package com.kent.lw.brainendurancetrainingmobileapp;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import io.flic.lib.FlicAppNotInstalledException;
import io.flic.lib.FlicManager;
import io.flic.lib.FlicManagerInitializedCallback;

public class MainActivity extends AppCompatActivity implements TaskCommunicator, TrainingCommunicator, OnMapReadyCallback {

    public static boolean trainingStarted = false;

    // fragments
    public static FragmentManager fragmentManager;
    public static FragmentTransaction transaction;
    public static TaskFragment taskFragment;
    public static TrainingFragment trainingFragment;

    // permission
    public static boolean mLocationPermissionGranted;

    // runnable
    public static Handler handler;
    public static Runnable countdownRunnbale, stimulusRunnable, durationRunnable;

    // data collection
    public static TrainingData trainingData;

    // TASK configuration
    public static com.kent.lw.brainendurancetrainingmobileapp.Task task;

    // overall
    public static OverallData overallData;

    // helper class
    public static SoundHelper soundHelper;
    public DialogHelper dialogHelper;
    public MapHelper mapHelper;
    private FirebaseDBHelper firebaseDBHelper;
    private FileHelper fileHelper;


    private ImageButton btnProfile, btnFlic, btnDiary, btnMap;

    // map
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private LatLng lastLocation;
    private boolean mapInited = false;
    private LocationRequest mLocationRequest;
    private List<Polyline> polylineList;
    private LatLng tempLocation;

    private int countdown = 4000;

    // duration
    private long time, min, sec;
    // stimulus

    // distance
    private float distance, speed, pace;

    private void initFragments() {
        taskFragment = new TaskFragment();
        trainingFragment = new TrainingFragment();
        fragmentManager = getSupportFragmentManager();
        transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(R.anim.enter_from_bottom, R.anim.exit_to_bottom);
        transaction.add(R.id.container, taskFragment, "TASK_FRAGMENT");
        transaction.commit();
    }

    public static void showTaskFragment() {
        transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(R.anim.enter_from_bottom, R.anim.exit_to_bottom);
        transaction.add(R.id.container, taskFragment, "TASK_FRAGMENT");
        transaction.commit();
    }

    public static void hideTrainingFragment() {
        transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(R.anim.enter_from_bottom, R.anim.exit_to_bottom);
        transaction.remove(trainingFragment);
        transaction.commit();
    }

    public static void showTrainingFragment() {
        transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(R.anim.enter_from_bottom, R.anim.exit_to_bottom);
        transaction.remove(taskFragment);
        transaction.add(R.id.container, trainingFragment, "TRAINING_FRAGMENT");
        transaction.commit();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        keepDisplayOn();
        hideStatusbar();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dialogHelper = new DialogHelper(this);
        soundHelper = new SoundHelper(this);

        initFragments();
        initBtns();

        // -- map --
        lastLocation = new LatLng(0, 0);
        polylineList = new ArrayList<Polyline>();

        initMap();

        // runnable
        handler = new Handler();
        initRunnables();

        // firebase data model
        trainingData = new TrainingData();

        overallData = FileHelper.readOverallDataFromLocal();
        firebaseDBHelper = new FirebaseDBHelper();

        // temp
        rd = new Random();
        mapHelper = new MapHelper();
        mLocationRequest = new LocationRequest();
        mapHelper.getLocationPermission(this);

        initTask();

        fileHelper = new FileHelper();

        FlicConfig.setFlicCredentials();

        SensorHelper sensorHelper = new SensorHelper(this);

    }

    private void keepDisplayOn() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void hideStatusbar() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    private void initTask() {
        apvtTask = new ApvtTask();
        gonogoTask = new GonogoTask();
    }


    private void initBtns() {
        btnProfile = findViewById(R.id.btn_profile);
        btnProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent i = new Intent(MainActivity.this, HistoryActivity.class);
                startActivity(i);
            }
        });

        btnFlic = findViewById(R.id.btn_flic);
        btnFlic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    FlicManager.getInstance(MainActivity.this, new FlicManagerInitializedCallback() {
                        @Override
                        public void onInitialized(FlicManager manager) {
                            manager.initiateGrabButton(MainActivity.this);
                        }
                    });
                } catch (FlicAppNotInstalledException err) {
                    Toast.makeText(MainActivity.this, "Flic App is not installed", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnDiary = findViewById(R.id.btn_diary);
        btnDiary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        btnMap = findViewById(R.id.btn_map);
        btnMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateLocation();
            }
        });
    }

    // fragment
    @Override
    public void startTraining(String taskSelected, String difSelected) {

        // replace task fragment with training fragment
        showTrainingFragment();

        btnProfile.setVisibility(View.GONE);
        btnFlic.setVisibility(View.GONE);
        btnDiary.setVisibility(View.GONE);

        trainingData.setId(System.currentTimeMillis());
        resetTrainingData();

        handler.postDelayed(countdownRunnbale, 0);


        if (!difSelected.equals(Dif.DIF_CUSTOM)) {

        } else {
            // start after count down
            handler.postDelayed(durationRunnable, 4000);

            ArrayList<Integer> indexList = new ArrayList<Integer>();
            int totalStiCount = task.getDurationInMili() / 1000 / task.getIntervalFrom();
            for (int i = 0; i < totalStiCount; i++) {
                trainingData.setStiTypeList(0);
                indexList.add(i);
            }
            Collections.shuffle(indexList);
            float nogoCount = totalStiCount * task.getNogoPropotion() / 100;
            for (int i = 0; i < nogoCount; i++) {
                trainingData.setStiTypeOn(indexList.get(i), 1);
            }

            handler.postDelayed(stimulusRunnable, 4000);
            trainingStarted = true;
        }
    }

    public void pauseTraining() {

        // show dialog
        dialogHelper.showPauseDialog();
        soundHelper.stopNoiseSound();
        // resume handler
        handler.removeCallbacks(durationRunnable);
        handler.removeCallbacks(stimulusRunnable);
        trainingStarted = false;
    }

    public static void resumeTraining() {
        handler.postDelayed(durationRunnable, 1000);
        handler.postDelayed(stimulusRunnable, 0);
        trainingStarted = true;
        soundHelper.playNoiseSound(task.getNoise(), task.getNoise(), 0, -1, 1);
    }

    public void finishTraining() {
        soundHelper.playFinishSound(1, 1, 0, 0, 1);
        soundHelper.stopNoiseSound();

        dialogHelper.dismissLockDialog();

        // update finish dialog
        min = (time / 1000) / 60;
        sec = (time / 1000) % 60;
        dialogHelper.showFinishDialog(trainingData);

        handler.removeCallbacks(durationRunnable);
        handler.removeCallbacks(stimulusRunnable);
        trainingStarted = false;
        mapHelper.removePolylines(polylineList);

        trainingData.setName(FirebaseAuth.getInstance().getCurrentUser().getEmail().replace(".", ""));

        // firebase upload

        firebaseDBHelper.uploadAllData(trainingData, apvtTask);
        // FirebaseStorageHelper.uploadFiles();
        fileHelper.saveTrainingDataToLocal();

        // overall
        overallData.setRtList(trainingData.getAvgResTime());
        overallData.setAccuracyList(trainingData.getAccuracy());
        fileHelper.saveOverallDataToLocal();

        hideTrainingFragment();

        btnProfile.setVisibility(View.VISIBLE);
        btnFlic.setVisibility(View.VISIBLE);
        btnDiary.setVisibility(View.VISIBLE);
        trainingData.reset();
    }

    public void resetTrainingData() {
        time = 0;
        min = 0;
        sec = 0;
        distance = 0;
        speed = 0;
        countdown = 4000;
    }

    public void initMap() {
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setPadding(10, 10, 10, 10);
        mapHelper.updateLocationUI(mMap, this);
        createLocationRequest();
        updateLocation();
    }

    public void updateLocation() {
        try {
            Task location = mFusedLocationProviderClient.getLastLocation();
            location.addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if (task.isSuccessful()) {
                        Location location = (Location) task.getResult();
                        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude() - 0.0035, location.getLongitude()), 15);
                        mMap.animateCamera(cameraUpdate);

                    } else {
                    }
                }
            });

        } catch (SecurityException e) {

        }
    }

    private void createLocationRequest() {
        mapHelper.initLocationRequestSettings(mLocationRequest);

        // init last location
        try {
            if (mLocationPermissionGranted) {
                Task task = mFusedLocationProviderClient.getLastLocation();
                task.addOnCompleteListener(this, new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful() && !mapInited) {
                            lastLocation = mapHelper.convertToLatLng((Location) task.getResult());
                            mapInited = true;
                        }
                    }
                });

                mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        if (trainingStarted) {

                            // update map
                            if (locationResult != null) {

                                for (Location location : locationResult.getLocations()) {
                                    tempLocation = mapHelper.convertToLatLng(location);

                                    if (mapHelper.getDistance(lastLocation, tempLocation) < mapHelper.MAX_DISTANCE_UPDATE_THRESHOLD) {

                                        mapHelper.drawAPolyline(mMap, polylineList, lastLocation, tempLocation, MainActivity.this);

                                        // update distance
                                        distance = distance + mapHelper.getDistance(lastLocation, tempLocation) / 1000;
                                        String distanceString = String.format("%.3f", distance);
                                        trainingFragment.setTvDistance(distanceString);
                                        trainingData.setDistance(distance);

                                        // update speed
                                        speed = (distance / time) * 1000 * 60 * 60;
                                        String speedString = String.format("%.1f", speed);
                                        //trainingFragment.setTvSpeed(speedString);
                                        trainingData.setAvgSpeed(speed);

                                        // update pace
                                        pace = 1 / ((distance / time) * 1000 * 60);
                                        String paceString = String.format("%.1f", pace);
                                        trainingFragment.setTvPace(paceString);
                                        trainingData.setAvgPace(pace);

                                        // update location
                                        lastLocation = tempLocation;
                                        trainingData.setLatList(lastLocation.latitude);
                                        trainingData.setLngList(lastLocation.longitude);

                                        // finally do prompt
                                        if (speed < 3) {
                                            soundHelper.playSpeedupSound(1, 1, 0, 0, 1);

                                        }
                                    }
                                }
                            }

                        }
                    }
                }, null);
            }
        } catch (SecurityException e) {

        }
    }
}
