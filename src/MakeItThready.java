import java.net.DatagramSocket;
import java.net.InetAddress;


public class MakeItThready extends Thread{
    private FSChunk fsChunk;
    private InetAddress ip;
    private int porta;
    private DatagramSocket socket;


    public MakeItThready(FSChunk f,DatagramSocket s,InetAddress i,int p){
        this.fsChunk = new FSChunk(f);
        this.socket = s;
        this.ip = i;
        this.porta = p;
    }

    //codigo que o thread vai correr
    @Override
    public void run() {

        // e aqui que se envia os dados

        // depende dos tipos
        // tipo metadados manda se ficheiro existe e o seu tamanho
        // tipo dados manda os chunks
        // tipo beacon para dizer que o server existe tem de estar sempre a mandar num tempo regular


    }



}
