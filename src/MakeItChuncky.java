import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class MakeItChuncky {
    private String filename;
    private int tamanhochunk;
    private long tamanhoMens;
    private Map<Integer,byte[]> chunks;


    public MakeItChuncky(){
        this.filename= new String();
        this.tamanhochunk=0;
        this.tamanhoMens=0;
        this.chunks=new HashMap<>();
    }

    public MakeItChuncky(String file,int tamC){
        this.filename = file;
        this.tamanhochunk=tamC;
        this.chunks=new HashMap<>();
    }

    public MakeItChuncky(MakeItChuncky m){
        this.filename = m.getFilename();
        this.tamanhochunk = m.getTamanhochunk();
        this.tamanhoMens = m.getTamanhoMens();
        this.chunks = m.getChunks();
    }

    public String getFilename() {
        return filename;
    }

    public int getTamanhochunk() {
        return tamanhochunk;
    }

    public long getTamanhoMens() {
        return tamanhoMens;
    }

    public Map<Integer, byte[]> getChunks() {
        Map<Integer,byte[]> map = new HashMap<>();
        for(Map.Entry<Integer,byte[]> entry : this.chunks.entrySet()){
            map.put(entry.getKey(),entry.getValue());
        }
        return map;
    }

    public byte[] getChunk(int i) {
        byte[] res = this.chunks.get(i);
        System.out.println("GET CHUNK: "+ i + "\n");
        return res;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setTamanhochunk(int tamanhochunk) {
        this.tamanhochunk = tamanhochunk;
    }

    public void setTamanhoMens(int tamanhoMens) {
        this.tamanhoMens = tamanhoMens;
    }

    public void setChunks(Map<Integer, byte[]> chunks) {
        this.chunks = new HashMap<>();
        chunks.entrySet().forEach(c -> this.chunks.put(c.getKey(),c.getValue()));
    }

    public MakeItChuncky clone(){ return  new MakeItChuncky(this);}


    public int divideFicheiro(){
        try {
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(new File(filename)));
            int i = 0;
            byte[] lines = in.readAllBytes();
            int numeroChunks = (int) Math.ceil( (double) lines.length/tamanhochunk);
            while(i<numeroChunks){
                int inicio = i*tamanhochunk;
                int fim = Math.min(inicio + tamanhochunk,lines.length);
                byte[] line = Arrays.copyOfRange(lines,inicio,fim);
                this.chunks.put(i,line);
                i++;

            }
            return numeroChunks;
        }catch ( IOException e){
            System.out.println("Nao foi possivel aceder ao ficheiro!");
        }
        return 0;
    }


}
