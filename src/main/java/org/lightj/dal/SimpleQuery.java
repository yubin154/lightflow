/*
 * Created on Dec 15, 2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.lightj.dal;

import org.lightj.util.StringUtil;

/**
 * @author biyu
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class SimpleQuery extends AbstractQuery {

	public static final SimpleQuery EMPTY = new SimpleQuery();
	private static final String PREFIX = "_TABLE_PREFIX_";	
	private String table;
	
	public SimpleQuery() {
		super();
	}
	
	public SimpleQuery select(String column, String colAlias) {
		if (columns.length() != 0) columns.append(",\n");
		columns.append(PREFIX).append('.').append(column);
		if (!StringUtil.isNullOrEmpty(colAlias)) {
			columns.append(" as ").append(colAlias);
		}
		return this;
	}
	
	public SimpleQuery from(String table) {
		if (tables.length() != 0) throw new RuntimeException("SimpleQuery can have only one table");
		this.table = table;
		tables.append(table).append(" ").append(PREFIX);
		return this;
	}

	public SimpleQuery and(String column, String operator, Object arg) {
		args.add(arg);
		if (whereExpression.length() != 0) {
			where(" and ");
		}
		where(PREFIX).where(".").where(column).where(operator).where("?");
		return this;
	}
	
	public SimpleQuery and(String column, String rVal) {
		if (whereExpression.length() != 0) {
			where(" and ");
		}
		where(PREFIX).where(".").where(column).where(rVal);
		return this;
	}
	
	public String whereString(String alias) {
		if (StringUtil.isNullOrEmpty(alias)) alias = table;
		return whereExpression.toString().replaceAll("_TABLE_PREFIX_", alias);
	}
}
