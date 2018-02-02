package td.ez.com.towerdefense.activities;

import android.content.Intent;
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
        setupSocketListeners(socket);

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
                sendTrap(position);
            }
        });

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(metrics);

        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
    }

    private void setupSocketListeners(Socket socket)
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

                finish();
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

        // Mocking the response from the server
        mockResponse();
    }

    private void mockResponse()
    {
        /*ImageView trapImg = new ImageView(this);
        trapImg.setLayoutParams(new FrameLayout.LayoutParams());

        FrameLayout frameLayout = findViewById(R.id.layout_trap);
        FrameLayout.LayoutParams params = frameLayout.getLayoutParams();*/
    }
}
