package org.lightj.task.asynchttp;

import org.lightj.task.TaskResultEnum;

import com.ning.http.client.Response;

/**
 * custom polling implementation
 * 
 * @author binyu
 *
 */
public interface IPollProcessor {

	/**
	 * process response and create poll request
	 * @param reponse
	 * @param pollReq
	 * @return
	 */
	public TaskResultEnum preparePollTask(Response reponse, UrlRequest pollReq); 
	
	/**
	 * process poll response for progress
	 * @param response
	 * @return
	 */
	public TaskResultEnum checkPollProgress(Response response);
}
