package com.aei.securelogin;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

/*

TODO:
Add security options like proximity sensor, compass, ambient, gps, move text and button, touch length

 */

public class MainActivity extends AppCompatActivity{

    private final long MAX_TIME = 1000, PUNISH_TIME = 30000;
    private static final int MAX_TRIES = 3;
    private Button loginBtn;
    private EditText idInput;
    private long timeTextChanged = 0;
    private long punishStartTime = 0;
    private boolean punished = false;
    private int numOfTries = 0;
    private Security.Sensors sensorsSecurity;
    private Security.Shuffle shuffelSecurity;
    private TextView azimuthText;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        loginBtn = findViewById(R.id.login_button);
        idInput = findViewById(R.id.id_input);
        azimuthText = findViewById(R.id.azimuth_txt);
        idInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                timeTextChanged = System.currentTimeMillis();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        loginBtn.setOnTouchListener(new View.OnTouchListener() {
            private Security.Timer timerSecurity = new Security.Timer(MAX_TIME);
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){
                    case MotionEvent.ACTION_UP:
                        if(timerSecurity.end())
                            login();
                        else
                            Toast.makeText(getApplicationContext(),
                                    String.format(Locale.ENGLISH,"Need to press for atleast %ss",
                                            (float) MAX_TIME / 1000),Toast.LENGTH_SHORT).show();
                        break;
                    case MotionEvent.ACTION_DOWN:
                        timerSecurity.start();
                        break;
                }
                return false;
            }
        });
        sensorsSecurity = new Security.Sensors(this);
        sensorsSecurity.resetSignificantTrigger();
        shuffelSecurity = new Security.Shuffle(idInput,loginBtn);
        sensorsSecurity.setAzimuthCallback(azimuth -> azimuthText.setText(Integer.toString(azimuth)));
    }

    @Override
    protected void onResume(){
        super.onResume();
        sensorsSecurity.onResume();
    }

    @Override
    protected void onPause(){
        super.onPause();
        sensorsSecurity.onPause();
    }

    private void login(){
        if(punished){
            long timePassed = System.currentTimeMillis() - punishStartTime;
            if(timePassed <= PUNISH_TIME){
                Toast.makeText(this,
                        "You'r still blocked... " + ((PUNISH_TIME - timePassed)/1000) + "s Left",Toast.LENGTH_SHORT).show();
                return;
            }else
                punished = false;
        }

        if(System.currentTimeMillis() - timeTextChanged <= 100){
            punish();
            return;
        }
//        if(!sensorsSecurity.isSignificantSensorTriggered()){
//            Toast.makeText(this,"Please move to another Location",Toast.LENGTH_SHORT).show();
//            return;
//        }
        if(!sensorsSecurity.checkBattery()){
            Toast.makeText(this,String.format(Locale.ENGLISH,
                    "Make sure battery is between %d%% and %d%%",Security.Sensors.MIN_BATTERY,Security.Sensors.MAX_BATTERY),Toast.LENGTH_SHORT).show();
            return;
        }
        if(!sensorsSecurity.checkHumidity()){
            Toast.makeText(this,String.format(Locale.ENGLISH,
                    "Make sure humidity is between %d%% and %d%%",Security.Sensors.MIN_HUMIDITY,Security.Sensors.MAX_HUMIDITY),Toast.LENGTH_SHORT).show();
            return;
        }
        if(!sensorsSecurity.checkTemperature()){
            Toast.makeText(this,String.format(Locale.ENGLISH,
                    "Make sure temperature is between %dC and %dC",Security.Sensors.MIN_TEMP,Security.Sensors.MAX_TEMP),Toast.LENGTH_SHORT).show();
            return;
        }
        if(!sensorsSecurity.checkAzimuth()){
            Toast.makeText(this,String.format(Locale.ENGLISH,
                    "Make sure azimuth is between %ddeg and %ddeg",Security.Sensors.MIN_AZIMUTH,Security.Sensors.MAX_AZIMUTH),Toast.LENGTH_SHORT).show();
            return;
        }
        if(idInput.getText().toString().equals("Aviv")){
            Toast.makeText(this,"Logged in!",Toast.LENGTH_SHORT).show();
        }else{
            sensorsSecurity.resetSignificantTrigger();
            shuffelSecurity.shuffel(findViewById(R.id.main_view));
            if(++numOfTries >= MAX_TRIES){
                numOfTries = 0;
                punish();
            }else{
                Toast.makeText(this,String.format(Locale.ENGLISH,
                        "Wrong Password! %d tries left",(MAX_TRIES - numOfTries)),Toast.LENGTH_SHORT).show();
            }
        }
    }

    void punish(){
        punished = true;
        punishStartTime = System.currentTimeMillis();
        Toast.makeText(this,String.format(Locale.ENGLISH,"You'r blocked for %ds!",PUNISH_TIME/1000),Toast.LENGTH_SHORT).show();
    }

}
