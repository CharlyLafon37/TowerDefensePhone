package td.ez.com.towerdefense.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.CountDownTimer;
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
import android.widget.ImageView;
import android.widget.TextView;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import td.ez.com.towerdefense.R;
import td.ez.com.towerdefense.dialogs.CustomGoldDialog;
import td.ez.com.towerdefense.dialogs.PickPlayerDialog;
import td.ez.com.towerdefense.enums.Power;
import td.ez.com.towerdefense.network.SocketSingleton;

/**
 * Would be very worth to build some data binding.
 */
public class GameActivity extends AppCompatActivity
{
    public static final int REQUEST_CODE_TRAP = 1;
    public static final String EXTRA_RESULT_GOLD = "td.ez.com.towerdefense.resultgold";

    public static final String EXTRA_PLAYERPSEUDO = "td.ez.com.towerdefense.playerpseudo";
    public static final String EXTRA_GOLD = "td.ez.com.towerdefense.gold";

    private int currentGoldAmount;
    private TextView currentGoldAmountView;

    private String playerPseudo;
    private List<String> othersPseudos;

    private Power power;
    private boolean powerEnabled = true;
    private boolean powerCancelMode = false;
    private ImageView powerButton;

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
        setupSocketListeners();

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        Intent launchIntent = getIntent();
        playerPseudo = launchIntent.getStringExtra(SplashActivity.EXTRA_PSEUDO_PLAYER);
        othersPseudos = launchIntent.getStringArrayListExtra(SplashActivity.EXTRA_PSEUDO_OTHERS);
        power = (Power) launchIntent.getSerializableExtra(SplashActivity.EXTRA_POWER);
        currentGoldAmount = launchIntent.getIntExtra(SplashActivity.EXTRA_GOLD, 0);

        TextView pseudoView = findViewById(R.id.pseudo_player);
        pseudoView.setText(playerPseudo);

        currentGoldAmountView = findViewById(R.id.current_gold);
        currentGoldAmountView.setText(Integer.toString(currentGoldAmount));

        powerButton = findViewById(R.id.power_button);
        powerButton.setImageDrawable(getDrawable(power.getPowerEnabledDrawable()));
    }

    private void enableImmersiveMode()
    {
        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    }

    private void setupSocketListeners()
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

        socket.on("power", new Emitter.Listener()
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
                                powerEnabled = false;
                                powerButton.setImageDrawable(getDrawable(power.getPowerDisabledDrawable()));

                                final TextView textPowerView = findViewById(R.id.power_button_text);
                                CountDownTimer timer = new CountDownTimer(5000, 1000)
                                {
                                    @Override
                                    public void onTick(long l)
                                    {
                                        textPowerView.setText(Integer.toString(((int) l) / 1000));
                                    }

                                    @Override
                                    public void onFinish()
                                    {
                                        powerEnabled = true;
                                        powerButton.setImageDrawable(getDrawable(power.getPowerEnabledDrawable()));
                                        vibrator.vibrate(150);
                                        textPowerView.setVisibility(View.INVISIBLE);
                                    }
                                };
                                timer.start();
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

        if(amount > currentGoldAmount)
            return;

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

        Snackbar.make(
                findViewById(R.id.game_layout),
                "Demande d'aide envoyée aux autres défenseurs.",
                Snackbar.LENGTH_SHORT).show();
    }


    /************       **********
     ************ POWER **********
     ************       **********/


    public void onClickPowerButton(View v)
    {
        if(powerEnabled)
        {
            try
            {
                JSONObject json = new JSONObject();
                json.put("player", playerPseudo);
                json.put("power", power.toString());

                if(!powerCancelMode)
                    json.put("action", "use");
                else
                    json.put("action", "cancel");

                socket.emit("power", json);
            }
            catch(JSONException e)
            {
                e.printStackTrace();
            }

            final TextView textPowerView = findViewById(R.id.power_button_text);
            if(!powerCancelMode)
            {
                textPowerView.setVisibility(View.VISIBLE);
                textPowerView.setText("X");
                textPowerView.setTypeface(null, Typeface.BOLD);
                textPowerView.setTextSize(35f);
                powerCancelMode = true;
            }
            else
            {
                textPowerView.setVisibility(View.INVISIBLE);
                powerCancelMode = false;
            }
        }
    }

    /************       **********
     ************ TRAP  **********
     ************       **********/

    public void launchTrapActivity(View v)
    {
        Intent intent = new Intent(this, TrapActivity.class);
        intent.putExtra(EXTRA_PLAYERPSEUDO, playerPseudo);
        intent.putExtra(EXTRA_GOLD, currentGoldAmount);

        startActivity(intent);
    }
}
