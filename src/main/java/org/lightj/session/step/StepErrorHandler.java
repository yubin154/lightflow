package org.lightj.session.step;

import java.util.LinkedHashMap;
import java.util.Map;

import org.lightj.session.FlowContext;
import org.lightj.session.FlowExecutionException;
import org.lightj.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An execution aspect of a flow step for error handling
 * 
 * @author biyu
 *
 */
@SuppressWarnings("rawtypes")
public class StepErrorHandler<T extends FlowContext> extends StepExecution<T> {
	
	static final Logger logger = LoggerFactory.getLogger(StepErrorHandler.class);

	public StepErrorHandler(String stepName) {
		super(StepTransition.runToStep(stepName));
	}

	public StepErrorHandler(Enum stepName) {
		super(StepTransition.runToStep(stepName));
	}

	public StepErrorHandler(StepTransition transition) {
		super(transition);
	}

	/**
	 * trigger error 
	 */
	protected Throwable t;

	@Override
	public StepTransition execute() throws FlowExecutionException {
		return executeOnError(t);
	}
	
	/**
	 * set triggering error
	 * @param t
	 */
	public final void setError(Throwable t) {
		this.t = t;
	}
	
	/**
	 * map an error type to a result
	 */
	protected Map<Class<? extends Throwable>, StepTransition> errorClass2ResultMap = new LinkedHashMap<Class<? extends Throwable>, StepTransition>();

	/**
	 * register an error class to a result
	 * @param errorKlass
	 * @param result
	 */
	public void mapErrorTo(Class<? extends Throwable> errorKlass, StepTransition result) {
		errorClass2ResultMap.put(errorKlass, result);
	}
	
	/**
	 * register an error class to a result
	 * @param errorKlass
	 * @param result
	 */
	public void mapErrorsTo(Map<Class<? extends Throwable>, StepTransition> errorClass2ResultMap) {
		this.errorClass2ResultMap.putAll(errorClass2ResultMap);
	}

	/**
	 * override this method for custom logic
	 * @param t
	 * @return
	 */
	public StepTransition executeOnError(Throwable t) {
		Throwable th = null;
		StepTransition tran = defResult;

		try {

			this.sessionContext.saveFlowError(this.flowStep.getStepId(), StringUtil.getStackTrace(t, 2000));
			if (t instanceof FlowExecutionException && t.getCause() != null) {
				// checked exception
				th = t.getCause();
			}
			else {
				// runtime exception
				th = t;
			}
			
			Class<? extends Throwable> errorKlass = th.getClass();
			for (Class<? extends Throwable> mappedKlass : errorClass2ResultMap.keySet()) {
				if (mappedKlass.isAssignableFrom(errorKlass)) {
					tran = errorClass2ResultMap.get(mappedKlass);
					break;
				}
			}

			tran.log(th.getMessage(), StringUtil.getStackTrace(th, 2000));

		}
		catch (Throwable t1) {
			logger.error("failed to handle flow error", t1);
		}
		return tran;
	}

	/**
	 * convenient method to create handler
	 * @return
	 */
	public static StepErrorHandler onException(String step) {
		StepErrorHandler handler = new StepErrorHandler(StepTransition.runToStep(step));
		return handler;
	}
	
	/**
	 * convenient method to create handler
	 * @return
	 */
	public static StepErrorHandler onException(Enum step) {
		StepErrorHandler handler = new StepErrorHandler(StepTransition.runToStep(step));
		return handler;
	}

}
