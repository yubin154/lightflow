package org.lightj.task.asynchttp;

import java.util.List;

import org.lightj.session.FlowContext;
import org.lightj.task.ExecutableTask;
import org.lightj.task.IGroupTask;

@SuppressWarnings("rawtypes")
public abstract class GroupHttpTask<T extends FlowContext, H extends SimpleHttpTask> extends ExecutableTask<T> implements IGroupTask<T> {

	@Override
	public abstract List<H> getTasks(T context);
	
}
