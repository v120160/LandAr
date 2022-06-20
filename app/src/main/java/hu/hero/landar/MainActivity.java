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
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.SceneView;
import com.google.ar.sceneform.Sceneform;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.CameraStream;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.BaseArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.lang.ref.WeakReference;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentOnAttachListener;
import hu.hero.landar.database.PICDATA;
import hu.hero.landar.helpers.GeoPermissionsHelper;
import hu.hero.landar.helpers.MapTouchWrapper;
import hu.hero.landar.helpers.MapView;
import hu.hero.landar.net.GetDataByDistance;

public class MainActivity extends AppCompatActivity implements
        FragmentOnAttachListener,
        BaseArFragment.OnTapArPlaneListener,
        BaseArFragment.OnSessionConfigurationListener,
        ArFragment.OnViewCreatedListener{
    private static final String TAG = MainActivity.class.getSimpleName();
    private MainActivity mActivity;
    private static final double MIN_OPENGL_VERSION = 3.0;
    private ArFragment arFragment;
    private Renderable mModel;
    private ViewRenderable mViewRenderable;
    public MapView mMapView = null;
    private AnchorNode mLastAnchor = null;

    private double mBaseLat = 0;
    private double mBaseLon = 119.0;

    private class PicAnchar extends Anchor{
        public LatLng pos;
    }

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

        // 設定點擊地圖時的動作
        MapTouchWrapper mapTouchWrapper = findViewById(R.id.map_wrapper);
        mapTouchWrapper.setup(screenLocation -> {
            LatLng latLng = mMapView.googleMap.getProjection().fromScreenLocation(screenLocation);
            Log.d("胡征懷",latLng.toString());
            Earth earth = arFragment.getArSceneView().getSession().getEarth();
            if (earth.getTrackingState() == TrackingState.TRACKING ){
                double altitude = earth.getCameraGeospatialPose().getAltitude() ;
                // The rotation quaternion of the anchor in the East-Up-South (EUS) coordinate system.
                float qx = 0f;
                float qy = 0f;
                float qz = 0f;
                float qw = 1f;
                Anchor anchor = earth.createAnchor(latLng.latitude, latLng.longitude, altitude, qx, qy, qz, qw);
                addAnchor(anchor, latLng );
            }
            mActivity.mMapView.addPicMarker( latLng );
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

        // Available modes: DEPTH_OCCLUSION_DISABLED, DEPTH_OCCLUSION_ENABLED
        // 啟用深度及遮蔽
//        arSceneView.getCameraStream().setDepthOcclusionMode(
//                        CameraStream.DepthOcclusionMode.DEPTH_OCCLUSION_ENABLED );

        // 取消平面偵測的白點點
        arSceneView.getPlaneRenderer().setVisible(false);

        // 設定 scene 每次畫面更新時呼叫
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

                // 移動距離大了, 重新要相片清單
                if( distance( cameraGeospatialPose.getLatitude(), cameraGeospatialPose.getLongitude(),
                            mBaseLat , mBaseLon ) > 50 ){
                    mBaseLat = cameraGeospatialPose.getLatitude();
                    mBaseLon = cameraGeospatialPose.getLongitude();
                    GetDataByDistance GDBD = new GetDataByDistance(mActivity);
                    GDBD.get( "VD01" , mBaseLat, mBaseLon, 500  );
                }


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
/*        ModelRenderable.builder()
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

 */
        MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.RED))
                .thenAccept(
                        material -> {
                            // 金屬表面
                            material.setFloat(MaterialFactory.MATERIAL_METALLIC, 1f);
                            // 球型
                     //       mModel = ShapeFactory.makeSphere(1.0f, new Vector3(0.0f, 0.15f, 0.0f), material);
                            // 圓柱
                            mModel = ShapeFactory.makeCylinder(0.5f, 3f,  new Vector3(0.0f, 0.15f, 0.0f), material);
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
        if ( mModel == null || mViewRenderable == null) {
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
        model.setRenderable(this.mModel)
                .animate(true).start();
        model.select();

        Node titleNode = new Node();
        titleNode.setParent(model);
        titleNode.setEnabled(false);
        titleNode.setLocalPosition(new Vector3(0.0f, 1.0f, 0.0f));
        titleNode.setRenderable(mViewRenderable);
        titleNode.setEnabled(true);
    }

    // 從伺服路要相片清單回來
    public void onReadPicListFinish( List<PICDATA> list ){
        Earth earth = arFragment.getArSceneView().getSession().getEarth();
        if (earth.getTrackingState() == TrackingState.TRACKING ) {
            // 高程暫時用手機高程
            double altitude = earth.getCameraGeospatialPose().getAltitude() ;

            // The rotation quaternion of the anchor in the East-Up-South (EUS) coordinate system.
            float qx = 0f;
            float qy = 0f;
            float qz = 0f;
            float qw = 1f;
            for( PICDATA pic : list ) {
                double lat = pic.getCoordy();
                double lon = pic.getCoordx();
                Anchor anchor = earth.createAnchor( lat, lon, altitude, qx, qy, qz, qw);
                AnchorNode anchorNode = new AnchorNode(anchor);
                anchorNode.setParent(arFragment.getArSceneView().getScene());
                // Create the transformable model and add it to the anchor.
                TransformableNode model = new TransformableNode(arFragment.getTransformationSystem());
                model.setParent(anchorNode);
                model.setRenderable( mModel );
                // .animate(true).start();
                model.select();

                // 計算告示牌方向，讓牌子都面向使用者,但是當使用者移動時，也要詬調整方位 @@
                float az=0;
                GeospatialPose base = earth.getCameraGeospatialPose();
                double daz = Math.atan(( lat-base.getLatitude())/(lon-base.getLongitude()));
                az = (float)(daz * 180f / Math.PI ) + 90f;
                // 告示牌
                Node titleNode = new Node();
                titleNode.setParent(model);
                titleNode.setEnabled(false);
                titleNode.setLocalPosition(new Vector3(0.0f, 2.0f, 0.0f));
                titleNode.setLocalRotation( Quaternion.axisAngle(new Vector3(0f, 1f, 0f), az));
                titleNode.setRenderable(mViewRenderable);
                titleNode.setEnabled(true);
            }
        }
        // 在GoogleMap 新增 Marker
        for( PICDATA pic : list ) {
            double lat = pic.getCoordy();
            double lon = pic.getCoordx();
            mActivity.mMapView.addPicMarker( new LatLng(lat, lon) );
        }
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

    private void addAnchor( Anchor anchor , LatLng latlng ){
        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setParent(arFragment.getArSceneView().getScene());

        // Create the transformable model and add it to the anchor.
        TransformableNode model = new TransformableNode(arFragment.getTransformationSystem());
        model.setParent(anchorNode);
        model.setRenderable( mModel );
               // .animate(true).start();
        model.select();

        // 計算告示牌方向，讓牌子都面向使用者,但是當使用者移動時，也要詬調整方位 @@
        float az=0;
        Earth earth = arFragment.getArSceneView().getSession().getEarth();
        GeospatialPose base;
        if (earth.getTrackingState() == TrackingState.TRACKING ) {
            // TODO: the Earth object may be used here.
            base = earth.getCameraGeospatialPose();
            double daz = Math.atan((latlng.latitude-base.getLatitude())/(latlng.longitude- base.getLongitude()));
            az = (float)(daz * 180f / Math.PI ) + 90f;
        }
        // 告示牌
        Node titleNode = new Node();
        titleNode.setParent(model);
        titleNode.setEnabled(false);
        titleNode.setLocalPosition(new Vector3(0.0f, 2.0f, 0.0f));
        titleNode.setLocalRotation( Quaternion.axisAngle(new Vector3(0f, 1f, 0f), az));
        titleNode.setRenderable(mViewRenderable);
        titleNode.setEnabled(true);

            /*
    First, find the vector extending between the two points and define a look rotation
    in terms of this Vector.
*/
        if( mLastAnchor == null)
        {
            mLastAnchor = anchorNode;
            return;
        }
        Vector3 point1 = anchorNode.getWorldPosition();
        Vector3 point2 = mLastAnchor.getWorldPosition();


        final Vector3 difference = Vector3.subtract(point1, point2);
        final Vector3 directionFromTopToBottom = difference.normalized();
        final Quaternion rotationFromAToB =
                Quaternion.lookRotation(directionFromTopToBottom, Vector3.up());
        MaterialFactory.makeOpaqueWithColor(getApplicationContext(), new Color(255, 0, 0))
                .thenAccept(
                        material -> {
                            /* Then, create a rectangular prism, using ShapeFactory.makeCube() and use the difference vector
                                   to extend to the necessary length.  */
                            ModelRenderable lineCube = ShapeFactory.makeCube(
                                    new Vector3(.1f, .1f, difference.length()),
                                    Vector3.zero(), material);
                            /* Last, set the world rotation of the node to the rotation calculated earlier and set the world position to
                                   the midpoint between the given points . */
                            Node node = new Node();
                            node.setParent(anchorNode);
                            node.setRenderable(lineCube);
                            node.setWorldPosition(Vector3.add(point1, point2).scaled(.5f));
                            node.setWorldRotation(rotationFromAToB);
                        }
                );
    }

    public static double distance( double LatA , double LonA , double LatB , double LonB )
    {
        // 東西經,南北緯處理,只在國內可以不處理(假設都是北半球,南半球只有澳洲具有應用意義)
        // 地球半徑(米)
        double R = 6371004;
        double C = Math.sin(Math.toRadians(LatA)) * Math.sin(Math.toRadians(LatB)) + Math.cos(Math.toRadians(LatA)) * Math.cos(Math.toRadians(LatB)) * Math.cos(Math.toRadians(LonA - LonB));
        return (R * Math.acos(C));
    }
}