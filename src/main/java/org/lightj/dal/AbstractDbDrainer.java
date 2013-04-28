package org.lightj.dal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Drain to db from an asynchronous queue
 * @author biyu
 *
 * @param <T>
 */
@SuppressWarnings({"rawtypes","unchecked"})
public abstract class AbstractDbDrainer<T extends AbstractDAO> {

	static Logger logger = LoggerFactory.getLogger(AbstractDbDrainer.class);

	/**
	 * dao class
	 */
	private T dao;
	
	/**
	 * each drainer has one queue
	 */
	private LinkedBlockingQueue<IData> dbQ = new LinkedBlockingQueue<IData>();
	
	/**
	 * how often at most the queue is being checked
	 */
	private long processInterval;
	
	/**
	 * @param factory
	 */
	protected AbstractDbDrainer(T dao, long processInterval) {
		this.dao = dao;
		this.processInterval = processInterval;
	}

	/**
	 * start the queue processing thread
	 *
	 */
	public final void startQ() {
		new Thread(new LogQRunner()).start();
		// this drain the queue to db when VM goes down gracefully
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				List<IData> processList = new ArrayList<IData>(); 
				synchronized (dbQ) {
					dbQ.drainTo(processList);
				}
				try {
					dao.saveBatch(processList);
				}
				catch (Throwable t) {
					// ignore
				}
			}
		});
	}
	
	/**
	 * Add an event to be monitored
	 * @param eventR
	 */
	public final void addData(IData data) {
		if (processInterval <= 0) {
			if (dbQ.isEmpty()) {
				synchronized (dbQ) {
					dbQ.offer(data);
					dbQ.notifyAll();
				}
			}
			else {
				dbQ.offer(data);
			}
		}
		else {
			// processing thread wakes up every processInterval miliseconds to check the queue, no need to wake it up
			dbQ.offer(data);
		}
	}
	
	/**
	 * Process awaiting events in the queue
	 */
	final void processQ() {
		while (true) {
			if (dbQ.isEmpty()) {
				if (processInterval <= 0) {
					synchronized (dbQ) {
						try {
							// we are willing to wait for up to 60 seconds if not specified otherwise
							dbQ.wait(60 * 1000L);
						} catch (InterruptedException e) {
						}
					}
				}
				else {
					try {
						Thread.sleep(processInterval);
					} catch (InterruptedException e) {
					}
				}
			} else {
				List<IData> processList = new ArrayList<IData>(); 
				synchronized (dbQ) {
					dbQ.drainTo(processList);
				}
				try {
					dao.saveBatch(processList);
				}
				catch (Throwable t) {
					// ignore
				}
			}
		}
	}

	/**
	 * pending actions list as a string
	 * 
	 * @return
	 */
	public String getQueueInfo() {
		StringBuffer buf = new StringBuffer();
		buf.append("DbQ size: ").append(dbQ.size()).append('\n');
		return buf.toString();
	}
	
	/**
	 * return the pending size of
	 * the queue
	 * @return
	 */
	public int getQueueSize() {
		return dbQ.size();
	}

	/**
	 * subject q runner
	 * @author biyu
	 *
	 */
	class LogQRunner implements Runnable {
		public void run() {
			processQ();
		}
	}
}
