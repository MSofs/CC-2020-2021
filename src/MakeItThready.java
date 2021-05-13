import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;


public class MakeItThready extends Thread{
    private FSChunk fsChunk;
    private InetAddress ip;
    private int porta;//do gateaway
    private DatagramSocket socket;


    public MakeItThready(FSChunk f,DatagramSocket s,InetAddress i,int p){
        this.fsChunk = new FSChunk(f);
        this.socket = s;
        this.ip = i;
        this.porta = p;
    }

    public void enviarFicheiro() throws FileNotFoundException {
        int numeroChunks;
        int tamanhochunk = 995;

        byte conteudo[] = this.fsChunk.getData();

        MakeItChuncky chuncky = new MakeItChuncky(new String(conteudo), tamanhochunk,this.fsChunk.getDataSize());
        String filename= new String(conteudo);
        File file = new File(filename);
        long fileSize = file.length();

        if(fileSize<=995){
            numeroChunks= 1;
        }
        else{
            numeroChunks = (int)fileSize/995;
            if ((int)fileSize%995 != 0) numeroChunks++;
        }


        chuncky.divideFicheiro();

        for (int i = 1; i <= chuncky.getChunks().size(); i++) {
            byte[] data = chuncky.getChunk(i);
            System.out.println(new String(data));
            FSChunk fc = new FSChunk(this.fsChunk.getSeqNumber(), this.fsChunk.getType(), fileSize, numeroChunks,i, data,data.length);

            byte[] content = fc.generateFSChunk();

            DatagramPacket packet = new DatagramPacket(content, content.length, ip, porta);
            try {
                socket.send(packet);
                String fileee = new String(data);
                System.out.println("---- SENT ----\n" + fileee);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public void existeFicheiro() throws IOException {
        byte c[] = this.fsChunk.getData();
        String file = new String(c);
        File filee = new File(file);

        if (!filee.exists()) {
            System.out.println("nao existe o ficheiro"); // mandar mensagem ao gateway a dizer que nao existe ficheiro
            }
        else{

            long fileSize = filee.length();
            FSChunk f = new FSChunk(this.fsChunk.getSeqNumber(),(byte)2,fileSize,1,1,c,c.length);
            byte[] data = f.generateFSChunk();
            System.out.println(f.toString());
            DatagramPacket packet = new DatagramPacket(data, data.length, ip, porta);
            try {
                socket.send(packet);
                String res = "Nome ficheiro: "+new String(c)+" \nTamanho do ficheiro: "+filee.length();
                System.out.println("---- SENT ----\n" + res);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    //codigo que o thread vai correr
            @Override
            public void run () {

                byte type = this.fsChunk.getType();

                switch (type) {
                    case 1: //pedir ficheiro
                        try {
                            enviarFicheiro();
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                        break;

                    case 2: // existe ficheiro ? e se sim tamanho ficheiro
                        try {
                            existeFicheiro();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                }

            }

 /*           public static void main(String[]args) throws UnknownHostException, SocketException {
                String s = "./src/teste.txt";
                byte[] data = s.getBytes();
                FSChunk f = new FSChunk(123, (byte) 2, 7, 1, 1, data);
                //System.out.println(f.toString());

                DatagramSocket s1 = new DatagramSocket(8880, InetAddress.getByName("127.0.0.1"));
                DatagramSocket s2 = new DatagramSocket(8888, InetAddress.getByName("127.0.0.1"));
                MakeItThready m = new MakeItThready(f, s1, InetAddress.getByName("127.0.0.1"), 8880);

                m.run();


            }*/
        }