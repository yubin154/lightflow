package org.lightj.task;

import java.util.List;

/**
 * a container task that at runtime fan out to a list of tasks for execution
 * this task should never be executed itself for real
 * 
 * @author binyu
 *
 * @param <T>
 */
public abstract class GroupTask<E extends ExecutableTask> extends ExecutableTask {

	/** fan out to list of tasks */
	public abstract List<E> getTasks();

	@Override
	public final TaskResult execute() throws TaskExecutionException {
		throw new TaskExecutionException("not supported");
	}
}
