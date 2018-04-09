package com.luwan.wechat;

/**
 * 观察者，接收微信推送消息
 * @author luwan
 *
 */
public class User implements Observer{

	private String name;
	//持有被观察者对象，接收被观察推送的消息
	private String message;
	
	public User(String name) {
		this.name = name;
	}
	
	@Override
	public void update(String message) {
		this.message = message;
		read();
	}

	private void read() {
		System.out.println(name + "收到推�?�消息：" + message);
	}

	
}
