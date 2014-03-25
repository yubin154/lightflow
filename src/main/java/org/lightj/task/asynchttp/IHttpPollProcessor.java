package org.lightj.task.asynchttp;

import java.io.IOException;

import org.lightj.task.Task;
import org.lightj.task.TaskResult;

import com.ning.http.client.Response;

/**
 * custom polling implementation
 * 
 * @author binyu
 *
 */
public interface IHttpPollProcessor {

	/**
	 * process response and create poll request
	 * @param response
	 * @param pollReq
	 * @return
	 */
	public TaskResult preparePollTask(Task task, Response response, UrlRequest pollReq) throws IOException; 
	
	/**
	 * process poll response for progress
	 * @param response
	 * @return
	 */
	public TaskResult checkPollProgress(Task task, Response response) throws IOException;
	
}
