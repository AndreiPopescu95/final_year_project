import java.util.HashMap;
import java.util.Map;

public class Duplicates {

    private HashMap<String, String> comm;
    private HashMap<String, Integer> comm_freq;

    public Duplicates(HashMap<String, String> comm){
        this.comm = comm;
        comm_freq = new HashMap<>();
        for(Map.Entry<String, String> entry : comm.entrySet()) {
            String key = entry.getKey();
            comm_freq.put(key,0);
        }
    }

    public void reset(){
        for(Map.Entry<String, Integer> entry : comm_freq.entrySet()) {
            String key = entry.getKey();
            if(entry.getValue() == 1) {
                comm_freq.put(key, 0);
            } else {

                comm_freq.put(key, 1);
            }
        }
    }

    public boolean check_dupes(String key){
        int count = comm_freq.get(key);
        if(count == 0){
            comm_freq.put(key,count+1);
            return false;
        }else{
            comm_freq.put(key,count+1);
            return true;
        }
    }
}
