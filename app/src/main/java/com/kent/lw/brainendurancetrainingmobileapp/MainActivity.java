package com.kent.lw.brainendurancetrainingmobileapp;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import io.flic.lib.FlicAppNotInstalledException;
import io.flic.lib.FlicBroadcastReceiverFlags;
import io.flic.lib.FlicButton;
import io.flic.lib.FlicManager;
import io.flic.lib.FlicManagerInitializedCallback;

public class MainActivity extends AppCompatActivity implements TaskCommunicator, TrainingCommunicator, OnMapReadyCallback, SensorEventListener {

    // permission
    public static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    //
    public static boolean trainingStarted = false;
    // fragments
    public static FragmentManager fragmentManager;
    public static FragmentTransaction transaction;
    public static TaskFragment taskFragment;
    public static TrainingFragment trainingFragment;
    public static boolean mLocationPermissionGranted;
    // runnable
    public static Handler handler;
    public static Runnable countdownRunnbale, stimulusRunnable, durationRunnable;
    // soundpool
    public static SoundHelper sh;
    // data collection
    public static TrainingData trainingData;
    // finish dialog
    public static double stiTotalCount, resCorrectCount, resTotalCount;
    public static long resTotalTime;
    public static float art, accuracy;
    // TASK configuration
    public static ApvtTask apvtTask;
    public static GonogoTask gonogoTask;
    //lock threshold
    public final int LOCK_THRESHOLD = 1;
    // A-PVT
    private final String TASK_A_PVT = "A-PVT";
    private final int A_PVT_DURATION = 10 * 60 * 1000;
    private final int A_PVT_INTERVAL_EASY = 4 * 1000;
    private final int A_PVT_INTERVAL_MEDIUM = 8 * 1000;
    private final int A_PVT_INTERVAL_HARD = 11 * 1000;
    // W-AVT
    private final String TASK_Gonogo = "W-AVT";
    private final int W_AVT_DURATION = 60 * 60 * 1000;
    private final int W_AVT_INTERVAL = 2 * 1000;
    // VISUAL
    private final String TASK_VISUAL = "Visual";
    // DIF
    private final String DIF_EASY = "Easy";
    private final String DIF_MEDIUM = "Medium";
    private final String DIF_HARD = "Hard";
    private final String DIF_ADAPTIVE = "Adaptive";
    private final String DIF_CUSTOM = "Custom";
    private final int accSensor = Sensor.TYPE_LINEAR_ACCELERATION;
    // ui
    public DialogHelper dh;
    public MapHelper mh;
    // TEMP
    Random rd;
    private ImageButton btnProfile, btnFlic, btnMap;
    // map
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private LatLng lastLocation;
    private boolean mapInited = false;
    private LocationRequest mLocationRequest;
    private List<Polyline> polylineList;
    private LatLng myLatLng;
    // acc
    private SensorManager sm;
    private Sensor accelerometer, gyroscope;
    private double x, y, z;
    private int MAX_DISTANCE_UPDATE_THRESHOLD = 100;
    private int countdown = 4000;
    // duration
    private long time, hour, min, sec;
    // stimulus
    private int stimulusInterval, trainingDuration;
    // distance
    private float distance, speed, pace;
    private LatLng tempLocation;
    private FirebaseHelper firebaseHelper;
    // saveHelper
    private FileHelper fh;

    public static void resumeTraining() {
        handler.postDelayed(durationRunnable, 1000);
        handler.postDelayed(stimulusRunnable, 0);
        trainingStarted = true;
        sh.playNoiseSound(apvtTask.getNoise(), apvtTask.getNoise(), 0, -1, 1);

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

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dh = new DialogHelper();
        dh.initDialog(this);

        sh = new SoundHelper();
        sh.initSoundHelper(this);

        initFragments();
        initBtns();

        // -- map --
        lastLocation = new LatLng(0, 0);
        initMap();
        polylineList = new ArrayList<Polyline>();

        // acc
        initAcc();

        // runnable
        handler = new Handler();

        // flic
        initFlic();

        // firebase data model
        trainingData = new TrainingData();
        firebaseHelper = new FirebaseHelper();

        // temp
        rd = new Random();
        mh = new MapHelper();
        mLocationRequest = new LocationRequest();
        mh.getLocationPermission(this);

        initTask();

        fh = new FileHelper();
        fh.initDir();
    }

    private void initTask() {
        apvtTask = new ApvtTask(0, 0, 0, 0, 0, 0, 0);
        gonogoTask = new GonogoTask(0, 0, 0, 0, 0, 0, 0);
    }

    private void initFragments() {
        taskFragment = new TaskFragment();
        trainingFragment = new TrainingFragment();

        fragmentManager = getSupportFragmentManager();
        transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(R.anim.enter_from_bottom, R.anim.exit_to_bottom);
        transaction.add(R.id.container, taskFragment, "TASK_FRAGMENT");
        transaction.commit();
    }

    private void initBtns() {
        btnProfile = findViewById(R.id.btn_profile);

        btnProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent i = new Intent(MainActivity.this, ProfileActivity.class);
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
        // start training

        // start  map

        // reset training dat
        trainingData.resetAllData();
        trainingData.setTask(taskSelected);
        trainingData.setDif(difSelected);
        trainingData.setId(System.currentTimeMillis());
        resetTrainingData();

        countdownRunnbale = new Runnable() {
            @Override
            public void run() {
                if (countdown > 1000 && countdown <= 4000) {
                    if (countdown == 4000) {
                        trainingData.setStartTime(System.currentTimeMillis());
                        sh.playStartSound(1, 1, 0, 0, 1);
                        dh.showCountdownDialog();
                    }
                    countdown = countdown - 1000;
                    dh.setCountdownText(countdown / 1000 + "");
                    handler.postDelayed(countdownRunnbale, 1000);
                } else {
                    dh.dismissCountdownDialog();
                    handler.removeCallbacks(countdownRunnbale);
                }
            }
        };
        handler.postDelayed(countdownRunnbale, 0);


        if (!difSelected.equals(DIF_CUSTOM)) {
            // get parameters // passing as parameters
            switch (taskSelected) {
                case TASK_A_PVT:

                    trainingDuration = A_PVT_DURATION;

                    // stimulus
                    switch (difSelected) {
                        case DIF_EASY:
                            stimulusInterval = A_PVT_INTERVAL_EASY;
                            break;

                        case DIF_MEDIUM:
                            stimulusInterval = A_PVT_INTERVAL_MEDIUM;
                            break;

                        case DIF_HARD:
                            stimulusInterval = A_PVT_INTERVAL_HARD;
                            break;
                    }
                    break;

                case TASK_Gonogo:
                    stimulusInterval = W_AVT_INTERVAL;
                    trainingDuration = W_AVT_DURATION;
                    break;
            }

            // duration
            durationRunnable = new Runnable() {
                @Override
                public void run() {
                    String durationString = hour + "h " + min + "m " + sec + "s";
                    trainingFragment.setTvDuration(durationString);
                    if (trainingDuration > 0) {
                        trainingDuration = trainingDuration - 1000;
                        time = time + 1000;
                        hour = (trainingDuration) / 1000 / 3600;
                        min = (trainingDuration / 1000) / 60;
                        sec = (trainingDuration / 1000) % 60;
                        handler.postDelayed(this, 1000);
                    } else {
                        finishTraining();
                    }

                }
            };
            handler.postDelayed(durationRunnable, 4000);

            // simtimulus
            stimulusRunnable = new Runnable() {
                @Override
                public void run() {
                    if (trainingDuration > 0) {
                        // can do volume and priority for background noise
                        //sp.play(beepSound, 1f, 1f, 0, 0, 1);
                        sh.playBeepSound(1, 1, 0, 0, 1);


                        trainingData.setStiMiliList(System.currentTimeMillis());
                        stiTotalCount++;
                        handler.postDelayed(this, stimulusInterval);
                    }
                }
            };
            handler.postDelayed(stimulusRunnable, 4000);
            trainingStarted = true;
        } else {

            // CUSTOM

            // duration
            durationRunnable = new Runnable() {
                @Override
                public void run() {

                    if (time == 0) {
                        Log.d("noise", apvtTask.getNoise() + "");
                        sh.playNoiseSound(apvtTask.getNoise(), apvtTask.getNoise(), 0, -1, 1
                        );
                    }

                    if (apvtTask.getDuration() > 0) {
                        String durationString = min + "m " + sec + "s";
                        min = (apvtTask.getDuration() / 1000) / 60;
                        sec = (apvtTask.getDuration() / 1000) % 60;
                        trainingFragment.setTvDuration(durationString);

                        apvtTask.setDuration(apvtTask.getDuration() - 1000);
                        time = time + 1000;
                        trainingData.setTime(time);

                        handler.postDelayed(this, 1000);
                    } else {
                        finishTraining();
                    }
                }
            };
            // start after count down
            handler.postDelayed(durationRunnable, 4000);

            // simtimulus
            stimulusRunnable = new Runnable() {
                @Override
                public void run() {
                    if (apvtTask.getDuration() > 0) {

                        float randomVolume = rd.nextFloat() * (apvtTask.getVolumeTo() - apvtTask.getVolumeFrom()) + apvtTask.getVolumeFrom();
                        sh.playBeepSound(randomVolume, randomVolume, 0, 0, 1);

                        trainingData.setStiMiliList(System.currentTimeMillis());

                        // update sti count on tv and td
                        trainingData.incStiCount();
                        trainingFragment.setTvStiCount(trainingData.getStiCount() + "");

                        // update accuracy
                        trainingFragment.setTvAccuracy(trainingData.getAccuracy() + "");


                        int randomInterval = rd.nextInt(apvtTask.getIntervalTo() - apvtTask.getIntervalFrom() + 1) + apvtTask.getIntervalFrom();
                        trainingFragment.setTvSti("Next stimulus in " + randomInterval + "s");
                        handler.postDelayed(this, randomInterval * 1000);
                    }
                }
            };
            handler.postDelayed(stimulusRunnable, 4000);
            trainingStarted = true;
        }
    }

    @Override
    public void pauseTraining() {

        // show dialog
        dh.showPauseDialog();
        sh.stopNoiseSound();
        // resume handler
        handler.removeCallbacks(durationRunnable);
        handler.removeCallbacks(stimulusRunnable);
        trainingStarted = false;
    }

    @Override
    public void finishTraining() {
        sh.playFinishSound(1, 1, 0, 0, 1);
        sh.stopNoiseSound();

        // update finish dialog
        hour = (time) / 1000 / 3600;
        min = (time / 1000) / 60;
        sec = (time / 1000) % 60;

        dh.setupFinishDialog(trainingData);
        dh.showFinishDialog();

        handler.removeCallbacks(durationRunnable);
        handler.removeCallbacks(stimulusRunnable);
        trainingStarted = false;
        mh.removePolylines(polylineList);

        trainingData.setName(FirebaseAuth.getInstance().getCurrentUser().getEmail().replace(".", ""));

        // firebase upload

        firebaseHelper.uploadAllData(trainingData, apvtTask);
        //FirestorageHelper.uploadFiles();
        fh.saveJsonToLocal();

        trainingData.printAllData();
        hideTrainingFragment();

        btnProfile.setVisibility(View.VISIBLE);
        btnFlic.setVisibility(View.VISIBLE);
    }

    public void resetTrainingData() {
        trainingDuration = 0;
        time = 0;
        hour = 0;
        min = 0;
        sec = 0;
        distance = 0;
        speed = 0;
        stiTotalCount = 0;
        resCorrectCount = 0;
        resTotalCount = 0;
        resTotalTime = 0;
        art = 0;
        accuracy = 0;
        countdown = 4000;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
        mh.updateLocationUI(mMap, this);
    }

    public void initMap() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setPadding(10, 10, 10, 10);
        mh.updateLocationUI(mMap, this);
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
                        myLatLng = new LatLng(location.getLatitude() - 0.0035, location.getLongitude());
                        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(myLatLng, 15);
                        mMap.animateCamera(cameraUpdate);

                    } else {
                    }
                }
            });

        } catch (SecurityException e) {

        }
    }

    private void createLocationRequest() {
        mh.initLocationRequestSettings(mLocationRequest);

        // init last location
        try {
            if (mLocationPermissionGranted) {
                Task task = mFusedLocationProviderClient.getLastLocation();
                task.addOnCompleteListener(this, new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful() && !mapInited) {
                            lastLocation = mh.convertToLatLng((Location) task.getResult());
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
                                    tempLocation = mh.convertToLatLng(location);

                                    if (mh.getDistance(lastLocation, tempLocation) < MAX_DISTANCE_UPDATE_THRESHOLD) {

                                        mh.drawAPolyline(mMap, polylineList, lastLocation, tempLocation, MainActivity.this);

                                        // update distance
                                        distance = distance + mh.getDistance(lastLocation, tempLocation) / 1000;
                                        String distanceString = String.format("%.3f", distance);
                                        trainingFragment.setTvDistance(distanceString);
                                        trainingData.setDistance(distance);

                                        // update speed
                                        speed = (distance / time) * 1000 * 60 * 60;
                                        String speedString = String.format("%.1f", speed);
                                        trainingFragment.setTvSpeed(speedString);
                                        trainingData.setAvgSpeed(speed);

                                        // update pace
                                        pace = 1 / ((distance / time) * 1000 * 60);
                                        String paceString = String.format("%.1f", pace);
                                        trainingFragment.setTvPace(paceString);
                                        trainingData.setAvgPace(pace);

                                        // update location
                                        lastLocation = tempLocation;
                                        trainingData.setLocLatList(lastLocation.latitude);
                                        trainingData.setLocLngList(lastLocation.longitude);

                                        // finally do prompt
                                        if (speed < 3) {
                                            sh.playSpeedupSound(1, 1, 0, 0, 1);

                                        }
                                    }
                                }
                            }

                        }
                    }
                }, null /* Looper */);
            }
        } catch (SecurityException e) {

        }
    }

    // acc
    public void initAcc() {
        sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        if (sm.getDefaultSensor(accSensor) != null && sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) {
            accelerometer = sm.getDefaultSensor(accSensor);
            gyroscope = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

            sm.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            sm.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);

        } else {
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {


        if (trainingStarted) {

            if (event.sensor.getType() == accSensor) {
                x = event.values[0];
                y = event.values[1];
                z = event.values[2];

                fh.saveTxtToLocal(System.currentTimeMillis() + "__" + x + " " + y + " " + z + "\n", "acc");


//            trainingData.setAccXList(x);
//            trainingData.setAccYList(y);
//            trainingData.setAccZList(z);

                Log.d("ACC", x + " " + y + " " + z);
                double mag = x * x + y * y + z * z;
                Log.d("mag", mag + "");

                if (mag > LOCK_THRESHOLD && !dh.isLockDialogShowing()) {
                    dh.showLockDialog();
                }

                if (mag < LOCK_THRESHOLD && dh.isLockDialogShowing()) {
                    dh.dismissLockDialog();
                }
            }




            if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {

                x = event.values[0];
                y = event.values[1];
                z = event.values[2];

                fh.saveTxtToLocal(System.currentTimeMillis() + "__" + x + " " + y + " " + z + "\n", "gyro");
//            trainingData.setGyroXList(x);
//            trainingData.setGyroYList(y);
//            trainingData.setGyroZList(z);


            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    // flic
    public void initFlic() {
        FlicManager.setAppCredentials("ddbfde99-d965-41df-8b9d-810bb0c26fe7", "f6e6938e-4d36-46e6-8fe1-d38436bdef83", "Brain Endurance Training Mobile App");
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        FlicManager.getInstance(this, new FlicManagerInitializedCallback() {
            @Override
            public void onInitialized(FlicManager manager) {
                FlicButton button = manager.completeGrabButton(requestCode, resultCode, data);
                if (button != null) {
                    button.registerListenForBroadcast(FlicBroadcastReceiverFlags.UP_OR_DOWN | FlicBroadcastReceiverFlags.REMOVED);
                    Toast.makeText(MainActivity.this, "Grabbed a button", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Did not grab any button", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
