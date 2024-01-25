package com.example.aplikacja.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.aplikacja.R;
import com.example.aplikacja.databinding.ActivityCallBinding;
import com.example.aplikacja.repository.MainRepository;
import com.example.aplikacja.repository.User;
import com.example.aplikacja.utils.DataModel;
import com.example.aplikacja.utils.DataModelType;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.android.PolyUtil;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class CallActivity extends AppCompatActivity implements MainRepository.Listener {

    private ActivityCallBinding views;
    private MainRepository mainRepository;
    private RegistrationActivity registrationActivity;
    private Boolean isCameraMuted = false;
    private Boolean isMicrophoneMuted = false;
    private Boolean isSearch = false;
    public String randomUser;
    public int userGroup;
    private static final String LATEST_EVENT_FIELD_NAME = "latest_event";
    private Marker marker;
    private double targetLatitude;
    private double targetLongitude;
    public double userLatitude;
    public double userLongitude;
    private Polyline pathPolyline;
    private Marker targetMarker;
    private LatLng targetPosition;
    private GoogleMap googleMap;
    private static final String FINE_LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION;
    private Boolean onCall = false;
    public SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public String currentDate = dateFormat.format(new Date());


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        views = ActivityCallBinding.inflate(getLayoutInflater());
        setContentView(views.getRoot());

        init();
        initMapView(savedInstanceState);
        handler.removeCallbacks(performCallRunnable);

        FirebaseDatabase.getInstance().getReference(Build.DEVICE).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
                userGroup = dataSnapshot.child("group").getValue(Integer.class);
                if (userGroup == 2) {
                    views.whoToCallLayout.setVisibility(View.VISIBLE);
                    views.whoVolunteerLayout.setVisibility(View.GONE);
                    views.videoButton.setVisibility(View.GONE);
                    views.localView.setVisibility(View.VISIBLE);
                } else {
                    views.whoToCallLayout.setVisibility(View.GONE);
                    views.whoVolunteerLayout.setVisibility(View.VISIBLE);
                    views.switchCameraButton.setVisibility(View.GONE);
                }
                FirebaseDatabase.getInstance().getReference(Build.DEVICE).removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("ERROR #onDataChange");
            }
        });
    }

    private void initMapView(Bundle savedInstanceState) {
        MapView mapView = views.mapView;
        mapView.onCreate(savedInstanceState);

        mapView.getMapAsync(this::onMapReady);
    }

    private void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        if (ContextCompat.checkSelfPermission(this, FINE_LOCATION_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            setupMyLocation(googleMap);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{FINE_LOCATION_PERMISSION}, 1);
        }
    }

    private void setupMyLocation(GoogleMap googleMap) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        googleMap.setMyLocationEnabled(true);
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        LocationRequest locationRequest = createLocationRequest();
        LocationCallback locationCallback = createLocationCallback(googleMap);

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    private LocationRequest createLocationRequest() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(1000);
        return locationRequest;
    }

    private LocationCallback createLocationCallback(GoogleMap googleMap) {
        return new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    userLatitude = location.getLatitude();
                    userLongitude = location.getLongitude();
                    LatLng newMarkerPosition = new LatLng(userLatitude, userLongitude);
                    handleLocationUpdate(googleMap, newMarkerPosition);
                }
            }
        };
    }

    private void handleLocationUpdate(GoogleMap googleMap, LatLng newMarkerPosition) {
        if (marker != null) {
            marker.remove();
        }

        BitmapDescriptor icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE);
        MarkerOptions markerOptions = new MarkerOptions()
                .position(newMarkerPosition)
                .title("Moja pozycja")
                .icon(icon);

        marker = googleMap.addMarker(markerOptions);

        if (targetPosition != null) {
            updatePathOnMap(googleMap, newMarkerPosition, targetPosition);
        }

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(newMarkerPosition, 15);
        googleMap.moveCamera(cameraUpdate);
    }

    private Handler handler = new Handler();
    private boolean callAccepted = false;

    private void handleExit(View view) {
        startActivity(new Intent(CallActivity.this, SelectgroupActivity.class));
        handler.removeCallbacks(performCallRunnable);
        FirebaseDatabase.getInstance().getReference().child(LATEST_EVENT_FIELD_NAME).setValue(null);
        FirebaseDatabase.getInstance().getReference().child(Build.DEVICE).setValue(new User(false, 0, 0, 0, "", ""));
        finish();
    }

    private void init() {
        mainRepository = MainRepository.getInstance();

        // views.callBtn.setOnClickListener(v -> performCall());
        views.enterBackCall.setOnClickListener(v -> handleExit(v));
        views.enterBackCallVolunteer.setOnClickListener(v -> handleExit(v));

        if (views.localView != null) {
            mainRepository.initLocalView(views.localView);
        }
        if (views.remoteView != null) {
            mainRepository.initRemoteView(views.remoteView);
        }
        mainRepository.listener = this;

        mainRepository.subscribeForLatestEvent(this::handleLatestEvent);

        views.switchCameraButton.setOnClickListener(v -> mainRepository.switchCamera());
        views.switchCameraButton2.setOnClickListener(v -> mainRepository.switchCamera());

        views.callBtn.setOnClickListener(v -> {
            views.callBtn.setVisibility(View.GONE);
            views.cancelCallBtn.setVisibility(View.VISIBLE);
            performCall();
        });

        views.cancelCallBtn.setOnClickListener(v -> {
            views.cancelCallBtn.setVisibility(View.GONE);
            views.callBtn.setVisibility(View.VISIBLE);
            handler.removeCallbacks(performCallRunnable);
            onCall = false;
        });

        views.videoButton.setOnClickListener(v -> {
            if (isCameraMuted) {
                views.micButton.setImageResource(R.drawable.ic_baseline_location_off);
                views.mapView.setVisibility(View.GONE);
            } else {
                views.micButton.setImageResource(R.drawable.ic_baseline_location_on);
                views.mapView.setVisibility(View.VISIBLE);
            }
            isCameraMuted = !isCameraMuted;
        });

        views.endCallButton.setOnClickListener(v -> {
            mainRepository.endCall();
            finish();
            FirebaseDatabase.getInstance().getReference().child(LATEST_EVENT_FIELD_NAME).setValue(null);
        });
        views.endCallButton2.setOnClickListener(v -> {
            mainRepository.endCall();
            finish();
            FirebaseDatabase.getInstance().getReference().child(LATEST_EVENT_FIELD_NAME).setValue(null);
        });
    }

    private void handleLatestEvent(DataModel data) {
        if (userGroup == 1) {
            updateTargetCoordinates(Build.DEVICE);
            if (data.getType() == DataModelType.StartCall) {
                handleIncomingCall(data);
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void handleIncomingCall(DataModel data) {
        runOnUiThread(() -> {

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                NotificationChannel channel = new NotificationChannel("help", "Prośba o pomoc", NotificationManager.IMPORTANCE_DEFAULT);
                NotificationManager manager = getSystemService(NotificationManager.class);
                manager.createNotificationChannel(channel);
            }

            NotificationCompat.Builder builder = new NotificationCompat.Builder(CallActivity.this, "help");
            builder.setContentTitle("Nowa prośba o pomoc");
            builder.setContentText("Otrzymałeś nową prośbę o pomoc! Pospiesz się i zaakceptuj prośbę");
            builder.setSmallIcon(R.drawable.help);
            builder.setAutoCancel(true);

            NotificationManagerCompat managerCompat = NotificationManagerCompat.from(CallActivity.this);
            managerCompat.notify(1, builder.build());

            views.incomingCallLayout.setVisibility(View.VISIBLE);
            views.acceptButton.setOnClickListener(v -> acceptCall(data));
            views.rejectButton.setOnClickListener(v -> rejectCall());

            handler.postDelayed(() -> {
                if (!callAccepted) {
                    views.incomingCallLayout.setVisibility(View.GONE);
                    rejectCallAutomatically();
                }
            }, 15000);
        });
    }

    private void sendStatistic() {
        Map<String, Object> values = new HashMap<>();
        values.put("wolontariusz", randomUser);
        values.put("osoba niewidoma", Build.DEVICE);
        values.put("typ", "Wezwanie pomocy");
        FirebaseDatabase.getInstance().getReference().child("statistics").child(currentDate).setValue(values);
    }

    private void acceptCall(DataModel data) {
        callAccepted = true;
        mainRepository.startCall(data.getSender());
        views.incomingCallLayout.setVisibility(View.GONE);
        handler.removeCallbacks(performCallRunnable);
        handler.removeCallbacksAndMessages(null);
        Toast.makeText(getApplicationContext(), "Tworzenie połączenia...", Toast.LENGTH_LONG).show();
    }

    private void rejectCall() {
        views.incomingCallLayout.setVisibility(View.GONE);
        FirebaseDatabase.getInstance().getReference().child(Build.DEVICE).setValue(new User(true, 1, 0, 0, "", ""));
        FirebaseDatabase.getInstance().getReference().child(LATEST_EVENT_FIELD_NAME).setValue(null);
        handler.removeCallbacksAndMessages(null);
    }

    private void rejectCallAutomatically() {
        views.incomingCallLayout.setVisibility(View.GONE);
        FirebaseDatabase.getInstance().getReference().child(Build.DEVICE).setValue(new User(true, 1, 0, 0, "", ""));
        FirebaseDatabase.getInstance().getReference().child(LATEST_EVENT_FIELD_NAME).setValue(null);
    }

    Runnable performCallRunnable = () -> {
        performCall();
    };

    private void performCall() {
        if (userGroup != 2 || randomUser != null) {
            FirebaseDatabase.getInstance().getReference(randomUser).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Object timeValue = dataSnapshot.child("time").getValue();

                    if (timeValue != null) {
                        String timeString = timeValue.toString();
                        views.textViewCallVolunteer3.setText("CZAS PODRÓŻY: " + timeString);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    handleFirebaseError("performCall", databaseError);
                }
            });
        }

        FirebaseDatabase.getInstance().getReference().orderByChild("group").equalTo(1).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<String> filteredUsers = new ArrayList<>();
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    Object statusToRequestValue = userSnapshot.child("statusToRequest").getValue();
                    if (statusToRequestValue != null && "true".equals(statusToRequestValue.toString())) {
                        filteredUsers.add(userSnapshot.getKey());
                    }
                }

                if (!filteredUsers.isEmpty() && onCall==false) {
                    randomUser = filteredUsers.get(new Random().nextInt(filteredUsers.size()));
                    FirebaseDatabase.getInstance().getReference(randomUser).child("latitude").setValue(userLatitude);
                    FirebaseDatabase.getInstance().getReference(randomUser).child("longitude").setValue(userLongitude);
                    mainRepository.sendCallRequest(randomUser, userLatitude, userLongitude, null);
                    FirebaseDatabase.getInstance().getReference().orderByChild("group").equalTo(1).removeEventListener(this);

                    handler.postDelayed(performCallRunnable, 15000);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                handleFirebaseError("performCall", databaseError);
            }
        });
    }

    private void handleFirebaseError(String methodName, DatabaseError databaseError) {
        System.out.println("ERROR #" + methodName + ": " + databaseError.getMessage());
    }



    private void updateTargetCoordinates(String user) {
        FirebaseDatabase.getInstance().getReference(user)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            targetLatitude = dataSnapshot.child("latitude").getValue(Double.class);
                            targetLongitude = dataSnapshot.child("longitude").getValue(Double.class);
                            updateTargetMarker(googleMap, targetLatitude, targetLongitude);

                            LatLng markerPosition = new LatLng(userLatitude, userLongitude);
                            targetPosition = new LatLng(targetLatitude, targetLongitude);
                            updatePathOnMap(googleMap, markerPosition, targetPosition);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        System.out.println("ERROR #onDataChange");
                    }
                });
    }

    private void updateTargetMarker(GoogleMap googleMap, double latitude, double longitude) {
        if (googleMap == null) {
            return;
        }

        if (targetMarker == null) {
            LatLng targetMarkerPosition = new LatLng(latitude, longitude);
            MarkerOptions targetMarkerOptions = new MarkerOptions()
                    .position(targetMarkerPosition)
                    .title("Cel");
            targetMarker = googleMap.addMarker(targetMarkerOptions);
        } else {
            LatLng newTargetPosition = new LatLng(latitude, longitude);
            targetMarker.setPosition(newTargetPosition);
        }
    }

    private void updatePathOnMap(GoogleMap googleMap, LatLng startPoint, LatLng endPoint) {
        if (pathPolyline != null) {
            pathPolyline.remove();
        }

        DirectionsResult result = getDirectionsResult(startPoint, endPoint);
        String estimatedTime = getEstimatedTime(startPoint, endPoint);

        if (isValidTime(getEstimatedTime(startPoint, endPoint), 15)) {}

        FirebaseDatabase.getInstance().getReference(Build.DEVICE).child("time").setValue(estimatedTime);

        if (result != null) {
            DirectionsRoute[] routes = result.routes;

            if (routes != null && routes.length > 0) {
                DirectionsRoute selectedRoute = routes[0];

                List<LatLng> decodedPath = PolyUtil.decode(selectedRoute.overviewPolyline.getEncodedPath());

                PolylineOptions polylineOptions = new PolylineOptions()
                        .addAll(decodedPath)
                        .width(5)
                        .color(Color.RED);

                pathPolyline = googleMap.addPolyline(polylineOptions);
            }
        }
    }

    private DirectionsResult getDirectionsResult(LatLng startPoint, LatLng endPoint) {
        GeoApiContext geoApiContext = new GeoApiContext.Builder()
                .apiKey("")
                .build();

        DirectionsApiRequest request = DirectionsApi.newRequest(geoApiContext)
                .origin(new com.google.maps.model.LatLng(startPoint.latitude, startPoint.longitude))
                .destination(new com.google.maps.model.LatLng(endPoint.latitude, endPoint.longitude));

        try {
            DirectionsResult result = request.await();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

   private String getEstimatedTime(LatLng startPoint, LatLng endPoint) {
        DirectionsResult directionsResult = getDirectionsResult(startPoint, endPoint);

        if (directionsResult != null && directionsResult.routes != null && directionsResult.routes.length > 0) {
            DirectionsRoute route = directionsResult.routes[0];

            if (route.legs != null && route.legs.length > 0) {
                DirectionsLeg leg = route.legs[0];

                if (leg.duration != null) {
                    String estimatedTime = leg.duration.humanReadable;
                    return estimatedTime;
                }
            }
        }
        return "";
    }

    private boolean isValidTime(String estimatedTime, int maxMinutes) {
        String[] timeParts = estimatedTime.split(" ");
        if (timeParts.length == 4) {
            int hours = Integer.parseInt(timeParts[0]);
            int minutes = Integer.parseInt(timeParts[2]);

            int totalMinutes = hours * 60 + minutes;
            return totalMinutes <= maxMinutes;
        }

        return false;
    }

    @Override
    public void webrtcConnected() {
        runOnUiThread(()->{
            onCall = true;
            views.incomingCallLayout.setVisibility(View.GONE);
            views.whoToCallLayout.setVisibility(View.GONE);
            if(userGroup==2) {
                views.callLayout.setVisibility(View.GONE);
                views.callLayoutBlind.setVisibility(View.VISIBLE);
                sendStatistic();
            }else{
                views.callLayout.setVisibility(View.VISIBLE);
                views.callLayoutBlind.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void webrtcClosed() {
        handler.removeCallbacks(performCallRunnable);
        runOnUiThread(this::finish);
        finish();
        startActivity(new Intent(CallActivity.this, LoginActivity.class));
        FirebaseDatabase.getInstance().getReference().child(Build.DEVICE).setValue(new User(false,0, 0, 0, "",""));
        onCall=false;
    }
}
