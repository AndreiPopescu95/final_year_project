import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Server extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private String command = "No Commands!";
    private Constructor constructor;

    public Server() {
        super();
        constructor = new Constructor();

        System.out.println("Created constructor");

    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.getOutputStream().println(command);

    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        try {
            int length = request.getContentLength();
            byte[] input = new byte[length];
            ServletInputStream sin = request.getInputStream();
            int c, count = 0 ;
            while ((c = sin.read(input, count, input.length-count)) != -1) {
                count +=c;
            }
            sin.close();

            String receivedString = new String(input);
            response.setStatus(HttpServletResponse.SC_OK);

            String str = receivedString;
            List<String> list = Arrays.asList(str.split(" "));
            constructor.set_list(list);

            System.out.println("Updated List");
            System.out.println("Received:" + receivedString);

            OutputStreamWriter writer = new OutputStreamWriter(response.getOutputStream());
            writer.write("Received Command!");
            writer.flush();
            writer.close();

            ArrayList<String> result;
            result = constructor.construct();
            try {
                File file = new File("/home/andrei/Commands_Folder/commands.txt");
               /* try {
                    file.mkdirs();
                    file.createNewFile();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }*/

                PrintWriter file_writer = new PrintWriter(file, "UTF-8");

                for (String aResult : result) {
                    command = aResult;
                    System.out.println(aResult);
                    file_writer.print(aResult);
                    file_writer.println();

                }
                file_writer.close();
            } catch (IOException e) {
                // System.err.println("error is: "+e.getMessage());
                e.printStackTrace();  // *** this is more informative ***
            }




        } catch (IOException e) {


            try{
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().print(e.getMessage());
                response.getWriter().close();
            } catch (IOException ioe) {
            }
        }
    }

}