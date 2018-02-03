package td.ez.com.towerdefense.activities;

import android.content.Intent;
import android.content.res.Resources;
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

import com.github.chrisbanes.photoview.OnViewTapListener;
import com.github.chrisbanes.photoview.PhotoView;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import td.ez.com.towerdefense.R;
import td.ez.com.towerdefense.network.SocketSingleton;

public class TrapActivity extends AppCompatActivity
{
    private final int REFERENCE_WIDTH = 1920;
    private final int REFERENCE_HEIGHT = 1080;

    private final int TRAP_PRICE = 30;

    private int screenWidth;
    private int screenHeight;

    private String playerPseudo;
    private int currentGoldAmount;

    private Socket socket;

    private Point lastNewTrapPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trap);

        enableImmersiveMode();

        Intent intent = getIntent();
        playerPseudo = intent.getStringExtra(GameActivity.EXTRA_PLAYERPSEUDO);
        currentGoldAmount = intent.getIntExtra(GameActivity.EXTRA_GOLD, 0);

        socket = SocketSingleton.getInstance().getSocket();
        setupSocketListeners();

        final PhotoView mapView = findViewById(R.id.mapView);
        mapView.setImageResource(R.drawable.map);

        mapView.getAttacher().setOnViewTapListener(new OnViewTapListener()
        {
            @Override
            public void onViewTap(View view, float x, float y)
            {
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
                lastNewTrapPosition = position;
                sendTrap(position);
            }
        });

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(metrics);

        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
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
                            if(json.getString("status").equals("success"))
                            {
                                ImageView trapImg = new ImageView(TrapActivity.this);
                                int nbPixels = convertDpToPixel(30);
                                trapImg.setLayoutParams(new FrameLayout.LayoutParams(nbPixels, nbPixels));

                                trapImg.setImageResource(R.drawable.trap_spiderweb);
                                trapImg.setX(lastNewTrapPosition.x - 15);
                                trapImg.setY(lastNewTrapPosition.y - 15);

                                FrameLayout frameLayout = findViewById(R.id.layout_trap);
                                frameLayout.addView(trapImg);

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
                    finish();
                }
            }, 2000);
        }
    }

    private int convertDpToPixel(int dp)
    {
        Resources resources = getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        int px = dp * (metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return px;
    }
}
