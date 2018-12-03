import com.sun.org.apache.xml.internal.security.algorithms.implementations.IntegrityHmac;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Command_Type {
    private HashMap<String, Integer> words;
    private HashMap<Integer, String> types;
    private HashMap<String, Integer> synonyms;
    private HashMap<String,String> numbers;


    private void create_synonyms(){
        String[] direction_syn_forward = {"forward","forwards","ahead","onward","forth", "leading"};
        String[] direction_syn_backward = {"backward","backwards","back","rear", "behind", "reverse"};
        String[] length_syn = {"unit","units","centimeters","centimeter", "millimeters","millimeter", "steps","step", "cm", "mm"};

        for(int i =1;i<=7;i++){
            if(i==4) {
                for (int j = 0; j < direction_syn_forward.length; j++) {
                    synonyms.put(direction_syn_forward[j],4);
                }
            }
            if(i==5) {
                for (int j = 0; j < direction_syn_backward.length; j++) {
                    synonyms.put(direction_syn_backward[j],5);
                }
            }
            if(i==7) {
                for (int j = 0; j < length_syn.length; j++) {
                    synonyms.put(length_syn[j],7);
                }
            }
        }
    }

    private void initialize_numbers(){
        numbers.put("zero","0");
        numbers.put("one","1");
        numbers.put("two","2");
        numbers.put("three","3");
        numbers.put("four","4");
        numbers.put("five","5");
        numbers.put("six","6");
        numbers.put("seven","7");
        numbers.put("eight","8");
        numbers.put("nine","9");
        numbers.put("ten","10");

    }

    public String replace_numbers(String s){
        if(numbers.get(s) != null){
            return numbers.get(s);
        }
        return "";
    }

    public Command_Type(){

        words = new HashMap<>();
        types = new HashMap<>();
        synonyms = new HashMap<>();
        numbers = new HashMap<>();
        initialize_numbers();

        types.put(1,"MOTION");
        //types.put(2,"SEARCH");
        types.put(3,"TURN");
        types.put(12,"DIRECTION");
        types.put(4,"DIRECTION_FORWARD");
        types.put(5,"DIRECTION_BACKWARD");
        types.put(6,"ORIENTATION");
        types.put(7,"LENGTH");
        types.put(8,"NUMBER");
        types.put(9,"FILLER");
        types.put(10,"SEPARATOR");
        types.put(13,"REPEAT");


        create_synonyms();

        words.put("go",1);
        words.put("move",1);
        words.put("turn",3);
        words.put("make",3);
        words.put("find",2);
        words.put("get",2);
        words.put("num",8);
        words.put("x",9);
        words.put("and",10);
        words.put("after",10);
        words.put("then",10);
        words.put("meters",7);
        words.put("meter",7);
        words.put("degrees",6);
        words.put("degree",6);
        words.put("left",12);
        words.put("right",12);
        words.put("times",13);
        words.put("time",13);


    }


    public String is_syn(String s){
        if(synonyms.get(s) != null) {
            if (synonyms.get(s) == 4) {
                return "forward";
            }
            if (synonyms.get(s) == 5) {
                return "backward";
            }
        }
        return s;
    }

    public String get_command_type(String s){
        int def = 0;
        int num;
        try {
            num = Integer.parseInt(s);
        } catch (NumberFormatException e){
            num = -1;

        }
        if(num != -1){
            s = "num";
        }
        if(words.get(s) == null){
            if(synonyms.get(s) == null) {
                return types.get(words.get("x"));
            } else{
                String str = "DIRECTION_";
                if(types.get(synonyms.get(s)).equals(str+"TURN") || types.get(synonyms.get(s)).equals(str+"BACKWARD") || types.get(synonyms.get(s)).equals(str+"FORWARD")){
                    return types.get(12);
                }
                return types.get(synonyms.get(s));
            }
        }
        return types.get(words.get(s));

    }

}
