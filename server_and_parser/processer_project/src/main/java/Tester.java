import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Tester {
    public static void main(String[] args) {
        String str = "go forward then back 20 cm turn left 20 degrees";
        List<String> list = Arrays.asList(str.split(" "));
        Constructor c = new Constructor();
        c.set_list(list);

        ArrayList<String> result;
            result = c.construct();
        for (String aResult : result) {
            System.out.println(aResult);
        }
    }
}
