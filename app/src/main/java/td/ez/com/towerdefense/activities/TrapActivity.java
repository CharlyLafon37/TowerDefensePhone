package td.ez.com.towerdefense.activities;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.github.chrisbanes.photoview.OnScaleChangedListener;
import com.github.chrisbanes.photoview.OnViewTapListener;
import com.github.chrisbanes.photoview.PhotoView;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import td.ez.com.towerdefense.R;
import td.ez.com.towerdefense.network.SocketSingleton;

public class TrapActivity extends AppCompatActivity implements SensorEventListener
{
    private final int REFERENCE_WIDTH = 1920;
    private final int REFERENCE_HEIGHT = 1080;

    private final int TRAP_PRICE = 30;
    private final int TRAP_SIZE_DP = 30;

    private ImageView mapView;
    private ImageView circularTargetView;

    private int screenWidth;
    private int screenHeight;

    private String playerPseudo;
    private int currentGoldAmount;

    private int colorCodePlayer;

    private Socket socket;

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trap);

        enableImmersiveMode();

        Intent intent = getIntent();
        playerPseudo = intent.getStringExtra(GameActivity.EXTRA_PLAYERPSEUDO);
        currentGoldAmount = intent.getIntExtra(GameActivity.EXTRA_GOLD, 0);
        colorCodePlayer = intent.getIntExtra(GameActivity.EXTRA_COLOR, 0);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        if(accelerometer != null)
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);

        socket = SocketSingleton.getInstance().getSocket();
        setupSocketListeners();

        requestAllTraps();

        mapView = findViewById(R.id.mapView);

        circularTargetView = findViewById(R.id.circular_target);
        circularTargetView.setColorFilter(Color.WHITE);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(metrics);

        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;

        /**** MOCK ****/
        Trap trap1 = new Trap();
        trap1.colorCode = colorCodePlayer;
        trap1.position = new Point(530, 630);

        Trap trap2 = new Trap();
        trap2.colorCode = colorCodePlayer;
        trap2.position = new Point(1020, 700);

        FrameLayout frameLayout = findViewById(R.id.layout_trap);
        printTrap(frameLayout, TRAP_SIZE_DP, trap1.position, trap1.colorCode);
        printTrap(frameLayout, TRAP_SIZE_DP, trap2.position, trap2.colorCode);
        /****/
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        if(sensorManager != null)
        {
            sensorManager.unregisterListener(this, accelerometer);
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if(accelerometer != null)
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
    }

    private void setupSocketListeners()
    {
        socket.on("trap", new Emitter.Listener()
        {
            @Override
            public void call(final Object... args)
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        JSONObject json = (JSONObject) args[0];
                        try
                        {
                            // Print the trap just put by the player
                            if(json.has("status") && json.getString("status").equals("success"))
                            {
                                circularTargetView.setColorFilter(Color.GREEN);

                                new Handler().postDelayed(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        socket.off("trap");
                                        finish();
                                    }
                                }, 1000);
                            }

                            else if(json.has("status") && json.getString("status").equals("failure"))
                            {
                                vibrator.vibrate(300);

                                circularTargetView.setColorFilter(Color.RED);
                                Handler handler = new Handler();
                                handler.postDelayed(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        circularTargetView.setColorFilter(Color.WHITE);
                                    }
                                }, 1000);

                                if(accelerometer != null)
                                    sensorManager.registerListener(TrapActivity.this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
                            }

                            // Print all the traps sent by the server
                            else if(json.has("traps"))
                            {
                                /**** Unmarshalling ****/
                                JSONArray trapsJson = json.getJSONArray("traps");
                                Trap[] allTraps = new Trap[trapsJson.length()];

                                for(int i = 0; i < trapsJson.length(); i++)
                                {
                                    JSONObject trapJson = trapsJson.getJSONObject(i);

                                    Trap trap = new Trap();

                                    trap.colorCode = Color.parseColor(trapJson.getString("color"));
                                    trap.trapType = trapJson.getString("trap");

                                    JSONObject positionJson = trapJson.getJSONObject("position");

                                    int trapX = (int) (positionJson.getInt("x") * ((float) screenWidth / (float) REFERENCE_WIDTH));
                                    int trapY = (int) (positionJson.getInt("y") * ((float) screenHeight / (float) REFERENCE_HEIGHT));
                                    trap.position = new Point(trapX, trapY);

                                    allTraps[i] = trap;
                                }
                                /*****/

                                /***** Printing the traps *****/
                                FrameLayout frameLayout = findViewById(R.id.layout_trap);
                                for(Trap trap : allTraps)
                                {
                                    printTrap(frameLayout, TRAP_SIZE_DP, trap.position, trap.colorCode);
                                }
                            }
                        }
                        catch(JSONException e)
                        {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    private void enableImmersiveMode()
    {
        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private void sendTrap(Point position)
    {
        if(currentGoldAmount > TRAP_PRICE)
        {
            try
            {
                JSONObject json = new JSONObject();

                json.put("player", playerPseudo);
                json.put("trap", "default");

                JSONObject positionJson = new JSONObject();
                positionJson.put("x", position.x);
                positionJson.put("y", position.y);
                json.put("position", positionJson);

                socket.emit("trap", json);
            }
            catch(JSONException e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            Snackbar.make(
                    findViewById(R.id.layout_trap),
                    "Pas assez d'or.",
                    Snackbar.LENGTH_SHORT).show();

            new Handler().postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    socket.off("trap");
                    finish();
                }
            }, 2000);
        }
    }

    private void requestAllTraps()
    {
        try
        {
            JSONObject json = new JSONObject();
            json.put("action", "getTraps");

            socket.emit("trap", json);
        }
        catch(JSONException e)
        {
            e.printStackTrace();
        }
    }

    private void printTrap(FrameLayout layout, int sizeInDP, Point position, int colorCode)
    {
        ImageView trapImg = new ImageView(this);
        int nbPixels = convertDpToPixel(sizeInDP);
        trapImg.setLayoutParams(new FrameLayout.LayoutParams(nbPixels, nbPixels));

        trapImg.setImageResource(R.drawable.trap_spiderweb);

        trapImg.setColorFilter(colorCode);

        trapImg.setX(position.x);
        trapImg.setY(position.y);

        layout.addView(trapImg);
    }

    private int convertDpToPixel(int dp)
    {
        Resources resources = getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        int px = dp * (metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return px;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent)
    {
        if(sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
        {
            int currentX = (int) circularTargetView.getX();
            int currentY = (int) circularTargetView.getY();

            // Handling when trying to move out of the screen
            if(currentX >= (screenWidth - circularTargetView.getWidth())
                    || currentY >= (screenHeight - circularTargetView.getHeight()))
                return;

            circularTargetView.setX(currentX + sensorEvent.values[1] * 2);
            circularTargetView.setY(currentY + sensorEvent.values[0] * 2);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) { }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        Point positionTrap = new Point();
        positionTrap.x = (int) (circularTargetView.getX() + circularTargetView.getWidth() / 2);
        positionTrap.y = (int) (circularTargetView.getY() + circularTargetView.getHeight() / 2);

        sendTrap(positionTrap);

        if(sensorManager != null)
        {
            sensorManager.unregisterListener(this, accelerometer);
        }

        return super.onTouchEvent(event);
    }
}
