package com.eazzyapps.webrtcclient;

import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.NotYetConnectedException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;

/**
 * Created by Александр on 17.08.2016.
 */
public class SignalingServer {

    private final OkHttpClient client;
    private WebSocketClient wsClient;
    boolean connected;
    String userId;
    URL serverUrl;

    public SignalingServer(String urlString) throws MalformedURLException {

        this.serverUrl = new URL(urlString);
        this.client = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .build();
        this.connected = false;
    }

    public Observable<String> connectToServer(String room, String userName) {

        //TODO check for internet connection

        return Observable
                .create(subscriber -> {
                            String timestamp = String.valueOf(System.currentTimeMillis());
                            String random = String.valueOf(Integer.valueOf(234846259));
                            Response response;

                            String url = new StringBuilder()
                                    .append(serverUrl.getProtocol()).append("://")
                                    .append(serverUrl.getHost())
                                    .append(serverUrl.getPort() > -1 ? ":" + serverUrl.getPort() : "")
                                    .append("/").append(Constants.apiKey)
                                    .append("/id?ts=")
                                    .append(timestamp)
                                    .append(".")
                                    .append(random)
                                    .toString();

                            Log.d(Constants.TAG, "getting url " + url);

                            Request request = new Request.Builder()
                                    .url(url)
                                    .build();

                            try {
                                response = client.newCall(request).execute();
                                if (response != null) {
                                    userId = response.body().string();
                                    Log.d(Constants.TAG, "got id from server: " + userId);

                                    subscriber.onNext(userId);
                                    subscriber.onCompleted();
                                } else
                                    subscriber.onError(new Exception("server response is null"));
                            } catch (IOException e) {
                                subscriber.onError(new Exception("error connecting signaling server", e));
                            }
                        }
                );
    }

    Observable<EAMessage> heartBeat = Observable
            .interval(45, TimeUnit.SECONDS)
            .map(aLong -> new EAMessage(EAMessage.TYPE_HEARTBEAT, null, null))
            .subscribeOn(Schedulers.newThread());

    Subscription heartBeatSubscription;

    public Observable<EAMessage> establishWebSocketConnection(String userId, String userName) {

        return Observable
                .create(subscriber -> {

                            String url = new StringBuilder()
                                    .append(Constants.wsProtocol)
                                    .append(serverUrl.getHost())
                                    .append(serverUrl.getPort() > -1 ? ":" + serverUrl.getPort() : "")
                                    .append("/").append("peerjs?key=")
                                    .append(Constants.apiKey)
                                    .append("&name=")
                                    .append(userName)
                                    .append("&id=")
                                    .append(userId)
                                    .append("&token=")
                                    .append(Constants.token)
                                    .toString();

                            Log.d(Constants.TAG, "opening socket " + url);

                            URI uri;
                            try {
                                uri = new URI(url);
                            } catch (URISyntaxException e) {
                                Log.d(Constants.TAG, "websocket creation failed - Uri creation error");
                                subscriber.onError(new Exception("uri syntax error", e));
                                return;
                            }

                            wsClient = new WebSocketClient(uri) {
                                @Override
                                public void onOpen(ServerHandshake handshakedata) {

                                    // in order to keep ws connection alive
                                    // (e.g. on Heroku it is closed after 55 sec of inactivity)
                                    // https://github.com/peers/peerjs/issues/227
                                    Log.d(Constants.TAG, "heartbeat started");
                                    heartBeatSubscription = heartBeat
                                            .subscribe(message -> {
//                                                Log.d(Constants.TAG, "... beat");
                                                wsClient.send(message.toString());
                                            });
                                }

                                @Override
                                public void onMessage(String message) {

                                    try {
                                        subscriber.onNext(new EAMessage(message));
                                    } catch (JSONException e) {
                                        subscriber.onNext(new EAMessage(e));
                                    }
                                }

                                @Override
                                public void onClose(int code, String reason, boolean remote) {
                                    connected = false;

                                    heartBeatSubscription.unsubscribe();
                                    Log.d(Constants.TAG, "heartbeat stopped");

                                    subscriber.onCompleted();
                                    Log.d(Constants.TAG, "webSocket closed");
                                }

                                @Override
                                public void onError(Exception ex) {
                                    Log.d(Constants.TAG, ex.getMessage());
                                    subscriber.onNext(new EAMessage(ex));
                                }
                            };

                            wsClient.connect();
                        }
                );
    }

    public Observable<List<String>> discoverPeers() {

        return Observable
                .create(subscriber -> {

                            String timestamp = String.valueOf(System.currentTimeMillis());
                            String random = String.valueOf(Integer.valueOf(234846259));

                            String url = new StringBuilder()
                                    .append(serverUrl.getProtocol()).append("://")
                                    .append(serverUrl.getHost())
                                    .append(serverUrl.getPort() > -1 ? ":" + serverUrl.getPort() : "")
                                    .append("/").append(Constants.apiKey)
                                    .append("/peers?ts=")
                                    .append(timestamp)
                                    .append(".")
                                    .append(random)
                                    .toString();

                            Log.d(Constants.TAG, "getting peers " + url);

                            Request request = new Request.Builder()
                                    .url(url)
                                    .build();

                            Response response = null;
                            Call call = client.newCall(request);
                            try {
                                response = call.execute();
                                // parse server response to get already connected peers list

                                List<String> peers = new ArrayList<>();
                                String respstring = response.body().string();

                                if (!respstring.equals("Not Found")) {
                                    JSONArray resp = new JSONArray(respstring);

                                    Log.d(Constants.TAG, respstring);

                                    for (int i = 0; i < resp.length(); i++)
                                        peers.add(resp.getString(i));

                                    Log.d(Constants.TAG, String.valueOf(resp.length()) + " peers discovered incl. myself");
                                    subscriber.onNext(peers);
                                    subscriber.onCompleted();
                                } else
                                    subscriber.onError(new Exception("peers not found"));
                            } catch (IOException | JSONException e) {
                                subscriber.onError(e);
                            }
                        }
                );
    }

    public void sendMsg(EAMessage message) throws NotYetConnectedException {

        if (wsClient != null) {
            message.setSource(userId);
            wsClient.send(message.toString());
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }
}
