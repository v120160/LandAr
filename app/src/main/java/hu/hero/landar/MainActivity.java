package hu.hero.landar;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;

import java.util.HashSet;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import hu.hero.landar.common.samplerender.SampleRender;
import hu.hero.landar.helpers.ARCoreSessionLifecycleHelper;
import hu.hero.landar.helpers.GeoPermissionsHelper;
import hu.hero.landar.helpers.HelloGeoView;
import hu.hero.landar.helpers.MapTouchWrapper;
import hu.hero.landar.helpers.MapView;

public class MainActivity extends AppCompatActivity{
    private static final String TAG = MainActivity.class.getSimpleName();
    private MainActivity mActivity;
    private static final double MIN_OPENGL_VERSION = 3.0;
    private ArFragment arFragment;
    private Renderable renderable;
    private ViewRenderable viewRenderable;
    private MapTouchWrapper mapTouchWrapper;

    public ARCoreSessionLifecycleHelper mArCoreSessionHelper;
    public HelloGeoRenderer mRenderer;
    public HelloGeoView view;

    public MapView mapView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = this;
        // Setup ARCore session lifecycle helper and configuration.
        Set<Session.Feature> features  = new HashSet<Session.Feature>();
        features.add(Session.Feature.SHARED_CAMERA);
        mArCoreSessionHelper = new ARCoreSessionLifecycleHelper(this,features);
        // If Session creation or Session.resume() fails, display a message and log detailed
        // information.
        mArCoreSessionHelper.setExceptionCallback( exception -> {
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
            view.snackbarHelper.showError(mActivity,message);
//            return null;
        });
        // Configure session features.
        mArCoreSessionHelper.setBeforeSessionResume(session -> {
            // TODO: Configure ARCore to use GeospatialMode.ENABLED.
            // Enable Geospatial Mode.
            session.configure(
                    session.getConfig().setGeospatialMode(Config.GeospatialMode.ENABLED) );
//            return null;

        });
        getLifecycle().addObserver(mArCoreSessionHelper);

        // Set up the Hello AR renderer.
        mRenderer = new HelloGeoRenderer(mActivity);
        getLifecycle().addObserver(mRenderer);

        // Set up Hello AR UI.
        view = new HelloGeoView(this);
        getLifecycle().addObserver(view);
        setContentView(view.root);

        // Sets up an example renderer using our HelloGeoRenderer.
        new SampleRender(view.surfaceView, mRenderer, getAssets());
    }

    @Override
    public void onResume(){
        super.onResume();
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

}