package com.xie.xuan.fastsensor;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.RandomAccessFile;

public class SensorService extends Service implements SensorEventListener {


    private SensorManager sm;
    private AudioManager am;

    private ComponentName componentName;


    private boolean Recording = false;

    private Sensor accelerometer;
    private Sensor Gyroscope;
    private Sensor magnetic; // 地磁场传感器


    private RandomAccessFile raf;
    private File file;
    private static String filePath = "/sdcard/GPS_Sensor/";
    private static String fileName = "test.txt";

    private float[] mGravity = new float[3];
    private float[] mGeomagnetic = new float[3];
    private float azimuth = 0f;

    private String StrAcceleration, StrGyroscope, StrGeomagnetic;

    public SensorService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        componentName = new ComponentName(getPackageName(), MediaButtonReceiver.class.getName());
        initRecordFile();
        writeTxtToRecorderFile("Time,Accelerometer_X,Accelerometer_Y,Accelerometer_Z," +
                "Gyroscope_X,Gyroscope_Y,Gyroscope_Z," +
                "Geomagnetic_X,Geomagnetic_Y,Geomagnetic_Z," +
                "GPS_Time,Longitude,Latitude,Altitude,Accuarcy,Speed");
    }

    @Override
    public IBinder onBind(Intent intent) {

        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        am = (AudioManager) getSystemService(AUDIO_SERVICE);
        am.registerMediaButtonEventReceiver(componentName);

        accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sm.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);

        Gyroscope = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sm.registerListener(this, Gyroscope, SensorManager.SENSOR_DELAY_FASTEST);

        magnetic = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sm.registerListener(this, magnetic, SensorManager.SENSOR_DELAY_FASTEST);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        closeRecorderFile();
        am.unregisterMediaButtonEventReceiver(componentName);
        super.onDestroy();

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == null) {
            return;
        }
        //
        //final float alpha = 0.03f;

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            StrAcceleration = event.values[0] + "," + event.values[1] + "," + event.values[2] + ",";
            mGravity = event.values;
            /*mGravity[0] = alpha * mGravity[0] + (1 - alpha)
                    * event.values[0];
            mGravity[1] = alpha * mGravity[1] + (1 - alpha)
                    * event.values[1];
            mGravity[2] = alpha * mGravity[2] + (1 - alpha)
                    * event.values[2];*/

        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            StrGyroscope = event.values[0] + "," + event.values[1] + "," + event.values[2] + ",";
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            /*mGeomagnetic[0] = alpha * mGeomagnetic[0] + (1 - alpha)
                    * event.values[0];
            mGeomagnetic[1] = alpha * mGeomagnetic[1] + (1 - alpha)
                    * event.values[1];
            mGeomagnetic[2] = alpha * mGeomagnetic[2] + (1 - alpha)
                    * event.values[2];*/
            mGeomagnetic = event.values;
            StrGeomagnetic = event.values[0] + "," + event.values[1] + "," + event.values[2] + ",";
        }


        float R[] = new float[9];
        float I[] = new float[9];
        boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
        if (success) {
            float orientation[] = new float[3];
            SensorManager.getOrientation(R, orientation);
            azimuth = (float) Math.toDegrees(orientation[0]); // orientation
            azimuth = (azimuth + 360) % 360;
            // Log.d(TAG, "azimuth (deg): " + azimuth);
            //Log.i(TAG, "azimut=" + azimuth +
            //            " pitch=" + (float) Math.toDegrees(orientation[1]) +
            //            " roll=" + (float) Math.toDegrees(orientation[2]));// orientation contains: azimut, pitch and roll
            /*I[0] = (float)Math.atan2(R[1], R[4]);
            I[1] = (float)Math.asin(-R[7]);
            I[2] = (float)Math.atan2(-R[6], R[8]);
            Log.i(TAG, "azimut=" + orientation[0] + " " +I[0]+
                                " pitch=" + orientation[1] + " " +I[1]+
                                " roll=" + orientation[2]+ " " +I[2]);// orientation contains: azimut, pitch and roll*/
        }
        //writeARecorder();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    private void initRecordFile() {
        fileName = "IMU_Sensor_" + System.currentTimeMillis() + ".txt";
        openRecorderFile();
        //writeTxtToFile("AX, AY, AZ, GX, GY, GZ, Time", filePath, fileName);//, Azimuth, Pitch, Roll
    }


    public void writeARecorder(String str) {
        if (Recording) {
            String strRecord;
            strRecord = System.currentTimeMillis() + "," +
                    StrAcceleration +
                    StrGyroscope +
                    StrGeomagnetic +
                    str;
            writeTxtToRecorderFile(strRecord);
        }
    }


    // Create folder
    public static void makeRootDirectory(String filePath) {
        File file;
        try {
            file = new File(filePath);
            if (!file.exists()) {
                file.mkdir();
            }
        } catch (Exception e) {
            Log.i("error:", e + "");
        }
    }


    public void writeARecorder() {
        writeARecorder("0,0,0,0,0,0");
    }

    public void writeTxtToRecorderFile(String strcontent) {

        String strContent = strcontent + "\r\n";
        try {
            //raf.seek(file.length());
            raf.write(strContent.getBytes());
        } catch (Exception e) {
            Log.e("RSSIRecorder", "Error on write File:" + e);
        }
    }


    // Create file
    public File makeFilePath(String filePath, String fileName) {
        File file = null;
        makeRootDirectory(filePath);
        try {
            file = new File(filePath + fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }


    public void openRecorderFile() {
        makeRootDirectory(filePath);
        String strFilePath = filePath + fileName;
        try {
            file = new File(strFilePath);
            if (!file.exists()) {
                Log.i(MainActivity.TAG, "Create the file:" + strFilePath);
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            raf = new RandomAccessFile(file, "rwd");
            raf.seek(file.length());
            //raf.close();
        } catch (Exception e) {
            Log.e("RSSIRecorder", "Error on open File:" + e);
        }
    }

    public void closeRecorderFile() {
        try {
            raf.close();
            Log.e("RSSIRecorder", "close File");
        } catch (Exception e) {
            Log.e("RSSIRecorder", "Error on close File:" + e);
        }
    }
}
