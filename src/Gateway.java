import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Gateway {
    private static Map<Long,ArrayList<FSChunk>> fchunksRecebidos;
    private static Map<Long,Integer> controloChunks;
    private static DatagramSocket socket1; // para os fastfileservers
    private static ServerSocket socket2; // para os clientes
    private static Map<Long,Socket> clientes; // portas e ips dos clientes
    private static Map<Long,byte[]> pedidosCliente;
    private static Map<Map.Entry<Integer,InetAddress>,Integer> poolServidores; // ip e porta do servidor e o numero de tentativas
    private static Long numerosSeq;


    public Gateway() throws IOException { //Depois temos de alterar as portas pq depende do qual o cliente e qual o fastfileserver
        socket1 = new DatagramSocket(8880, InetAddress.getByName("127.0.0.1"));// socket para o fastfile
        socket2 = new ServerSocket(8880); // socket para o cliente
        controloChunks = new ConcurrentHashMap<>();
        fchunksRecebidos = new ConcurrentHashMap<>();
        poolServidores = new ConcurrentHashMap<>();
        clientes = new ConcurrentHashMap<>();
        numerosSeq = 1L;
        pedidosCliente = new ConcurrentHashMap<>();
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
            }
    }

    public static void atualizaTempoChunks(int milisegundos){
        for(Long i : controloChunks.keySet()){
            int tempo = controloChunks.get(i) + milisegundos;
            controloChunks.replace(i,tempo);
        }
    }

    public static void atualizaServerBeaconRecebido(Integer porta, InetAddress ip){
        poolServidores.replace(new AbstractMap.SimpleEntry<>(porta,ip),0);
    }


    public static void removeServers(){

        for(Map.Entry<Integer,InetAddress> k : poolServidores.keySet()){
            int tempo = poolServidores.get(k);
            if(tempo>= 10000){
                poolServidores.remove(k);
                System.out.println("Servidor retirado : "+k.toString()+"\n");
            }
        }
    }

    public static void novopedidoRetransmicao() throws IOException {
        FSChunk f;
        for (Long sqn : controloChunks.keySet()) {
            int tempo = controloChunks.get(sqn);
            if (tempo >= 1000) {
                System.out.println(controloChunks.entrySet());
                System.out.println("VOU FAZER NOVO PEDIDO DE RETRANSMISSAO: " + sqn + tempo);
                byte[] conteudo = pedidosCliente.get(sqn);
                f = new FSChunk(sqn, (byte) 1, 0, 1, 1, conteudo, conteudo.length);
                byte[] request = f.generateFSChunk();
                int porta = escolheServer();
                controloChunks.replace(f.getSeqNumber(),0);
                System.out.println(controloChunks.entrySet());
                fchunksRecebidos.remove(sqn);
                socket1.send(new DatagramPacket(request, request.length, InetAddress.getByName("127.0.0.1"), porta));
            }
        }
    }

    public void recebeMetadados(long sqn) throws IOException {
        int pos = 0;
        ArrayList<FSChunk> pedido = this.fchunksRecebidos.get(sqn);
        byte[] mensagem = null;
        byte[] fileSize = null;


    for(FSChunk f : pedido){ // so tem 1 logo nao tem problema fazer isto no for MAS E MELHOR VER OUTRA SOLUCAO DEPOIS
        mensagem = f.getData(); // tem o nome do ficheiro
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(f.getFileSize());
        fileSize = buffer.array();
        System.out.println("Este e o pacote dos metadados: "+f.toString() + f.getFileSize());
    }
    byte[] pacote = new byte[fileSize.length+mensagem.length];

    for(int i = 0; i<fileSize.length;i++,pos++){ pacote[pos] = fileSize[i]; }

    for (int i=0;i<mensagem.length;i++,pos++){ pacote[pos] = mensagem[i]; }

    System.out.println("Pacote do pedido " + sqn + ": "+new String(pacote));

    }

    public static void chunks()  {
        int timeElapsed = 0;
        while (true) {
            try {
                Instant start = Instant.now();
                atualizaTempoChunks(timeElapsed);
                novopedidoRetransmicao();
                Instant end = Instant.now();
                timeElapsed = (int) Duration.between(start, end).toMillis();
            }catch (IOException e){
                System.out.println("Catch na funcao chunks");
            }
        }
    }

    public void recolheChunks(FSChunk f) throws IOException { // mas e se perder um pacote !???

        ArrayList<FSChunk> fs = this.fchunksRecebidos.get(f.getSeqNumber());
        ByteArrayOutputStream mensagem = new ByteArrayOutputStream();
        int nBytes = 0;

        for(FSChunk c : fs) {mensagem.write(c.getData());}

        System.out.println("RECEBI TUDO VOU ENVIAR");
        controloChunks.remove(f.getSeqNumber());
        if(clientes.containsKey(f.getSeqNumber())){
            System.out.println("Existe");
            Socket cli=  clientes.get(f.getSeqNumber());

            OutputStream out = null;

            System.out.println(cli.isConnected());
            out = cli.getOutputStream();
            System.out.println(cli.isConnected());

            out.write(mensagem.toByteArray());
            out.flush();
            cli.close();
        }

    }

    public static Integer escolheServer(){
        int min = 20000;
        int porta = 0;

        for(Map.Entry<Integer,InetAddress> k : poolServidores.keySet()){
            int tempo = poolServidores.get(k);
            if(tempo<min){
                min=tempo;
                porta = k.getKey();
            }
        }
        return porta;
    }

    public static void servers() {
        int timeElapsed =0;

        while (true) {
            Instant start = Instant.now();
            try {
                Thread.sleep(1000);
            }catch (InterruptedException e){
                System.out.println("Catch funcao servers");
            }
            atualizaTempoServers(timeElapsed); // atualiza os tempos aos servidores
            removeServers();
            Instant end = Instant.now();
            timeElapsed = (int) Duration.between(start, end).toMillis();
        }
    }

    public static void cliente() {
        Socket cliente;
        while(true){
        try {
            cliente = socket2.accept();
            BufferedReader in = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
            String pedido = in.readLine();
            String filename = pedido.split(" ")[1].substring(1);

            byte[] conteudo = filename.getBytes();
            long ns = numerosSeq;
            FSChunk f = new FSChunk(ns, (byte) 1, 0, 1, 1, conteudo, conteudo.length);
            clientes.put(ns, cliente);
            pedidosCliente.put(ns,conteudo);
            System.out.println("Numero sequencia:" + numerosSeq);
            numerosSeq++;
            byte[] request = f.generateFSChunk();
            int porta = escolheServer();
            socket1.send(new DatagramPacket(request, request.length, InetAddress.getByName("127.0.0.1"), porta));

        } catch (IOException e) {
            System.out.println("Catch cliente");
        }
   }
}


    public void start() throws IOException {
        Scanner cin = new Scanner(System.in);
        cin.nextLine();


        new Thread(Gateway::servers).start();
        new Thread(Gateway::cliente).start();
        new Thread(Gateway::chunks).start();


        while(true)
        {
            byte[] msgem= new byte[1024];
            DatagramPacket payload = new DatagramPacket(msgem,1024);

            try {
              //  System.out.println("Gateway listening...");
                socket1.setSoTimeout(10000);
                socket1.receive(payload);

                trataServer(payload.getPort(),payload.getAddress());

                FSChunk f1 = new FSChunk();
                f1 = f1.degenerateFSChunk(payload.getData());

                //adiciona resposta do fastfileserver no map de fschunks recebidos
                if(!fchunksRecebidos.containsKey(f1.getSeqNumber())&&f1.getType()!=3){
                    fchunksRecebidos.put(f1.getSeqNumber(),new ArrayList<>());
                    fchunksRecebidos.get(f1.getSeqNumber()).add(f1);
                    controloChunks.put(f1.getSeqNumber(),0);
                }else if (f1.getType()!=3){
                    fchunksRecebidos.get(f1.getSeqNumber()).add(f1);
                }

                byte type = f1.getType();
                switch (type){
                    case 1:
                        controloChunks.replace(f1.getSeqNumber(),0);
                        if(this.fchunksRecebidos.get(f1.getSeqNumber()).size()==f1.getNrChunks()) { recolheChunks(f1);}
                        break;
                    case 2:
                        recebeMetadados(f1.getSeqNumber()); // nr Sequencia identifica o pedido + o ip e porta do cliente que fez o pedido
                        break;
                    case 3:
                        System.out.println("RECEBI BEACON " + payload.getPort() + " VOU ATUALIZAR");
                        atualizaServerBeaconRecebido(payload.getPort(),payload.getAddress());
                        break;
                }

            } catch (IOException e) {
                System.out.println("ENTREI NO CATCH");
                System.out.println("Entry set catch:" +poolServidores.entrySet());

            }

        }
    }
    public static void main(String[] args) throws IOException {
        Gateway gateway = new Gateway();
        gateway.start();

    }

}
