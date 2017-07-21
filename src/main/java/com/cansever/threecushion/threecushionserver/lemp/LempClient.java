package com.cansever.threecushion.threecushionserver.lemp;

import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.lemp.object.Administrative;
import com.lemp.object.Authentication;
import com.lemp.packet.Datum;
import com.lemp.packet.Request;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ayberkcansever on 13/07/2017.
 */
@Component
public class LempClient implements WebSocketHandler, Runnable {

    private static Logger logger = LogManager.getLogger();

    @Value( "${lemp.url}" )
    private String lempUrl;

    @Value( "${lemp.admin.username}" )
    private String lempAdminUsername;

    @Value( "${lemp.admin.password}" )
    private String lempAdminPassword;

    private WebSocketSession webSocketSession;
    private ConnectionCheckerThread connectionCheckerThread;
    private Gson gson = new Gson();
    private ConcurrentHashMap<String, LempMessage> lempMessageMap = new ConcurrentHashMap<>();
    private AtomicInteger messageCounter = new AtomicInteger(0);
    private String idPrefix = Hashing.murmur3_32(12).hashString(String.valueOf(System.currentTimeMillis()), Charset.defaultCharset()).toString();

    @PostConstruct
    public void init() {
        new StandardWebSocketClient().doHandshake(this, lempUrl);
        connectionCheckerThread = new ConnectionCheckerThread(this, lempUrl);
        connectionCheckerThread.start();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession webSocketSession) throws Exception {
        this.webSocketSession = webSocketSession;
        connectionCheckerThread.stopRetryConnecting();
        Request request = new Request();
        request.setId(generateMsgId());
        Authentication authentication = new Authentication();
        authentication.setI(lempAdminUsername);
        authentication.setT(lempAdminPassword);
        request.setA(authentication);
        send(new LempMessage(new Datum(request)));
    }

    @Override
    public void handleMessage(WebSocketSession webSocketSession, WebSocketMessage<?> webSocketMessage) throws Exception {
        String payload = (String)webSocketMessage.getPayload();
        logger.info("Received: " + payload);
        Datum datum = gson.fromJson(payload, Datum.class);
    }

    @Override
    public void handleTransportError(WebSocketSession webSocketSession, Throwable throwable) throws Exception {

    }

    @Override
    public void afterConnectionClosed(WebSocketSession webSocketSession, CloseStatus closeStatus) throws Exception {
        connectionCheckerThread.startRetryConnecting();
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    private String generateMsgId() {
        return idPrefix + "-" + messageCounter.incrementAndGet();
    }

    private void send(LempMessage lempMessage) throws IOException {
        webSocketSession.sendMessage(new TextMessage(gson.toJson(lempMessage.getDatum())));
        String id = "";
        if(lempMessage.getDatum().getRq() != null) {
            id = lempMessage.getDatum().getRq().getId();
            lempMessageMap.put(id, lempMessage);
        } else if(lempMessage.getDatum().getSrq() != null) {
            id = lempMessage.getDatum().getSrq().getId();
            lempMessageMap.put(id, lempMessage);
        }
        logger.info("Lemp message sent: " + id);
    }

    public void createUser(String userId) throws IOException {
        Administrative administrative = new Administrative();
        administrative.setC(Administrative.Command.create.getKey());
        administrative.setI(userId);
        administrative.setT("p_" + userId);
        Request request = new Request();
        request.setId(generateMsgId());
        request.setAd(administrative);
        send(new LempMessage(new Datum(request)));
    }

    @Override
    public void run() {
        while (true && webSocketSession.isOpen()) {
            try {
                Iterator<LempMessage> iter = lempMessageMap.values().iterator();
                while(iter.hasNext()) {
                    LempMessage lempMessage = iter.next();
                    if(lempMessage.getSentTime() + 10 * 1000 > System.currentTimeMillis()) {
                        if(lempMessage.getRetry() < 3) {
                            lempMessage.increaseRetryCount();
                            send(lempMessage);
                        }
                    }
                }

            } catch (Exception ex) {

            }
        }
    }

    public class ConnectionCheckerThread extends Thread {

        private boolean running = true;
        private boolean check = false;
        private LempClient lempClient;
        private String url;

        public ConnectionCheckerThread(LempClient lempClient, String url) {
            this.lempClient = lempClient;
            this.url = url;
        }

        public void startRetryConnecting(){
            this.check = true;
        }

        public void stopRetryConnecting(){
            this.check = false;
        }

        public void run(){
            while (running) {
                try {
                    if(check) {
                        System.out.println("Trying to connect...");
                        new StandardWebSocketClient().doHandshake(lempClient, url);
                    }
                } catch (Exception ex) {

                } finally {
                    try {
                        TimeUnit.SECONDS.sleep(5);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
