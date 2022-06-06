package hu.hero.landar.helpers;

import android.opengl.GLSurfaceView;
import android.view.View;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.ar.core.Session;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import hu.hero.landar.MainActivity;
import hu.hero.landar.R;
import hu.hero.landar.common.helpers.SnackbarHelper;

public class HelloGeoView implements DefaultLifecycleObserver{
    public SnackbarHelper snackbarHelper = new SnackbarHelper();
    public View root;
    public GLSurfaceView surfaceView;
    public MapView mapView = null;
    private MainActivity activity;

    public HelloGeoView(MainActivity activity) {
        this.activity = activity;
        root = View.inflate(activity, R.layout.activity_main, null);
        surfaceView = (GLSurfaceView) root.findViewById(R.id.surfaceview);
        Session session = activity.mArCoreSessionHelper.getSession();
        SnackbarHelper snackbarHelper = new SnackbarHelper();
        MapTouchWrapper mapTouchWrapper = (MapTouchWrapper) root.findViewById(R.id.map_wrapper);


 /*       mapTouchWrapper.setup((screenLocation) -> {
            LatLng latLng = mapView.googleMap.getProjection().fromScreenLocation(screenLocation);
            activity.renderer.onMapClick(latLng);
            return;
        });
*/
        mapTouchWrapper.setup(screenLocation -> {
            LatLng latLng = mapView.googleMap.getProjection().fromScreenLocation(screenLocation);
            activity.mRenderer.onMapClick(latLng);
        });

        SupportMapFragment mapFragment = (SupportMapFragment) activity.getSupportFragmentManager().findFragmentById(R.id.map);
//        mapFragment.getMapAsync((googleMap) -> mapView = new MapView(activity, googleMap));
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull GoogleMap googleMap) {
                mapView = new MapView(activity, googleMap);
            }
        });
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        surfaceView.onResume();
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        surfaceView.onPause();
    }
}
