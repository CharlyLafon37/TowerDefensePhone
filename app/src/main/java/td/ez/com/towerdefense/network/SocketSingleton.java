package td.ez.com.towerdefense.network;

import com.github.nkzawa.socketio.client.IO;

import java.net.URISyntaxException;
import com.github.nkzawa.socketio.client.Socket;

import td.ez.com.towerdefense.R;

/**
 * Created by Charly on 18/01/2018.
 */

public class SocketSingleton
{
    private static final SocketSingleton ourInstance = new SocketSingleton();

    private Socket socket;

    public static SocketSingleton getInstance()
    {
        return ourInstance;
    }

    private SocketSingleton()
    {
        try
        {
            socket = IO.socket("http://192.168.43.254:3000");
        }
        catch(URISyntaxException e)
        {
            e.printStackTrace();
        }

        socket.connect();
    }

    public Socket getSocket() {return socket;}
}
