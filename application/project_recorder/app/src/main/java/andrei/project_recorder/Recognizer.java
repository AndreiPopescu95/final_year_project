package andrei.project_recorder;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;

import com.google.auth.Credentials;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechGrpc;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.StreamingRecognitionConfig;
import com.google.cloud.speech.v1.StreamingRecognitionResult;
import com.google.cloud.speech.v1.StreamingRecognizeRequest;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.StreamingRecognizeResponse;
import com.google.protobuf.ByteString;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.internal.DnsNameResolverProvider;
import io.grpc.okhttp.OkHttpChannelProvider;
import io.grpc.stub.StreamObserver;

/**
 * Created by andrei on 11/03/2018.
 */

public class Recognizer  extends Service{


    private static final String PREFS = "Recognizer";
    private static final String PREF_ACCESS_TOKEN_VALUE = "access_token_value";
    private static final String PREF_ACCESS_TOKEN_EXPIRATION_TIME = "access_token_expiration_time";

    /** We reuse an access token if its expiration time is longer than this. */
    private static final int ACCESS_TOKEN_EXPIRATION_TOLERANCE = 30 * 60 * 1000; // thirty minutes
    /** We refresh the current access token before it expires. */
    private static final int ACCESS_TOKEN_FETCH_MARGIN = 60 * 1000; // one minute

    private final ArrayList<Listener> listeners = new ArrayList<>();
    private final SpeechBinder binder = new SpeechBinder();
    public interface Listener {

        void recognized(String text);

    }

    private RecognitionConfig config;
    private static final String TAG = "Recognizer";
    private static final String HOSTNAME = "speech.googleapis.com";
    private static final int PORT = 443;
    public static final List<String> SCOPE =
            Collections.singletonList("https://www.googleapis.com/auth/cloud-platform");


    private final StreamObserver<StreamingRecognizeResponse> response_observer
            = new StreamObserver<StreamingRecognizeResponse>() {
        @Override
        public void onNext(StreamingRecognizeResponse response) {
            String text = null;
            if (response.getResultsCount() > 0) {
                final StreamingRecognitionResult result = response.getResults(0);
                if (result.getAlternativesCount() > 0) {
                    final SpeechRecognitionAlternative alternative = result.getAlternatives(0);
                    text = alternative.getTranscript();
                }
            }
            if (text != null) {
                for (Listener listener : listeners) {
                    listener.recognized(text);
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            Log.e(TAG, "Error calling the API.", t);
        }

        @Override
        public void onCompleted() {
            Log.i(TAG, "API completed.");
        }

    };

    private StreamObserver<StreamingRecognizeRequest> request_observer;
    private static Handler handler;
    private AccessToken token;
    private SpeechGrpc.SpeechStub api;
    private volatile AccessTokenTask mAccessTokenTask;


    public static Recognizer from(IBinder binder) {
        return ((SpeechBinder) binder).getService();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        get_token();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(get_token_Runnable);
        handler = null;
        // Release the gRPC channel.
        if (api != null) {
            final ManagedChannel channel = (ManagedChannel) api.getChannel();
            if (channel != null && !channel.isShutdown()) {
                try {
                    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Error shutting down the gRPC channel.", e);
                }
            }
            api = null;
        }
    }


    private void get_token(){
        if (mAccessTokenTask != null) {
            return;
        }
        mAccessTokenTask = new AccessTokenTask();
        mAccessTokenTask.execute();
    }


    public void start_recognizing(){
        if (api == null) {
            Log.w(TAG, "API not ready.");
            return;
        }
        Log.w(TAG, "Recognition Started");
        request_observer = api.streamingRecognize(response_observer);
        // Builds the sync recognize request
        config = RecognitionConfig.newBuilder()
                .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                .setSampleRateHertz(16000)
                .setLanguageCode("en-US")
                .build();
        request_observer.onNext(StreamingRecognizeRequest.newBuilder()
                .setStreamingConfig(StreamingRecognitionConfig.newBuilder()
                        .setConfig(config)
                        .setInterimResults(true)
                        .setSingleUtterance(true)
                        .build())
                .build());
    }

    public void recognize(byte[] data, int size) {

        if (request_observer == null) {
            return;
        }

        Log.w(TAG, "Recognizing...");
        StreamingRecognizeRequest audio = StreamingRecognizeRequest.newBuilder()
                .setAudioContent(ByteString.copyFrom(data, 0, size))
                .build();
        request_observer.onNext(audio);


    }

    public void finishRecognizing() {
        if (request_observer == null) {
            return;
        }
        Log.w(TAG, "Recognized");
        request_observer.onCompleted();
        request_observer = null;
    }

    public void addListener(@NonNull Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(@NonNull Listener listener) {
        listeners.remove(listener);
    }

    private class SpeechBinder extends Binder {

        Recognizer getService() {
            return Recognizer.this;
        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private final Runnable get_token_Runnable = new Runnable() {
        @Override
        public void run() {
            get_token();
        }
    };



    private class AccessTokenTask extends AsyncTask<Void, Void, AccessToken> {

        @Override
        protected AccessToken doInBackground(Void... voids) {
            final SharedPreferences prefs =
                    getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String tokenValue = prefs.getString(PREF_ACCESS_TOKEN_VALUE, null);
            long expirationTime = prefs.getLong(PREF_ACCESS_TOKEN_EXPIRATION_TIME, -1);
            if (tokenValue != null && expirationTime > 0) {
                if (expirationTime
                        > System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION_TOLERANCE) {
                    return new AccessToken(tokenValue, new Date(expirationTime));
                }
            }
            final InputStream stream = getResources().openRawResource(R.raw.service_account);
            try {
                final GoogleCredentials credentials = GoogleCredentials.fromStream(stream)
                        .createScoped(SCOPE);
                final AccessToken token = credentials.refreshAccessToken();
                prefs.edit()
                        .putString(PREF_ACCESS_TOKEN_VALUE, token.getTokenValue())
                        .putLong(PREF_ACCESS_TOKEN_EXPIRATION_TIME,
                                token.getExpirationTime().getTime())
                        .apply();
                return token;
            } catch (IOException e) {
                Log.e(TAG, "Failed to obtain access token.", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(AccessToken accessToken) {
            mAccessTokenTask = null;
            final ManagedChannel channel = new OkHttpChannelProvider()
                    .builderForAddress(HOSTNAME, PORT)
                    .nameResolverFactory(new DnsNameResolverProvider())
                    .intercept(new Credentials_Interceptor(new GoogleCredentials(accessToken)
                            .createScoped(SCOPE)))
                    .build();
            api = SpeechGrpc.newStub(channel);

            if (handler != null) {
                handler.postDelayed(get_token_Runnable,
                        Math.max(accessToken.getExpirationTime().getTime()
                                - System.currentTimeMillis()
                                - ACCESS_TOKEN_FETCH_MARGIN, ACCESS_TOKEN_EXPIRATION_TOLERANCE));
            }
        }
    }


}
