package org.lightj.task.asynchttp;

import java.io.IOException;

import org.lightj.task.Task;
import org.lightj.task.TaskResult;

import com.ning.http.client.Response;

/**
 * process http response
 * @author biyu
 *
 */
public interface IHttpProcessor {
	
	/**
	 * process http response
	 * @param task
	 * @param response
	 * @return
	 * @throws IOException
	 */
	public TaskResult processHttpReponse(Task task, Response response) throws IOException;
	
}
