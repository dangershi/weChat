package com.luwan.wechat;

/**
 * 运用观察者模式，接收微信推送消息
 * @author luwan
 *
 */
public class ObserverPatternTest {

	public static void main(String[] args) {
		WeChatServer weChatServer = new WeChatServer();
		
		Observer user = new User("User1");
		Observer user2 = new User("User2");
		Observer user3 = new User("User3");
		
		weChatServer.registerObserver(user);
		weChatServer.registerObserver(user2);
		weChatServer.registerObserver(user3);
		
		weChatServer.setMessage("微信服务推送消息：微信推送消息啦：就是看不起Java！");
		
		System.out.println("-------------------------");
		weChatServer.removeObserver(user);
		weChatServer.removeObserver(user2);
		System.out.println("User1和User2都取消关注，不接收消息了");
		
		weChatServer.setMessage("微信服务推送消息：微信再次推送：大佬我错了，Java最好了！");
	}
}
