package com.luwan.wechat;

import java.util.ArrayList;
import java.util.List;

/**
 * 被观察者，微信公众号服务，向观察者推送消息
 * @author luwan
 *
 */
public class WeChatServer implements Observerable{

	private List<Observer> list;
	private String message;
	
	public WeChatServer() {
		list = new ArrayList<Observer>();
	}
	
	@Override
	public void registerObserver(Observer o) {
		list.add(o);
	}

	@Override
	public void removeObserver(Observer o) {
		if (!list.isEmpty()) {
			list.remove(o);
		}
		
	}

	@Override
	public void notifyObserver() {
		//遍历观察者集合，通知所有观察者
		for (Observer observer : list) {
			observer.update(message);
		}
		
	}
	
	public void setMessage(String s){
		this.message = s;
		System.out.println("微信服务推送消息：" + s);
		//通知所有
		notifyObserver();
	}
	
}
