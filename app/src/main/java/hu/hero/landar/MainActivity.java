package hu.hero.landar;

import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
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

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentOnAttachListener;
import hu.hero.landar.Geo.Point3;
import hu.hero.landar.common.JumpingNode;
import hu.hero.landar.common.RotatingNode;
import hu.hero.landar.database.PICDATA3D;
import hu.hero.landar.helpers.GeoPermissionsHelper;
import hu.hero.landar.helpers.HUMapView;
import hu.hero.landar.helpers.MapTouchWrapper;
import hu.hero.landar.net.GetDataByDistance;

public class MainActivity extends AppCompatActivity implements
        FragmentOnAttachListener,
        BaseArFragment.OnTapArPlaneListener,
        BaseArFragment.OnSessionConfigurationListener,
        ArFragment.OnViewCreatedListener{
    private static final String TAG = MainActivity.class.getSimpleName();
    private MainActivity mActivity;
    private int mLastOrientation;
    private TextView mPosInfo;
    private MapTouchWrapper mMapPannel;
    private static final double MIN_OPENGL_VERSION = 3.0;
    private ArFragment arFragment;
    private Renderable mPicModel;
    private Renderable mBaseModel;
    private Renderable mBPointModel;
    private Renderable mYellowTubeModel;
    private Renderable mTubeModel;
    private Renderable mBlueTubeModel;
    private ViewRenderable mViewRenderable;

    public HUMapView mMapView = null;
    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";
    public static final String KEY_LAST_ORIENTATION = "last_orientation";

    private AnchorNode mLastAnchor = null;
    private Earth mEarth = null;

    private double mBaseLat = 0;
    private double mBaseLon = 119.0;
    private double mBaseElevate = 20.2;
    private double mBaseAltitute = 0;

    private class PicAnchar extends Anchor{
        public LatLng pos;
    }

    private Earth getEarth(){
        if( arFragment != null ) {
            ArSceneView sceneview = arFragment.getArSceneView();
            if (sceneview != null) {
                Session session = sceneview.getSession();
                if (session != null) {
                    return session.getEarth();
                }
            }
        }
        return null;
    }

    private boolean getEarthTrackingState(){
        mEarth = getEarth();
        if( mEarth == null )
            return false;
        return mEarth.getTrackingState() == TrackingState.TRACKING;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = this;

        setContentView(R.layout.activity_main);

        getSupportFragmentManager().addFragmentOnAttachListener(this);

        if (savedInstanceState == null) {
            mLastOrientation = getResources().getConfiguration().orientation;
            if (Sceneform.isSupported(this)) {
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.arFragment, ArFragment.class, null)
                        .commit();
            }
        }
        loadModels();

        mPosInfo = findViewById(R.id.posinfo);
        // 設定點擊地圖時的動作
        mMapPannel = findViewById(R.id.map_wrapper);
        mMapPannel.setRotate(Configuration.ORIENTATION_PORTRAIT);

        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }
        mMapView = (HUMapView) findViewById(R.id.mapview);
        mMapView.onCreate(mapViewBundle);
    }

    /*
        設備旋轉時
    */


    /* **  注意 **
    AndroidManifest.xml 要設定 android:configChanges=orientation , 手機旋轉時才能保持session
    */
    @Override
    public void onConfigurationChanged(@NotNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mMapPannel.setRotate(Configuration.ORIENTATION_LANDSCAPE);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            mMapPannel.setRotate(Configuration.ORIENTATION_PORTRAIT);
        }
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
//                       CameraStream.DepthOcclusionMode.DEPTH_OCCLUSION_ENABLED );
        arSceneView.getCameraStream().setDepthOcclusionMode(
                        CameraStream.DepthOcclusionMode.DEPTH_OCCLUSION_DISABLED );

        // 取消平面偵測的白點點
//        arSceneView.getPlaneRenderer().setVisible(false);

        // 設定 scene 每次畫面更新時呼叫
        arSceneView.getScene().addOnUpdateListener(frameTime->{
            if ( getEarthTrackingState() ){
                // TODO: the Earth object may be used here.
                GeospatialPose cameraGeospatialPose = mEarth.getCameraGeospatialPose();
                double lat = cameraGeospatialPose.getLatitude();
                double lon = cameraGeospatialPose.getLongitude();
                double mBaseAltitute = cameraGeospatialPose.getAltitude();
                double heading = cameraGeospatialPose.getHeading();

                if( mPosInfo != null )
                    mPosInfo.setText( String.format("高程：%.2f",mBaseAltitute));
                if( mMapView != null )
                    mMapView.updateMapPosition( lat, lon, heading );

                // 移動距離大了, 重新向伺服器要資料
                if( distance( cameraGeospatialPose.getLatitude(), cameraGeospatialPose.getLongitude(),
                            mBaseLat , mBaseLon ) > 50 ){
                    System.out.println("移動距離大了, 重新向伺服器要資料" );
                    mBaseLat = cameraGeospatialPose.getLatitude();
                    mBaseLon = cameraGeospatialPose.getLongitude();
                    GetDataByDistance GDBD = new GetDataByDistance(mActivity);
                    GDBD.get( "VD01" , mBaseLat, mBaseLon, 100  );
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

        // 相片的 3D Model
        CompletableFuture<ModelRenderable> marker = ModelRenderable
                .builder()
                .setSource(this
                        , Uri.parse("models/scene.gltf"))
                .setIsFilamentGltf(true)
                .setAsyncLoadEnabled(true)
                .build();
        CompletableFuture.allOf(marker)
                .handle((ok, ex) -> {
                    try {
                        mPicModel = marker.get();
                    } catch (InterruptedException | ExecutionException ignore) {

                    }
                    return null;
                });

        // 相片基礎點的 3D Model
        MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.RED))
                .thenAccept(
                        material -> {
                            // 金屬表面
                            material.setFloat(MaterialFactory.MATERIAL_METALLIC, 1f);
                            // 圓柱
                            mBaseModel = ShapeFactory.makeCylinder(0.4f, 0.3f,  new Vector3(0.0f, 0.15f, 0.0f), material);
                        });

        // 界均點 3D Model
        MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.BLUE))
                .thenAccept(
                        material -> {
                            // 金屬表面
                            material.setFloat(MaterialFactory.MATERIAL_METALLIC, 1f);
                            // 圓柱
                            mBPointModel = ShapeFactory.makeCylinder(0.4f, 0.3f,  new Vector3(0.0f, 0.15f, 0.0f), material);
                        });



        // 告示牌的 Model
        WeakReference<MainActivity> weakActivity = new WeakReference<>(this);
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

    /*
        點擊螢幕畫面時
    */
    @Override
    public void onTapPlane(HitResult hitResult, Plane plane, MotionEvent motionEvent) {
        Anchor anchor = hitResult.createAnchor();
        createPicNode( anchor );
    }

    /*
        建立相片的跳動圖示
    */
    private Node createPicNode( Anchor anchor ){
        AnchorNode baseNode = new AnchorNode(anchor);
        baseNode.setRenderable( mBaseModel );
        JumpingNode jNode = new JumpingNode();
        jNode.setJumpingBegin(0.3f);
        jNode.setJumpingHeight(25.0f);
        jNode.setJumpingPace(10.0f);
        jNode.setParent( baseNode );
        RotatingNode picNode = new RotatingNode(false);
        picNode.setRenderable( mPicModel );
        picNode.setParent( jNode );
        picNode.setLocalScale(new Vector3(0.2f, 0.2f, 0.2f));
        arFragment.getArSceneView().getScene().addChild(baseNode);
        return picNode;
    }
    /*
       從伺服路要圖資回來
    */
    public void onReadDataFinish( GetDataByDistance.SpatialIndexPackage pack ){
        if( pack == null )
        {
            Toast.makeText(this, "伺服主機取得資料失敗", Toast.LENGTH_LONG)
                    .show();
            return;
        }

        List <PICDATA3D> picList = pack.pics;
        List<List<Point3>>  ptLists = pack.ptlists;
        if( getEarthTrackingState() ){
            // The rotation quaternion of the anchor in the East-Up-South (EUS) coordinate system.
            float qx = 0f;
            float qy = 0f;
            float qz = 0f;
            float qw = 1f;

            // 宗地
            for( List<Point3> list :ptLists ) {
                AnchorNode lastNode=null;
                AnchorNode firstNode=null;
                for( int i=0 ; i<list.size() ; i++ ){
                    Point3 p = list.get(i);
                    Anchor anchor = mEarth.createAnchor( p.y, p.x, p.h, qx, qy, qz, qw);
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    anchorNode.setParent(arFragment.getArSceneView().getScene());
                    // Create the transformable model and add it to the anchor.
                    TransformableNode model = new TransformableNode(arFragment.getTransformationSystem());
                    model.setParent(anchorNode);
                    model.setRenderable( mBPointModel );
                    // .animate(true).start();
                    model.select();
                    if( i==0 ) {
                        lastNode = firstNode = anchorNode;
                    }
                    if( i > 0 ) {
                        addLine(lastNode, anchorNode);
                        lastNode = anchorNode;
                    }
                }
                addLine(lastNode, firstNode);
            }

            // 相片
            for( PICDATA3D pic : picList ) {
                double lat = pic.getCoordy();
                double lon = pic.getCoordx();
                double h = pic.getElevation();
                Anchor anchor = mEarth.createAnchor( lat, lon, h, qx, qy, qz, qw);
                Node picNode = createPicNode( anchor );

                // 計算告示牌方向，讓牌子都面向使用者,但是當使用者移動時，也要詬調整方位 @@
                float az=0;
                GeospatialPose base = mEarth.getCameraGeospatialPose();
                double daz = Math.atan(( lat-base.getLatitude())/(lon-base.getLongitude()));
                az = (float)(daz * 180f / Math.PI ) + 90f;
 /*               // 告示牌
                Node titleNode = new Node();
                titleNode.setParent(model);
                titleNode.setEnabled(false);
                titleNode.setLocalPosition(new Vector3(0.0f, 2.0f, 0.0f));
                titleNode.setLocalRotation( Quaternion.axisAngle(new Vector3(0f, 1f, 0f), az));
                titleNode.setRenderable(mViewRenderable);
                titleNode.setEnabled(true);*/
            }
        }
        // 在GoogleMap 新增經界線 Marker
        for( List<Point3> list :ptLists ) {
            if( mMapView != null )
                mMapView.addParcelMarker( list );
        }
        for( PICDATA3D pic : picList ) {
            double lat = pic.getCoordy();
            double lon = pic.getCoordx();
            if( mMapView != null )
                mMapView.addPicMarker( new LatLng(lat, lon) );
        }

    }

    // 程式設定狀態存取
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mLastOrientation = savedInstanceState.getInt(KEY_LAST_ORIENTATION);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt(KEY_LAST_ORIENTATION, mLastOrientation);
        Bundle mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            savedInstanceState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle);
        }
        mMapView.onSaveInstanceState(mapViewBundle);
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

    /*
     點擊地圖時新增 anchor
    */
    private void addAnchor( Anchor anchor , LatLng latlng ){
        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setParent(arFragment.getArSceneView().getScene());

        // Create the transformable model and add it to the anchor.
        TransformableNode model = new TransformableNode(arFragment.getTransformationSystem());
        model.setParent(anchorNode);
        model.setRenderable( mPicModel );
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

    private void addLine( AnchorNode a1 , AnchorNode a2 ) {
        Vector3 point1 = a1.getWorldPosition();
        Vector3 point2 = a2.getWorldPosition();
        final Vector3 difference = Vector3.subtract(point1, point2);
        final Vector3 directionFromTopToBottom = difference.normalized();
        final Quaternion rotationFromAToB =
                Quaternion.lookRotation(directionFromTopToBottom, Vector3.up());
        MaterialFactory.makeOpaqueWithColor(getApplicationContext(), new Color(0, 0, 255))
            .thenAccept(
            material -> {
                            /* Then, create a rectangular prism, using ShapeFactory.makeCube() and use the difference vector
                                   to extend to the necessary length.  */
                ModelRenderable lineCube = ShapeFactory.makeCube(
                        new Vector3(.25f, .25f, difference.length()),
                        Vector3.zero(), material);
                            /* Last, set the world rotation of the node to the rotation calculated earlier and set the world position to
                                   the midpoint between the given points . */
                Node node = new Node();
                node.setParent(a2);
                node.setRenderable(lineCube);
                node.setWorldPosition(Vector3.add(point1, point2).scaled(.5f));
                node.setWorldRotation(rotationFromAToB);
            });
    }
    public static double distance( double LatA , double LonA , double LatB , double LonB )
    {
        // 東西經,南北緯處理,只在國內可以不處理(假設都是北半球,南半球只有澳洲具有應用意義)
        // 地球半徑(米)
        double R = 6371004;
        double C = Math.sin(Math.toRadians(LatA)) * Math.sin(Math.toRadians(LatB)) + Math.cos(Math.toRadians(LatA)) * Math.cos(Math.toRadians(LatB)) * Math.cos(Math.toRadians(LonA - LonB));
        return (R * Math.acos(C));
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
        Log.d("MainActivity","onResume called");
    }

    @Override
    protected void onStart() {
        super.onStart();
        mMapView.onStart();
        Log.d("MainActivity","onStart called");
    }

    @Override
    protected void onStop() {
        super.onStop();
        mMapView.onStop();
        Log.d("MainActivity","onStop called");
    }


    @Override
    protected void onPause() {
        mMapView.onPause();
        super.onPause();
        Log.d("MainActivity","onPause called");
    }

    @Override
    protected void onDestroy() {
        mMapView.onDestroy();
        super.onDestroy();
        Log.d("MainActivity","onDestroy called");
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

}