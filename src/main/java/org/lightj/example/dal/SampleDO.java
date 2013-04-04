package org.lightj.example.dal;

import java.io.Serializable;
import java.util.Date;

import org.lightj.dal.BaseSequenceEnum;
import org.lightj.dal.IData;


/**
 * @author biyu
 * sample DO
 */
public class SampleDO implements IData {
	
	public static final String TABLENAME = "test_dbframework";
	public static final BaseSequenceEnum SEQNAME = SampleSequenceEnum.TEST_DBFRAMEWORK;
	
	private long id;
	private String colVc;
	private long colLong;
	private double colFloat;
	private Date colDate;
	private String colClob;
	private Serializable colBlob;
	
	/////////// IData Interface //////////
	public long getPrimaryKey() {
		return id;
	}

	public Serializable getColBlob() {
		return colBlob;
	}
	public void setColBlob(Serializable colBlob) {
		this.colBlob = colBlob;
	}
	public String getColClob() {
		return colClob;
	}
	public void setColClob(String colClob) {
		this.colClob = colClob;
	}
	public Date getColDate() {
		return colDate;
	}
	public double getColFloat() {
		return colFloat;
	}
	public long getColLong() {
		return colLong;
	}
	public String getColVc() {
		return colVc;
	}
	public long getId() {
		return id;
	}
	public void setColDate(Date timestamp) {
		colDate = timestamp;
	}
	public void setColFloat(double d) {
		colFloat = d;
	}
	public void setColLong(long l) {
		colLong = l;
	}
	public void setColVc(String string) {
		colVc = string;
	}
	public void setId(long i) {
		id = i;
	}

}
