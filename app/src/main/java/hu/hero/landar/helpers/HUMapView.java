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
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.icu.text.Transliterator;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;

import com.google.android.gms.common.GoogleApiAvailabilityLight;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import hu.hero.landar.Geo.Point3;
import hu.hero.landar.R;

import java.util.ArrayList;
import java.util.List;

import hu.hero.landar.MainActivity;

public class HUMapView extends MapView implements OnMapReadyCallback {
    private GoogleMap mGoogleMap = null;
    private MainActivity mActivity;
    private int CAMERA_MARKER_COLOR = Color.argb(255, 0, 255, 0);
    private int EARTH_MARKER_COLOR = Color.argb(255, 125, 125, 125);
    private Marker cameraMarker = null;
    private boolean cameraIdle = true;
    private ArrayList<Marker> mMarkers = new ArrayList<>();

    public HUMapView(Context context){
        super(context);
    }
    public HUMapView(Context context, AttributeSet attrs){
        super( context , attrs );
        mActivity = (MainActivity)context;
    }
    public HUMapView(Context context, AttributeSet attrs, int defStyle){
        super( context , attrs , defStyle );
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.mGoogleMap = googleMap;
        UiSettings settings = googleMap.getUiSettings();
        settings.setMapToolbarEnabled(false);
        settings.setIndoorLevelPickerEnabled(false);
        settings.setZoomControlsEnabled(false);
        settings.setTiltGesturesEnabled(false);
        settings.setScrollGesturesEnabled(false);

        cameraMarker = createMarker(CAMERA_MARKER_COLOR);

        mGoogleMap.setOnMarkerClickListener((marker) -> false);

        // Add listeners to keep track of when the GoogleMap camera is moving.
        mGoogleMap.setOnCameraMoveListener(() -> {
            cameraIdle = false;
        });
        mGoogleMap.setOnCameraIdleListener(() -> {
            cameraIdle = true;
        });
        CameraPosition cp = new CameraPosition.Builder()
                .target(new LatLng(22.756630287023807, 121.14233546867824))
                .zoom(18.0f)
                .build();
        mGoogleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cp));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mGoogleMap = null;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    /*
        更新地圖位置
    */
    public void updateMapPosition(double latitude, double longitude, double heading) {
        if( mGoogleMap == null )
            return;
        LatLng position = new LatLng(latitude, longitude);
        mActivity.runOnUiThread(() -> {
            // If the map is already in the process of a camera update, then don't move it.
            if (!cameraIdle) {
                return;
            }
            cameraMarker.setVisible(true);
            cameraMarker.setPosition(position);
            cameraMarker.setRotation((float) heading);

            float fZoom = mGoogleMap.getCameraPosition().zoom;
            CameraPosition cp = new CameraPosition.Builder()
                        .target(position)
                        .zoom(fZoom)
                        .bearing((float) heading)
                        .build();
            mGoogleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cp));
        });
    }

    /*
         加入經界線
    */
    public void addParcelMarker( List<Point3> ptlist ){
        PolygonOptions polygonOptions = new PolygonOptions();
        for ( Point3 bp : ptlist ) {
                LatLng L =new LatLng(bp.y, bp.x);
                polygonOptions.add(L);
        }
        polygonOptions.strokeColor(Color.BLUE).strokeWidth(4.0f);
        mGoogleMap.addPolygon(polygonOptions);
    }

    public void addPicMarker( LatLng  latlon ){
        MarkerOptions marker = new MarkerOptions().position(latlon).title("FUCK").anchor(0.5f, 0.5f);

        // Changing marker icon
        BitmapDescriptor bp_G = BitmapDescriptorFactory.fromResource(R.drawable.redball_medium);
        marker.zIndex(0.0f);
        marker.icon(bp_G);
        mGoogleMap.addMarker(marker);
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
        return mGoogleMap.addMarker(markersOptions);
    }

    private Bitmap createColoredMarkerBitmap(int color) {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inMutable = true;
        Bitmap navigationIcon =
                BitmapFactory.decodeResource(mActivity.getResources(), R.drawable.ic_navigation_white_48dp,opt);
        Paint p = new Paint();
        p.setColorFilter(new LightingColorFilter(color,  /* add= */1));
        Canvas canvas = new Canvas(navigationIcon);
        canvas.drawBitmap(navigationIcon,  /* left= */0f,  /* top= */0f, p);
        return navigationIcon;
    }
}