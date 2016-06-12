package com.xie.xuan.fastsensor;

import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.RandomAccessFile;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    final String TAG="GPS_SENSOR";
    SensorManager sm;
    Sensor accelerometer;
    Sensor Gyroscope;
    Sensor magnetic; // 地磁场传感器
    float[] mGravity= new float[3];
    float[] mGeomagnetic= new float[3];
    private float azimuth = 0f;

    Handler handler;
    boolean Recording = false;
    boolean Running = true;
    Button recordButton;// = (Button)findViewById(R.id.btnRecord);
    String StrAcceleration, StrGyroscope,StrGeomagnetic;
    long currentTimeMillis;
    RandomAccessFile raf;
    File file;
    String filePath = "/sdcard/GPS_Sensor/";
    String fileName = "test.txt";
    private void initRecordFile(){
        currentTimeMillis = System.currentTimeMillis();
        fileName = "IMU_Sensor_"+currentTimeMillis+".txt";
        openRecorderFile();
        //writeTxtToFile("AX, AY, AZ, GX, GY, GZ, Time", filePath, fileName);//, Azimuth, Pitch, Roll
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        //Log.d(TAG, "onKeyUp: %d " + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_HEADSETHOOK:
                //add your code here
                DoRecorder();
                Log.d(TAG, "onKeyUp: %d " + keyCode);
                //ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_SYSTEM, ToneGenerator.MAX_VOLUME);
                //toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP);
                ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 50);
                if(Recording)
                    toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD);//, 200);
                else
                    toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_INCALL_LITE);//, 200);
                return true;


        }
        return super.onKeyUp(keyCode, event);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //IntentFilter filter = new IntentFilter(Intent.ACTION_MEDIA_BUTTON);
        //filter.setPriority(999);
        //registerReceiver(receiver, filter);

        setupRecordButton();
        sm=(SensorManager)getSystemService(SENSOR_SERVICE);
        /*List<Sensor> sensorList = sm.getSensorList(Sensor.TYPE_ALL);
        for(Sensor sensor : sensorList){
            Log.i(TAG, sensor.getName()+" "+sensor.getType());
        }*/
        accelerometer=sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sm.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);

        Gyroscope=sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sm.registerListener(this, Gyroscope, SensorManager.SENSOR_DELAY_FASTEST);

        magnetic = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sm.registerListener(this, magnetic, SensorManager.SENSOR_DELAY_FASTEST);

        handler = new Handler();
        Runnable runnable = new Runnable(){
            @Override
            public void run(){
                while(Running){
                    try{
                        Thread.sleep(20);}
                    catch(InterruptedException e){
                        e.printStackTrace();
                    }
                    handler.post(new Runnable(){
                        @Override
                        public void run(){
                            writeARecorder();
                        }
                    });
                }
            }
        };
        new Thread(runnable).start();
    }

    protected void onPause() {
        super.onPause();
        //sm.unregisterListener(this);
    }

   protected void onResume() {
        super.onResume();

        sm.registerListener(
                this,
                sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST );
        sm.registerListener(
                this,
                sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_FASTEST );
        sm.registerListener(
                this,
                sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_FASTEST);


    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy){
        //
    }

    @Override
    public void onSensorChanged(SensorEvent event){
        if (event.sensor == null) {
            return;
        }
        //
        //final float alpha = 0.03f;

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            StrAcceleration=event.values[0] + "," + event.values[1] + "," + event.values[2] + ",";
            mGravity=event.values;
            /*mGravity[0] = alpha * mGravity[0] + (1 - alpha)
                    * event.values[0];
            mGravity[1] = alpha * mGravity[1] + (1 - alpha)
                    * event.values[1];
            mGravity[2] = alpha * mGravity[2] + (1 - alpha)
                    * event.values[2];*/

        }
        else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            StrGyroscope=event.values[0] + "," + event.values[1] + "," + event.values[2] + ",";
        }
        else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            /*mGeomagnetic[0] = alpha * mGeomagnetic[0] + (1 - alpha)
                    * event.values[0];
            mGeomagnetic[1] = alpha * mGeomagnetic[1] + (1 - alpha)
                    * event.values[1];
            mGeomagnetic[2] = alpha * mGeomagnetic[2] + (1 - alpha)
                    * event.values[2];*/
            mGeomagnetic=event.values;
            StrGeomagnetic=event.values[0] + "," + event.values[1] + "," + event.values[2] + ",";
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
    private void setupRecordButton(){

        //1. Get a reference to the button
        Log.i(TAG, "setupRecordButton before recordbutton ");
        recordButton = (Button)findViewById(R.id.btnRecord);
        Log.i(TAG, "setupRecordButton in setupRecordButton ");

        ChangeRecordButtonText();
        //2. Set the click listener to run my code
        recordButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                DoRecorder();
            }
        });
    }
    private void DoRecorder(){
        Recording = !Recording;
        ChangeRecordButtonText();
        if (Recording) {
            initRecordFile();
            writeTxtToRecorderFile("Time,Accelerometer_X,Accelerometer_Y,Accelerometer_Z," +
                    "Gyroscope_X,Gyroscope_Y,Gyroscope_Z," +
                    "Geomagnetic_X,Geomagnetic_Y,Geomagnetic_Z," +
                    "GPS_Time,Longitude,Latitude,Altitude,Accuarcy,Speed");

        }else{
            closeRecorderFile();
        }
    }
    private void ChangeRecordButtonText() {
        if (Recording) {
            recordButton.setText("Stop & Save");

        } else {
            recordButton.setText("Start recording");
        }
    }

    public void openRecorderFile(){
        makeRootDirectory(filePath);
        String strFilePath = filePath+fileName;
        try {
            file = new File(strFilePath);
            if (!file.exists()) {
                Log.i(TAG, "Create the file:" + strFilePath);
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
    public void closeRecorderFile(){
        try {
            raf.close();
            Log.e("RSSIRecorder", "close File" );
        } catch (Exception e) {
            Log.e("RSSIRecorder", "Error on close File:" + e);
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

    // Create folder
    public static void makeRootDirectory(String filePath) {
        File file = null;
        try {
            file = new File(filePath);
            if (!file.exists()) {
                file.mkdir();
            }
        } catch (Exception e) {
            Log.i("error:", e + "");
        }
    }

    public void writeARecorder(String str){
        if(Recording){
            String strRecord;
            currentTimeMillis = System.currentTimeMillis();
            strRecord = currentTimeMillis+","+
                    StrAcceleration+
                    StrGyroscope+
                    StrGeomagnetic+
                    str;
            writeTxtToRecorderFile(strRecord);
        }
    }

    public void writeARecorder(){
        writeARecorder("0,0,0,0,0,0");
    }

    public void writeTxtToRecorderFile(String strcontent){
        if(Recording){
            String strContent = strcontent + "\r\n";
            try{
                //raf.seek(file.length());
                raf.write(strContent.getBytes());
            }catch (Exception e) {
                Log.e("RSSIRecorder", "Error on write File:" + e);
            }
        }
    }
}
