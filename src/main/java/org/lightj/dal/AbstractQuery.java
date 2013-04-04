/*
 * Created on Dec 15, 2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.lightj.dal;

import java.util.ArrayList;
import java.util.List;

/**
 * @author biyu
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 * add some lines to test delta coverage
 * add some lines to test delta coverage
 * add some lines to test delta coverage
 * add some lines to test delta coverage
 * add some lines to test delta coverage
 */

@SuppressWarnings({"rawtypes","unchecked"})
public abstract class AbstractQuery {

	/**
	 * internal members
	 */
	protected StringBuffer tables = new StringBuffer();
	protected StringBuffer columns = new StringBuffer();
	protected StringBuffer whereExpression = new StringBuffer();
	protected StringBuffer union = null;
	protected StringBuffer groupBy = null;
	protected StringBuffer orderBy = null;

	protected List<Object> args;

	public AbstractQuery() {args = new ArrayList<Object>();}

	protected AbstractQuery where(String s) {
		whereExpression.append(s);
		return this;
	}

	public List getArgs() {
		return args;
	}

	/**
	 * beautify query string
	 * @param result
	 * @param s
	 * @param prefix
	 */
	protected static void indent(
		StringBuffer result,
		String s,
		String prefix) {
		int index = s.indexOf('\n');
		if (index == -1) {
			result.append(prefix).append(s).append(" ");
			return;
		}

		char[] chars = s.toCharArray();
		int prevIndex = 0;
		do {
			result.append(prefix).append(
				chars,
				prevIndex,
				index - prevIndex).append(
				" ");
			prevIndex = index + 1;
		} while ((index = s.indexOf('\n', prevIndex)) != -1);

		result.append(prefix).append(
			chars,
			prevIndex,
			chars.length - prevIndex).append(
			" ");
	}

	/**
	 * Generate query string with argument values replaced ? for debuging output
	 * @return
	 */
	public String debugString() {
		String statement = (tables.length() > 0) ? toString() : daoString();
		StringBuffer parms = new StringBuffer();
		for (int i = 0, size = args.size(); i < size; i++) {
			parms.append(args.get(i) != null ? args.get(i).toString() : "null").append(',');
		}
//		for (int i = 0, size = args.size(); i < size; i++) {
//			// not very efficient but ok
//			statement = statement.replaceFirst("\\?", (args.get(i) != null ? args.get(i).toString() : "null"));
//		}
		return statement + ":" + parms.toString();
	}

	/**
	 * Generate the whole query string
	 */
	public String toString() {
		StringBuffer statement = new StringBuffer();
		statement.append("SELECT ");
		indent(statement, columns.toString(), " ");
		statement.append(" FROM ");
		indent(statement, tables.toString(), " ");
		statement.append(daoString());
		return statement.toString();
	}

	/**
	 * Generate query string without SELECT and FROM clause
	 * @return
	 */
	public String daoString() {
		StringBuffer statement = new StringBuffer();
		if (whereExpression.length() != 0) {
			statement.append(" WHERE ");
			indent(statement, whereExpression.toString(), " ");
		}
		if (union != null) {
			statement.append(" ");
			statement.append(union);
		}
		if (groupBy != null) {
			statement.append(" GROUP BY ");
			statement.append(groupBy);
		}
		if (orderBy != null) {
			statement.append(" ORDER BY ");
			statement.append(orderBy);
		}
		return statement.toString();
	}

	/**
	 * everything after where and before group by/order by
	 * @return
	 */
	public String subqueryDaoString() {
		StringBuffer statement = new StringBuffer();
		if (whereExpression.length() != 0) {
			indent(statement, whereExpression.toString(), " ");
		}
		return statement.toString();
	}

	public void clone(AbstractQuery copy) {
		if (this.tables != null)
			copy.tables = new StringBuffer(this.tables.toString());
		if (this.columns != null)
			copy.columns = new StringBuffer(this.columns.toString());
		if (this.whereExpression != null)
			copy.whereExpression =
				new StringBuffer(this.whereExpression.toString());
		if (this.union != null)
			copy.union = new StringBuffer(this.union.toString());
		if (this.groupBy != null)
			copy.groupBy = new StringBuffer(this.groupBy.toString());
		if (this.orderBy != null)
			copy.orderBy = new StringBuffer(this.orderBy.toString());
		if(this.args != null){
			copy.args = new ArrayList<Object>(this.getArgs());
		}
	}

	public boolean isFullQuery() {
		return (tables.length() > 0 && columns.length() > 0);
	}

	public void addArg(int idx, Object arg) {
		if (idx < 0 || idx > args.size()) {
			args.add(arg);
		}
		else {
			args.add(idx, arg);
		}
	}

}
