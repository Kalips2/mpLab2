package com.example.lab2mobile;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Looper;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.TravelMode;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapHandler implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Context context;
    private FusedLocationProviderClient fusedLocationClient;
    private boolean isLocationProcessed = false;

    public MapHandler(SupportMapFragment mapFragment, Context context) {
        this.context = context;
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.mMap = googleMap;
    }

    public void showRoute(String destinationName) {
        if (mMap != null) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation(destinationName);
            } else {
                ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }

    private void getCurrentLocation(String destinationName) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    fetchDestinationAndDrawRoute(userLatLng, destinationName);
                } else {
                    Toast.makeText(context, "Unable to get current location", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void fetchDestinationAndDrawRoute(LatLng userLatLng, String destinationName) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(destinationName, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                LatLng destinationLatLng = new LatLng(address.getLatitude(), address.getLongitude());

                mMap.clear();
                mMap.addMarker(new MarkerOptions().position(userLatLng).title("Your Location"));
                mMap.addMarker(new MarkerOptions().position(destinationLatLng).title(destinationName));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 10));

                drawRoute(userLatLng, destinationLatLng);
            } else {
                Toast.makeText(context, "Destination not found", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Error fetching destination", Toast.LENGTH_SHORT).show();
        }
    }

    private void drawRoute(LatLng origin, LatLng destination) {
        GeoApiContext geoApiContext = new GeoApiContext.Builder()
                .apiKey("AIzaSyBz-d_UjKgoXI7GWoJXC5DhI_nZsmIOC9U") // Вставь сюда свой ключ Google Maps Directions API
                .build();

        DirectionsApi.newRequest(geoApiContext)
                .origin(new com.google.maps.model.LatLng(origin.latitude, origin.longitude))
                .destination(new com.google.maps.model.LatLng(destination.latitude, destination.longitude))
                .mode(TravelMode.DRIVING)
                .setCallback(new PendingResult.Callback<DirectionsResult>() {
                    @Override
                    public void onResult(DirectionsResult result) {
                        List<com.google.maps.model.LatLng> path = result.routes[0].overviewPolyline.decodePath();
                        PolylineOptions polylineOptions = new PolylineOptions();

                        for (com.google.maps.model.LatLng point : path) {
                            polylineOptions.add(new LatLng(point.lat, point.lng));
                        }

                        ((Activity) context).runOnUiThread(() -> mMap.addPolyline(polylineOptions));
                    }

                    @Override
                    public void onFailure(Throwable e) {
                        e.printStackTrace();
                        ((Activity) context).runOnUiThread(() ->
                                Toast.makeText(context, "Failed to fetch route", Toast.LENGTH_SHORT).show());
                    }
                });
    }

}