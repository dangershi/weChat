package com.luwan.wechat;

/**
 * 抽象被观察者接口
 * 声明了添加、删除和通知观察者的方法
 * @author luwan
 *
 */
public interface Observerable {
	
	/**
	 * 注册观察者
	 * @param Observer o
	 */
	public void registerObserver(Observer o);
	
	/**
	 * 删除观察者
	 * @param o
	 */
	public void removeObserver(Observer o);
	
	/**
	 * 通知观察着
	 * @param o
	 */
	public void notifyObserver();
}
