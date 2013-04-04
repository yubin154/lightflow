/*
 * Created on Dec 13, 2005
 */
package org.lightj.dal;

import java.util.Collection;
import java.util.List;

import org.lightj.util.StringUtil;


/**
 * @author biyu
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
@SuppressWarnings({"rawtypes","unchecked"})
public class Query extends AbstractQuery {
	
	public static final Query EMPTY = new Query();
	public static final String START_PAREN = " (";
	public static final String END_PAREN = ") ";
	protected int top = 0;
	protected int fetchSize = 0;
	
	/**
	 * constructor
	 */
	public Query() { super(); }

	// select expressions	
	public Query select(String column, String alias) {
		if (columns.length() != 0) columns.append(",\n");
		columns.append(column);
		if (!StringUtil.isNullOrEmpty(alias)) {
			columns.append(" as ").append(alias);
		}
		return this;
	}

	public Query select(String column) {
		return select(column, null);
	}

	// from expressions
	public Query from(String table, String alias) {
		if (tables.length() != 0) tables.append(" ,\n");
		tables.append(table);
		if (!StringUtil.isNullOrEmpty(alias)) {
			tables.append(" ").append(alias);
		}
		return this;
	}

	public Query from(String table) {
		return from(table, null);
	}
	
	public Query from(AbstractQuery statement, String alias) {
		if (tables.length() != 0) tables.append(" ,\n");
		tables.append("(" + statement.toString() + ")");
		if (!StringUtil.isNullOrEmpty(alias)) {
			tables.append(" ").append(alias);
		}
		return this;
	}
	
	/**
	 * this call will take pass in clause as it. 
	 * eg. or("t1.col1 = 4")
	 * eg. or("t1.col1 = t2.col2")
	 * 
	 * @param s
	 * @return
	 */
	public Query or(String s) {
		if (StringUtil.isNullOrEmpty(s)) return this;
		if (whereExpression.length() != 0 && ((whereExpression.indexOf(START_PAREN) + START_PAREN.length()) != whereExpression.length()) ) {
			if (s.length() != 0 )
				where(" or ");
		}
		where(s);
		return this;
	}
	
	/**
	 * this call shall be used ONLY when doing queries with real parameters,
	 * and the number of question marks have to be the same as the parameters
	 * eg. or("t1.col1 in (?,?,?)", List<Integer>)
	 * eg. or("t1.col1 = ", 4)
	 * 
	 * @param s
	 * @param arg
	 * @return
	 */
	public Query or(String s, Object arg) {
		if (StringUtil.isNullOrEmpty(s)) return this;
		if (arg instanceof Collection) {
			if (s.indexOf('?') < 0 && ((Collection) arg).size() > 0) throw new IllegalArgumentException("Unmatched argument");
			args.addAll((Collection) arg);
		}
		else {
			if (s.indexOf('?') < 0) throw new IllegalArgumentException("Unmatched argument");
			args.add(arg);
		}
		or(" ").where(s);
		return this;
	}
	
	/**
	 * this call shall be used ONLY when doing queries with real parameters,
	 * for join clause, please use {@link #and(String)}
	 * eg. or("t1.col1", "=", 4) - it's ok
	 * eg. or("t1.col1", "=", "t2.col2") - its wrong
	 * eg. or("t1.col1 = t2.col2") - it's ok
	 * 
	 * @param lVal
	 * @param operator
	 * @param arg
	 * @return
	 */
	public Query or(String lVal, String operator, Object arg) {
		if ("IN".equalsIgnoreCase(operator)) {
			if (arg instanceof List) {
				List list = (List) arg;

				// if no value in arguments, then no result will match the whole
				// OR expression
				if (list.isEmpty()) {
					or("1=0"); // it's a false condition
				} else {
					or(" ").in(lVal, (List) arg);
				}
			} else {
				throw new IllegalArgumentException(
						"only List is supported as argument for IN operator");
			}
		} else {
			args.add(arg);
			or(" ").where(lVal).where(operator).where("?\n");
		}
		
		return this;
	}
	
	/**
	 * this call will take pass in clause as it. 
	 * eg. and("t1.col1 = 4")
	 * eg. and("t1.col1 = t2.col2")
	 * 
	 * @param s
	 * @return
	 */
	public Query and(String s) {
		if (StringUtil.isNullOrEmpty(s)) return this;
		if (whereExpression.length() != 0 && ((whereExpression.indexOf(START_PAREN) + START_PAREN.length()) != whereExpression.length())) {
			if (s.length() != 0)
				where(" and ");
		}
		where(s);
		return this;
	}
	
	/**
	 * this call shall be used ONLY when doing queries with real parameters,
	 * and the number of question marks have to be the same as the parameters
	 * eg. and("t1.col1 in (?,?,?)", List<Integer>)
	 * eg. and("t1.col1 = ", 4)
	 * 
	 * @param s
	 * @param arg
	 * @return
	 */
	public Query and(String s, Object arg) {
		if (StringUtil.isNullOrEmpty(s)) return this;
		if (arg instanceof Collection) {
			if (s.indexOf('?') < 0 && ((Collection) arg).size() > 0) throw new IllegalArgumentException("Unmatched argument");
			args.addAll((Collection) arg);
		}
		else {
			if (s.indexOf('?') < 0) throw new IllegalArgumentException("Unmatched argument");
			args.add(arg);
		}
		and(" ").where(s);
		return this;
	}
	
	/**
	 * this call shall be used ONLY when doing queries with real parameters,
	 * for join clause, please use {@link #and(String)}
	 * eg. and("t1.col1", "=", 4) - it's ok
	 * eg. and("t1.col1", "=", "t2.col2") - its wrong
	 * eg. and("t1.col1 = t2.col2") - it's ok
	 * 
	 * @param lVal
	 * @param operator
	 * @param arg
	 * @return
	 */
	public Query and(String lVal, String operator, Object arg) {
		if ("IN".equalsIgnoreCase(operator)) {
			if (arg instanceof List) {
				List list = (List) arg;

				// if no value in arguments, then no result will match the whole
				// AND expression
				if (list.isEmpty()) {
					and("1=0"); // it's a false condition
				} else {
					and(" ").in(lVal, (List) arg);
				}
			} else {
				throw new IllegalArgumentException(
						"only List is supported as argument for IN operator");
			}
		} else {
			args.add(arg);
			and(" ").where(lVal).where(operator).where("?");
		}
		
		return this;
	}
	
	/**
	 * This method shall be used ONLY when doing queries with IN clause
	 * 
	 * eg. andIn("t1.col1", List of ids (1,2,3,4))
	 * 
	 * @param lVal
	 * @param ids
	 * @return
	 */
	private Query in(String lVal, List ids) {
		StringBuffer sb = new StringBuffer(256);
		int size = ids.size();

		for (int i = 0; i < size; i++) {
			if (i > 0) {
				sb.append(',');
			}

			sb.append('?');
			args.add(ids.get(i));
		}

		if (sb.length() > 0) {
			where(lVal).where(" IN ").where("(" + sb.toString() + ") ");
		}

		return this;
	}
	
	/**
	 * This method will start a Parent [ "(" ] in your query. 
	 * If you add another or /and statement after this call it will not append "or".
	 * Important: You should manually close the paren in your query.
	 * @return
	 */
	public Query startParen(){
		where(START_PAREN);
		return this;
	}

	/**
	 * Closes the paren if you have started with startParen call.
	 * In future we can easily add an exception so that endParen will not do anything if there was no startParen.
	 * 
	 * @return
	 */
	public Query endParen(){
		where(END_PAREN);
		return this;
	}

	// union
	protected void unionAppend(String s) {
		if (union == null) {
			union = new StringBuffer();
		}
		union.append(s);
	}
	
	public Query unionAll(Query query) {
		unionAppend("UNION ALL ");
		unionAppend(query.toString());
		this.args.addAll(query.getArgs());
		return this;
	}
	
	public Query union(Query query) {
		unionAppend("UNION ");
		unionAppend(query.toString());
		this.args.addAll(query.getArgs());
		return this;
	}
	
	// group by
	public Query groupBy(String groupBy) {
		if (this.groupBy == null)
			this.groupBy = new StringBuffer();
		else if (groupBy.length() != 0)
			this.groupBy.append(", ");
		this.groupBy.append(groupBy);
		return this;
	}
	
	// order by
	public Query orderBy(String orderBy) {
		if (this.orderBy == null)
			this.orderBy = new StringBuffer();
		else if (orderBy.length() != 0)
			this.orderBy.append(", ");
		this.orderBy.append(orderBy);
		return this;
	}

	/**
	 * make a deep copy of Query
	 */
	public Object clone() {
		Query copy = new Query();
		super.clone(copy);
		return copy;
	}

	/**
	 * @return
	 */
	public int getTop() {
		return top;
	}

	public Query setTop(int count) {
		this.top = count;
		return this;
	}

	public void clearSelect() {
		this.columns = new StringBuffer("");
		
	}
	public void clearOrderBy() {
		this.orderBy= null;
		
	}
	
	public int getFetchSize() {
		return fetchSize;
	}

	public Query setFetchSize(int fetchSize) {
		this.fetchSize = fetchSize;
		return this;
	}
	
}
