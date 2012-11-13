package com.corundumstudio.socketio.demo;

import com.corundumstudio.socketio.listener.*;
import com.corundumstudio.socketio.*;
import java.util.ArrayList;

public class ChatLauncher {

    public static void main(String[] args) throws InterruptedException {

        Configuration config = new Configuration();
        config.setHostname("localhost");	//서버 주소
        config.setPort(9092);						//서버 포트

        final SocketIOServer server = new SocketIOServer(config);
        server.addJsonObjectListener(ChatObject.class, new DataListener<ChatObject>() {
            @Override
            public void onData(SocketIOClient client, ChatObject data, AckRequest ackRequest) {
                // broadcast messages to all clients
                server.getBroadcastOperations().sendJsonObject(data);
                
                /* woosung 추가 */
                System.out.println("I'm in ChatLauncher!");
                SendAPNS apns = new SendAPNS();
        		try {
        			String message = "["+ data.getUserName() + "]" + data.getMessage();
//        			apns.sendApns(1, "푸시푸시 테스트입니다.", 1, "default" );
        			apns.sendApns(1, message, 1, "default" );
        		} catch (Exception e) {
        			// TODO Auto-generated catch block
        			e.printStackTrace();
        		}
        		ArrayList<String> deviceTokens = apns.sendFeedback(1);
        		for (String deviceToken : deviceTokens) {
        			System.out.println(deviceToken);
        		}
        		/* woosung 추가 */
        		
//                System.out.println("--------------------------------");
                System.out.println("송신자: " + data.getUserName());
                System.out.println("메시지: " + data.getMessage());
                System.out.println("--------------------------------");
            }
        });

        server.start();
		
        Thread.sleep(Integer.MAX_VALUE);

        server.stop();
    }

}
