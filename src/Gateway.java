import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class Gateway {
    private Map<Long,ArrayList<FSChunk>> fchunksRecebidos;
    private static DatagramSocket socket1; // para os fastfileservers
    private static ServerSocket socket2; // para os clientes
    private static Map<Integer,DatagramPacket> datagramas; //pedidos dos clientes recebidos
    private Map<Integer,InetAddress> ips; // portas e ips dos clientes
    private static Map<Map.Entry<Integer,InetAddress>,Integer> poolServidores; // ip e porta do servidor e o numero de tentativas
    private static Long numerosSeq;


    public Gateway() throws IOException { //Depois temos de alterar as portas pq depende do qual o cliente e qual o fastfileserver
        this.socket1 = new DatagramSocket(8880, InetAddress.getByName("127.0.0.1"));// socket para o fastfile
        this.socket2 = new ServerSocket(); // socket para o cliente
        fchunksRecebidos = new ConcurrentHashMap<>();
        poolServidores = new ConcurrentHashMap<>();
        datagramas = new ConcurrentHashMap<>();
        this.ips = new HashMap<>();
        this.numerosSeq = Long.valueOf(1);
    }

    public void trataServer(int portaServer,InetAddress ip){
        Map.Entry<Integer,InetAddress> map = new AbstractMap.SimpleEntry<>(portaServer,ip);

        if(!poolServidores.containsKey(map)){
            poolServidores.put(map,0);
        }

    }

    public static void atualizaTempoServers(int milisegundos){
        for(Map.Entry<Integer,InetAddress> k : poolServidores.keySet()){
            int tempo = poolServidores.get(k) + milisegundos;
            poolServidores.replace(k,tempo);

    //        System.out.println("Tempos atualizados: "+k+" "+poolServidores.get(k)+"\n");
            }
    }

    public static void atualizaServerBeaconRecebido(Integer porta, InetAddress ip){
        Map.Entry<Integer,InetAddress> key = new AbstractMap.SimpleEntry<>(porta,ip);
        poolServidores.replace(new AbstractMap.SimpleEntry<>(porta,ip),0);
     //   System.out.println("Tempos atualizados do beacon recebido : "+porta+" "+poolServidores.get(key)+"\n");
    }


    public static void removeServers(){


        for(Map.Entry<Integer,InetAddress> k : poolServidores.keySet()){
            int tempo = poolServidores.get(k);
            if(tempo>= 10000){
                poolServidores.remove(k);
                System.out.println("Servidor retirado : "+k.toString()+"\n");

            }
        }
      //  System.out.println("Servidores que ficaram apos retirar:  "+poolServidores.keySet()+poolServidores.values()+"\n");
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

   //
        // System.out.println(new String(fileSize));
    for(int i = 0; i<fileSize.length;i++,pos++){ pacote[pos] = fileSize[i]; }
   // String fileSi = new String(pacote);System.out.println(fileSi);

    for (int i=0;i<mensagem.length;i++,pos++){ pacote[pos] = mensagem[i]; }


    DatagramPacket packet = new DatagramPacket(pacote,pacote.length,InetAddress.getByName("127.0.0.1"),8880);//ip e porta do cliente que fez o pedido que ainda nao sei qual e
    datagramas.put(8880,packet);
    System.out.println("Pacote do pedido " + sqn + ": "+new String(pacote));

    }


    public void recolheChunks(FSChunk f) throws IOException { // mas e se perder um pacote !???

        int i=1, pos=0;
        boolean fim= false;
        byte[] mensagem = new byte[Math.toIntExact(f.getFileSize())];
        ArrayList<FSChunk> fs = this.fchunksRecebidos.get(f.getSeqNumber());

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
        DatagramPacket pacote = new DatagramPacket(mensagem,mensagem.length,InetAddress.getByName("127.0.0.1"),8880);
        datagramas.put(8880,pacote);

    }

    public static Integer escolheServer(){
        for(int i =0;i<poolServidores.size();i++){
            return poolServidores.get(i);
        };
        return 0;
    }

    public static void servers() {
        int timeElapsed =0;

        while (true) {
            Instant start = Instant.now();
            try {
                Thread.sleep(1000);
            }catch (InterruptedException e){}
            atualizaTempoServers(timeElapsed); // atualiza os tempos aos servidores
            removeServers();
            Instant end = Instant.now();
            timeElapsed = (int) Duration.between(start, end).toMillis();
        }
    }
    public static void cliente() {

        Socket cliente = null;
        try {
            cliente = socket2.accept();
            BufferedReader in = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
            String pedido = in.readLine();
            String filename = pedido.split(" ")[1].substring(1);
            byte[] conteudo = filename.getBytes();
            FSChunk f = new FSChunk(numerosSeq++, (byte) 1, 0, 1, 1, conteudo, conteudo.length);
            byte[] request = f.generateFSChunk();
            int porta = escolheServer();
            socket1.send(new DatagramPacket(request, request.length, InetAddress.getByName("127.0.0.1"), porta));


        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public static void enviaCliente(){
        for(DatagramPacket p : datagramas.values()){
            //nao sei o que fazer mais
        }
    }

    public void start() throws IOException {
        Scanner cin = new Scanner(System.in);
        cin.nextLine();

        new Thread(Gateway::servers).start(); // atualiza tempo
        new Thread(Gateway::cliente).start();
        new Thread(Gateway::enviaCliente).start();

   /*     System.out.println("SENDING REQUEST");
        byte[] conteudo = "./src/TESTE.txt".getBytes();
        FSChunk f = new FSChunk(numerosSeq++, (byte) 1, 0, 1, 1, conteudo, conteudo.length);
        FSChunk f2= new FSChunk(numerosSeq++, (byte) 2, 0, 1, 1, conteudo, conteudo.length);
        FSChunk f3= new FSChunk(numerosSeq++, (byte) 2, 0, 1, 1, conteudo, conteudo.length);

        byte[] request = f.generateFSChunk();
        byte[] request2 = f2.generateFSChunk();
        byte[] request3 = f3.generateFSChunk();

        socket1.send(new DatagramPacket(request, request.length, InetAddress.getByName("127.0.0.1"), 8888));
        socket1.send(new DatagramPacket(request2, request2.length, InetAddress.getByName("127.0.0.1"), 8888));
        socket1.send(new DatagramPacket(request3, request3.length, InetAddress.getByName("127.0.0.1"), 8889));
       */
        while(true)
        {
            byte msgem[] = new byte[1024];
            DatagramPacket payload = new DatagramPacket(msgem,1024);

            try {
              //  System.out.println("Gateway listening...");
                this.socket1.setSoTimeout(10000);
                this.socket1.receive(payload);
              //  System.out.println("Payload server received");

                trataServer(payload.getPort(),payload.getAddress());

                FSChunk f1 = new FSChunk();
                f1 = f1.degenerateFSChunk(payload.getData());

                //adiciona resposta do fastfileserver no map de fschunks recebidos
                if(!this.fchunksRecebidos.containsKey(f1.getSeqNumber())&&f1.getType()!=3){
                    this.fchunksRecebidos.put(f1.getSeqNumber(),new ArrayList<>());
                    this.fchunksRecebidos.get(f1.getSeqNumber()).add(f1);
                }else if (f1.getType()!=3){
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
                        System.out.println("RECEBI BEACON " + payload.getPort() + " VOU ATUALIZAR");
                        atualizaServerBeaconRecebido(payload.getPort(),payload.getAddress());
                        break;
                }

            } catch (IOException e) {
                System.out.println("ENTREI NO CATCH");
                System.out.println(poolServidores.entrySet());
                System.out.println("Todos os FSCHUNKS: "+this.fchunksRecebidos.values());
            }

        }
    }



    public static void main(String[] args) throws IOException {
        Gateway gateway = new Gateway();
        gateway.start();
    }


}
