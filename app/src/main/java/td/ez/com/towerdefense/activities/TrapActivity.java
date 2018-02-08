package td.ez.com.towerdefense.activities;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
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

public class TrapActivity extends AppCompatActivity
{
    private final int REFERENCE_WIDTH = 1920;
    private final int REFERENCE_HEIGHT = 1080;

    private final int TRAP_PRICE = 30;
    private final int TRAP_SIZE_DP = 30;

    private PhotoView mapView;

    private int screenWidth;
    private int screenHeight;

    private String playerPseudo;
    private int currentGoldAmount;

    private int colorCodePlayer;

    private Socket socket;

    private Point lastNewTrapPositionAbsolute;

    private List<ImageView> trapsView = new ArrayList<>();

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

        socket = SocketSingleton.getInstance().getSocket();
        setupSocketListeners();

        requestAllTraps();

        mapView = findViewById(R.id.mapView);
        mapView.setImageResource(R.drawable.map);

        mapView.getAttacher().setOnViewTapListener(new OnViewTapListener()
        {
            @Override
            public void onViewTap(View view, float x, float y)
            {
                lastNewTrapPositionAbsolute = new Point((int) x, (int) y);

                Matrix inverse = new Matrix();
                mapView.getImageMatrix().invert(inverse);

                float[] pointR = new float[2];
                pointR[0] = x;
                pointR[1] = y;
                inverse.mapPoints(pointR);

                // Adding an offset
                pointR[0] *= 1.29;
                pointR[1] *= 1.29;

                // Scaling to the reference scale
                pointR[0] /= (screenWidth/REFERENCE_WIDTH);
                pointR[1] /= (screenHeight/REFERENCE_HEIGHT);

                Point position = new Point((int) pointR[0], (int) pointR[1]);
                sendTrap(position);
            }
        });

        mapView.getAttacher().setOnScaleChangeListener(new OnScaleChangedListener()
        {
            @Override
            public void onScaleChange(float scaleFactor, float focusX, float focusY)
            {
                if(Math.abs(mapView.getScale() - 1.0f) < 0.02) // Scale = 1.0
                {
                    for(ImageView imageView : trapsView)
                    {
                        imageView.setVisibility(View.VISIBLE);
                    }
                }
                else
                {
                    for(ImageView imageView : trapsView)
                    {
                        imageView.setVisibility(View.INVISIBLE);
                    }
                }
            }
        });

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(metrics);

        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;

        /**** MOCK ****/
        /*Trap trap1 = new Trap();
        trap1.colorCode = colorCodePlayer;
        trap1.position = new Point(530, 630);

        Trap trap2 = new Trap();
        trap2.colorCode = colorCodePlayer;
        trap2.position = new Point(1020, 700);

        FrameLayout frameLayout = findViewById(R.id.layout_trap);
        printTrap(frameLayout, TRAP_SIZE_DP, trap1.position, trap1.colorCode);
        printTrap(frameLayout, TRAP_SIZE_DP, trap2.position, trap2.colorCode);*/
        /****/
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        socket.off();
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
                                FrameLayout frameLayout = findViewById(R.id.layout_trap);
                                float currentScale = mapView.getScale();

                                printTrap(frameLayout, (int) (TRAP_SIZE_DP * currentScale), lastNewTrapPositionAbsolute, colorCodePlayer);

                                new Handler().postDelayed(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        finish();
                                    }
                                }, 2000);
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
                                    trap.position = new Point(positionJson.getInt("x"), positionJson.getInt("y"));

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

            //mockSuccess();
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

        trapImg.setX(position.x - nbPixels / 2);
        trapImg.setY(position.y - nbPixels / 2);

        layout.addView(trapImg);

        trapsView.add(trapImg);
    }

    private void mockSuccess()
    {
        FrameLayout frameLayout = findViewById(R.id.layout_trap);
        float currentScale = mapView.getScale();

        printTrap(frameLayout, (int) (TRAP_SIZE_DP * currentScale), lastNewTrapPositionAbsolute, colorCodePlayer);

        new Handler().postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                finish();
            }
        }, 2000);
    }

    private int convertDpToPixel(int dp)
    {
        Resources resources = getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        int px = dp * (metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return px;
    }
}
