import javax.sound.sampled.Port;
import javax.swing.plaf.synth.SynthOptionPaneUI;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class Gateway {
    private Map<Long,ArrayList<FSChunk>> fchunksRecebidos;
    private DatagramSocket socket1; // para os fastfileservers
    private DatagramSocket socket2; // para os clientes
    private ArrayList<DatagramPacket> datagramas; //pedidos dos clientes recebidos
    private Map<Integer,InetAddress> ips; // portas e ips dos clientes
    private static Map<Map<Integer,InetAddress>,Long> poolServidores; // ip e porta do servidor e o numero de tentativas
    private Long numerosSeq;

    //receber pedido do cliente -- dado numero de sequencia
    //encapsular pedido do cliente para enviar ao fastfileserver -- ter em conta os tipos de pedido ???
    //enviar resultado para o cliente -- para o tal numero de sequencia


    public Gateway() throws UnknownHostException, SocketException { //Depois temos de alterar as portas pq depende do qual o cliente e qual o fastfileserver
        this.socket1 = new DatagramSocket(8880, InetAddress.getByName("127.0.0.1"));// socket para o fastfile
        this.socket2 = new DatagramSocket(8884,InetAddress.getByName("127.0.0.1")); // socket para o cliente
        this.fchunksRecebidos = new HashMap<>();
        poolServidores = new HashMap<>();
        this.datagramas = new ArrayList<>();
        this.ips = new HashMap<>();
        this.numerosSeq = Long.valueOf(0);
    }


    public static void removeServers(long x){
        Map<Map<Integer,InetAddress>,Long> m = poolServidores;

        for(Map<Integer,InetAddress> k : m.keySet()){
            for(Long t : m.values()){
                if(t>= x){
                    System.out.println(" Servidor retirado : "+m.toString()+"\n");
                    m.remove(k);

                }
            }
        }
        System.out.println("Servidores que ficaram apos retirar:  "+poolServidores.keySet()+poolServidores.values()+"\n");
    }


    public void recebeMetadados(long sqn,InetAddress ipPedido, int portaPedido) throws IOException {
        int pos = 0;
    // neste caso apenas tratamos de um unico chunk pois a resposta so envolve o envio do tamanho e nome do ficheiro
    ArrayList<FSChunk> pedido = this.fchunksRecebidos.get(sqn);
        byte[] mensagem = null;
        byte[] fileSize = null;


    for(FSChunk f : pedido){ // so tem 1 logo nao tem problema fazer isto no for MAS E MELHOR VER OUTRA SOLUCAO DEPOIS
        mensagem = f.getData(); // tem o nome do ficheiro
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(f.getFileSize());
        fileSize = buffer.array();
        //fileSize = ByteBuffer.allocate(8).putLong(f.getFileSize()).array(); // tem tamanho do file
        System.out.println("Este e o pacote dos metadados: "+f.toString() + f.getFileSize());
    }
    byte[] pacote = new byte[fileSize.length+mensagem.length];

    System.out.println(new String(fileSize));
    for(int i = 0; i<fileSize.length;i++,pos++){ pacote[pos] = fileSize[i]; }
   // String fileSi = new String(pacote);System.out.println(fileSi);

    for (int i=0;i<mensagem.length;i++,pos++){ pacote[pos] = mensagem[i]; }


    DatagramPacket packet = new DatagramPacket(pacote,pacote.length,ipPedido,portaPedido);//ip e porta do cliente que fez o pedido que ainda nao sei qual e
    //socket2.send(packet);
    System.out.println("Pacote do pedido " + sqn + ": "+new String(pacote));

    }

    public void trataServer(int portaServer,InetAddress ip){

        Map<Integer,InetAddress> map = new HashMap<>();
        map.put(portaServer,ip);
        if(!poolServidores.containsKey(map)){
            poolServidores.put(map,(long)0);
        }


        for(Map<Integer,InetAddress> mp: poolServidores.keySet()){
            System.out.println(mp.toString()+" "+poolServidores.get(mp));
           // System.out.println("Tempo : "+ poolServidores.get(mp)+ "\n");
            }


    }

    public void atualizaTempoServers(Long milisegundos){
        for(Map<Integer,InetAddress> k : poolServidores.keySet()){
            for(Long tempo : poolServidores.values() ){
                Long novoTempo = tempo+milisegundos;
                poolServidores.replace(k,novoTempo);

           System.out.println("Tempos atualizados: "+poolServidores.get(k)+"\n");
            }

        }


    }

    public void atualizaServerBeaconRecebido(Integer porta, InetAddress ip){
        Map<Integer,InetAddress> key = new HashMap<>();
        key.put(porta,ip);
        poolServidores.replace(key,(long) 0);
       System.out.println("Tempos atualizados do beacon recebido : "+porta+" "+poolServidores.get(key)+"\n");
    }


    public void recolheChunks(FSChunk f) throws IOException { // mas e se perder um pacote !???

        int i=1, pos=0;
        boolean fim= false;
        byte[] mensagem = new byte[Math.toIntExact(f.getFileSize())];
        ArrayList<FSChunk> fs = this.fchunksRecebidos.get(f.getSeqNumber());
        //DEBUG
       // System.out.println("Tamanho do array: "+fs.size());

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
            //debug
            fim = true;
        }
        //DEBUG
        if(fim) {
            String mensa = new String(mensagem);
            System.out.println("\nPedido: "+ f.getSeqNumber()+ "\nTamanho da mensagem a enviar: \n" + mensagem.length + "\nTamanho da mensagem enviada: \n" + f.getFileSize() + " \nESTA É A MENSAGEM A ENVIAR AO CLIENTE: " + mensa);
        }
        //depois temos de mudar para o ip e porta do respetivo cliente
        // podemos procurar na lista de pedidos aquele cujo numero sequencia e igual e obtemos o ip e a porta
        DatagramPacket pacote = new DatagramPacket(mensagem,mensagem.length,InetAddress.getByName("127.0.0.1"),8882);
        //socket2.send(pacote);
    }


    public void start() throws IOException {
        Scanner cin = new Scanner(System.in);
        cin.nextLine();


        System.out.println("SENDING REQUEST");


        byte[] conteudo = "./src/TESTE.txt".getBytes();
        FSChunk f = new FSChunk(123, (byte) 1, 0, 1, 1, conteudo, conteudo.length);
        FSChunk f2 = new FSChunk(124, (byte) 2, 0, 1, 1, conteudo, conteudo.length);
        FSChunk f3 = new FSChunk(125, (byte) 3, 0, 1, 1, conteudo, conteudo.length);
        byte[] request = f.generateFSChunk();
        byte[] request2 = f2.generateFSChunk();
        byte[] request3 = f3.generateFSChunk();

        //this.socket1.send(new DatagramPacket(request, request.length, InetAddress.getByName("127.0.0.1"), 8888));
        //this.socket1.send(new DatagramPacket(request2, request2.length, InetAddress.getByName("127.0.0.1"), 8889));
        //this.socket1.send(new DatagramPacket(request3, request3.length, InetAddress.getByName("127.0.0.1"), 8888));


        while(true)
        {
            byte msgem[] = new byte[1024];
            DatagramPacket payload = new DatagramPacket(msgem,1024);
            byte msg[] = new byte[1024];
            DatagramPacket mensacliente = new DatagramPacket(msg, 1024);
            Instant start = Instant.now();

            try {

                System.out.println("Gateway listening...");
                this.socket1.setSoTimeout(10000);//juntar + um 0
                this.socket1.receive(payload);

                System.out.println("Payload server received");

                trataServer(payload.getPort(),payload.getAddress()); //adiciona server caso ele nao esteja ja na pool de servidores

                FSChunk f1 = new FSChunk();
                f1 = f1.degenerateFSChunk(payload.getData());
               // System.out.println(f1.toString());
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
                        // se recebeu um beacon vai a esse servidor que o mandou dar reset no tempo (= 0)
                        //atualizaServerBeaconRecebido(payload.getPort(),payload.getAddress());
                        // vai tratar de retirar da pool de servidores todos os servidores cujo tempo e superior que x ou seja inativos
                        break;
                }


            } catch (IOException e) { e.printStackTrace(); }

            Instant end = Instant.now();
            Duration timeElapsed = Duration.between(start,end);

            atualizaTempoServers(timeElapsed.toMillis()); // atualiza os tempos aos servidores
            atualizaServerBeaconRecebido(payload.getPort(),payload.getAddress());

            GatewayThread thread = new GatewayThread(payload.getPort(),payload.getAddress());
            thread.start();
        }
    }

    public static void main(String[] args) throws IOException {
        Gateway gateway = new Gateway();
        gateway.start();
    }


}
