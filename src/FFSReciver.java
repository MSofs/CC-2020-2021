import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class FFSReciver {
    private DatagramSocket socket;
    private ArrayList<DatagramPacket> datagramas;
    private InetAddress ip;
    // used when receiving a connection request


    public FFSReciver(InetAddress receiverIP) {
        try {
            this.socket = new DatagramSocket(8888, InetAddress.getByName("127.0.0.1"));
            this.datagramas = new ArrayList<>();
            this.ip = receiverIP;

        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public InetAddress getIp() {
        return ip;
    }

    public void setIp(InetAddress ip) {
        this.ip = ip;
    }

    public void setTimeout(int timeout) {
        try {
            this.socket.setSoTimeout(timeout);
        } catch (SocketException e) {
            System.err.println(e.toString());
        }
    }


    public void run() {

        DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);

        while (true) {
            try {
                System.out.println(" A escuta ");
                socket.receive(packet);
                datagramas.add(packet);

            } catch (IOException e) {
                System.out.println("Erro ao receber datagrama");
                continue;
            }
            FSChunk fsChunk = new FSChunk();
            fsChunk.degenerateFSChunk(packet.getData());

            MakeItThready threads = new MakeItThready(fsChunk,this.socket,packet.getAddress(),packet.getPort());
            threads.start();


        }


    }
}




