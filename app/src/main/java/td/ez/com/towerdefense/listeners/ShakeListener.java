package td.ez.com.towerdefense.listeners;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Created by Charly on 15/02/2018.
 */

public class ShakeListener implements SensorEventListener
{
    private final int FORCE_THRESHOLD = 1000;
    private final int TIME_THRESHOLD = 100;
    private final int SHAKE_TIMEOUT = 500;
    private final int SHAKE_DURATION = 800;
    private final int SHAKE_COUNT = 3;

    private Context context;

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private OnShakeListener listener;

    private float lastX = -1.0f, lastY = -1.0f, lastZ = -1.0f;
    private long lastTime;
    private int shakeCount = 0;
    private long lastShake;
    private long lastForce;

    public interface OnShakeListener
    {
        void onShake();
    }

    public ShakeListener(Context ctx)
    {
        context = ctx;
        resume();
    }

    public void resume()
    {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if(accelerometer != null)
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
    }

    public void pause()
    {
        if(sensorManager != null)
        {
            sensorManager.unregisterListener(this, accelerometer);
            sensorManager = null;
        }
    }

    public void setOnShakeListener(OnShakeListener listener)
    {
        this.listener = listener;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent)
    {
        long now = System.currentTimeMillis();

        if ((now - lastForce) > SHAKE_TIMEOUT)
            shakeCount = 0;

        if ((now - lastTime) > TIME_THRESHOLD)
        {
            long diff = now - lastTime;

            float speed = Math.abs(sensorEvent.values[0] + sensorEvent.values[1] + sensorEvent.values[2] - lastX - lastY - lastZ) / diff * 10000;
            if (speed > FORCE_THRESHOLD)
            {
                if ((++shakeCount >= SHAKE_COUNT) && (now - lastShake > SHAKE_DURATION))
                {
                    lastShake = now;
                    shakeCount = 0;
                    if (listener != null)
                        listener.onShake();
                }
                lastForce = now;
            }
            lastTime = now;
            lastX = sensorEvent.values[0];
            lastY = sensorEvent.values[1];
            lastZ = sensorEvent.values[2];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) { }
}
