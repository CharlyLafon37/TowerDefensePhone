package td.ez.com.towerdefense.network;

import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import java.net.URISyntaxException;

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
            socket = IO.socket("http://192.168.1.26:9091");
        }
        catch(URISyntaxException e)
        {
            e.printStackTrace();
        }

        socket.connect();
    }

    public Socket getSocket() {return socket;}
}
