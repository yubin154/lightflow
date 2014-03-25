package org.lightj.task;

import java.util.Map;

import org.lightj.session.FlowContext;

/**
 * simple handler, does nothing
 * @author binyu
 *
 * @param <T>
 */

public class SimpleTaskEventHandler<T extends FlowContext> implements ITaskEventHandler<T> {

	@Override
	public void executeOnCreated(T ctx, Task task) {
	}

	@Override
	public void executeOnSubmitted(T ctx, Task task) {
	}

	@Override
	public void executeOnResult(T ctx, Task task, TaskResult result) {
	}

	@Override
	public TaskResultEnum executeOnCompleted(T ctx,
			Map<String, TaskResult> results) {
		return null;
	}

}
