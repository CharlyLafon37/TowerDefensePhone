package td.ez.com.towerdefense;

import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.eyalbira.loadingdots.LoadingDots;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

public class SplashActivity extends AppCompatActivity
{
    private Socket socket;

    private TextView stateView;
    private EditText nameView;
    private Button validateButton;
    private LoadingDots loadingDots;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);
        stateView = findViewById(R.id.state);
        nameView = findViewById(R.id.name);
        validateButton = findViewById(R.id.sendName);
        loadingDots = findViewById(R.id.loadingDots);

        enableImmersiveMode();

        initSocket();

        final Handler handler1 = new Handler();
        handler1.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                stateView.setText("Collecte des pseudos en cours");
                nameView.setVisibility(View.VISIBLE);
                validateButton.setVisibility(View.VISIBLE);
            }
        }, 3000);
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

    private void initSocket()
    {
        try
        {
            socket = IO.socket("http://192.168.43.254:3000");
        }
        catch(URISyntaxException e)
        {
            e.printStackTrace();
        }

        setupSocketListeners(socket);

        socket.connect();
    }

    private void setupSocketListeners(Socket socket)
    {
        socket.on("setup", new Emitter.Listener()
        {
            @Override
            public void call(final Object... args)
            {
                try
                {
                    JSONObject json = (JSONObject) args[0];
                    if(json.getString("action").equals("name"))
                    {
                        stateView.setText("Setup en cours.");
                        nameView.setVisibility(View.VISIBLE);
                        validateButton.setVisibility(View.VISIBLE);
                    }
                    else if(json.getString("action").equals("ready"))
                    {
                        stateView.setText("Partie peut commencer.");
                        stateView.setTextColor(ContextCompat.getColor(SplashActivity.this, R.color.colorAccent));
                        loadingDots.setVisibility(View.GONE);
                    }
                }
                catch (JSONException e)
                {
                    e.printStackTrace();
                }
            }
        });
    }

    public void sendName(View v)
    {
        nameView.setVisibility(View.GONE);
        validateButton.setVisibility(View.GONE);

        try
        {
            JSONObject json = new JSONObject();
            json.put("action", "ready");

            JSONObject body = new JSONObject();
            body.put("name", nameView.getText().toString().trim());
            json.put("body", body);

            socket.emit("setup", json);
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        enableImmersiveMode();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                stateView.setText("Partie peut commencer.");
                stateView.setTextColor(ContextCompat.getColor(SplashActivity.this, R.color.colorAccent));
                loadingDots.setVisibility(View.GONE);
            }
        }, 2000);
    }
}
