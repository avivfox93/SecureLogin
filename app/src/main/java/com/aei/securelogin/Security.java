package com.aei.securelogin;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.os.BatteryManager;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

import static android.hardware.Sensor.TYPE_ACCELEROMETER;
import static android.hardware.Sensor.TYPE_AMBIENT_TEMPERATURE;
import static android.hardware.Sensor.TYPE_LIGHT;
import static android.hardware.Sensor.TYPE_MAGNETIC_FIELD;
import static android.hardware.Sensor.TYPE_RELATIVE_HUMIDITY;
import static android.hardware.SensorManager.SENSOR_DELAY_NORMAL;

public final class Security {
    public static int random(int from, int to){
        return (int)((Math.random()*(to + 1)) + from);
    }

    public static boolean isViewOverlapping(View firstView, View[] others) {
        if(others == null)
            return false;
        for(View view : others) {
            if (isViewOverlapping(firstView, view))
                return true;
        }
        return false;
    }

    public static boolean isViewOverlapping(View firstView, View secondView) {
        int[] firstPosition = new int[2];
        int[] secondPosition = new int[2];

        firstView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        firstView.getLocationOnScreen(firstPosition);
        secondView.getLocationOnScreen(secondPosition);

        int r = firstView.getMeasuredWidth() + firstPosition[0];
        int l = secondPosition[0];
        return r >= l && (r != 0 && l != 0);
    }

    public static int getBatteryPercentage(Context context) {

        IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, iFilter);

        int level = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
        int scale = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : -1;

        float batteryPct = level / (float) scale;

        return (int) (batteryPct * 100);
    }

    public static class Shuffle{
        private View uName,password;
        public Shuffle(View uName, View password){
            this.uName = uName; this.password = password;
        }
        public void shuffle(View root, View...others){
            int width = root.getWidth();
            int height = root.getHeight();
            do {
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) uName.getLayoutParams();
                params.topMargin = random(0, (int) (height / 1.5f));
                params.leftMargin = random(0, width / 2);
                uName.setLayoutParams(params);
            }while (isViewOverlapping(uName,others));
            do{
                RelativeLayout.LayoutParams pParams = (RelativeLayout.LayoutParams) password.getLayoutParams();
                pParams.topMargin = random(0,(int)(height/1.5f));
                pParams.leftMargin = random(0,width/2);
                password.setLayoutParams(pParams);
            }while(isViewOverlapping(uName,password) || isViewOverlapping(password,others));
        }
        public void shuffel(View root){
            shuffle(root,null);
        }
    }

    public static class Timer{
        private long start = 0, limit;
        public Timer(long limit){
            this.limit = limit;
        }
        public void start(){
            start = System.currentTimeMillis();
        }
        public boolean end(){
            return (System.currentTimeMillis() - start) >= limit;
        }
    }

    public static class Sensors implements SensorEventListener {
        public static final int MAX_BATTERY = 99, MIN_BATTERY = 5;
        public static final int MAX_HUMIDITY = 99, MIN_HUMIDITY = 15;
        public static final int MAX_TEMP = 99, MIN_TEMP = 15;
        public static final int MAX_AZIMUTH = 185, MIN_AZIMUTH = 175;

        private float[] mLastAccelerometer = new float[3];
        private float[] mLastMagnetometer = new float[3];
        private boolean mLastAccelerometerSet = false;
        private boolean mLastMagnetometerSet = false;
        private float[] mR = new float[9];
        private float[] mOrientation = new float[3];
        int azimuth = 0;
        private CompassCallback azimuthCallback;

        private Activity activity;
        private SensorManager sensorManager;
        private Sensor sigEventSensor,proximitySensor, temperatureSensor, humiditySensor, lightSensor, gSensor, mSensor;
//        private BatteryManager batteryManager;
        private boolean significantSensorTriggered = true;
        private float humidity = MIN_HUMIDITY, temperature = MIN_TEMP, light = 0;

        public Sensors(Activity activity){
            this.activity = activity;
            sensorManager = (SensorManager)activity.getSystemService(Context.SENSOR_SERVICE);
//            batteryManager = (BatteryManager)activity.getSystemService(Context.BATTERY_SERVICE);
            sigEventSensor = sensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION);
            proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            temperatureSensor = sensorManager.getDefaultSensor(TYPE_AMBIENT_TEMPERATURE);
            lightSensor = sensorManager.getDefaultSensor(TYPE_LIGHT);
            humiditySensor = sensorManager.getDefaultSensor(TYPE_RELATIVE_HUMIDITY);
            gSensor = sensorManager.getDefaultSensor(TYPE_ACCELEROMETER);
            mSensor = sensorManager.getDefaultSensor(TYPE_MAGNETIC_FIELD);
        }

        public void onResume(){
            sensorManager.registerListener(this,proximitySensor,SENSOR_DELAY_NORMAL);
            sensorManager.registerListener(this,temperatureSensor,SENSOR_DELAY_NORMAL);
            sensorManager.registerListener(this,humiditySensor,SENSOR_DELAY_NORMAL);
            sensorManager.registerListener(this,lightSensor,SENSOR_DELAY_NORMAL);
            sensorManager.registerListener(this, gSensor, SENSOR_DELAY_NORMAL);
            sensorManager.registerListener(this, mSensor, SENSOR_DELAY_NORMAL);
        }

        public void onPause(){
            sensorManager.unregisterListener(this,proximitySensor);
            sensorManager.unregisterListener(this,temperatureSensor);
            sensorManager.unregisterListener(this,humiditySensor);
            sensorManager.unregisterListener(this,lightSensor);
            sensorManager.unregisterListener(this, gSensor);
            sensorManager.unregisterListener(this, mSensor);
        }

        public void setAzimuthCallback(CompassCallback azimuthCallback) {
            this.azimuthCallback = azimuthCallback;
        }

        public void resetSignificantTrigger(){
            significantSensorTriggered = false;
            sensorManager.requestTriggerSensor(new TriggerEventListener() {
                @Override
                public void onTrigger(TriggerEvent event) {
                    significantSensorTriggered = true;
                }
            }, sigEventSensor);
        }

        public boolean isSignificantSensorTriggered(){
            return significantSensorTriggered;
        }

        public boolean checkBattery(){
            int bat = getBatteryPercentage(activity);
            return bat <= MAX_BATTERY && bat >= MIN_BATTERY;
        }

        public boolean checkTemperature(){
            return temperature <= MAX_TEMP && temperature >= MIN_TEMP;
        }

        public boolean checkHumidity(){
            return humidity <= MAX_HUMIDITY && humidity >= MIN_HUMIDITY;
        }

        public boolean checkAzimuth(){
            return azimuth <= MAX_AZIMUTH && azimuth >= MIN_AZIMUTH;
        }

        public float getHumidity() {
            return humidity;
        }

        public float getTemperature() {
            return temperature;
        }

        public float getLight() {
            return light;
        }

        public int getAzimuth(){
            return azimuth;
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            synchronized (this) {
                switch (event.sensor.getType()) {
                    case TYPE_RELATIVE_HUMIDITY:
                        humidity = event.values[0];
                        break;
                    case TYPE_AMBIENT_TEMPERATURE:
                        temperature = event.values[0];
                        break;
                    case TYPE_LIGHT:
                        light = event.values[0];
                        break;
                    case TYPE_ACCELEROMETER:
//                    Log.d("ACCEL","GOT VALUE " + event.values[0] + " " + event.values[1] + " " + event.values[2]);
                        System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.length);
                        mLastAccelerometerSet = true;
                        break;
                    case TYPE_MAGNETIC_FIELD:
                        System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.length);
                        mLastMagnetometerSet = true;
                        break;
                    default:
                        break;
                }
                boolean success = SensorManager.getRotationMatrix(mR, null, mLastAccelerometer, mLastMagnetometer);
                if (success) {
                    SensorManager.getOrientation(mR, mOrientation);
                    float azimuthInRadians = mOrientation[0];
                    int temp = (int)(Math.toDegrees(azimuthInRadians)+360)%360;
                    if(azimuthCallback != null && temp != azimuth)
                        azimuthCallback.onAzimuth(temp);
                    azimuth = temp;
                    Log.d("AZIMUTH", "azimuth: " + azimuth);
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }

        interface CompassCallback{
            void onAzimuth(int azimuth);
        }
    }
}
