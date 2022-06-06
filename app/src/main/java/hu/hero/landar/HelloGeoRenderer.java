package hu.hero.landar;

import android.opengl.Matrix;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.ar.core.Anchor;
import com.google.ar.core.Camera;
import com.google.ar.core.Earth;
import com.google.ar.core.Frame;
import com.google.ar.core.GeospatialPose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;

import java.io.IOException;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import hu.hero.landar.common.helpers.DisplayRotationHelper;
import hu.hero.landar.common.helpers.TrackingStateHelper;
import hu.hero.landar.common.samplerender.Framebuffer;
import hu.hero.landar.common.samplerender.Mesh;
import hu.hero.landar.common.samplerender.SampleRender;
import hu.hero.landar.common.samplerender.Shader;
import hu.hero.landar.common.samplerender.Texture;
import hu.hero.landar.common.samplerender.arcore.BackgroundRenderer;

public class HelloGeoRenderer implements SampleRender.Renderer, DefaultLifecycleObserver {
    private String TAG = "HelloGeoRenderer";
    private float  Z_NEAR = 0.1f;
    private float  Z_FAR = 1000f;
    private MainActivity   mActivity;
    private BackgroundRenderer mBackgroundRenderer;
    private Framebuffer mVirtualSceneFramebuffer;
    private boolean            mHasSetTextureNames = false;

    // Virtual object (ARCore pawn)
    private Mesh mVirtualObjectMesh;
    private Shader mVirtualObjectShader;
    private Texture mVirtualObjectTexture;


    // Temporary matrix allocated here to reduce number of allocations for each frame.
    float [] modelMatrix = new float[16];
    float [] viewMatrix = new float[16];
    float [] projectionMatrix = new float[16];
    float [] modelViewMatrix = new float[16]; // view x model

    float [] modelViewProjectionMatrix = new float[16]; // projection x view x model

    private DisplayRotationHelper mDisplayRotationHelper;
    private TrackingStateHelper mTrackingStateHelper;
    private Session mSession;
//    private Anchor  mEarthAnchor = null;
    private ArrayList<Anchor> mAnchors = new ArrayList<>();


    public HelloGeoRenderer(MainActivity activity ){
        mActivity = activity;
//        mSession = mActivity.arCoreSessionHelper.getSession();
        mDisplayRotationHelper = new DisplayRotationHelper(mActivity);
        mTrackingStateHelper = new TrackingStateHelper(mActivity);
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        mDisplayRotationHelper.onResume();
        mHasSetTextureNames = false;
        // 胡移的
        mSession = mActivity.mArCoreSessionHelper.getSession();
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        mDisplayRotationHelper.onPause();
    }

    @Override
    public void onSurfaceCreated(SampleRender render) {
        // Prepare the rendering objects.
        // This involves reading shaders and 3D model files, so may throw an IOException.
        try {
            mBackgroundRenderer = new BackgroundRenderer(render);
            mVirtualSceneFramebuffer = new Framebuffer(render,1, 1);

            // Virtual object to render (Geospatial Marker)
            mVirtualObjectTexture =
                    Texture.createFromAsset(
                            render,
                            "models/spatial_marker_baked.png",
                            Texture.WrapMode.CLAMP_TO_EDGE,
                            Texture.ColorFormat.SRGB
                    );

            mVirtualObjectMesh = Mesh.createFromAsset(render, "models/geospatial_marker.obj");
            mVirtualObjectShader =
                    Shader.createFromAssets(
                            render,
                            "shaders/ar_unlit_object.vert",
                            "shaders/ar_unlit_object.frag",
                             null)
                             .setTexture("u_Texture", mVirtualObjectTexture);

            mBackgroundRenderer.setUseDepthVisualization(render, false);
            mBackgroundRenderer.setUseOcclusion(render, false);
        } catch (IOException e) {
            Log.e(TAG, "Failed to read a required asset file", e);
            showError("Failed to read a required asset file: $e");
        }

    }

    @Override
    public void onSurfaceChanged(SampleRender render, int width, int height) {
        mDisplayRotationHelper.onSurfaceChanged(width, height);
        mVirtualSceneFramebuffer.resize(width, height);
    }
    //</editor-fold>

    @Override
    public void onDrawFrame(SampleRender render) {
        if( mSession == null )return;

        //<editor-fold desc="ARCore frame boilerplate" defaultstate="collapsed">
        // Texture names should only be set once on a GL thread unless they change. This is done during
        // onDrawFrame rather than onSurfaceCreated since the session is not guaranteed to have been
        // initialized during the execution of onSurfaceCreated.
        if (!mHasSetTextureNames) {
            int [] ids = new int[1];
            ids[0] = mBackgroundRenderer.getCameraColorTexture().getTextureId();
            mSession.setCameraTextureNames(ids);
            mHasSetTextureNames = true;
        }
        // -- Update per-frame state

        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        mDisplayRotationHelper.updateSessionIfNeeded(mSession);

        // Obtain the current frame from ARSession. When the configuration is set to
        // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
        // camera framerate.
        Frame frame;
        try {
            frame = mSession.update();
        } catch (CameraNotAvailableException e) {
            Log.e(TAG, "Camera not available during onDrawFrame", e);
            showError("Camera not available. Try restarting the app.");
            return;
        }
        Camera camera = frame.getCamera();
        // BackgroundRenderer.updateDisplayGeometry must be called every frame to update the coordinates
        // used to draw the background camera image.
        mBackgroundRenderer.updateDisplayGeometry(frame);

        // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
        mTrackingStateHelper.updateKeepScreenOnFlag(camera.getTrackingState());

        // -- Draw background
        if (frame.getTimestamp() != 0L) {
            // Suppress rendering if the camera did not produce the first frame yet. This is to avoid
            // drawing possible leftover data from previous sessions if the texture is reused.
            mBackgroundRenderer.drawBackground(render);
        }

        // If not tracking, don't draw 3D objects.
        if (camera.getTrackingState() == TrackingState.PAUSED) {
            return;
        }

        // Get projection matrix.
        camera.getProjectionMatrix(projectionMatrix, 0, Z_NEAR, Z_FAR);

        // Get camera matrix and draw.
        camera.getViewMatrix(viewMatrix, 0);

        render.clear(mVirtualSceneFramebuffer, 0f, 0f, 0f, 0f);
        //</editor-fold>

        // TODO: Obtain Geospatial information and display it on the map.
        Earth earth = mSession.getEarth();
        if (earth.getTrackingState() == TrackingState.TRACKING ){
            // TODO: the Earth object may be used here.
            GeospatialPose cameraGeospatialPose = earth.getCameraGeospatialPose();
            mActivity.view.mapView.updateMapPosition(
                    cameraGeospatialPose.getLatitude(),
                    cameraGeospatialPose.getLongitude(),
                    cameraGeospatialPose.getHeading()
            );
            // Draw the placed anchor, if it exists.
//            if( mEarthAnchor != null )
//                renderCompassAtAnchor( render , mEarthAnchor );
            for (Anchor anchor:mAnchors) {
                renderCompassAtAnchor( render , anchor );
            }
        }

        // Compose the virtual scene with the background.
        mBackgroundRenderer.drawVirtualScene(render, mVirtualSceneFramebuffer, Z_NEAR, Z_FAR);
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

        Anchor anchor = earth.createAnchor(latlng.latitude, latlng.longitude, altitude, qx, qy, qz, qw);
        mAnchors.add( anchor );

        // Show the placed marker on the map
        //mActivity.view.mapView.earthMarker.setPosition(latlng);
        //mActivity.view.mapView.earthMarker.setVisible(true);
        mActivity.view.mapView.addMarker( latlng );
    }

    private void renderCompassAtAnchor( SampleRender render , Anchor anchor ) {
        // Get the current pose of the Anchor in world space. The Anchor pose is updated
        // during calls to session.update() as ARCore refines its estimate of the world.
        anchor.getPose().toMatrix(modelMatrix, 0);

        // Calculate model/view/projection matrices
        Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0);

        // Update shader properties and draw
        mVirtualObjectShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix);
        render.draw(mVirtualObjectMesh, mVirtualObjectShader, mVirtualSceneFramebuffer);
    }

    private void renderPhotoAnchor( SampleRender render , Anchor anchor ) {
        // Get the current pose of the Anchor in world space. The Anchor pose is updated
        // during calls to session.update() as ARCore refines its estimate of the world.
        anchor.getPose().toMatrix(modelMatrix, 0);

        // Calculate model/view/projection matrices
        Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0);

        // Update shader properties and draw
        mVirtualObjectShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix);
        render.draw(mVirtualObjectMesh, mVirtualObjectShader, mVirtualSceneFramebuffer);
    }

    private void showError(String errorMessage) {
        mActivity.view.snackbarHelper.showError(mActivity, errorMessage);
    }
}
