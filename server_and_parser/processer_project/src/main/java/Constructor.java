import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Constructor {

    private List<String> src;
    private HashMap<String, String> comm;
    private ArrayList<String> big_comm;
    private ArrayList<String> commands;
    private String last_command;

    Command_Type t;
    Duplicates d;

    public Constructor(){
        commands = new ArrayList<>();
        big_comm = new ArrayList<>();
        big_comm.add("MOTION");
        big_comm.add("TURN");
        t = new Command_Type();
        create_hash_map();
        d = new Duplicates(comm);

    }

    public void set_list(List<String> l){
        src = l;
        commands = new ArrayList<>();
    }

    private void create_hash_map(){
        comm = new HashMap<>();
        comm.put("COMMAND", null);
        comm.put("DIRECTION", null);
        comm.put("LENGTH_NUMBER", null);
        comm.put("ORIENTATION_NUMBER", null);
        comm.put("REPEAT_NUMBER", null);
        comm.put("ORIENTATION", null);
        comm.put("LENGTH", null);
        comm.put("REPEAT", null);
    }

    private void create(int start, int end, int loc){ //the big command for loc 1 = is at start , loc = 0 is at the end, loc = 2 is unknown
        String type;
        String formater;
        String command;

        comm.put("COMMAND", null);
        comm.put("DIRECTION", null);
        comm.put("LENGTH_NUMBER", null);
        comm.put("ORIENTATION_NUMBER", null);
        comm.put("REPEAT_NUMBER", null);
        comm.put("ORIENTATION", null);
        comm.put("LENGTH", null);
        comm.put("REPEAT", null);

        /*if(loc == 0){
            System.out.println("COMM is at the end");
        }else if(loc == 1){
            System.out.println("COMM is at the start");
        }else{
            System.out.println("COMM is at unknown pos");
        }*/

        if(loc == 1){
            type = t.get_command_type(src.get(start));
            if(type.equals("SEPARATOR")){
                /*if(big_comm.contains(src.get(start+1))){
                    type = t.get_command_type(src.get(start+1));
                    comm.put("COMMAND",type);
                } else{
                    comm.put("COMMAND",last_command);
                }*/
                comm.put("COMMAND",last_command);
            }else{
                comm.put("COMMAND",type);
            }
        }else if (loc == 0){
            type = t.get_command_type(src.get(end));
            comm.put("COMMAND",type);
        } else {
            for(int i = start ;i<=end;i++){
                type = t.get_command_type(src.get(i));
                if(big_comm.contains(type)){
                    comm.put("COMMAND",type);
                }
            }
        }
        for(int i = start;i<=end;i++){
            type = t.get_command_type(src.get(i));
            if(type.equals("DIRECTION")){
                if((src.get(i).equals("left") || src.get(i).equals("right"))){
                    if(comm.get("COMMAND").equals("MOTION")){
                        comm.put("COMMAND","TURN");
                    }
                }
                if((src.get(i).equals("forward") || src.get(i).equals("backward"))){
                    if(comm.get("COMMAND").equals("TURN")){
                        comm.put("COMMAND","MOTION");
                    }
                }
            }
            if(comm.containsKey(type)){
                comm.put(type,src.get(i));
            } else if(type.equals("NUMBER")){
                formater = t.get_command_type(src.get(i+1)) + "_" + type;
                if(comm.containsKey(formater)) {
                    comm.put(formater, src.get(i));
                }
            }

        }


        command = "";
        if(comm.get("REPEAT") != null){
            command +="(" + comm.get("REPEAT_NUMBER")+")";
        } else{
            command +="(1)";
        }
        command += comm.get("COMMAND") + "-";

        if(comm.get("DIRECTION") == null){
            if(comm.get("COMMAND").equals("TURN")){
                command +="dir:left ";
            } else if(comm.get("COMMAND").equals("MOTION")){
                command +="dir:forward ";
            }


        } else {
            command +="dir:" + comm.get("DIRECTION")+" ";
        }
        if(comm.get("ORIENTATION") != null){
            if(comm.get("COMMAND").equals("TURN")) {
                command += "ori:" + comm.get("ORIENTATION_NUMBER") + " ";
            }
        }
        if(comm.get("LENGTH") != null){
            if(comm.get("COMMAND").equals("MOTION")) {
                double num = Integer.parseInt(comm.get("LENGTH_NUMBER"));
                String len_unit = comm.get("LENGTH");
                if (len_unit.equals("centimeters") || len_unit.equals("cm") || len_unit.equals("centimeter")) {
                    num = num / 100;
                }
                if (len_unit.equals("millimeters") || len_unit.equals("mm") || len_unit.equals("millimeter")) {
                    num = num / 1000;
                }
                command += "len:" + num + " ";
            }
        }

        commands.add(command);

    }

    public ArrayList<String> construct(){
        int start = 0;
        int end = src.size()-1;
        int expecting_and = 0;
        int last_and_position = -1;
        int last_end_position = -1;
        int found = 0;
        int size_left = src.size();
        int[] filler_pos = new int[src.size()];
        boolean isDupe = false;

        String str_no_fillers = "";

        for(int i = 0;i<src.size();i++){
            String type = t.get_command_type(src.get(i));

            if(type.equals("DIRECTION") ) {
                src.set(i, t.is_syn(src.get(i)));
            }

            if(type.equals("FILLER")){
                filler_pos[i] = 1;
            }

            if(!t.replace_numbers(src.get(i)).equals("")){
                filler_pos[i] = 0;
                src.set(i,t.replace_numbers(src.get(i)));
            }
            /*if(src.get(i).equals("one")){
                filler_pos[i] = 0;
                src.set(i,"1");
            }
            if(src.get(i).equals("two")){
                filler_pos[i] = 0;
                src.set(i,"2");
            }*/

            if(src.get(i).equals("twice")){
                filler_pos[i] = 0;
                src.set(i,"2 times");
            }
        }

        for(int i = 0;i<src.size();i++){
            if(filler_pos[i] != 1){
                str_no_fillers += src.get(i) + " ";
            }
        }
        List<String> new_src = Arrays.asList(str_no_fillers.split(" "));
        src = new_src;

        for(int i = 0;i<src.size();i++){

            isDupe = false;
            String type = t.get_command_type(src.get(i));
            if(!type.equals("NUMBER") && ! type.equals("SEPARATOR")){
                if(type.equals("MOTION")){
                    d.check_dupes("ORIENTATION");
                } else
                if(type.equals("TURN")){
                    d.check_dupes("LENGTH");
                }else {
                    isDupe = d.check_dupes(type);
                }
            }

            if(isDupe && found == 1){
                if(i>0) {
                    if(t.get_command_type(src.get(i-1)).equals("NUMBER")) {
                        create(last_end_position+1, i-2, 2);
                        last_end_position = i-2;
                        size_left = size_left - (i - start - 3);
                        start = i-1;
                        found = 0;
                    }else{
                        create(last_end_position+1, i-1, 2);
                        last_end_position = i-1;
                        size_left = size_left - (i - start - 2);
                        start = i;
                        found = 0;
                    }
                    d.reset();
                }
            }

            if (big_comm.contains(type)) {
                if(found == 1){
                    create(start, i-1, 2);
                    last_end_position = i-1;
                    size_left = size_left - (i - start - 2);
                    d.reset();
                    start = i;
                } else {
                    found = 1;
                }
                /*if(start < i){
                    create(start, i, 0);
                    last_end_position = i-1;
                    size_left = size_left - (i - start - 1);
                    d.reset();
                    start = i+1;
                    found = 0;
                    expecting_and = 1;
                }else{
                    start = i;
                }*/
                last_command = type;
            }

            if(type.equals("SEPARATOR") && found == 1){
                create(start, i - 1, 2);
                last_end_position = i-1;
                d.reset();
                size_left = size_left - (i - start - 2);
                last_and_position = i;
                start = i+1;
                found = 0;
            } else
            if(type.equals("SEPARATOR") && found == 0){

                if(!t.get_command_type(src.get(i-1)).equals("SEPARATOR")) {
                    if (expecting_and == 1) {
                        last_and_position = i;
                        start = i + 1;
                    } else {
                        if (start < i) {
                            create(last_and_position, i - 1, 1);
                            last_end_position = i-1;
                            d.reset();
                            size_left = size_left - (i - last_and_position - 2);
                            last_and_position = i;
                            start = i;
                        }
                    }
                }
            }


           /* if(type.equals("SEPARATOR") ){
                create(start, i - 1, 1);
                if(big_comm.contains(t.get_command_type(src.get(i+1)))){
                    start = i+1;
                    i++;
                }else{
                    start = i;
                }
            }else {
                if (big_comm.contains(type) && found == 1) {
                    end = i;
                    create(start, end - 1, 1);
                    found = 0;
                    last_command = type;
                    start = i;
                    size_left = size_left - (end - start - 1);
                } else if (big_comm.contains(type)) {
                    found = 1;
                    last_command = type;
                }
            }*/
        }
        if(isDupe && found == 0){
            create(last_end_position+1, src.size()-1, 2);
            d.reset();
        }
        if(size_left > 0 && found == 1) {
            create(start, src.size() - 1, 2);
            d.reset();
        }
        if(size_left > 0 && found == 0) {
            if(last_and_position > -1) {
                create(last_and_position, src.size() - 1, 1);
                d.reset();
            }
        }
        return commands;
    }
}
