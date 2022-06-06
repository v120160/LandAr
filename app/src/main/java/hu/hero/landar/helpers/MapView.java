/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package hu.hero.landar.helpers;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.util.TypedValue;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import hu.hero.landar.R;

import java.util.ArrayList;

import hu.hero.landar.MainActivity;

public class MapView {
    public GoogleMap googleMap;
    private MainActivity activity;
    private int CAMERA_MARKER_COLOR = Color.argb(255, 0, 255, 0);
    private int EARTH_MARKER_COLOR = Color.argb(255, 125, 125, 125);
    private boolean setInitialCameraPosition = false;
    private Marker cameraMarker = null;
    private boolean cameraIdle = true;
    public Marker earthMarker = null;
    private ArrayList<Marker> mMarkers = new ArrayList<>();

    public MapView(MainActivity activity , GoogleMap googleMap ) {
        this.googleMap = googleMap;
        this.activity = activity;
        UiSettings settings = googleMap.getUiSettings();
        settings.setMapToolbarEnabled(false);
        settings.setIndoorLevelPickerEnabled(false);
        settings.setZoomControlsEnabled(false);
        settings.setTiltGesturesEnabled(false);
        settings.setScrollGesturesEnabled(false);

        cameraMarker = createMarker(CAMERA_MARKER_COLOR);
        earthMarker = createMarker(EARTH_MARKER_COLOR);

        googleMap.setOnMarkerClickListener((marker) -> false);

        // Add listeners to keep track of when the GoogleMap camera is moving.
        googleMap.setOnCameraMoveListener(() -> {
            cameraIdle = false;
        });
        googleMap.setOnCameraIdleListener(() -> {
            cameraIdle = true;
        });
    }

    public void updateMapPosition(double latitude, double longitude, double heading) {
        LatLng position = new LatLng(latitude, longitude);
        activity.runOnUiThread(() -> {
            // If the map is already in the process of a camera update, then don't move it.
            if (!cameraIdle) {
                return;
            }
            cameraMarker.setVisible(true);
            cameraMarker.setPosition(position);
            cameraMarker.setRotation((float) heading);

            CameraPosition.Builder cameraPositionBuilder = new CameraPosition.Builder();
            if (!setInitialCameraPosition) {
                setInitialCameraPosition = true;
                cameraPositionBuilder.zoom(21f).target(position);
            } else {
                cameraPositionBuilder.zoom(googleMap.getCameraPosition().zoom).target(position);
            }

            googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPositionBuilder.build()));
        });
    }

    public void addMarker( LatLng latLng ){
        Marker marker = createMarker(EARTH_MARKER_COLOR);
        marker.setPosition(latLng);
        marker.setVisible(true);
        mMarkers.add( marker );
    }

    /**
     * Creates and adds a 2D anchor marker on the 2D map view.
     */
    private Marker createMarker(int color) {
        MarkerOptions markersOptions = new MarkerOptions();
        markersOptions.position(new LatLng(0.0, 0.0));
        markersOptions.draggable(false);
        markersOptions.anchor(0.5f, 0.5f);
        markersOptions.flat(true);
        markersOptions.visible(false);
        markersOptions.icon(BitmapDescriptorFactory.fromBitmap(createColoredMarkerBitmap(color)));
        return googleMap.addMarker(markersOptions);
    }

    private Bitmap createColoredMarkerBitmap(int color) {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inMutable = true;
        Bitmap navigationIcon =
                BitmapFactory.decodeResource(activity.getResources(), R.drawable.ic_navigation_white_48dp,opt);
        Paint p = new Paint();
        p.setColorFilter(new LightingColorFilter(color,  /* add= */1));
        Canvas canvas = new Canvas(navigationIcon);
        canvas.drawBitmap(navigationIcon,  /* left= */0f,  /* top= */0f, p);
        return navigationIcon;
    }
}