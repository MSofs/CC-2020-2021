import java.io.IOException;
import java.net.*;
import java.util.Scanner;

public class Gateway {

    //receber pedido do cliente -- dado numero de sequencia
    //encapsular pedido do cliente para enviar ao fastfileserver -- ter em conta os tipos de pedido ???
    //receber datagrama do fastfileserver  -- tendo em conta os tipos
    //concatenar chunks se caso disso -- vai ser funcao auxiliar penso para a aliena acima
    //enviar resultado para o cliente -- para o tal numero de sequencia


    private DatagramSocket socket;

    public Gateway(String address,int port) throws UnknownHostException, SocketException {
        this.socket = new DatagramSocket(port, InetAddress.getByName(address));
    }

    public void start() throws IOException {
        Scanner cin = new Scanner(System.in);

        cin.nextLine();
        System.out.println("SENDING REQUEST");
        byte[] conteudo = "./src/Teste1.txt".getBytes();
        FSChunk f = new FSChunk(123,(byte)1,0,1,1,conteudo,conteudo.length);
        byte[] request = f.generateFSChunk();
        System.out.print(f.toString());
        this.socket.send(new DatagramPacket(request,request.length,InetAddress.getByName("127.0.0.1"),8888));

        while(true)
        {
            byte msg[] = new byte[1024];
            DatagramPacket payload = new DatagramPacket(msg,1024);

            try {
                System.out.println("Gateway listening...");
                this.socket.receive(payload);
                System.out.println("Payload received");
                FSChunk f1 = new FSChunk();
                f1 = f1.degenerateFSChunk(payload.getData());
                System.out.println(f1.toString());
                System.out.println("");
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }
        }
    }

    public static void main(String[] args) throws IOException {
        Gateway gateway = new Gateway("127.0.0.1",8880);
        gateway.start();
    }


}
