import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;



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
        int tamanhochunk = 5; // mudar de volta para 995

        byte conteudo[] = this.fsChunk.getData();

        MakeItChuncky chuncky = new MakeItChuncky(new String(conteudo), tamanhochunk);
        String filename= new String(conteudo);
        File file = new File(filename);
        long fileSize = file.length();

        int numeroChunks = chuncky.divideFicheiro();

        for (int i = 0; i < numeroChunks; i++) {

            byte[] data = chuncky.getChunk(i);

            FSChunk fc = new FSChunk(this.fsChunk.getSeqNumber(), this.fsChunk.getType(), fileSize, numeroChunks,i, data,data.length);

            byte[] content = fc.generateFSChunk();
            DatagramPacket packet = new DatagramPacket(content, content.length, ip, porta);
            try {
                socket.send(packet);
                String fileee = new String(data);
                System.out.println("---- SENT ----\n" + fileee);
            } catch (IOException e) {
                System.out.println("CATCH NO MAKEITHREADY");
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


        }