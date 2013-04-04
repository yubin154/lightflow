/*
 * Created on Jan 4, 2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.lightj.util;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @author biyu
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public final class DateUtil {
	
	static String[] QUARTERS = { "Q1", "Q2", "Q3", "Q4" };
	static SimpleDateFormat fmt = new SimpleDateFormat("EEE MMM dd hh:mm:ss z yyyy");
	static String defaultdateFormat = "MM/dd/yyyy";
	final static long MILLIS_IN_A_DAY = 24 * 60 * 60 * 1000;
	final static long ONE_SECOND = 1000;
	final static long SECONDS = 60;
	final static long ONE_MINUTE = ONE_SECOND * 60;
	final static long MINUTES = 60;
	final static long ONE_HOUR = ONE_MINUTE * 60;
	final static long HOURS = 24;
	final static long ONE_DAY = ONE_HOUR * 24;

	/**
	 * converts time (in milliseconds) to human-readable format "<w> days, <x>
	 * hours, <y> minutes and (z) seconds"
	 */
	public static String millisToLongDHMS(long duration, boolean includeSecondSmart) {
		StringBuffer res = new StringBuffer();
		long temp = 0;
		if (duration >= ONE_SECOND) {
			temp = duration / ONE_DAY;
			if (temp > 0) {
				duration -= temp * ONE_DAY;
				res.append(temp).append(" Day").append(temp > 1 ? "s" : "").append(duration >= ONE_MINUTE ? " " : "");
			}

			temp = duration / ONE_HOUR;
			if (temp > 0) {
				duration -= temp * ONE_HOUR;
				res.append(temp).append(" Hr").append(temp > 1 ? "s" : "").append(duration >= ONE_MINUTE ? " " : "");
			}

			temp = duration / ONE_MINUTE;
			if (temp > 0) {
				duration -= temp * ONE_MINUTE;
				res.append(temp).append(" Min").append(temp > 1 ? "s" : "");
			}

			if (res.length()==0 || includeSecondSmart) {
				temp = duration / ONE_SECOND;
				if (temp > 0) {
					res.append(" ").append(temp).append(" Sec").append(temp > 1 ? "s" : "");
				}
			}
			return res.toString();
		} else {
			return "0 Sec";
		}
	}

	/**
	 * converts time (in milliseconds) to human-readable format "<dd:>hh:mm:ss"
	 */
	public static String millisToShortDHMS(long duration, boolean includeSecondSmart) {
		String res = "";
		duration /= ONE_SECOND;
		int seconds = (int) (duration % SECONDS);
		duration /= SECONDS;
		int minutes = (int) (duration % MINUTES);
		duration /= MINUTES;
		int hours = (int) (duration % HOURS);
		int days = (int) (duration / HOURS);
		if (days == 0) {
			res = String.format("%02d:%02d:%02d", hours, minutes, seconds);
		} else {
			res = String.format("%dd%02d:%02d:%02d", days, hours, minutes,
					seconds);
		}
		return res;
	}

	/**
	 * converts time (in milliseconds) to human-readable format 
	 * "<x> hours, <y> minutes and (z) seconds"
	 */
	public static String millisToHour(long duration, String format) {
		DecimalFormat f = new DecimalFormat(format!=null ? format : "#.##");
		return f.format(((double)duration)/(60 * 60 * 1000L));
	}

	/**
	 * compare two dates
	 * 
	 * @param d1
	 * @param d2
	 * @param treatNullAsMin
	 * @return
	 */
	public static final Date min(Date d1, Date d2, boolean treatNullAsMin) {
		if (d1 == null) {
			return treatNullAsMin ? d1 : d2;
		}
		if (d2 == null) {
			return treatNullAsMin ? d2 : d1;
		}
		return (d1.compareTo(d2) < 0) ? d1 : d2; 
	}
	
	/**
	 * compare two dates
	 * @param d1
	 * @param d2
	 * @param treatNullAsMax
	 * @return
	 */
	public static final Date max(Date d1, Date d2, boolean treatNullAsMax) {
		if (d1 == null) {
			return treatNullAsMax ? d1 : d2;
		}
		if (d2 == null) {
			return treatNullAsMax ? d2 : d1;
		}
		return (d1.compareTo(d2) >= 0) ? d1 : d2;
	}
	
	/**
	 * Trim the date
	 * @param d
	 * @param startPos
	 */
	public static final void trim(Date d, int startPos) {
		if (d == null) return;
		Calendar cal = Calendar.getInstance();
		cal.setTime(d);
		if (Calendar.MONTH >= startPos) {
			cal.set(Calendar.MONTH, Calendar.JANUARY);
		}
		if (Calendar.DAY_OF_MONTH >= startPos) {
			cal.set(Calendar.DAY_OF_MONTH, 1);
		}
		if (Calendar.HOUR_OF_DAY >= startPos) {
			cal.set(Calendar.HOUR_OF_DAY, 0);
		}
		if (Calendar.MINUTE >= startPos) {
			cal.set(Calendar.MINUTE, 0);
		}
		if (Calendar.SECOND >= startPos) {
			cal.set(Calendar.SECOND, 0);
		}
		if (Calendar.MILLISECOND >= startPos) {
			cal.set(Calendar.MILLISECOND, 0);
		}
		/* Fix for FindBugs Violation              
		 * Violation Name: DLS_DEAD_LOCAL_STORE
		 * Category: STYLE
		 */
		//d = cal.getTime();
	}
	
	public static final void resetTimer() {
		long[] values = timer.get();
		values[1] = System.currentTimeMillis();
		values[0] = System.currentTimeMillis();
		timer.set(values);
	}
	
	public static final void setCheckpoint() {
		timer.set(System.currentTimeMillis());
	}
	
	public static final long getDurationMs(boolean fromEpoch) {
		long now = System.currentTimeMillis();
		long delta = now - timer.get(fromEpoch);
		timer.set(now);
		return delta;
	}
	
	private static class ThreadLocalTimer extends ThreadLocal<long[]> {
		public synchronized long[] initialValue() {
			return new long[] {System.currentTimeMillis(), System.currentTimeMillis()};
		}
	    public void set(long checkpoint) {
	    	long[] values = get();
	    	values[1] = checkpoint;
	    	set(values);
	    }
	    public long get(boolean fromEpoch) {
	    	return fromEpoch ? get()[0] : get()[1];
	    }

	}
	
	private static ThreadLocalTimer timer = new ThreadLocalTimer();
	
	/**
	 * generate quarter strings for a year
	 * @param year
	 * @return
	 */
	public static String[] getAllQuartersByYear(int year) {
		String[] rst = new String[QUARTERS.length];
		for (int i = 0, len = rst.length; i < len; i++) {
			rst[i] = QUARTERS[i] + '_' + Integer.toString(year);
		}
		return rst;
	}
	
	/**
	 * give quarter name, return its start and end dates
	 * @param quarter
	 * @return
	 */
	public static Calendar[] getDateRangeForQuarter(String quarter) {
		Calendar startDate = Calendar.getInstance();
		Calendar endDate = Calendar.getInstance();
		int q = Integer.parseInt(quarter.substring(1,2));
		int y = Integer.parseInt(quarter.substring(3,7));
		startDate.set(Calendar.YEAR, y);
		startDate.set(Calendar.MONTH, ((q-1)*3));
		startDate.set(Calendar.DAY_OF_MONTH, 1);
		startDate.set(Calendar.HOUR, 0);
		startDate.set(Calendar.MINUTE, 0);
		startDate.set(Calendar.SECOND, 0);
		startDate.set(Calendar.MILLISECOND, 0);
		endDate.setTime(startDate.getTime());
		endDate.add(Calendar.MONTH, 3);
		endDate.add(Calendar.SECOND, -1);
		return new Calendar[] {startDate, endDate};
	}
	
	/**
	 * @param date
	 * @param format
	 * @return
	 */
	public static String format(Date date,String format) {
		SimpleDateFormat datefmt = new SimpleDateFormat("MM/dd/yyyy"); 
		if (format != null){		
			datefmt = new SimpleDateFormat(format);			
		}
		return (date != null)? datefmt.format(date):"";		
	}

	/**
	 * format it to EEE MMM dd hh:mm:ss z yyyy
	 * @param date
	 * @return
	 */
	public static String format(Date date) {
		if (date != null) {
			return fmt.format(date);
		}
		else {
			return "";
		}
	}

	public static String formatUTC(Date date) {
		if (date != null)
			return String.valueOf(date.getTime());
		else 
			return "";
	}
	
	/**
	 * Method to parse the given date string for th given format 
	 * @param String that needs to be parsed
	 * @return Date that is parsed 
	 * 
	 */
	public static Date parse(String date, String format) {
		Date formattedDate = null;
		format = (format == null)?defaultdateFormat:format;		
		DateFormat formatter = new SimpleDateFormat(format);
		
		try {
			if (date != null){
				formattedDate = formatter.parse(date);
			}
		} catch (ParseException e) {
		}
		return formattedDate;
	}
	
	/**
	 * Returns number of days passed since today. This method will return positive values
	 * for past dates and negative values for future dates.
	 * 
	 * Best suited for doing oracle queries where date > sysdate - 30.
	 * 
	 * @param startDate
	 * @return
	 */
	public static long getNumberOfDaysDifferentialFromNow(Date startDate){
		long differenceInMillis = System.currentTimeMillis() - startDate.getTime();
		
		return (differenceInMillis/(MILLIS_IN_A_DAY));
	}
	
	/**
	 * 
	 * @param monthInYear
	 * @param year
	 * @return
	 */
	public static Calendar[] getDateRangeForMonth(int monthInYear, int year) {
		Calendar startDate = Calendar.getInstance();
		Calendar endDate = Calendar.getInstance();
		startDate.set(Calendar.YEAR, year);
		startDate.set(Calendar.MONTH, monthInYear);
		startDate.set(Calendar.DATE, 1);
		endDate = (Calendar) startDate.clone();
		endDate.set(Calendar.MONTH, monthInYear+1);
		endDate.set(Calendar.DATE, endDate.get(Calendar.DATE) - 1);
		return new Calendar[] {startDate, endDate};
	}
	
	/**
	 * 
	 * @param weekInMonth
	 * @param monthInYear
	 * @param year
	 * @return
	 */
	public static Calendar[] getDateRangeForWeek(int weekInYear, int year) {
		Calendar startDate = Calendar.getInstance();
		Calendar endDate = Calendar.getInstance();
		startDate.set(Calendar.YEAR, year);
		startDate.set(Calendar.WEEK_OF_YEAR, weekInYear);
		startDate.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		endDate = (Calendar) startDate.clone();
		endDate.set(Calendar.DATE, endDate.get(Calendar.DATE) + 6);
		return new Calendar[] {startDate, endDate};
	}
	
	/**
	 * Checks if the supplied date string is of valid numeric date format
	 * Supported formats - dd/MM/yyyy, dd.MM.yyyy, dd-MM-yyyy, MM/dd/yyyy, 
	 * 		MM.dd.yyyy, MM-dd-yyyy
	 *
	 * @param dateFormat
	 * @param dateString
	 * @param delimitter
	 * @param validateDMYLength
	 * @return
	 */
	public static boolean isValidDateFormat(String dateFormat, String dateString, 
			String delimitter, boolean validateDMYLength) {
		
		if(StringUtil.isNullOrEmptyAfterTrim(dateString) 
				|| StringUtil.isNullOrEmptyAfterTrim(delimitter)
				|| StringUtil.isNullOrEmptyAfterTrim(dateFormat)){
			return false;
		}
		
		// Just a safety repair for '.' as it cannot be directly used without
		// the escape sequence to string tokenize
		if(delimitter.equals(".")){
			delimitter = "\\.";
		}
		
		// Check if the length of D, M & Y are within limits as 
		// per the format specified
		int[] formatTokenSize = null;
		int i = 0;
		if(validateDMYLength){
			String[] formatTokens = dateFormat.split(delimitter);
			if(formatTokens != null && formatTokens.length > 0){
				formatTokenSize = new int[formatTokens.length];
				for(String formatToken : formatTokens){
					formatTokenSize[i++] = formatToken.length();
				}
			}else{
				return false;
			}
		}
		
		// Check if D, M & Y are numerics and within specified length
		i = 0;
		String[] tokens = dateString.split(delimitter);
		for(String token : tokens){
			try{
				Integer.parseInt(token.trim());
				if(validateDMYLength && token.trim().length() > formatTokenSize[i++]){
					return false;
				}
			}catch(Exception e){
				return false;
			}
		}
		
		// Parse the string for the date format specified
		return parse(dateString, dateFormat) == null ? false:true;
	}
	
	/**
	 * Convert the milliseconds to division of Hours/Mins/Seconds
	 * 
	 * @param timeInMilliSeconds
	 * @return String
	 */
	public static String calculateTimeElapsed(long timeInMilliSeconds) {
	      long timeInSeconds,hours, minutes, seconds;
	      timeInSeconds = timeInMilliSeconds/1000;
	      hours = timeInSeconds / 3600;
	      timeInSeconds = timeInSeconds - (hours * 3600);
	      minutes = timeInSeconds / 60;
	      timeInSeconds = timeInSeconds - (minutes * 60);
	      seconds = timeInSeconds;
	      return String.valueOf(hours + " hour(s) " + minutes + " minute(s) " + seconds + " second(s)");
	}
	
	/**
	 * parse data from date control 
	 * @param startDate
	 * @param dRange
	 * @return
	 */
	public static Calendar[] getDateRange(Date startDate, int dRange) {
		Calendar start = Calendar.getInstance();
		start.setTime(startDate);
		Calendar end = Calendar.getInstance();
		end.setTime(startDate);
		switch(dRange) {
		case 1: // day
			end.add(Calendar.DAY_OF_YEAR, 1);
			break;
		case 2: // week: special case, start date is actually the end date
			start.add(Calendar.WEEK_OF_YEAR, -1);
			break;
		case 3: // month
			end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
			break;
		case 4: // quarter
			end.add(Calendar.MONTH, 3);
			break;
		case 5: // year
			end.add(Calendar.YEAR, 1);
			break;
		default:
			break;
		}
		return new Calendar[] {start, end};
	}
	
	/**
	 * utitlity to convert html calendar control to a date range
	 * @param startDate
	 * @param dateRange
	 * @return
	 */
	public static Calendar[] dateRange(Date startDate, int dateRange) {
		if (startDate == null) {
			throw new RuntimeException("StartDate is null");
		}
		Calendar start = Calendar.getInstance();
		start.setTime(startDate);
		Calendar end = Calendar.getInstance();
		end.setTime(startDate);
		switch(dateRange) {
		case 1: // day
			end.add(Calendar.DAY_OF_YEAR, 1);
			break;
		case 2: // week: special case, start date is actually the end date
			start.add(Calendar.WEEK_OF_YEAR, -1);
			break;
		case 3: // month
			end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
			break;
		case 4: // quarter
			end.add(Calendar.MONTH, 3);
			break;
		case 5: // year
			end.add(Calendar.YEAR, 1);
			break;
		default:
			break;
		}
 		return new Calendar[] { start, end };
	}
	
	/**
	 * if 2 dates are equal
	 */
	public static boolean equals(Date src, Date target) {
		return (src==target) ? true : ((src!=null && target!=null) ? src.equals(target) : false);
	}
}
