package hu.hero.landar;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.ar.core.Anchor;
import com.google.ar.core.Config;
import com.google.ar.core.Earth;
import com.google.ar.core.GeospatialPose;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.SceneView;
import com.google.ar.sceneform.Sceneform;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.BaseArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentOnAttachListener;
import hu.hero.landar.helpers.GeoPermissionsHelper;
import hu.hero.landar.helpers.MapTouchWrapper;
import hu.hero.landar.helpers.MapView;

public class MainActivity extends AppCompatActivity implements
        FragmentOnAttachListener,
        BaseArFragment.OnTapArPlaneListener,
        BaseArFragment.OnSessionConfigurationListener,
        ArFragment.OnViewCreatedListener{
    private static final String TAG = MainActivity.class.getSimpleName();
    private MainActivity mActivity;
    private static final double MIN_OPENGL_VERSION = 3.0;
    private ArFragment arFragment;
    private Renderable mRenderable;
    private ViewRenderable mViewRenderable;
    public MapView mMapView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = this;

        setContentView(R.layout.activity_main);
        getSupportFragmentManager().addFragmentOnAttachListener(this);

        if (savedInstanceState == null) {
            if (Sceneform.isSupported(this)) {
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.arFragment, ArFragment.class, null)
                        .commit();
            }
        }

        loadModels();
        MapTouchWrapper mapTouchWrapper = findViewById(R.id.map_wrapper);
        mapTouchWrapper.setup(screenLocation -> {
            LatLng latLng = mMapView.googleMap.getProjection().fromScreenLocation(screenLocation);
            Log.d("胡征懷",latLng.toString());
            Earth earth = arFragment.getArSceneView().getSession().getEarth();
            if (earth.getTrackingState() == TrackingState.TRACKING ){
                double altitude = earth.getCameraGeospatialPose().getAltitude() - 1;
                // The rotation quaternion of the anchor in the East-Up-South (EUS) coordinate system.
                float qx = 0f;
                float qy = 0f;
                float qz = 0f;
                float qw = 1f;
                Anchor anchor = earth.createAnchor(latLng.latitude, latLng.longitude, altitude, qx, qy, qz, qw);
                drawAnchor(anchor);
            }
            mActivity.mMapView.addMarker( latLng );
        });


        SupportMapFragment mapFragment = (SupportMapFragment) mActivity.getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull GoogleMap googleMap) {
                mMapView = new MapView(mActivity, googleMap);
            }
        });

/*
        // Set up the Hello AR renderer.
        mRenderer = new HelloGeoRenderer(mActivity);
        getLifecycle().addObserver(mRenderer);

        // Set up Hello AR UI.
        view = new HelloGeoView(this);
        getLifecycle().addObserver(view);
        setContentView(view.root);

        // Sets up an example renderer using our HelloGeoRenderer.
        new SampleRender(view.surfaceView, mRenderer, getAssets());

 */
    }

    @Override
    public void onAttachFragment(@NonNull FragmentManager fragmentManager, @NonNull Fragment fragment) {
        if (fragment.getId() == R.id.arFragment) {
            arFragment = (ArFragment) fragment;
            arFragment.setOnSessionConfigurationListener(this);
            arFragment.setOnViewCreatedListener(this);
            arFragment.setOnTapArPlaneListener(this);
        }
    }

    @Override
    public void onSessionConfiguration(Session session, Config config) {
        if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
            config.setDepthMode(Config.DepthMode.AUTOMATIC);
        }
        if (session.isGeospatialModeSupported( Config.GeospatialMode.ENABLED)) {
            config.setGeospatialMode(Config.GeospatialMode.ENABLED);
        }
    }

    @Override
    public void onViewCreated(ArSceneView arSceneView) {
        arFragment.setOnViewCreatedListener(null);

        // Fine adjust the maximum frame rate
        arSceneView.setFrameRateFactor(SceneView.FrameRate.FULL);
        arSceneView.getScene().addOnUpdateListener(frameTime->{
            Earth earth = arSceneView.getSession().getEarth();
            if (earth.getTrackingState() == TrackingState.TRACKING ){
                // TODO: the Earth object may be used here.
                GeospatialPose cameraGeospatialPose = earth.getCameraGeospatialPose();
                mMapView.updateMapPosition(
                        cameraGeospatialPose.getLatitude(),
                        cameraGeospatialPose.getLongitude(),
                        cameraGeospatialPose.getHeading()
                );
                // Draw the placed anchor, if it exists.
//            if( mEarthAnchor != null )
//                renderCompassAtAnchor( render , mEarthAnchor );
//                for (Anchor anchor:mAnchors) {
//                    renderCompassAtAnchor( render , anchor );
                }
        });
    }

    public void loadModels() {
        WeakReference<MainActivity> weakActivity = new WeakReference<>(this);
        ModelRenderable.builder()
                .setSource(this, Uri.parse("https://storage.googleapis.com/ar-answers-in-search-models/static/Tiger/model.glb"))
                .setIsFilamentGltf(true)
                .setAsyncLoadEnabled(true)
                .build()
                .thenAccept(model -> {
                    MainActivity activity = weakActivity.get();
                    if (activity != null) {
                        activity.mRenderable = model;
                    }
                })
                .exceptionally(throwable -> {
                    Toast.makeText(
                            this, "Unable to load model", Toast.LENGTH_LONG).show();
                    return null;
                });
        ViewRenderable.builder()
                .setView(this, R.layout.view_model_title)
                .build()
                .thenAccept(viewRenderable -> {
                    MainActivity activity = weakActivity.get();
                    if (activity != null) {
                        activity.mViewRenderable = viewRenderable;
                    }
                })
                .exceptionally(throwable -> {
                    Toast.makeText(this, "Unable to load model", Toast.LENGTH_LONG).show();
                    return null;
                });
    }
    @Override
    public void onTapPlane(HitResult hitResult, Plane plane, MotionEvent motionEvent) {
        if ( mRenderable == null || mViewRenderable == null) {
            Toast.makeText(this, "Loading...", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create the Anchor.
        Anchor anchor = hitResult.createAnchor();
        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setParent(arFragment.getArSceneView().getScene());

        // Create the transformable model and add it to the anchor.
        TransformableNode model = new TransformableNode(arFragment.getTransformationSystem());
        model.setParent(anchorNode);
        model.setRenderable(this.mRenderable)
                .animate(true).start();
        model.select();

        Node titleNode = new Node();
        titleNode.setParent(model);
        titleNode.setEnabled(false);
        titleNode.setLocalPosition(new Vector3(0.0f, 1.0f, 0.0f));
        titleNode.setRenderable(mViewRenderable);
        titleNode.setEnabled(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (!GeoPermissionsHelper.hasGeoPermissions(this)) {
            // Use toast instead of snackbar here since the activity will exit.
            Toast.makeText(this, "Camera and location permissions are needed to run this application", Toast.LENGTH_LONG)
                    .show();
            if (!GeoPermissionsHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                GeoPermissionsHelper.launchPermissionSettings(this);
            }
            finish();
        }
    }

    private void drawAnchor( Anchor anchor ){
        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setParent(arFragment.getArSceneView().getScene());

        // Create the transformable model and add it to the anchor.
        TransformableNode model = new TransformableNode(arFragment.getTransformationSystem());
        model.setParent(anchorNode);
        model.setRenderable(this.mRenderable)
                .animate(true).start();
        model.select();

        Node titleNode = new Node();
        titleNode.setParent(model);
        titleNode.setEnabled(false);
        titleNode.setLocalPosition(new Vector3(0.0f, 1.0f, 0.0f));
        titleNode.setRenderable(mViewRenderable);
        titleNode.setEnabled(true);
    }

}