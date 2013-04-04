/*
 * Created on Nov 29, 2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.lightj.dal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.lightj.util.Log4jProxy;


/**
 * @author biyu
 *
 * A helper class to encapsulate all database transactions. Anyone who needs
 * their codes to be executed in db transaction needs to implement ITransactional
 * interface and do their work in execute()
 * 
 */
public class TransactionManager {
	
	static final Log4jProxy log = Log4jProxy.getInstance(TransactionManager.class.getName());
	
	/**
	 * Wether current thread is in a transaction
	 */
	private static ThreadLocal<Boolean> inTransaction = new ThreadLocal<Boolean>() {
		protected synchronized Boolean initialValue() {
			return Boolean.FALSE;
		}
	};
	
	/**
	 * Call back handlers on rollback, if transaction commits successfully,
	 * user would have chance to do additional work later, however if the
	 * transaction interrupted by exception, we need to provide call back
	 * interface for the calling thread to do any cleanup work.
	 */
	private static ThreadLocal<List<TransactionHandler>> onRollbackHandlers = new ThreadLocal<List<TransactionHandler>>() {
		protected synchronized List<TransactionHandler> initialValue() {
			return new ArrayList<TransactionHandler>();
		}
	};
	
	/**
	 * Check if a thread is already in a transaction
	 * @param dbEnum
	 * @return
	 */
	public static final boolean check(BaseDatabaseType dbEnum) {
		return ((Boolean) inTransaction.get()).booleanValue();
	}

	/**
	 * execute a job in db transaction context. If an exception is thrown
	 * by the implementer the transaction will be rolled back automatically.
	 * 
	 * @param dbEnum 	db transaction in which database
	 * @param job		actual work
	 * @throws Throwable
	 */
	public static final void execute(BaseDatabaseType dbEnum, ITransactional job) throws Throwable
	{
		boolean exceptionThrown = false;
		
		try{
			// reserve a connection for this thread and make it transactional
			// all the subsequent call is going to use the same connection 
			ConnectionHelper.startTr(dbEnum);

			// everything from here on will be transaction guarded
			job.execute();
			// commit transaction, this version of jdbc doesn't support checkpoint yet, 
			// so the commit will commit whatever committed so far, 
			// this would be a problem if we call TransactionManger.execute() within another transaction

			ConnectionHelper.commitTr(dbEnum);
			log.debug("Transaction commited");
		}
		catch (RuntimeException rte) {
			exceptionThrown = true;
			log.error("Transaction failed for " + job.getClass().getName() + ": " + rte.getMessage());
			throw rte;
			
		}
		catch (Exception e) {
			exceptionThrown = true;
			log.error("Transaction failed for " + job.getClass().getName() + ": " + e.getMessage());
			throw e;
		}
		finally {
			/*Rollback the transaction if there was an exception thrown in the transaction.*/
			if (exceptionThrown) {
				// something wrong with transaction, rollback
				ConnectionHelper.rollbackTr(dbEnum);
				log.debug("Transaction rolledback");

				// call rollback handler if any
				List<TransactionHandler> handlers = onRollbackHandlers.get();
				for (Iterator<TransactionHandler> iter = handlers.iterator(); iter.hasNext();) {
					TransactionHandler handler = iter.next();
					handler.cleanup();
				}
			}
		}
	}
	
	/**
	 * Add onRollbackHandler
	 * @param handler
	 */
	@SuppressWarnings({"rawtypes","unchecked"})
	public static final void addOnRollbackHandler(TransactionHandler handler) {
		((List) onRollbackHandlers.get()).add(handler);
	}
	
	/**
	 * Remove onRollbackHandler
	 * @param handler
	 */
	public static final void removeOnRollbackHandler(TransactionHandler handler) {
		onRollbackHandlers.get().remove(handler);
	}
	
	public static interface TransactionHandler {
		public void cleanup();
	}
}
