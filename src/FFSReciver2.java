import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class FFSReciver2 {

        private DatagramSocket socket;
        private ArrayList<FSChunk> fsChunks; // Pedidos recebidos  vai premitir dar um estado dos ficheiros a tratar
        private InetAddress ip;
        // used when receiving a connection request


        public FFSReciver2(InetAddress receiverIP) {
            try {
                this.socket = new DatagramSocket(8889, InetAddress.getByName("127.0.0.1"));
                this.fsChunks = new ArrayList<>();
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


        public void beacon (long nuS,int porta) {

            byte[] mensagem = "ESTOU VIVO".getBytes();
            FSChunk f = new FSChunk(nuS,(byte)3,0,1,1,mensagem,mensagem.length);
            byte[] data = f.generateFSChunk();
            System.out.println(f.toString());
            DatagramPacket packet = new DatagramPacket(data, data.length, ip, porta);
            try {

                socket.send(packet);
                String be = new String(packet.getData());
                System.out.println("---- SENT ----\n" + be);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public ArrayList<String> filesTratar() {
            ArrayList<String> arrayFiles = new ArrayList<>();

            for (FSChunk f : this.fsChunks) {
                if (f.getType() == 1) {
                    byte c[] = f.getData();
                    String file = new String(c);
                    File filee = new File(file);

                    if (filee.exists()) {
                        arrayFiles.add(file);
                    }
                }

            }
            return arrayFiles;
        }

        public void run() {

            DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);


            while (true) {

                try {
                    socket.setSoTimeout(1000);
                    System.out.println(" A escuta ");
                    socket.receive(packet);
                    System.out.println("RECEBIIIIIIIIII");

                } catch (IOException  e) {
                    FSChunk fsChunk = new FSChunk();
                    fsChunk = fsChunk.degenerateFSChunk(packet.getData());
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
            FFSReciver2 f = new FFSReciver2(InetAddress.getByName("127.0.0.1"));
            f.run();
        }

    }


