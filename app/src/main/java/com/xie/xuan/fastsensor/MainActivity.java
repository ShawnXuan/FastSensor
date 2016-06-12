package com.xie.xuan.fastsensor;

import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    final static String TAG = "GPS_SENSOR";


    private boolean recording = false;
    private Button recordButton;

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        //Log.d(TAG, "onKeyUp: %d " + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_HEADSETHOOK:
                //add your code here
                doRecorder();
                Log.d(TAG, "onKeyUp: %d " + keyCode);
                //ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_SYSTEM, ToneGenerator.MAX_VOLUME);
                //toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP);
                ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 50);
                if (recording)
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

        recordButton = (Button) findViewById(R.id.btnRecord);
        changeRecordButtonText();

        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doRecorder();
            }
        });

    }

    private void doRecorder() {
        recording = !recording;
        changeRecordButtonText();
        Intent service = new Intent(this, SensorService.class);

        if (recording) {
            startService(service);
        } else {
            stopService(service);
        }
    }

    private void changeRecordButtonText() {
        if (recording) {
            recordButton.setText("Stop & Save");
        } else {
            recordButton.setText("Start recording");
        }
    }


}
