package com.luwan.wechat;

/**
 * 抽象观察者
 * 定义update()方法，当被观察者调用notifyObserver()方法时，观察者的update()方法会被调用
 * @author luwan
 *
 */
public interface Observer {

	/**
	 * 观察者接收被观察者的消息
	 * @param message
	 */
	public void update(String message);
}
