package td.ez.com.towerdefense.network;

/**
 * Created by Charly on 18/01/2018.
 */

public class SocketSingleton
{
    private static final SocketSingleton ourInstance = new SocketSingleton();

    public static SocketSingleton getInstance()
    {
        return ourInstance;
    }

    private SocketSingleton()
    {

    }
}
