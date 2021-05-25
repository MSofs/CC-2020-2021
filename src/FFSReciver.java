import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;


public class FFSReciver {
    private DatagramSocket socket;
    private ArrayList<FSChunk> fsChunks; // Pedidos recebidos  vai premitir dar um estado dos ficheiros a tratar
    private InetAddress ip;
    // used when receiving a connection request


    public FFSReciver(Integer i) {
        try {
            this.socket = new DatagramSocket(i, InetAddress.getByName("127.0.0.1"));
            this.fsChunks = new ArrayList<>();
            this.ip = InetAddress.getByName("127.0.0.1");

        } catch (SocketException | UnknownHostException e) {
            System.out.println("CATCH DO FFSRECIVER CONSTRUTOR");
        }
    }


    public void beacon (long nuS,int porta) {

        byte[] mensagem = "ESTOU VIVO".getBytes();
        FSChunk f = new FSChunk(nuS,(byte)3,0,1,1,mensagem,mensagem.length);
        byte[] data = f.generateFSChunk();
        //System.out.println(f.toString());
        DatagramPacket packet = new DatagramPacket(data, data.length, ip, porta);
        try {

            socket.send(packet);
            String be = new String(packet.getData());
           // System.out.println("---- SENT ---- \n" );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {

        DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);


        while (true) {

            try {
                socket.setSoTimeout(1000);
                System.out.println(" A escuta ");
                socket.receive(packet);
                System.out.println("RECEBIIIIIIIIII (porta " + this.socket.getLocalPort()+")");

            } catch (IOException  e) {
                System.out.println(packet.getPort());
                beacon(0,8880);
                System.out.println("Erro ao receber datagrama");
                continue;
            }
            FSChunk fsChunk = new FSChunk();
            fsChunk = fsChunk.degenerateFSChunk(packet.getData());
            fsChunks.add(fsChunk);
            //debug
            System.out.println(fsChunk.toString());

            MakeItThready threads = new MakeItThready(fsChunk,this.socket,packet.getAddress(),packet.getPort());
            threads.start();


        }


    }

    public static void main(String[] args) throws UnknownHostException {
        Thread thread1 = new Thread () {
            public void run () {
                FFSReciver f1 = new FFSReciver(8888);
                f1.run();
            }
        };
        Thread thread2 = new Thread () {
            public void run () {
                FFSReciver f2 = new FFSReciver(8889);
                f2.run();
            }
        };
        thread1.start();
        thread2.start();

    }

}




