package com.hfad.odometer;

import android.Manifest.permission;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;

public class OdometerService extends Service {

    private IBinder mBinder = new OdometerBinder();
    private static double mDistanceInMeters = 0.0;
    private static Location mLastLocation = null;
    private int NOTIFY_ID = 101;
    private int NOTIFY_ID2 = 102;

    public OdometerService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.cancel(NOTIFY_ID);
        manager.cancel(NOTIFY_ID2);
        reset();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (mLastLocation == null) {
                    notifyMe(NOTIFY_ID,"1st onLocationChanged()");
                    mLastLocation = location;
                    mDistanceInMeters = 0.0;
                }

                mDistanceInMeters += location.distanceTo(mLastLocation);
                mLastLocation = location;
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);


        if (ActivityCompat.checkSelfPermission(this, permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            throw new AssertionError("no permission ACCESS_FINE_LOCATION");
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, listener);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public double getMeters() {
        return mDistanceInMeters; // / 1609.344;
    }

    public void reset() {
        mLastLocation = null;
        mDistanceInMeters = 0.0;
    }
    public class OdometerBinder extends Binder {
         OdometerService getOdometer() {
            return OdometerService.this;
        }

    }

    @Override
    public void onRebind(Intent intent) {
        // Is called when activity issues a `bindService` after an `undbindService`
        super.onRebind(intent);
    /* http://stackoverflow.com/a/29372950/6905587 */
    }

    @Override
    public void onDestroy() {
        notifyMe(NOTIFY_ID2, "onDestroy() Distance: "+mDistanceInMeters);
        reset();
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        super.onUnbind(intent);

        /* http://stackoverflow.com/a/29372950/6905587 */
        return true; // future binds from activity will call onRebind
    }

    //onDestroy()
    private  void notifyMe(int id, String text) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new Builder(this)
                .setContentTitle("OdometerService notification")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setDefaults(Notification.DEFAULT_SOUND);
        manager.notify(id, builder.build());
    }
}
