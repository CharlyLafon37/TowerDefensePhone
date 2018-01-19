package td.ez.com.towerdefense.activities;

import android.content.Context;
import android.content.SharedPreferences;
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
import java.util.HashSet;
import java.util.Set;

import td.ez.com.towerdefense.R;
import td.ez.com.towerdefense.dialogs.PickPlayerDialog;
import td.ez.com.towerdefense.network.SocketSingleton;

/**
 * Would be very worth to build some data binding.
 */
public class GameActivity extends AppCompatActivity
{
    private int currentGoldAmount = 50;
    private TextView currentGoldAmountView;

    private String playerPseudo;

    private Socket socket;

    private String power;

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

        currentGoldAmountView = findViewById(R.id.current_gold);
        currentGoldAmountView.setText(Integer.toString(currentGoldAmount));

        /********* Picking the player to whom send the message *******/
        SharedPreferences sharedPrefPlayers = getSharedPreferences(getString(R.string.sp_pseudos), Context.MODE_PRIVATE);
        playerPseudo = sharedPrefPlayers.getString("player_pseudo", "UNDEFINED");

        Button powerButton = findViewById(R.id.power_button);
        SharedPreferences sharedPrefPower = getSharedPreferences(getString(R.string.sp_power), Context.MODE_PRIVATE);
        power = sharedPrefPower.getString("power", "UNDEFINED");
        powerButton.setText(powerButton.getText() + power);
    }

    private void enableImmersiveMode()
    {
        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private void setupSocketListeners(Socket socket)
    {
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
                            Snackbar.make(findViewById(R.id.game_layout), "Received " + amount + " gold.", Snackbar.LENGTH_SHORT).show();

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

                            Snackbar.make(findViewById(R.id.game_layout), "From " + from + " : " + message, Snackbar.LENGTH_SHORT)
                                    .show();
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

    public void onClickSendGold(View v)
    {
        EditText amountView = findViewById(R.id.gold_amount);

        /***** Validating the input : just numbers. *****/
        try
        {
            Integer.parseInt(amountView.getText().toString());
        }
        catch(NumberFormatException e)
        {
            e.printStackTrace();
            return;
        }
        final int amount = Integer.parseInt(amountView.getText().toString());

        /********* Picking the player to whom send the gold *******/
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.sp_pseudos), Context.MODE_PRIVATE);
        Set<String> names = sharedPref.getStringSet("other_pseudos", new HashSet<String>());

        DialogFragment dialogFragment = new PickPlayerDialog();
        Bundle args = new Bundle();
        args.putSerializable("names", (HashSet) names);
        args.putSerializable("listener", new PickPlayerDialog.PickPlayerListener()
        {
            @Override
            public void onPlayerPicked(String playerName)
            {
                sendGold(playerName, amount);

                // FUCKING TEMPORARY !!!!!!!!
                enableImmersiveMode();
                // FUCKING TEMPORARY !!!!!!!!
            }
        });

        dialogFragment.setArguments(args);
        dialogFragment.show(getSupportFragmentManager(), "PickPlayerDialog");
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

    public void onClickSendMessage(View v)
    {
        EditText messageView = findViewById(R.id.message_tosend);
        final String message = messageView.getText().toString().trim();

        /********* Picking the player to whom send the message *******/
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.sp_pseudos), Context.MODE_PRIVATE);
        Set<String> names = sharedPref.getStringSet("other_pseudos", new HashSet<String>());

        DialogFragment dialogFragment = new PickPlayerDialog();
        Bundle args = new Bundle();
        args.putSerializable("names", (HashSet) names);
        args.putSerializable("listener", new PickPlayerDialog.PickPlayerListener()
        {
            @Override
            public void onPlayerPicked(String playerName)
            {
                sendMessage(playerName, message);

                // FUCKING TEMPORARY !!!!!!!!
                enableImmersiveMode();
                // FUCKING TEMPORARY !!!!!!!!
            }
        });

        dialogFragment.setArguments(args);
        dialogFragment.show(getSupportFragmentManager(), "PickPlayerDialog");
    }

    public void sendMessage(String player, String message)
    {
        try
        {
            JSONObject json = new JSONObject();
            json.put("from", playerPseudo);
            json.put("to", player);
            json.put("msg", message);

            socket.emit("message", json);
        }
        catch(JSONException e)
        {
            e.printStackTrace();
        }
    }

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
