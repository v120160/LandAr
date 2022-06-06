package hu.hero.landar.helpers;

import android.app.Activity;
import android.util.Log;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.CameraNotAvailableException;

import java.util.Set;

import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;


public class ARCoreSessionLifecycleHelper implements DefaultLifecycleObserver {
    private boolean mInstallRequested = false;
    private Session mSession = null;
    private Set<Session.Feature> mFeatures = null;
    private Activity mActivity = null;

    private ExceptionCallback exceptionCallback = null;
    private BeforeSessionResume beforeSessionResume = null;

    public interface ExceptionCallback{
        void invoke(Exception e);
    }
    public interface BeforeSessionResume{
        void invoke(Session session);
    }

    public ARCoreSessionLifecycleHelper(Activity activity , Set<Session.Feature> set){
        mActivity = activity;
        mFeatures = set;
    }

    private Session tryCreateSession(){
        // The app must have been given the CAMERA permission. If we don't have it yet, request it.
        if (!GeoPermissionsHelper.hasGeoPermissions(mActivity)) {
            GeoPermissionsHelper.requestPermissions(mActivity);
            return null;
        }

        try {
            // Request installation if necessary.
            ArCoreApk.InstallStatus status = ArCoreApk.getInstance().requestInstall( mActivity, !mInstallRequested );
            if( status == ArCoreApk.InstallStatus.INSTALL_REQUESTED ){
                    mInstallRequested = true;
                    // tryCreateSession will be called again, so we return null for now.
                    return null;
            }
            if( status == ArCoreApk.InstallStatus.INSTALLED ){
                    // Left empty; nothing needs to be done.
            }
            // Create a session if Google Play Services for AR is installed and up to date.
            return new Session( mActivity, mFeatures );
        }catch(Exception e) {
            exceptionCallback.invoke(e);
            Log.d( "胡征懷" , e.getMessage());
            return null;
        }
    }


    @Override
    public void onResume( LifecycleOwner owner ) {
        Session session=null;
        if( mSession == null ){
            session = tryCreateSession();
            if( session == null )
                return;
        }else{
            session = mSession;
        }
        try {
            beforeSessionResume.invoke(session);
            session.resume();
            mSession = session;
        }catch(CameraNotAvailableException e) {
            Log.d( "胡征懷" , e.getMessage());
            exceptionCallback.invoke(e);
        }
    }

    @Override
    public void onPause(LifecycleOwner owner) {
        mSession.pause();
    }

    @Override
    public void onDestroy(LifecycleOwner owner) {
        // Explicitly close the ARCore session to release native resources.
        // Review the API reference for important considerations before calling close() in apps with
        // more complicated lifecycle requirements:
        // https://developers.google.com/ar/reference/java/arcore/reference/com/google/ar/core/Session#close()
        mSession.close();
        mSession = null;
    }

    public void setExceptionCallback( ExceptionCallback callback ){
        exceptionCallback = callback;
    }
    public void setBeforeSessionResume( BeforeSessionResume callback ){
        beforeSessionResume = callback;
    }

    public Session getSession(){
        return mSession;
    }
}
