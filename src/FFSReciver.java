import java.lang.reflect.Array;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;

public class FFSReciver {
    private DatagramSocket socket;
    private ArrayList<DatagramPacket> datagramas;
    private ArrayList<FSChunk> fschunks;
    private InetAddress ip;


    public FFSReciver (InetAddress receiverIP){
        try {
            this.socket= new DatagramSocket(8888);
            this.datagramas = new ArrayList<>();
            this.fschunks = new ArrayList<>();
            this.ip = receiverIP;

        }catch (SocketException e){
            e.printStackTrace();
        }
    }














}
