package andrei.project_recorder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;
import android.view.View;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by andrei on 11/03/2018.
 */

public class Recorder {

    private static final int AMPLITUDE_THRESHOLD = 1500;
    private static final int SPEECH_TIMEOUT_MILLIS = 2000;
    private static final int MAX_SPEECH_LENGTH_MILLIS = 30 * 1000;
    public static final int SAMPLING_RATE = 16000;
    public static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    public static final int CHANNEL_IN_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    public static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLING_RATE, CHANNEL_IN_CONFIG, AUDIO_FORMAT);
    public static final String LOGTAG = "recorder";
    AudioRecord recorder;
    byte[] audioData;

    private Thread recordingThread;
    private long voice_last_heard = Long.MAX_VALUE;
    private final Object lock = new Object();

    public static abstract class Methods {

        public void speech_start_recognition() {
        }

        public void speech_recognize(byte[] data, int size) {
        }

        public void speech_stop_recognition() {
        }
    }

    private final Methods methods;


    public Recorder(Methods _methods){
        methods = _methods;
    }

    public void create() {
        stop();

        audioData = new byte[BUFFER_SIZE];
        recorder = new AudioRecord(AUDIO_SOURCE,
                SAMPLING_RATE, CHANNEL_IN_CONFIG,
                AUDIO_FORMAT, BUFFER_SIZE);
        recorder.startRecording();
        if (recorder == null) {
            throw new RuntimeException("Cannot instantiate Recorder");
        }

    }

    public void start(){
        Log.v(LOGTAG, "Starting recording…");
        //recorder.startRecording();
        // Start processing the captured audio.
        recordingThread = new Thread(new process_voice());
        recordingThread.start();
    }

    private class process_voice implements Runnable {

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                synchronized (lock) {

                    if(Thread.currentThread().isInterrupted()){
                        break;
                    }
                    int status = recorder.read(audioData, 0, audioData.length);
                    long now = System.currentTimeMillis();
                    if (status == AudioRecord.ERROR_INVALID_OPERATION ||
                            status == AudioRecord.ERROR_BAD_VALUE) {
                        Log.e(LOGTAG, "Error reading audio data!");
                        return;
                    }

                    if(hears_voice(audioData,status)){

                        if(voice_last_heard == Long.MAX_VALUE){
                            Log.v(LOGTAG, "Heard Voice…");
                            methods.speech_start_recognition();
                        }
                        methods.speech_recognize(audioData,status);
                        voice_last_heard = now;
                        if(now - voice_last_heard > MAX_SPEECH_LENGTH_MILLIS){
                            end();
                        }
                    } else if(voice_last_heard != Long.MAX_VALUE){
                        methods.speech_recognize(audioData,status);
                        if(now - voice_last_heard > SPEECH_TIMEOUT_MILLIS){
                            end();
                        }
                    }
                }
            }
        }

        private void end() {
            voice_last_heard = Long.MAX_VALUE;
        }

        private boolean hears_voice(byte[] buffer, int size) {
            for (int i = 0; i < size - 1; i += 2) {
                int s = buffer[i + 1];
                if (s < 0) s *= -1;
                s <<= 8;
                s += Math.abs(buffer[i]);
                if (s > AMPLITUDE_THRESHOLD) {
                    return true;
                }
            }
            return false;
        }
    }

    public void stop_temp() {
        Log.v(LOGTAG, "Stopping recording temporarily");
        //synchronized (lock) {
            dismiss();
            if(recordingThread != null){
                recordingThread.interrupt();
                recordingThread = null;
            }
        //}
    }

    public void stop() {
        Log.v(LOGTAG, "Stopping recording…");
        synchronized (lock) {
            dismiss();
            if(recordingThread != null){
                recordingThread.interrupt();
                recordingThread = null;
            }
            if(recorder != null){
                recorder.stop();
                recorder.release();
                recorder = null;
            }
            audioData = null;
        }

    }


    public void dismiss() {
        if (voice_last_heard != Long.MAX_VALUE) {
            voice_last_heard = Long.MAX_VALUE;
            methods.speech_stop_recognition();
        }
    }



}
