package td.ez.com.towerdefense.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.Spanned;
import android.transition.Slide;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.eyalbira.loadingdots.LoadingDots;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import td.ez.com.towerdefense.R;
import td.ez.com.towerdefense.enums.Power;
import td.ez.com.towerdefense.network.SocketSingleton;

public class SplashActivity extends AppCompatActivity
{
    public static final String EXTRA_PSEUDO_PLAYER = "td.ez.com.towerdefense.extrapseudoplayer";
    public static final String EXTRA_PSEUDO_OTHERS = "td.ez.com.towerdefense.extrapseudoothers";
    public static final String EXTRA_POWER = "td.ez.com.towerdefense.power";
    public static final String EXTRA_GOLD = "td.ez.com.towerdefense.gold";
    public static final String EXTRA_COLOR = "td.ez.com.towerdefense.color";
    public static final String EXTRA_BASES = "td.ez.com.towerdefense.bases";
    public static final String EXTRA_DOTUTORIAL = "td.ez.com.towerdefense.dotutorial";


    private Socket socket;

    private TextView stateView;
    private EditText nameView;
    private Button validateButton;
    private LoadingDots loadingDots;
    private ImageView colorCircle;
    private Button buttonLaunchGame;

    private String pseudoPlayer;
    private List<String> pseudoOthers = new ArrayList<>();
    private String pseudoAttacker;

    private String colorPlayer;
    private int colorCode;

    private Power power;

    private int currentGold;

    private Map<String, Integer[]> bases = new HashMap<>();

    private boolean doTutorial = true;

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
        colorCircle = findViewById(R.id.color_circle);
        buttonLaunchGame = findViewById(R.id.buttonLaunchGame);

        buttonLaunchGame.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                launchGameActivity();
            }
        });

        enableImmersiveMode();

        socket = SocketSingleton.getInstance().getSocket();
        setupSocketListeners();
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

                                pseudoAttacker = json.getString("attacker");

                                loadingDots.setVisibility(View.GONE);


                                Spanned htmlText = Html.fromHtml(R.string.sumup_goal_part_one
                                        + " " + "<b>" + pseudoPlayer + "</b>"
                                        + getString(R.string.sumup_goal_part_two)
                                        + " " + "<b>" + pseudoAttacker + "</b>"
                                        + " " + getString(R.string.sumup_goal_part_three)
                                        + "<br><br>" + "<center>" + "Prenez votre tag couleur :" + "</center>");

                                stateView.setText(htmlText);

                                colorCircle.setColorFilter(colorCode);
                                colorCircle.setVisibility(View.VISIBLE);

                                buttonLaunchGame.setVisibility(View.VISIBLE);
                            }

                            else if(json.getString("action").equals("start"))
                            {
                                colorCircle.setVisibility(View.GONE);

                                launchGameActivity();
                            }

                            else if(json.getString("action").equals("reset"))
                            {
                                currentGold = json.getInt("gold");

                                JSONArray basesJson = json.getJSONArray("bases");
                                for(int i = 0; i < basesJson.length(); i++)
                                {
                                    JSONObject baseJson = basesJson.getJSONObject(i);
                                    Integer[] hp = new Integer[2];
                                    hp[0] = baseJson.getInt("hp");
                                    hp[1] = baseJson.getInt("hp");
                                    bases.put(baseJson.getString("name"), hp);
                                }

                                doTutorial = false;
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
                JSONObject json = (JSONObject) args[0];

                try
                {
                    JSONObject colorJson = json.getJSONObject("color");
                    colorPlayer = colorJson.getString("name");
                    colorCode = Color.parseColor(colorJson.getString("value"));
                }
                catch(JSONException e)
                {
                    e.printStackTrace();
                }
            }
        });

        socket.on("base", new Emitter.Listener()
        {
            @Override
            public void call(final Object... args)
            {
                JSONObject json = (JSONObject) args[0];

                try
                {
                    JSONArray basesJson = json.getJSONArray("bases");
                    for(int i = 0; i < basesJson.length(); i++)
                    {
                        JSONObject baseJson = basesJson.getJSONObject(i);
                        Integer[] hp = new Integer[2];
                        hp[0] = baseJson.getInt("hp");
                        hp[1] = baseJson.getInt("hp");
                        bases.put(baseJson.getString("name"), hp);
                    }
                }
                catch(JSONException e)
                {
                    e.printStackTrace();
                }
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
        socket.off();

        Intent gameActivity = new Intent(SplashActivity.this, GameActivity.class);
        gameActivity.putExtra(EXTRA_PSEUDO_PLAYER, pseudoPlayer);
        gameActivity.putStringArrayListExtra(EXTRA_PSEUDO_OTHERS, (ArrayList<String>) pseudoOthers);
        gameActivity.putExtra(EXTRA_POWER, power);
        gameActivity.putExtra(EXTRA_GOLD, currentGold);
        gameActivity.putExtra(EXTRA_COLOR, colorCode);
        gameActivity.putExtra(EXTRA_BASES, (HashMap<String, Integer[]>) bases);
        gameActivity.putExtra(EXTRA_DOTUTORIAL, doTutorial);

        startActivity(gameActivity, ActivityOptionsCompat.makeSceneTransitionAnimation(SplashActivity.this).toBundle());
    }
}
