package com.hfad.odometer;

import android.Manifest.permission;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.TextView;

import com.hfad.odometer.OdometerService.OdometerBinder;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.support.design.widget.Snackbar.LENGTH_LONG;

public class MainActivity extends AppCompatActivity {

    private static OdometerService mOdometerService;
    private static Boolean mBound = false;

    private Button buttonStart;
    private Button buttonFinish;

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            OdometerBinder binder = (OdometerBinder) iBinder;
            mOdometerService = binder.getOdometer();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBound = false;
            mOdometerService = null;
        }
    };

    private static final int PERMISSION_REQUEST_CODE = 101;


    @Override
    public void onBackPressed() {

        if (mBound) {
            Snackbar snackbar = Snackbar.make(MainActivity.this.findViewById(R.id.activity_main), "You must Finish odometer first", LENGTH_LONG)
                    /*.setAction("FINISH", new OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            finishOdometer(view);
                            MainActivity.this.finish();
                        }
                    })
                    .setActionTextColor(Color.RED)*/;
            snackbar.show();
            return;
        }
        super.onBackPressed();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonStart = (Button) findViewById(R.id.button_start);
        buttonFinish = (Button) findViewById(R.id.button_finish);

    }

    private boolean checkProvider() {
        boolean b = false;
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        try {
            b = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception e) {
            e.printStackTrace();// TODO: 24.10.2016
        }
        return b;
    }

    private void alertProvider() {
        AlertDialog.Builder builder = new Builder(this)
                .setMessage("Please, enable GPS")
                .setCancelable(false)
                .setPositiveButton("Yes", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    }
                })
                .setNegativeButton("No", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length != 0 && grantResults[0] == PERMISSION_GRANTED) {

                if (checkProvider()) {
                    startServ();
                    bind();
                    watchMileage();
                } else
                    alertProvider();
            }
        }
    }

    private void startServ() {
        Intent intent = new Intent(this, OdometerService.class);
        startService(intent);
    }



    private void bind() {
        if (!mBound || mOdometerService == null) {
            Intent intent = new Intent(this, OdometerService.class);
            Boolean res = bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
            if (!res)
                throw new AssertionError("bindService failed");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    private void watchMileage() {
        final TextView metersView = (TextView) findViewById(R.id.distance_meters);
        final Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                double distance = 0.0;
                if (mOdometerService != null)
                    distance = mOdometerService.getMeters();
                String meters = String.format("%1$,.2f meters", distance);
                metersView.setText(meters);
                handler.postDelayed(this, 1000);
            }
        });
    }


    public void startOdometer(View view) {
        if (ActivityCompat.checkSelfPermission(this, permission.ACCESS_FINE_LOCATION) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);

            return;
        }
        if (checkProvider()) {
            startServ();
            bind();
            watchMileage();
        } else {
            alertProvider();
        }

        //disable button Start until button Finish been pressed
        buttonStart.setEnabled(false);
        buttonFinish.setEnabled(true);

        //to keep the screen turned on until button Finish been pressed
        getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);

    }

    public void finishOdometer(View view) {
        unbindService(mServiceConnection);
        Intent intent = new Intent(this, OdometerService.class);
        boolean res = stopService(intent);
        if (!res)
            throw new AssertionError("stopService() failed");
        mBound = false;
        mOdometerService = null;
        buttonStart.setEnabled(true);
        buttonFinish.setEnabled(false);

        getWindow().clearFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}
