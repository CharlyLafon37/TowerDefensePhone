package td.ez.com.towerdefense.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Vibrator;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.transition.Explode;
import android.transition.Slide;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import td.ez.com.towerdefense.R;
import td.ez.com.towerdefense.dialogs.CustomGoldDialog;
import td.ez.com.towerdefense.dialogs.PickPlayerDialog;
import td.ez.com.towerdefense.enums.Power;
import td.ez.com.towerdefense.network.SocketSingleton;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;

/**
 * Would be very worth to build some data binding.
 */
public class GameActivity extends AppCompatActivity
{
    public static final int REQUEST_CODE_TRAP = 1;
    public static final String EXTRA_RESULT_GOLD = "td.ez.com.towerdefense.resultgold";

    public static final String EXTRA_PLAYERPSEUDO = "td.ez.com.towerdefense.playerpseudo";
    public static final String EXTRA_GOLD = "td.ez.com.towerdefense.gold";
    public static final String EXTRA_COLOR = "td.ez.com.towerdefense.color";

    private int currentGoldAmount;
    private TextView currentGoldAmountView;

    private String playerPseudo;
    private List<String> othersPseudos;

    private Power power;
    private boolean powerEnabled = true;
    private boolean powerCancelMode = false;
    private ImageView powerButton;

    private Map<String, Integer[]> basesPdv;

    private int colorCodePlayer;

    private Socket socket;

    private Vibrator vibrator;

    private boolean doTutorial;
    private int stepTuto = 1;
    private MaterialShowcaseView showCase;

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

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        Intent launchIntent = getIntent();
        playerPseudo = launchIntent.getStringExtra(SplashActivity.EXTRA_PSEUDO_PLAYER);
        othersPseudos = launchIntent.getStringArrayListExtra(SplashActivity.EXTRA_PSEUDO_OTHERS);
        power = (Power) launchIntent.getSerializableExtra(SplashActivity.EXTRA_POWER);
        currentGoldAmount = launchIntent.getIntExtra(SplashActivity.EXTRA_GOLD, 0);
        colorCodePlayer = launchIntent.getIntExtra(SplashActivity.EXTRA_COLOR, 0);
        basesPdv = (Map<String, Integer[]>) launchIntent.getSerializableExtra(SplashActivity.EXTRA_BASES);
        doTutorial = launchIntent.getBooleanExtra(SplashActivity.EXTRA_DOTUTORIAL, true);

        setupSocketListeners();

        /*** MOCK ***/
        /*power = Power.FIRE; // MOCK
        currentGoldAmount = 1000;
        colorCodePlayer = Color.parseColor("#00C853");
        basesPdv.put("windmill", new Integer[]{30, 30});
        basesPdv.put("castle", new Integer[]{40, 40});
        basesPdv.put("cathedral", new Integer[]{50, 50});
        basesPdv.put("tavern", new Integer[]{60, 60});
        doTutorial = true;*/
        /************/

        TextView pseudoView = findViewById(R.id.pseudo_player);
        pseudoView.setText(playerPseudo);

        currentGoldAmountView = findViewById(R.id.current_gold);
        currentGoldAmountView.setText(Integer.toString(currentGoldAmount));

        powerButton = findViewById(R.id.power_button);
        powerButton.setImageDrawable(getDrawable(power.getPowerEnabledDrawable()));

        updateBasesPdv(null);

        if(doTutorial)
        {
            setupStep1();
        }
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

        socket.on("msg", new Emitter.Listener()
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
                            int delta = json.getInt("delta"); // Delta negative.

                            if(delta < 0)
                            {
                                basesPdv.get(base)[0] += delta;
                                updateBasesPdv(base);
                                vibrator.vibrate(100);
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
                        JSONObject json = (JSONObject) args[0];

                        try
                        {
                            if(json.getString("action").equals("reset"))
                            {
                                if(showCase.getParent() != null)
                                    showCase.hide();

                                currentGoldAmount = json.getInt("gold");
                                currentGoldAmountView.setText(Integer.toString(currentGoldAmount));

                                basesPdv = new HashMap<>();
                                JSONArray basesJson = json.getJSONArray("bases");
                                for(int i = 0; i < basesJson.length(); i++)
                                {
                                    JSONObject baseJson = basesJson.getJSONObject(i);
                                    Integer[] hp = new Integer[2];
                                    hp[0] = baseJson.getInt("hp");
                                    hp[1] = baseJson.getInt("hp");
                                    basesPdv.put(baseJson.getString("name"), hp);
                                }
                                updateBasesPdv(null);
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

        if(doTutorial)
        {
            socket.on("tuto", new Emitter.Listener()
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
                                if(json.has("status") && json.getString("status").equals("success"))
                                {
                                    if(stepTuto == 1)
                                    {
                                        stepTuto++;
                                        showCase.hide();
                                        setupStep2();
                                    }
                                    else if(stepTuto == 2)
                                    {
                                        stepTuto++;
                                        setupStep3();
                                        sendStepToServer();
                                    }
                                    else if(stepTuto == 3)
                                    {
                                        // Do nothing. The showcase is hidden and the tuto is over.
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

            socket.emit("msg", json);
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
        intent.putExtra(EXTRA_COLOR, colorCodePlayer);

        startActivity(intent);
    }

    /************       **********
     ************ BASE  **********
     ************       **********/

    private void updateBasesPdv(String whichOne)
    {
        if(whichOne == null)
        {
            for(Map.Entry<String, Integer[]> entry : basesPdv.entrySet())
            {
                TextView pdvView = null;
                switch(entry.getKey())
                {
                    case "windmill" :
                    {
                        pdvView = findViewById(R.id.windmill_pdv_text);
                        break;
                    }
                    case "castle" :
                    {
                        pdvView = findViewById(R.id.castle_pdv_text);
                        break;
                    }
                    case "cathedral" :
                    {
                        pdvView = findViewById(R.id.church_pdv_text);
                        break;
                    }
                    case "tavern" :
                    {
                        pdvView = findViewById(R.id.tavern_pdv_text);
                        break;
                    }
                }
                pdvView.setText(entry.getValue()[0] + " / " + entry.getValue()[1]);
            }
        }
        else
        {
            TextView pdvView = null;
            switch(whichOne)
            {
                case "windmill" :
                {
                    pdvView = findViewById(R.id.windmill_pdv_text);
                    break;
                }
                case "castle" :
                {
                    pdvView = findViewById(R.id.castle_pdv_text);
                    break;
                }
                case "cathedral" :
                {
                    pdvView = findViewById(R.id.church_pdv_text);
                    break;
                }
                case "tavern" :
                {
                    pdvView = findViewById(R.id.tavern_pdv_text);
                    break;
                }
            }
            pdvView.setText(basesPdv.get(whichOne)[0] + " / " + basesPdv.get(whichOne)[1]);

            pdvView.setTextColor(getColor(R.color.error));

            final TextView newRef = pdvView;
            Handler handler = new Handler();
            handler.postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    int colorNormal = new TextView(GameActivity.this).getCurrentTextColor();
                    newRef.setTextColor(colorNormal);
                }
            }, 2000);
        }
    }

    /************       **********
     ************ TUTO  **********
     ************       **********/

    private void sendStepToServer()
    {
        try
        {
            JSONObject json = new JSONObject();

            json.put("player", playerPseudo);
            json.put("step", stepTuto);

            socket.emit("tuto", json);
        }
        catch(JSONException e)
        {
            e.printStackTrace();
        }
    }

    private void setupStep1()
    {
        TextView pseudoPlayerView = findViewById(R.id.pseudo_player);
        showCase = new MaterialShowcaseView.Builder(this)
                .setTarget(pseudoPlayerView)
                .setContentText(R.string.showcase_step1)
                .setDelay(500)
                .show();
        sendStepToServer();
    }

    private void setupStep2()
    {
        ImageView trapButton = findViewById(R.id.trap);
        showCase = new MaterialShowcaseView.Builder(this)
                .setTarget(trapButton)
                .setContentText(R.string.showcase_step2)
                .setDelay(1000)
                .setDismissOnTargetTouch(true)
                .setTargetTouchable(true)
                .show();
        sendStepToServer();
    }

    private void setupStep3()
    {
        showCase = new MaterialShowcaseView.Builder(this)
                .setTarget(powerButton)
                .setContentText(R.string.showcase_step3)
                .setDelay(1000)
                .setDismissOnTargetTouch(true)
                .setTargetTouchable(true)
                .show();
        sendStepToServer();
    }
}
