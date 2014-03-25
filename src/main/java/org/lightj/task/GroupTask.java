package org.lightj.task;

import java.util.List;

/**
 * at runtime fan out to a list of tasks for execution
 * @author binyu
 *
 * @param <T>
 */
public abstract class GroupTask<E extends ExecutableTask> extends ExecutableTask {

	/** create new instance of a task */
	public abstract E createTaskInstance();
	
	/** fan out to list of tasks */
	public abstract List<E> getTasks();

	@Override
	public final TaskResult execute() throws TaskExecutionException {
		throw new TaskExecutionException("not supported");
	}
}
