package com.corundumstudio.socketio.demo;
import javapns.back.PushNotificationManager;
import javapns.back.SSLConnectionHelper;
import javapns.data.Device;
import javapns.data.PayLoad;
import javapns.back.FeedbackServiceManager;
import java.util.*;

public class SendAPNS {
	public static int RUN_MODE_DEVELOPMENT = 1;
	public static int RUN_MODE_PRODUCTION = 2;
	public static String APNS_DEVELOPMENT_GATEWAY = "gateway.sandbox.push.apple.com";
	public static String APNS_PRODUCTION_GATEWAY = "gateway.push.apple.com";
	public static String APNS_DEVELOPMENT_FEEDBACK = "feedback.sandbox.push.apple.com";
	public static String APNS_PRODUCTION_FEEDBACK = "feedback.push.apple.com";
	
	//TODO : 서버 환경, 인증서에 따라 세팅할 것 
	public static String APNS_DEVELOPMENT_KEY = "/Users/WS/Documents/eclipse/workspace/Socketio Demo/src/apns_development_key.p12";	//".p12 파일의 주소"
	public static String APNS_PRODUCTION_KEY = "/Users/WS/Documents/eclipse/workspace/Socketio Demo/src/apns_development_key.p12";	
	public static String APNS_DEVELOPMENT_KEY_PASSWORD = "apns";
	public static String APNS_PRODUCTION_KEY_PASSWORD = "apns";
	
	//64자리의 문자열 값. 테스트 용 
	public String deviceToken = "ae68368c 60105033 93ea3312 578fcd78 11babc37 7e28b65e 56a5a7b7 317c4463";	//우성 아이폰
	public String deviceToken1 = "906b26a1 c7800caf 6727b8a7 7f53bbe3 419f3156 d4f14b4f 5969d8f3 111a878b";	//우성 아이패드
	
	public void sendApns(int runMode, String alertMessage, int badgeCount, String soundFile) throws Exception {
		try {
			PayLoad payLoad = new PayLoad();
			payLoad.addAlert(alertMessage);
			payLoad.addBadge(badgeCount);
			payLoad.addSound(soundFile);	//없으면 defalut로 인자를 설정해주어야 한다. 

			PushNotificationManager pushManager = PushNotificationManager.getInstance();
			pushManager.addDevice("iPhone", deviceToken);
			pushManager.addDevice("iPad", deviceToken1);
			
			int port = 2195;
			String host = null;
			String certificatePath = null;
			String certificatePassword = null;
			
			System.out.println("I'm in SendAPNS");
			//runMode : APNS는 Development용과 Production용을 구별하고 있다. 
			//그것에 따라서 사용해야하는 키와 게이트웨이가 다르다.
			if (runMode == RUN_MODE_DEVELOPMENT) {
				host = APNS_DEVELOPMENT_GATEWAY;
				certificatePath = APNS_DEVELOPMENT_KEY;
				certificatePassword = APNS_DEVELOPMENT_KEY_PASSWORD;
			} else if (runMode == RUN_MODE_PRODUCTION) {
				host = APNS_PRODUCTION_GATEWAY;
				certificatePath = APNS_PRODUCTION_KEY;
				certificatePassword = APNS_PRODUCTION_KEY_PASSWORD;
			}
			
			//Connect to APNs
			pushManager.initializeConnection(host, port, certificatePath, certificatePassword, SSLConnectionHelper.KEYSTORE_TYPE_PKCS12);

			//TODO : 반복문으로 Device 가져오도록 Logic 수정할 것 !
			Device client = pushManager.getDevice("iPhone");
			Device client1 = pushManager.getDevice("iPad");
			
			pushManager.sendNotification(client, payLoad);
			pushManager.sendNotification(client1, payLoad);
			
			pushManager.stopConnection();

			//Device 삭제 
			pushManager.removeDevice("iPhone");
			pushManager.removeDevice("iPad");
		} catch (Exception ex) {
			ex.printStackTrace(); 
		}
	}
	
	//참고 사이트 : http://scaleup.tistory.com/5
	//APNS 서버는 APNS Feedback 서버에 deviceToken을 등록하여 Service Provider들이 이를 확인할 수 있도록 하고 있다.
	public ArrayList<String> sendFeedback(int runMode) {
		ArrayList<String> deviceTokens = new ArrayList<String> ();
		
		try {
			int port = 2196;
			String host = null;
			String certificatePath = null;
			String certificatePassword = null;
			if (runMode == RUN_MODE_DEVELOPMENT) {
				host = APNS_DEVELOPMENT_FEEDBACK;
				certificatePath = APNS_DEVELOPMENT_KEY;
				certificatePassword = APNS_DEVELOPMENT_KEY_PASSWORD;
			} else if (runMode == RUN_MODE_PRODUCTION) {
				host = APNS_PRODUCTION_FEEDBACK;
				certificatePath = APNS_PRODUCTION_KEY;
				certificatePassword = APNS_PRODUCTION_KEY_PASSWORD;
			}
			
			FeedbackServiceManager feedbackManager = FeedbackServiceManager.getInstance();
			LinkedList<Device> devices = feedbackManager.getDevices(host, port, certificatePath, certificatePassword, SSLConnectionHelper.KEYSTORE_TYPE_PKCS12);
			
			if (devices.size() > 0) {
				ListIterator<Device> itr = devices.listIterator();
				while (itr.hasNext()) {
					Device device = itr.next();
					deviceTokens.add(device.getToken());
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		return deviceTokens;
	}

	// TODO: 보내는 부분에 추가할 것 ! 
	/*
	public static void main(String... args) throws Exception{
		SendAPNS apns = new SendAPNS();
		apns.sendApns(
				RUN_MODE_DEVELOPMENT,
				"푸시푸시 테스트입니다.",
				1,
				"default");
		ArrayList<String> deviceTokens = apns.sendFeedback(RUN_MODE_DEVELOPMENT);
		for (String deviceToken : deviceTokens) {
			System.out.println(deviceToken);
		}

	}
	*/
}
