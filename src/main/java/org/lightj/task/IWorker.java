package org.lightj.task;

/**
 * IWorker interface for task exeucting actors
 * @author binyu
 *
 */
public interface IWorker {

	public static enum WorkerMessageType {
		REPROCESS_REQUEST, COMPLETE_REQUEST, COMPLETE_TASK, RETRY_TASK
	}
}
