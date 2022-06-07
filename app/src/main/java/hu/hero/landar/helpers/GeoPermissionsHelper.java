package hu.hero.landar.helpers;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class GeoPermissionsHelper {
    private static String PERMISSIONS[] = { Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION };
    /** Check to see we have the necessary permissions for this app.  */
    public static boolean hasGeoPermissions(Activity activity){
        for (String per:PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(activity, per) != PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }

    /** Check to see we have the necessary permissions for this app, and ask for them if we don't.  */
    public static void requestPermissions(Activity activity) {
        ActivityCompat.requestPermissions(activity, PERMISSIONS, 0);
    }

    /** Check to see if we need to show the rationale for this permission.  */
    public static boolean shouldShowRequestPermissionRationale( Activity activity ){
        for (String per:PERMISSIONS) {
            if( ActivityCompat.shouldShowRequestPermissionRationale( activity , per ) )
                return true;
        }
        return false;
    }

    /** Launch Application Setting to grant permission.  */
    public static void launchPermissionSettings(Activity activity) {
        Intent intent = new Intent();
        intent.setAction( Settings.ACTION_APPLICATION_DETAILS_SETTINGS );
        intent.setData( Uri.fromParts("package", activity.getPackageName(), null));
        activity.startActivity(intent);
    }

}