import java.nio.ByteBuffer;
import java.util.Arrays;


public class FSChunk {
    private long seqNumber;
    private byte type;
    private int dataSize;
    private long fileSize;
    private int nrChunks;
    private int nrChunk;
    private byte[] data;


    public FSChunk(){
        this.seqNumber=0;
        this.type = 0;
        this.fileSize =0;
        this.nrChunks=0;
        this.nrChunk=0;
        this.data = new byte[995];
        this.dataSize = 0;
    }

    public FSChunk(long sqn, byte t,long fileS, int nCs, int nC, byte[] data, int dS){
        this.seqNumber = sqn;
        this.type = t;
        this.fileSize = fileS;
        this.nrChunks = nCs;
        this.nrChunk = nC;
        this.data = data;
        this.dataSize = dS;
    }

    public FSChunk(FSChunk fsc){
        this.seqNumber=fsc.getSeqNumber();
        this.type = fsc.getType();
        this.fileSize=fsc.getFileSize();
        this.nrChunks= fsc.getNrChunks();
        this.nrChunk = fsc.getNrChunk();
        this.data = fsc.getData();
        this.dataSize = fsc.getDataSize();
    }

    public int getDataSize() { return dataSize; }

    public long getSeqNumber() {
        return seqNumber;
    }

    public byte getType() {
        return type;
    }

    public long getFileSize() {
        return fileSize;
    }

    public int getNrChunks() {
        return nrChunks;
    }

    public int getNrChunk() {
        return nrChunk;
    }

    public byte[] getData() {

        byte[] dat = new byte[dataSize];
        int i = 0;

        for(byte b : this.data){
            dat[i] = b;
            i++;
        }
        return dat;
    }

    public void setSeqNumber(int seqNumber) {
        this.seqNumber = seqNumber;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public void setDataSize(int dataSize) { this.dataSize = dataSize; }

    // empacuta dados para enviar para o gateaway
    // ali aqui ja recebe os chunks e trataos como datagramas normais
    // a divisao em chunks e feito no FFSSender

    public byte[] generateFSChunk() {
        byte[] seqNumbeer= ByteBuffer.allocate(8).putLong(this.getSeqNumber()).array();
        byte typee = this.getType();
        byte[] fileSizee = ByteBuffer.allocate(8).putLong(this.fileSize).array();
        byte[] numerooChunks = ByteBuffer.allocate(4).putInt(this.nrChunks).array();
        byte[] numerooChunk = ByteBuffer.allocate(4).putInt(this.nrChunk).array();
        byte[] dataSize = ByteBuffer.allocate(4).putInt(this.dataSize).array();
        byte[] datagrama = new byte[29];
        int pos = 0;

        for(int i= 0; i<seqNumbeer.length;i++,pos++){
            datagrama[pos]= seqNumbeer[i];
        }
        datagrama[pos]=typee;
        pos++;

        for(int i = 0; i<fileSizee.length;i++,pos++){
            datagrama[pos] = fileSizee[i];
        }

        for(int i = 0; i<numerooChunks.length;i++,pos++){
            datagrama[pos] = numerooChunks[i];
        }

        for(int i = 0; i<numerooChunk.length;i++,pos++){
            datagrama[pos] = numerooChunk[i];
        }
        for (int i = 0; i<dataSize.length;i++,pos++){
            datagrama[pos] = dataSize[i];
        }

//byteoutputstream


        byte[] datagramafinal = new byte[datagrama.length + data.length];

        System.arraycopy(datagrama,0,datagramafinal,0,datagrama.length);
        System.arraycopy(this.data,0,datagramafinal,29,this.dataSize);

        return  datagramafinal;

    }


    // receber um pacote do gateaway e desempacuta os dados
    public FSChunk degenerateFSChunk(byte[] data){

        long seqNumber = ByteBuffer.wrap(data,0,8).getLong();
        byte type = data[8];
        long fileSize = ByteBuffer.wrap(data,9,8).getLong();
        int numeroChunks = ByteBuffer.wrap(data,17,4).getInt();
        int numeroChunk = ByteBuffer.wrap(data,21,4).getInt();
        int dataS = ByteBuffer.wrap(data,25,4).getInt();
        byte[] dados = Arrays.copyOfRange(data,29,29+dataS);

        FSChunk datagrama = new FSChunk(seqNumber,type,fileSize,numeroChunks,numeroChunk,dados,dataS);

        return datagrama;
    }


public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("Nr. Sequencia: ").append(seqNumber).append("\n");
        sb.append("Type: ").append(type).append("\n");
        sb.append("File Size: ").append(fileSize).append("\n");
        sb.append("Nr. Chunks: ").append(nrChunks).append("\n");
        sb.append("Nr. Chunk: ").append(nrChunk).append("\n");
        sb.append("Tamanho Mensagem: ").append(dataSize).append("\n");
        sb.append("Data: ").append(new String(data)).append("\n");

        return sb.toString();
}

public static void main(String[] args) {
        String s = "Ola a todos o meu nome e Sofia";
        byte[] data = s.getBytes();
        FSChunk f = new FSChunk(123, (byte) 1,12345,3,1,data,data.length);
        System.out.println(f.toString());
        byte[] datagrama = f.generateFSChunk();

        FSChunk fs = f.degenerateFSChunk(datagrama);
        System.out.println(fs.toString());
}


}
