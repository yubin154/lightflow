package org.lightj.task;

import java.util.List;

import org.lightj.session.FlowContext;


/**
 * group task, can be single or a batch of tasks
 * @author binyu
 *
 */
public interface IGroupTask<T extends FlowContext> {

	/**
	 * get individual tasks if is batch
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public List<? extends Task> getTasks(T ctx);
	
}
