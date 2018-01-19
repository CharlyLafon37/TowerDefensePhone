package td.ez.com.towerdefense.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import td.ez.com.towerdefense.R;
import td.ez.com.towerdefense.network.SocketSingleton;

public class SplashActivity extends AppCompatActivity
{
    private Socket socket;

    private TextView stateView;
    private EditText nameView;
    private Button validateButton;
    private LoadingDots loadingDots;

    private String pseudoPlayer;

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
        setupSocketListeners(socket);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        socket.off("setup");
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

    private void setupSocketListeners(Socket socket)
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
                                /***** Saving the other players' pseudos *****/
                                JSONArray namesJson = json.getJSONArray("names");
                                Set<String> names = new HashSet<>();

                                for(int i = 0; i < namesJson.length(); i++)
                                {
                                    if(namesJson.getString(i).equals(pseudoPlayer)) // Not registering the player's pseudo
                                        continue;
                                    names.add(namesJson.getString(i));
                                }

                                SharedPreferences sharedPref = getSharedPreferences(getString(R.string.sp_pseudos), Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPref.edit();
                                editor.putStringSet("other_pseudos", names);
                                editor.commit();
                                /********/

                                stateView.setText("La partie peut commencer.");
                                stateView.setTextColor(ContextCompat.getColor(SplashActivity.this, R.color.colorAccent));
                                loadingDots.setVisibility(View.GONE);

                                /***** Launching the game : starting a new activity *****/
                                Handler handlerLaunch = new Handler();
                                handlerLaunch.postDelayed(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        Intent gameActivity = new Intent(SplashActivity.this, GameActivity.class);
                                        startActivity(gameActivity, ActivityOptionsCompat.makeSceneTransitionAnimation(SplashActivity.this).toBundle());
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
    }

    public void sendName(View v)
    {
        nameView.setVisibility(View.GONE);
        validateButton.setVisibility(View.GONE);

        pseudoPlayer = nameView.getText().toString().trim();

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

        /***** Registering the player's pseudo *****/
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.sp_pseudos), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("player_pseudo", pseudoPlayer);
        editor.commit();

        enableImmersiveMode();
    }
}
