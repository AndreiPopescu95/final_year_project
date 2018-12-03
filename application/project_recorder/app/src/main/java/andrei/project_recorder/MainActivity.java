package andrei.project_recorder;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.fasterxml.jackson.core.util.BufferRecycler;
import com.google.auth.oauth2.GoogleCredentials;

import com.google.protobuf.ByteString;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

public class MainActivity extends Activity {

    public static final int SAMPLING_RATE = 16000;
    public static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    public static final int CHANNEL_IN_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    public static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLING_RATE, CHANNEL_IN_CONFIG, AUDIO_FORMAT);
    public static final String LOGTAG = "recorder";
    private final String TAG_toServer = "Server_Request";
    private final String TAG_fromServer = "Server_Response";

    private Recognizer speech;
    public TextView transcription_view;
    private ImageView img;
    private Recorder record;
    private EditText input_value;
    private Button button;
    public String IP;

    private Thread server_sender;
    private Thread press_thread;
    private Thread btn_thread;



    private final Recorder.Methods Methods = new Recorder.Methods() {

        @Override
        public void speech_start_recognition() {
            //hear_voice(true);
            if (speech != null) {
                Log.v(LOGTAG, "Starting recognition…");
               speech.start_recognizing();
            }
        }

        @Override
        public void speech_recognize(byte[] data, int size) {
            if (speech != null) {
                speech.recognize(data, size);
            }
        }

        @Override
        public void speech_stop_recognition() {
           // hear_voice(false);
            if (speech != null) {
                speech.finishRecognizing();
            }
        }

    };

    private final ServiceConnection service_connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            speech = Recognizer.from(binder);
            speech.addListener(speech_listener);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            speech = null;
        }

    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        final Resources resources = getResources();
        transcription_view = (TextView)findViewById(R.id.recognition);
        img = (ImageView)findViewById(R.id.mic);
        img.setImageResource(R.drawable.mic_off);
        IP = "192.168.0.24";
        input_value = (EditText) findViewById(R.id.input_ip);

        button = (Button)findViewById(R.id.button);
        Context context = getApplicationContext();
        btn_thread = new Thread(new press_button(button, context));
        btn_thread.start();


    }

    protected void onStart() {
        super.onStart();
        bindService(new Intent(this, Recognizer.class), service_connection, BIND_AUTO_CREATE);
        record = new Recorder(Methods);
        record.create();
    }

    @Override
    protected void onStop() {
        // Stop listening to voice
        stop_recorder();

        // Stop Cloud Speech API
        speech.removeListener(speech_listener);
        unbindService(service_connection);
        speech = null;

        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    private class press_button implements Runnable {

        private Button btn;
        Context context;

        public  press_button(Button button, Context c){
            btn = button;
            context = c;
        }

        @Override
        public void run() {
            btn.setOnTouchListener(new View.OnTouchListener() {

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch(event.getAction()) {
                        case MotionEvent.ACTION_DOWN:

                            hear_voice(true);
                            start_recorder();

                            return true;
                        case MotionEvent.ACTION_UP:
                            hear_voice(false);
                            Log.v(LOGTAG, "In Action UP");
                            record.stop_temp();

                            server_sender = new Thread(new send_to_server());
                            server_sender.start();

                            return true;
                    }
                    return false;
                }
            });
        }
    }

    private void start_recorder() {
        record.start();
    }

    private void stop_recorder() {
        if (record != null) {
            record.stop();
            record = null;
        }
    }

    private void hear_voice(final boolean voice) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(voice){
                    img.setImageResource(R.drawable.mic_on);
                } else{
                    img.setImageResource(R.drawable.mic_off);

                }
            }
        });
    }

    private final Recognizer.Listener speech_listener =
            new Recognizer.Listener() {
                @Override
                public void recognized(final String text) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                transcription_view.setText(text);
                            }
                        });
                }
            };
    public void set_ip(View v){
        String ip;
        ip = input_value.getText().toString();
        IP = ip;
        input_value.setText("");
    }


    private class send_to_server implements Runnable {

        @Override
        public void run() {
            try {
                URL url = new URL("http://"+IP+":8080/server/server");
                URLConnection connection = url.openConnection();

                String command_to_send = transcription_view.getText().toString();

                Log.d("Command to send is: ",command_to_send);

                connection.setDoOutput(true);
                OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
                out.write(command_to_send);
                Log.v(TAG_toServer,"Command sent!");
                out.close();

                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String return_string="";
                while((return_string = in.readLine()) != null){
                    Log.v(TAG_fromServer,return_string);
                }
                in.close();

            }catch (Exception e){
                Log.d("Exception ",e.toString());
            }
        }
    }

}