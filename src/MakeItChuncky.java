import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class MakeItChuncky {
    private String filename;
    private int tamanhochunk;
    private int tamanhoMens;
    private Map<Integer,String> chunks;


    public MakeItChuncky(){
        this.filename= new String();
        this.tamanhochunk=0;
        this.tamanhoMens=0;
        this.chunks=new HashMap<>();
    }

    public MakeItChuncky(String file,int tamC, int tamM){
        this.filename = file;
        this.tamanhochunk=tamC;
        this.tamanhoMens=tamM;
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

    public int getTamanhoMens() {
        return tamanhoMens;
    }

    public Map<Integer, String> getChunks() {
        Map<Integer,String> map = new HashMap<>();
        for(Map.Entry<Integer,String> entry : this.chunks.entrySet()){
            map.put(entry.getKey(),entry.getValue());
        }
        return map;
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

    public void setChunks(Map<Integer, String> chunks) {
        this.chunks = new HashMap<>();
        chunks.entrySet().forEach(c -> this.chunks.put(c.getKey(),c.getValue()));
    }

    public MakeItChuncky clone(){ return  new MakeItChuncky(this);}


    public void divideFicheiro() throws FileNotFoundException {

        try {
            Scanner in = new Scanner(new FileReader(this.filename));
            int i = 0, ind = 1;
            String line = "";
            while (in.hasNextLine()) {
                line= line + in.nextLine()+"\n";
            }
            line = line.substring(0,line.length()-1);

            while (i < line.length()) {
                this.chunks.put(ind, line.substring(i, Math.min(i + tamanhochunk, line.length())));
                i += tamanhochunk;
                ind++;
            }

        }catch (FileNotFoundException e){
            System.out.println("Nao foi possivel aceder ao ficheiro!");
        }
    }

    public static void main(String[] args) throws FileNotFoundException {
        MakeItChuncky m;
        m = new MakeItChuncky("./src/teste.txt",10,10);
        m.divideFicheiro();
        Map<Integer,String> map = m.getChunks();
        map.entrySet().forEach(c-> System.out.println(c.toString()));
    }


}
