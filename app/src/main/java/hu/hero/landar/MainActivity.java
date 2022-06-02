package hu.hero.landar;

import android.app.ActivityManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION_CODES;
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
import java.util.HashSet;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentOnAttachListener;
import hu.hero.landar.helpers.ARCoreSessionLifecycleHelper;
import hu.hero.landar.helpers.MapTouchWrapper;
import hu.hero.landar.helpers.MapView;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class MainActivity extends AppCompatActivity implements
        FragmentOnAttachListener,
        BaseArFragment.OnTapArPlaneListener,
        BaseArFragment.OnSessionConfigurationListener,
        ArFragment.OnViewCreatedListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private MainActivity mActivity;
    private static final double MIN_OPENGL_VERSION = 3.0;
    private Session    mSession;
    private ArFragment arFragment;
    private Renderable renderable;
    private ViewRenderable viewRenderable;
    MapTouchWrapper mapTouchWrapper;
    public ARCoreSessionLifecycleHelper arCoreSessionHelper;
/*
    private static class AnimationInstance {
        Animator animator;
        Long startTime;
        float duration;
        int index;

        AnimationInstance(Animator animator, int index, Long startTime) {
            this.animator = animator;
            this.startTime = startTime;
            this.duration = animator.getAnimationDuration(index);
            this.index = index;
        }
    }

    private final Set<AnimationInstance> animators = new ArraySet<>();

    private final List<Color> colors =
            Arrays.asList(
                    new Color(0, 0, 0, 1),
                    new Color(1, 0, 0, 1),
                    new Color(0, 1, 0, 1),
                    new Color(0, 0, 1, 1),
                    new Color(1, 1, 0, 1),
                    new Color(0, 1, 1, 1),
                    new Color(1, 0, 1, 1),
                    new Color(1, 1, 1, 1));
    private int nextColor = 0;

 */

    public MapView mapView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = this;
        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }

        setContentView(R.layout.activity_main);
        getSupportFragmentManager().addFragmentOnAttachListener(this);

        // Setup ARCore session lifecycle helper and configuration.
        Set<Session.Feature> features  = new HashSet<Session.Feature>();
        features.add(Session.Feature.SHARED_CAMERA);
        arCoreSessionHelper = new ARCoreSessionLifecycleHelper(this,features);
        // If Session creation or Session.resume() fails, display a message and log detailed
        // information.
        arCoreSessionHelper.setExceptionCallback( new Function1() {
            @Override
            public Object invoke(Object o) {
                return null;
            }
            public final void invoke(Exception exception) {
                String message="";
                String a = exception.getClass().toString();
                switch( exception.getClass().toString() ) {
                    case "UnavailableUserDeclinedInstallationException":
                        message = "Please install Google Play Services for AR";
                    case "UnavailableApkTooOldException":
                        message = "Please update ARCore";
                    case "UnavailableSdkTooOldException":
                        message = "Please update this app";
                    case "UnavailableDeviceNotCompatibleException":
                        message = "This device does not support AR";
                    case "CameraNotAvailableException":
                        message = "Camera not available. Try restarting the app.";
                    default:
                        message = "Failed to create AR session";
                }
                Log.e( TAG, "ARCore threw an exception", exception);
            }
        } );
        // Configure session features.
        arCoreSessionHelper.setBeforeSessionResume( new Function1(){
            public Object invoke(Object var1) {
                this.invoke((Session)var1);
                return Unit.INSTANCE;
            }
            public final void invoke(Session session) {
                // TODO: Configure ARCore to use GeospatialMode.ENABLED.
                // Enable Geospatial Mode.
                session.configure(
                        session.getConfig().setGeospatialMode(Config.GeospatialMode.ENABLED) );
            }
        });
        getLifecycle().addObserver(arCoreSessionHelper);

        if (savedInstanceState == null) {
            if (Sceneform.isSupported(this)) {
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.arFragment, ArFragment.class, null)
                        .commit();
            }
        }

        mapTouchWrapper = findViewById(R.id.map_wrapper);
        mapTouchWrapper.setup( screenLocation -> {
            LatLng latLng = mapView.googleMap.getProjection().fromScreenLocation(screenLocation);
            mActivity.onMapClick(latLng);
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady( GoogleMap googleMap) {
                mapView = new MapView( mActivity, googleMap);
            }
        });

        loadModels();
/*
        ModelRenderable.builder()
                .setSource(
                        this,
                        Uri.parse(
                                "https://storage.googleapis.com/ar-answers-in-search-models/static/Tiger/model.glb"))
//                .setIsFilamentGltf(true)
                .build()
                .thenAccept(
                        modelRenderable -> {
                            MainActivity activity = weakActivity.get();
                            if (activity != null) {
                                activity.renderable = modelRenderable;
                            }
                        })
                .exceptionally(
                        throwable -> {
                            Toast toast =
                                    Toast.makeText(this, "Unable to load Tiger renderable", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return null;
                        });

        arFragment.setOnTapArPlaneListener(
                (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
                    if (renderable == null) {
                        return;
                    }

                    // Create the Anchor.
                    Anchor anchor = hitResult.createAnchor();
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    anchorNode.setParent(arFragment.getArSceneView().getScene());

                    // Create the transformable model and add it to the anchor.
                    TransformableNode model = new TransformableNode(arFragment.getTransformationSystem());
                    model.setParent(anchorNode);
                    model.setRenderable(renderable);
                    model.select();

                    FilamentAsset filamentAsset = model.getRenderableInstance().getFilamentAsset();
                    if (filamentAsset.getAnimator().getAnimationCount() > 0) {
                        animators.add(new AnimationInstance(filamentAsset.getAnimator(), 0, System.nanoTime()));
                    }

                    Color color = colors.get(nextColor);
                    nextColor++;
                    for (int i = 0; i < renderable.getSubmeshCount(); ++i) {
                        Material material = renderable.getMaterial(i);
                        material.setFloat4("baseColorFactor", color);
                    }

                    Node tigerTitleNode = new Node();
                    tigerTitleNode.setParent(model);
                    tigerTitleNode.setEnabled(false);
                    tigerTitleNode.setLocalPosition(new Vector3(0.0f, 1.0f, 0.0f));
                    ViewRenderable.builder()
                            .setView(this, R.layout.tiger_card_view)
                            .build()
                            .thenAccept(
                                    (renderable) -> {
                                        tigerTitleNode.setRenderable(renderable);
                                        tigerTitleNode.setEnabled(true);
                                    })
                            .exceptionally(
                                    (throwable) -> {
                                        throw new AssertionError("Could not load card view.", throwable);
                                    }
                            );
                });

        arFragment
                .getArSceneView()
                .getScene()
                .addOnUpdateListener(
                        frameTime -> {
                            Long time = System.nanoTime();
                            for (AnimationInstance animator : animators) {
                                animator.animator.applyAnimation(
                                        animator.index,
                                        (float) ((time - animator.startTime) / (double) SECONDS.toNanos(1))
                                                % animator.duration);
                                animator.animator.updateBoneMatrices();
                            }
                        });
*/
    }

    @Override
    public void onResume(){
        super.onResume();
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
        if( session.isGeospatialModeSupported(Config.GeospatialMode.ENABLED) ) {
            Log.d("胡征懷", "isGeospatialModeSupported ENABLED");
//            config.setGeospatialMode(Config.GeospatialMode.ENABLED);
        }else
            Log.d("胡征懷","isGeospatialModeSupported DISABLED");

        mSession = session;


//        config.setGeospatialMode(Config.GeospatialMode.ENABLED);

//        // 去除平面的白點點
//        config.setPlaneFindingMode(Config.PlaneFindingMode.DISABLED);
//        session.configure(config);
    }

    @Override
    public void onViewCreated(ArSceneView arSceneView) {
        arFragment.setOnViewCreatedListener(null);

        // Fine adjust the maximum frame rate
        arSceneView.setFrameRateFactor(SceneView.FrameRate.FULL);
        arSceneView.getScene().addOnUpdateListener(this::onSceneUpdate);
    }

    private void onSceneUpdate(FrameTime updatedTime) {
        if( mSession == null )
            return;
        // TODO: Obtain Geospatial information and display it on the map.
        Earth earth = mSession.getEarth();
        if ( earth !=null && earth.getTrackingState() == TrackingState.TRACKING ){
            // TODO: the Earth object may be used here.
            GeospatialPose cameraGeospatialPose = earth.getCameraGeospatialPose();
            mActivity.mapView.updateMapPosition(
                    cameraGeospatialPose.getLatitude(),
                    cameraGeospatialPose.getLongitude(),
                    cameraGeospatialPose.getHeading()
            );
            // Draw the placed anchor, if it exists.
//            if( mEarthAnchor != null )
//                renderCompassAtAnchor( render , mEarthAnchor );
//            for (Anchor anchor:mAnchors) {
//                renderCompassAtAnchor( render , anchor );
//            }
        }
    }
    public void loadModels() {
        WeakReference<MainActivity> weakActivity = new WeakReference<>(this);
        ModelRenderable.builder()
//                .setSource(this, Uri.parse("https://storage.googleapis.com/ar-answers-in-search-models/static/Tiger/model.glb"))
                .setSource(this,Uri.parse("models/Rabbit.glb"))
                .setIsFilamentGltf(true)
                .setAsyncLoadEnabled(true)
                .build()
                .thenAccept(model -> {
                    MainActivity activity = weakActivity.get();
                    if (activity != null) {
                        activity.renderable = model;
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
                        activity.viewRenderable = viewRenderable;
                    }
                })
                .exceptionally(throwable -> {
                    Toast.makeText(this, "Unable to load model", Toast.LENGTH_LONG).show();
                    return null;
                });
    }

    @Override
    public void onTapPlane(HitResult hitResult, Plane plane, MotionEvent motionEvent) {
        if (renderable == null || viewRenderable == null) {
            Toast.makeText(this, "Loading...", Toast.LENGTH_SHORT).show();
            return;
        }
        Earth earth = mSession.getEarth();
        if (earth.getTrackingState() != TrackingState.TRACKING)
            return;

        // Create the Anchor.
        Anchor anchor = hitResult.createAnchor();
        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setParent(arFragment.getArSceneView().getScene());

        // Create the transformable model and add it to the anchor.
        TransformableNode model = new TransformableNode(arFragment.getTransformationSystem());
        model.setParent(anchorNode);
        model.setRenderable(this.renderable)
                .animate(true).start();
        model.select();

        Node titleNode = new Node();
        titleNode.setParent(model);
        titleNode.setEnabled(false);
        titleNode.setLocalPosition(new Vector3(0.0f, 1.0f, 0.0f));
        titleNode.setRenderable(viewRenderable);
        titleNode.setEnabled(true);
    }

    /**
     * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
     * on this device.
     *
     * <p>Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
     *
     * <p>Finishes the activity if Sceneform can not run
     */
    public static boolean checkIsSupportedDeviceOrFinish(final AppCompatActivity activity) {
        if (Build.VERSION.SDK_INT < VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later");
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        String openGlVersionString =
                ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }

    public void onMapClick( LatLng latlng ){
        // TODO: place an anchor at the given position.
        if( mSession == null )return;
        Earth earth = mSession.getEarth();
        if( earth == null )return;
        if (earth.getTrackingState() != TrackingState.TRACKING) {
            return;
        }
//        if( mEarthAnchor != null )
//            mEarthAnchor.detach();
        // Place the earth anchor at the same altitude as that of the camera to make it easier to view.
        double altitude = earth.getCameraGeospatialPose().getAltitude() - 1;
        // The rotation quaternion of the anchor in the East-Up-South (EUS) coordinate system.
        float qx = 0f;
        float qy = 0f;
        float qz = 0f;
        float qw = 1f;

        // Create the Anchor.
        Anchor anchor = earth.createAnchor(latlng.latitude, latlng.longitude, altitude, qx, qy, qz, qw);

        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setParent(arFragment.getArSceneView().getScene());

        // Create the transformable model and add it to the anchor.
        TransformableNode model = new TransformableNode(arFragment.getTransformationSystem());
        model.setParent(anchorNode);
        model.setRenderable(this.renderable)
                .animate(true).start();
        model.select();

        Node titleNode = new Node();
        titleNode.setParent(model);
        titleNode.setEnabled(false);
        titleNode.setLocalPosition(new Vector3(0.0f, 1.0f, 0.0f));
        titleNode.setRenderable(viewRenderable);
        titleNode.setEnabled(true);
        // Show the placed marker on the map
        //mActivity.view.mapView.earthMarker.setPosition(latlng);
        //mActivity.view.mapView.earthMarker.setVisible(true);
        mActivity.mapView.addMarker( latlng );
    }
}