package org.lightj.task;

public interface IWorker {

	public static enum WorkerMessageType {
		REPROCESS_REQUEST, COMPLETE_REQUEST
	}
}
