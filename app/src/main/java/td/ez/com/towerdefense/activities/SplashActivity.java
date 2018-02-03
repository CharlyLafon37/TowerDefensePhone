package td.ez.com.towerdefense.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.transition.Slide;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.eyalbira.loadingdots.LoadingDots;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import td.ez.com.towerdefense.R;
import td.ez.com.towerdefense.enums.Power;
import td.ez.com.towerdefense.network.SocketSingleton;

public class SplashActivity extends AppCompatActivity
{
    public static final String EXTRA_PSEUDO_PLAYER = "td.ez.com.towerdefense.extrapseudoplayer";
    public static final String EXTRA_PSEUDO_OTHERS = "td.ez.com.towerdefense.extrapseudoothers";
    public static final String EXTRA_POWER = "td.ez.com.towerdefense.power";
    public static final String EXTRA_GOLD = "td.ez.com.towerdefense.gold";

    private Socket socket;

    private TextView stateView;
    private EditText nameView;
    private Button validateButton;
    private LoadingDots loadingDots;

    private String pseudoPlayer;
    private List<String> pseudoOthers = new ArrayList<>();

    private String colorPlayer;

    private Power power;

    private int currentGold;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        getWindow().setExitTransition(new Slide(Gravity.LEFT));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        stateView = findViewById(R.id.state);
        nameView = findViewById(R.id.name);
        validateButton = findViewById(R.id.sendName);
        loadingDots = findViewById(R.id.loadingDots);

        enableImmersiveMode();

        socket = SocketSingleton.getInstance().getSocket();
        setupSocketListeners();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        socket.off();
    }

    private void enableImmersiveMode()
    {
        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private void setupSocketListeners()
    {
        socket.on("setup", new Emitter.Listener()
        {
            @Override
            public void call(final Object... args)
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            JSONObject json = (JSONObject) args[0];
                            if(json.getString("action").equals("name"))
                            {
                                stateView.setText("Collecte des pseudos en cours");
                                nameView.setVisibility(View.VISIBLE);
                                validateButton.setVisibility(View.VISIBLE);
                            }

                            else if(json.getString("action").equals("ready"))
                            {
                                JSONArray namesJson = json.getJSONArray("names");

                                for(int i = 0; i < namesJson.length(); i++)
                                {
                                    if(namesJson.getString(i).equals(pseudoPlayer)) // Not accounting the player's pseudo
                                        continue;
                                    pseudoOthers.add(namesJson.getString(i));
                                }

                                stateView.setText("Placez votre tag couleur " + colorPlayer + " sur la table.");
                                loadingDots.setVisibility(View.GONE);
                            }

                            else if(json.getString("action").equals("start"))
                            {
                                stateView.setText("La partie peut commencer.");
                                stateView.setTextColor(ContextCompat.getColor(SplashActivity.this, R.color.colorAccent));

                                /***** Launching the game : starting a new activity *****/
                                Handler handlerLaunch = new Handler();
                                handlerLaunch.postDelayed(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        launchGameActivity();
                                    }
                                }, 2000);
                            }
                        }
                        catch (JSONException e)
                        {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

        socket.on("power", new Emitter.Listener()
        {
            @Override
            public void call(final Object... args)
            {
                JSONObject json = (JSONObject) args[0];

                try
                {
                    switch(json.getString("power"))
                    {
                        case "fire" :
                        {
                            power = Power.FIRE;
                            break;
                        }
                        case "ice" :
                        {
                            power = Power.ICE;
                            break;
                        }
                        case "thunder" :
                        {
                            power = Power.THUNDER;
                            break;
                        }
                    }
                }
                catch (JSONException e)
                {
                    e.printStackTrace();
                }
            }
        });

        socket.on("gold", new Emitter.Listener()
        {
            @Override
            public void call(final Object... args)
            {
                JSONObject json = (JSONObject) args[0];

                try
                {
                    currentGold = json.getInt("amount");
                }
                catch(JSONException e)
                {
                    e.printStackTrace();
                }
            }
        });

        socket.on("tag", new Emitter.Listener()
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
                            String color = json.getString("color");
                            colorPlayer = color;
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

    public void sendName(View v)
    {
        pseudoPlayer = nameView.getText().toString().trim();
        if(pseudoPlayer.isEmpty())
        {
            /*validateButton.setBackgroundColor(ContextCompat.getColor(this, R.color.error));
            Handler handlerButtonGreen = new Handler(Looper.getMainLooper());
            handlerButtonGreen.postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    validateButton.setBackgroundColor(ContextCompat.getColor(SplashActivity.this, R.color.colorAccent));
                }
            }, 2000);*/
            return;
        }

        nameView.setVisibility(View.GONE);
        validateButton.setVisibility(View.GONE);

        try
        {
            JSONObject json = new JSONObject();
            json.put("action", "ready");

            JSONObject body = new JSONObject();
            body.put("name", pseudoPlayer);
            json.put("body", body);

            socket.emit("setup", json);
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        enableImmersiveMode();
    }

    private void launchGameActivity()
    {
        Intent gameActivity = new Intent(SplashActivity.this, GameActivity.class);
        gameActivity.putExtra(EXTRA_PSEUDO_PLAYER, pseudoPlayer);
        gameActivity.putStringArrayListExtra(EXTRA_PSEUDO_OTHERS, (ArrayList<String>) pseudoOthers);
        gameActivity.putExtra(EXTRA_POWER, power);
        gameActivity.putExtra(EXTRA_GOLD, currentGold);

        startActivity(gameActivity, ActivityOptionsCompat.makeSceneTransitionAnimation(SplashActivity.this).toBundle());
    }
}
