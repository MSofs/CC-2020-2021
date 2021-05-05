import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

public class GatewayThread extends Thread{
    private Integer porta;
    private InetAddress ip;



    public GatewayThread(Integer p, InetAddress i){
        this.porta = p;
        this.ip = i;
    }

    public void removeServers(){
        Gateway.removeServers(10000);
    }

    @Override
    public void run() {
        removeServers();
    }
}
