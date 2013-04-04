package org.lightj.initialization;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * @author biyu
 */
public class ShutdownException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5411255701634923759L;
	final Collection<Exception> m_exceptions;

	public ShutdownException(final Collection<Exception> exceptions) {
		super("Shutdown sequence has failed");
		m_exceptions = exceptions;
	}

	public ShutdownException(Exception exception) {
		super("Shutdown sequence has failed");
		m_exceptions = new ArrayList<Exception>();
		m_exceptions.add(exception);
	}

	Collection<Exception> getExceptions() {
		return m_exceptions;
	}

	public void printStackTrace() {
		printStackTrace(System.err);
	}

	public void printStackTrace(PrintStream ps) {
		synchronized (ps) {
			ps.println(this);
			String stackTrace = getStackTraceX();
			ps.print(stackTrace);
		}
	}

	public void printStackTrace(PrintWriter pw) {
		synchronized (pw) {
			pw.println(this);
			String stackTrace = getStackTraceX();
			pw.print(stackTrace);
		}
	}

	private String getStackTraceX() {
		StringBuffer buf = new StringBuffer();
		buf.append("The following shutdown exceptions occured:\n");

		int i = 1;
		Iterator<Exception> iter = m_exceptions.iterator();
		while (iter.hasNext()) {
			RuntimeException re = (RuntimeException)iter.next();

			StringWriter buf2 = new StringWriter();
			PrintWriter pout = new PrintWriter(buf2);
			re.printStackTrace(pout);
			pout.flush();

			String message = buf2.toString();
			buf.append("Exception # " + i + ":\r\n");
			buf.append(message);
			i++;
		}
		String stackTrace = buf.toString();
		return stackTrace;
	}
}
