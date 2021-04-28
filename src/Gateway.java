import javax.swing.plaf.synth.SynthOptionPaneUI;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Gateway {
    private Map<Long,ArrayList<FSChunk>> fchunksRecebidos;
    private DatagramSocket socket1; // para os fastfileservers
    private DatagramSocket socket2; // para os clientes
    private ArrayList<DatagramPacket> datagramas; //pedidos dos clientes recebidos
    private Map<Integer,InetAddress> ips; // portas e ips dos clientes
    private Map<Map<Integer,InetAddress>,Integer> poolServidores; // ip e porta do servidor e o numero de tentativas

    //receber pedido do cliente -- dado numero de sequencia
    //encapsular pedido do cliente para enviar ao fastfileserver -- ter em conta os tipos de pedido ???
    //receber datagrama do fastfileserver  -- tendo em conta os tipos
    //concatenar chunks se caso disso -- vai ser funcao auxiliar penso para a aliena acima
    //enviar resultado para o cliente -- para o tal numero de sequencia


    public Gateway() throws UnknownHostException, SocketException { //Depois temos de alterar as portas pq depende do qual o cliente e qual o fastfileserver
        this.socket1 = new DatagramSocket(8880, InetAddress.getByName("127.0.0.1"));// socket para o fastfile
        this.socket2 = new DatagramSocket(0,InetAddress.getByName("127.0.0.1")); // socket para o cliente
        this.fchunksRecebidos = new HashMap<>();
        this.poolServidores = new HashMap<>();
        this.datagramas = new ArrayList<>();
        this.ips = new HashMap<>();
    }


    public void recebeMetadados(long sqn,InetAddress ipPedido, int portaPedido) throws IOException {
    // neste caso apenas tratamos de um unico chunk pois a resposta so envolve o envio do tamanho e nome do ficheiro
    ArrayList<FSChunk> pedido = this.fchunksRecebidos.get(sqn);
        byte[] mensagem = null;
        byte[] fileSize = null;

    for(FSChunk f : pedido){ // so tem 1 logo nao tem problema fazer isto no for MAS E MELHOR VER OUTRA SOLUCAO DEPOIS
        mensagem = f.getData(); // tem o nome do ficheiro
        fileSize = ByteBuffer.allocate(8).putLong(f.getFileSize()).array(); // tem tamanho do file
    }
    byte[] pacote = new byte[mensagem.length+fileSize.length];
    DatagramPacket packet = new DatagramPacket(pacote,pacote.length,ipPedido,portaPedido);//ip e porta do cliente que fez o pedido que ainda nao sei qual e
    socket2.send(packet);

    }

    public void trataBeacon(int portaServer,InetAddress ip){
        Map<Integer,InetAddress> map = new HashMap<>();
        map.put(portaServer,ip);
        if(!this.poolServidores.containsKey(map)){
            this.poolServidores.put(map,0);
        }
    }

  /*  public  void removeServer(){
        for(Map<Integer,InetAddress> keys : this.poolServidores.keySet()){
           int numeroTrys = this.poolServidores.get(keys);
           if(numeroTrys>=5){
               this.poolServidores.remove(keys);
           }
        }
    }*/

    public  void removeServer(Map<Integer,InetAddress> s){
            int nrTentativas = this.poolServidores.get(s);
            if(nrTentativas>=5){
                this.poolServidores.remove(s);
            }
    }

    public void recolheChunks(FSChunk f) throws UnknownHostException { // mas e se perder um pacote !???

        int i=1, pos=0;
        byte[] mensagem = new byte[Math.toIntExact(f.getFileSize())];
        ArrayList<FSChunk> fs = this.fchunksRecebidos.get(f.getSeqNumber());
        //DEBUG
        System.out.println("Tamanho do array: "+fs.size());

        if(fs.size()==f.getNrChunks()){ // verifica se ja temos todos os chunks necessarios e se sim reconstroi
            for(FSChunk c : fs){
                if(i==c.getNrChunk()){
                    byte[] conteudo = c.getData();
                    for(int j=0; j<c.getDataSize();j++) {
                        mensagem[pos]=conteudo[j]; pos++;}
                    pos++;
                    i++;
                }
            }
        }
        //DEBUG
        String mensa = new String(mensagem);
        System.out.println("\n Tamanho da mensagem a enviar: \n"+mensagem.length+"Tamanho da mensagem enviada: \n" + f.getFileSize()+" \nESTA É A MENSAGEM A ENVIAR AO CLIENTE: "+mensa );

        //depois temos de mudar para o ip e porta do respetivo cliente
        // podemos procurar na lista de pedidos aquele cujo numero sequencia e igual e obtemos o ip e a porta
        DatagramPacket pacote = new DatagramPacket(mensagem,mensagem.length,InetAddress.getByName("127.0.0.1"),8882);

    }
    // tratar de receber do cliente um pedido e enviar resposta
    // enviar pedido ao fastfileserver receber resposta do fastfileserver

    public void start() throws IOException {
        Scanner cin = new Scanner(System.in);
        cin.nextLine();

        System.out.println("SENDING REQUEST");
        byte[] conteudo = "./src/TESTE.txt".getBytes();

        FSChunk f = new FSChunk(123,(byte)1,0,1,1,conteudo,conteudo.length);
        byte[] request = f.generateFSChunk();
        //para debug
        System.out.print(f.toString());
        try {
            this.socket1.send(new DatagramPacket(request, request.length, InetAddress.getByName("127.0.0.1"), 8888));
        }catch (PortUnreachableException e){ // caso de o servidor ter perdido a conexao ou fechado a conexao
            Map<Integer,InetAddress> server = new HashMap<>();
            server.put(8888,InetAddress.getByName("127.0.0.1"));
            int numeroTentativas = this.poolServidores.get(server);
            this.poolServidores.put(server,numeroTentativas++);
            removeServer(server);
            System.out.println("Nao foi possivel enviar o ficheiro");
        }

        while(true)
        {
            byte msg[] = new byte[1024];
            DatagramPacket payload = new DatagramPacket(msg,1024);

            try {
                System.out.println("Gateway listening...");
                this.socket1.receive(payload);
                System.out.println("Payload received");

                FSChunk f1 = new FSChunk();
                f1 = f1.degenerateFSChunk(payload.getData());
                System.out.println(f1.toString());
                if(!this.fchunksRecebidos.containsKey(f1.getSeqNumber())){
                    this.fchunksRecebidos.put(f1.getSeqNumber(),new ArrayList<>());
                    this.fchunksRecebidos.get(f1.getSeqNumber()).add(f1);
                }else{
                    this.fchunksRecebidos.get(f1.getSeqNumber()).add(f1);
                }

                byte type = f1.getType();
                switch (type){
                    case 1:
                        recolheChunks(f1);
                        break;
                    case 2:
                        recebeMetadados(f1.getSeqNumber(),InetAddress.getByName("127.0.0.1"),0); // nr Sequencia identifica o pedido + o ip e porta do cliente que fez o pedido
                        break;
                    case 3:
                        trataBeacon(payload.getPort(),payload.getAddress());
                        break;
                }

            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }
        }
    }

    public static void main(String[] args) throws IOException {
        Gateway gateway = new Gateway();
        gateway.start();
    }


}
