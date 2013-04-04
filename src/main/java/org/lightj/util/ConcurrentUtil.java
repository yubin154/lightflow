package org.lightj.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ConcurrentUtil {

	/**
	 * wait on a condition
	 * @param lock
	 * @param cond
	 */
	public static void wait(ReentrantLock lock, Condition cond) {
		lock.lock();
		try {
			cond.await();
		} catch (InterruptedException ie) {
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * wait on a condition
	 * @param lock
	 * @param cond
	 * @throws InterruptedException 
	 */
	public static boolean wait(ReentrantLock lock, Condition cond, long timeoutMilliSec) throws InterruptedException {
		lock.lock();
		try {
			return cond.await(timeoutMilliSec, TimeUnit.MILLISECONDS);
		} catch (InterruptedException ie) {
			throw ie;
		} finally {
			lock.unlock();
		}
	}
	
	
	/**
	 * signal a condition
	 * @param lock
	 * @param cond
	 */
	public static void signal(ReentrantLock lock, Condition cond) {
		lock.lock();
		try {
			cond.signal();
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * signal all on a condition
	 * @param lock
	 * @param cond
	 */
	public static void signalAll(ReentrantLock lock, Condition cond) {
		lock.lock();
		try {
			cond.signalAll();
		} finally {
			lock.unlock();
		}
	}
	

}
