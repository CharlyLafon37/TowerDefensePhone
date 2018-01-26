package td.ez.com.towerdefense.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Vibrator;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.transition.Explode;
import android.transition.Slide;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import td.ez.com.towerdefense.R;
import td.ez.com.towerdefense.dialogs.CustomGoldDialog;
import td.ez.com.towerdefense.dialogs.PickPlayerDialog;
import td.ez.com.towerdefense.network.SocketSingleton;

/**
 * Would be very worth to build some data binding.
 */
public class GameActivity extends AppCompatActivity
{
    private int currentGoldAmount;
    private TextView currentGoldAmountView;

    private String playerPseudo;
    private List<String> othersPseudos;

    private String power;

    private Socket socket;

    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        getWindow().setAllowEnterTransitionOverlap(true);
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        getWindow().setEnterTransition(new Slide(Gravity.RIGHT));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        enableImmersiveMode();

        socket = SocketSingleton.getInstance().getSocket();
        setupSocketListeners(socket);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        Intent launchIntent = getIntent();
        playerPseudo = launchIntent.getStringExtra(SplashActivity.EXTRA_PSEUDO_PLAYER);
        othersPseudos = launchIntent.getStringArrayListExtra(SplashActivity.EXTRA_PSEUDO_OTHERS);
        power = launchIntent.getStringExtra(SplashActivity.EXTRA_POWER);
        currentGoldAmount = launchIntent.getIntExtra(SplashActivity.EXTRA_GOLD, 0);

        TextView pseudoView = findViewById(R.id.pseudo_player);
        pseudoView.setText(playerPseudo);

        currentGoldAmountView = findViewById(R.id.current_gold);
        currentGoldAmountView.setText(Integer.toString(currentGoldAmount));

        Button powerButton = findViewById(R.id.power_button);
        powerButton.setText(powerButton.getText() + power);
    }

    private void enableImmersiveMode()
    {
        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    }

    private void setupSocketListeners(Socket socket)
    {
        socket.on("gift", new Emitter.Listener()
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
                            String from = json.getString("from");
                            int amount = json.getInt("amount");
                            Snackbar.make(
                                    findViewById(R.id.game_layout),
                                    "Received " + amount + " gold from " + from + ".",
                                    Snackbar.LENGTH_SHORT).show();

                            currentGoldAmount += amount;
                            currentGoldAmountView.setText(Integer.toString(currentGoldAmount));
                        }
                        catch(JSONException e)
                        {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

        socket.on("gold", new Emitter.Listener()
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
                            int amount = json.getInt("amount");
                            currentGoldAmount += amount;
                            currentGoldAmountView.setText(Integer.toString(currentGoldAmount));
                        }
                        catch(JSONException e)
                        {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

        socket.on("message", new Emitter.Listener()
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
                            String from = json.getString("from");
                            String message = json.getString("msg");

                            Snackbar.make(
                                    findViewById(R.id.game_layout),
                                    "De " + from + " : " + message,
                                    Snackbar.LENGTH_LONG).show();
                        }
                        catch(JSONException e)
                        {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

        socket.on("state", new Emitter.Listener()
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
                            String action = json.getString("action");
                            Snackbar.make(
                                    findViewById(R.id.game_layout),
                                    "Action : " + action,
                                    Snackbar.LENGTH_SHORT).show();
                        }
                        catch(JSONException e)
                        {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

        socket.on("base", new Emitter.Listener()
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
                            String base = json.getString("base");
                            int delta = json.getInt("delta");

                            if(delta < 0)
                            {
                                vibrator.vibrate(100);
                                /*Snackbar.make(
                                        findViewById(R.id.game_layout),
                                        base + " a perdu " + delta + " PDV.",
                                        Snackbar.LENGTH_SHORT).show();*/
                                // Remplacer par le nb pdv de la base en rouge
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


    /************       **********
     ************ GOLD **********
     ************       **********/


    public void onClickSendGold(View v)
    {
        final int amount;

        switch(v.getId())
        {
            case R.id.sendGold10:
            {
                amount = 10;
                break;
            }
            case R.id.sendGold50:
            {
                amount = 50;
                break;
            }
            case R.id.sendGold100:
            {
                amount = 100;
                break;
            }
            default: amount = 0;
        }

        DialogFragment dialogFragment = new PickPlayerDialog();
        Bundle args = new Bundle();
        args.putStringArrayList("names", (ArrayList<String>) othersPseudos);
        args.putSerializable("listener", new PickPlayerDialog.PickPlayerListener()
        {
            @Override
            public void onPlayerPicked(String playerName)
            {
                sendGold(playerName, amount);
            }
        });
        dialogFragment.setArguments(args);

        dialogFragment.show(getSupportFragmentManager(), "PickPlayerDialog");
    }

    public void onClickSendCustomGold(View v)
    {
        DialogFragment dialogFragment = new CustomGoldDialog();
        Bundle args = new Bundle();
        args.putStringArrayList("names", (ArrayList<String>) othersPseudos);
        args.putInt("currentGold", currentGoldAmount);
        args.putSerializable("listener", new CustomGoldDialog.CustomGoldListener()
        {
            @Override
            public void onPositiveClick(String playerName, int amount)
            {
                sendGold(playerName, amount);
            }
        });
        dialogFragment.setArguments(args);

        dialogFragment.show(getSupportFragmentManager(), "CustomGoldDialog");
    }

    private void sendGold(String player, int amount)
    {
        try
        {
            JSONObject json = new JSONObject();
            json.put("from", playerPseudo);
            json.put("to", player);
            json.put("amount", amount);

            socket.emit("gift", json);
        }
        catch(JSONException e)
        {
            e.printStackTrace();
        }

        currentGoldAmount -= amount;
        currentGoldAmountView.setText(Integer.toString(currentGoldAmount));
    }

    public void sendHelpGold(View v)
    {
        try
        {
            JSONObject json = new JSONObject();
            json.put("from", playerPseudo);
            json.put("broadcast", true);
            json.put("msg", "J'ai besoin d'or !");

            socket.emit("message", json);
        }
        catch(JSONException e)
        {
            e.printStackTrace();
        }
    }


    /************       **********
     ************ POWER **********
     ************       **********/


    public void onClickPowerButton(View v)
    {
        try
        {
            JSONObject json = new JSONObject();
            json.put("player", playerPseudo);
            json.put("power", power);

            socket.emit("power", json);

        }
        catch(JSONException e)
        {
            e.printStackTrace();
        }
    }
}
