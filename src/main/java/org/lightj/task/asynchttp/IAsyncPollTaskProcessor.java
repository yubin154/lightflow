package org.lightj.task.asynchttp;

import org.lightj.task.TaskResultEnum;

import com.ning.http.client.Response;

/**
 * process the async poll task
 * 
 * @author biyu
 *
 */
public interface IAsyncPollTaskProcessor {

	/**
	 * from original request, generate poll task
	 * @param reponse
	 * @param pollReq
	 * @return
	 */
	public TaskResultEnum preparePollTask(Response reponse, UrlRequest pollReq); 
	
	/**
	 * check poll progress
	 * @param response
	 * @return
	 */
	public TaskResultEnum checkPollProgress(Response response);
	
}
