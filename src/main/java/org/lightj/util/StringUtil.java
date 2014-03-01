/*
 * Created on Mar 12, 2004
 *
 * This file should not include any file in ICE.  It is a utility file!
 * All utility files should be static!  All methods in this file should
 * rever to nothing but their own local variables.
 */
package org.lightj.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author biyu
 */
@SuppressWarnings({"rawtypes","unchecked"})
public class StringUtil {

	private static final String UTF8_ENCODING_TYPE = "UTF-8";
	
	static String quote(String src)	{
		return "\'" + src + "\'";
	}

	public static String genUuid() {
		return UUID.randomUUID().toString();
	}
	
	/**
	 * Splits a string containing separators into an arraylist.  By default does not generate empty or null 
	 * values.  
	 * For example 
	 * "foo;;bar;baz" -> ("foo", "bar", "baz") not ("foo", "", "bar", "baz") 
	 */
	public static List<String> split (String input, String separator) {
		if (input == null) return Collections.emptyList();
		else return Arrays.asList(input.split(separator));
	}

	/**
	 * convert map to a string
	 * @param map
	 * @param separator
	 * @return
	 */
	public static String join(Map map, String separator) {
		StringBuffer rtn = new StringBuffer();
		if (map != null) {
			Iterator istrings = map.keySet().iterator();
			// first one has no separator 
			while (istrings.hasNext()) {
				Object k = istrings.next();
				if (k!=null) rtn.append(k).append('=');
				Object v = map.get(k);
				if (v!=null) rtn.append(v);
				rtn.append(separator);
			}
			if (rtn.length()>0) rtn.deleteCharAt(rtn.length()-1);
		}
		return (rtn.toString());
	}
	
	/**
	 * Combines a ArrayList of strings into a string separated by the delimiter
	 */
	public static String join (Collection strings, String separator) {
		StringBuffer rtn = new StringBuffer();
		if (strings != null) {
			Iterator istrings = strings.iterator();
			// first one has no separator 
			if (istrings.hasNext()) {
				rtn.append(istrings.next());
			}
			while (istrings.hasNext()) {
				rtn.append(separator + istrings.next());
			}
		}
		return (rtn.toString());
	}
	
	/**
	 * overloading join
	 * @param strings
	 * @param separator
	 * @param startIdx
	 * @param endIdx
	 * @return
	 */
	public static String join(Object[] strings, String separator, int startIdx, int endIdx) {
		StringBuffer rtn = new StringBuffer();
		if (strings != null && startIdx < strings.length) {
			rtn.append(strings[startIdx]);
			for (int i = startIdx+1; i <= endIdx; i++) {
				rtn.append(separator + strings[i]);
			}
		}
		return (rtn.toString());
	}
	
	/**
	 * overloading join
	 * @param strings
	 * @param separator
	 * @return
	 */
	public static String join(Enumeration strings, String separator) {
		StringBuffer rtn = new StringBuffer();
		if (strings != null) {
			// first one has no separator 
			if (strings.hasMoreElements()) {
				rtn.append(strings.nextElement());
			}
			while (strings.hasMoreElements()) {
				rtn.append(separator + strings.nextElement());
			}
		}
		return (rtn.toString());
	}
	
	/**
	 * join strings
	 * @param strings
	 * @param separator
	 * @return
	 */
	public static String join (Object[] strings, String separator) {
		return join (strings, separator, 0, strings.length-1);
	}

	/**
	 * convertStringArrayToLongArray - returns -1 on number format exception
	 * 
	 */
	public static long[] convertStringArrayToLongArray(String[] strings) {
		long[] returnLongValues = new long[strings.length];
		
		for (int i = 0; i < strings.length ; i++){
			try{
				returnLongValues[i] = Long.parseLong(strings[i]);
			}catch(NumberFormatException e){
				returnLongValues[i] = -1;
			}
		}
		return returnLongValues;
	} 
	
	/**
	 * null/empty check
	 * @param str
	 * @return
	 */
	public static boolean isNullOrEmpty(String str) {
		return (str == null || str.length() == 0);
	}
	
	/**
	 * Print a pojo object
	 * @param o
	 * @return
	 */
	public static String pojo2String(Object o) {
		return pojo2String(o, true);
	}
	
	/**
	 * overloading pojo2string
	 * @param o
	 * @param shallow
	 * @return
	 */
	public static String pojo2String(Object o, boolean shallow) {
		return pojo2String("", o, new Stack(), shallow);
	}
	
	/**
	 * Print a pojo object
	 * @param o
	 * @return
	 */
	private static String pojo2String(String indent, Object o, Stack stack, boolean shallow) {
		StringBuffer buf = new StringBuffer();
		if (o != null) {
			buf.append("Class : ").append(o.getClass().getName()).append('\n').append(indent);
			Class c = o.getClass();
			Method[] theMethods = c.getMethods();

			for (int i = 0, len = theMethods.length; i < len; i++) {
				String methodString = theMethods[i].getName();
				Class[] parameterTypes = theMethods[i].getParameterTypes();
				if (parameterTypes.length == 0 && methodString.startsWith("get")) {
					try {
						Object retValue = theMethods[i].invoke(o, new Object[0]);
						if (retValue != null) {
							if (retValue.getClass().getName().startsWith("com.ebay")) {
								if (!shallow) {
									if (stack.isEmpty() || stack.search(retValue) == -1) {
										stack.push(o);
										buf.append(pojo2String(indent + '\t', retValue, stack, shallow));
										stack.pop();
									}
								}
								else {
									buf.append(methodString.substring(3)).append('=').append(retValue).append('\n').append(indent);	
								}
							}
							buf.append(methodString.substring(3)).append('=').append(retValue).append('\n').append(indent);	
						}
					} catch (Throwable e) {
						continue;
					}
				}
			}
		}
		return buf.toString();
	}
	
	/**
	 * print a tree as a string, tree node need to have "getPath" and "getChildren"
	 * @param indent
	 * @param root
	 * @return
	 */
	public static String tree2String(String indent, Object root, String methodToGetLabel, String methodToGetChildren) {
		StringBuffer buf = new StringBuffer();
		tree2String(buf, "", indent, root, methodToGetLabel, methodToGetChildren);
		return buf.toString();
	}
	
	/**
	 * format the tree to a string
	 * @param buf
	 * @param indent
	 * @param root
	 */
	private static void tree2String(StringBuffer buf, String prefix, String indent, Object root, String methodToGetLabel, String methodToGetChildren) {
		Class treeNodeClass = root.getClass();
		do {
			try {
				Method getPathMethod = treeNodeClass.getDeclaredMethod(methodToGetLabel, (Class[]) null);
				Object rst = getPathMethod.invoke(root, (Object[]) null);
				if (rst != null) {
					buf.append(prefix).append(indent).append(rst.toString()).append('\n');
				}
				Method getChildrenMethod = treeNodeClass.getDeclaredMethod(methodToGetChildren, (Class[]) null);
				rst = getChildrenMethod.invoke(root, (Object[]) null);
				if (rst != null) {
					if (rst instanceof Collection) {
						for (Object child : (Collection) rst) {
							tree2String(buf,prefix+indent, indent, child, methodToGetLabel, methodToGetChildren);
						}
					}
					else {
						for (Object child : (Object[]) rst) {
							tree2String(buf, prefix+indent, indent, child, methodToGetLabel, methodToGetChildren);
						}
					}
				}
			} catch (Exception e) {
				// ignore
			}
		} while ((treeNodeClass = treeNodeClass.getSuperclass()) != Object.class);
	}
	
	/**
	 * fuzzy match two strings
	 * @param source
	 * @param phrase
	 * @return
	 */
	public static boolean fuzzyMatched(String source, String phrase) {
		if (source == null) return false;
		else return source.matches(".*" + phrase + ".*");
	}
	
	/**
	 * 
	 * @param field
	 * @return
	 */
	public static boolean isAllString(String field) {
		Pattern mask = Pattern.compile("[a-zA-z]+");
		Matcher matcher = mask.matcher(field);
		if (!matcher.matches())
		{
			return false;
		}
		return true;
	}

	/**
	 * 
	 * @param field
	 * @return
	 */
	public static boolean isAllNumber(String field) {
		Pattern mask = Pattern.compile("[0-9]+");
		Matcher matcher = mask.matcher(field);
		if (!matcher.matches())
		{
			return false;
		}
		return true;
	}

	/**
	 * equalIgnoreCase with proper NULL handling
	 * @param source
	 * @param target
	 * @return
	 */
	public static boolean equalIgnoreCase (String source, String target) {
		if (source == null && target == null) {
			return true;
		} else if (source == null || target == null) {
			return false;
		} else {
			return source.equalsIgnoreCase(target);
		}
	}

	/**
	 * capitalize the first letter of a string
	 * @param source
	 * @return
	 */
	public static final String capitFirstLetter(String source) {
		if (!isNullOrEmpty(source)) {
			return source.substring(0, 1).toUpperCase() + source.substring(1); 
		}
		else return source;
	}

	/**
	 * 
	 * @param str
	 * @return
	 */
	public static boolean isNullOrEmptyAfterTrim(String str) {
		return (str == null || str.trim().length() == 0);
	}

	/**
	 * wether a string is a number
	 * @param numericStr
	 * @return
	 */
	public static boolean isNumeric(String numericStr) {
		try {
			Double.parseDouble(numericStr);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	/**
	 * generate a regex pattern
	 * @param pattern
	 * @param fuzzy
	 * @return
	 */
	public static Pattern compileRegEx(String pattern, boolean fuzzy) {
		String safePattern = Pattern.quote(pattern);
		return pattern == null ? null : (fuzzy ? Pattern.compile(".*" + safePattern + ".*", Pattern.CASE_INSENSITIVE) :
			Pattern.compile(Pattern.quote(pattern)));
	}
	
	/**
	 * test if src contained in list of regular expressions
	 * @param regexs
	 * @param src
	 * @return
	 */
	public static boolean regexContain(List regexs, String src) {
		if (regexs.contains(src)) {
			return true;
		}
		else {
			int i = 0;
			for (; i < regexs.size(); i++) {
				if (Pattern.matches(regexs.get(i).toString(), src)) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * To remove last character in a string while doing a ',' append or 
	 * something of that sort
	 * @param original
	 * @return
	 */
	public static String removeCharAtLast(String original) {
		String newStr = "";
		if(isNullOrEmptyAfterTrim(original)){
			newStr = original;
		}else{
			newStr = original.substring(0, original.length() - 1);
		}
		return newStr;
	}

	/**
	 * convert an integer to its hex string representation
	 * @param value
	 * @return
	 */
	public static String toHexString(int value) {
		return "0X" + Integer.toHexString(value).toUpperCase();
	}
	
	/**
	 * count the number of occurence of c in a string
	 * @param str
	 * @param c
	 * @return
	 */
	public static int count(String str, char c) {
		if (isNullOrEmpty(str)) return 0;
		int occurs = 0;
		for (int i = 0, size = str.length(); i < size; i++) {
			if (str.charAt(i) == c) occurs++;
		}
		return occurs;
	}
	
	/**
	 * Returns the contents of the given String as a
	 * UTF-8 encoded byte array.
	 * 
	 * @param inputString the string to convert
	 * @return the encoded bytes
	 */
	public static byte[] getUtf8Bytes(String inputString) {
		
		try {
			return inputString.getBytes( UTF8_ENCODING_TYPE );
		}
		
		// This, of course, should never happen.
		catch (UnsupportedEncodingException ex) {
			throw new RuntimeException( UTF8_ENCODING_TYPE + " is unsupported!!!" );
		}
	}
	
	
	/**
	 * Returns a regular String from the given RawString, by decoding
	 * the RawString's encoded bytes with UTF-8.
	 * 
	 * @param inputRawString the RawString to convert
	 * @return the decoded String
	 */
	public static String getString(byte[] inputUtf8Bytes) {
		try {
			return new String( inputUtf8Bytes, UTF8_ENCODING_TYPE );
		}
		
		// This, of course, should never happen.
		catch (UnsupportedEncodingException ex) {
			throw new RuntimeException( UTF8_ENCODING_TYPE + " is unsupported!!!" );
		}
	}

	/**
	 * Takes a number string and adds leading zeros to make it a specifed length
	 * 
	 * 
	 * @param number The number string
	 * @param numSize The desired length of the number string
	 * @return String representing the desired number with the specified length.
	 */
	public static String padLeadingZeros(String number, int numSize) {
		StringBuffer buf = new StringBuffer(number);
		for (int i = number.length(); i < numSize; i++) {
			buf.insert(0, 0);
		}
		return buf.toString();
	}
	
	/**
	 * format a decimal
	 * @param decimal
	 * @param format
	 * @return
	 */
	public static String formatDecimal(double decimal, String pattern) {
		return new DecimalFormat(pattern).format(decimal);
	}
	
	/**
	 * trim a string to a desired length
	 * @param src
	 * @param len
	 * @return
	 */
	public static String trimToLength(String src, int len) {
		return src != null && src.length() > len ? src.substring(0, len) : src; 
	}	

	/**
	 * <p>
	 * Gets the stack trace from a Throwable as a String.
	 * </p>
	 * 
	 * @param throwable
	 *            the <code>Throwable</code> to be examined
	 * @return the stack trace as generated by the exception's
	 *         <code>printStackTrace(PrintWriter)</code> method
	 */
	public static String getStackTrace(Throwable throwable, int trimToLength) {
		if (throwable != null) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw, true);
			throwable.printStackTrace(pw);
			return trimToLength > 0 ? trimToLength(sw.getBuffer().toString(), trimToLength) : sw.getBuffer().toString();
		}
		else {
			return null;
		}
	}

	/**
	 * get stack trace trim to 1000
	 * @param throwable
	 * @return
	 */
	public static String getStackTrace(Throwable throwable) {
		return getStackTrace(throwable, 1000);		
	}
	
	/**
	 * remove non UTF-8 character from a string
	 * @param inString
	 * @return
	 */
	public static String removeNonUtf8CompliantCharacters( final String inString ) {
	    if (null == inString ) return null;
	    byte[] byteArr = inString.getBytes();
	    for ( int i=0; i < byteArr.length; i++ ) {
	        byte ch= byteArr[i]; 
	        // remove any characters outside the valid UTF-8 range as well as all control characters
	        // except tabs and new lines
	        if ( !( (ch > 31 && ch < 253 ) || ch == '\t' || ch == '\n' || ch == '\r') ) {
	            byteArr[i]=' ';
	        }
	    }
	    return new String( byteArr );
	}

	/**
	 * encode source string based on dictionary (key->value)
	 * @param dict
	 * @param source
	 * @return
	 */
	public static String encode(Map<String, String> dict, String source) {
		for (Entry<String, String> d : dict.entrySet()) {
			source = source.replaceAll(d.getKey(), d.getValue());
		}
		return source;
	}
	
	/**
	 * decode source string based on dictionary (value->key)
	 * @param dict
	 * @param source
	 * @return
	 */
	public static String decode(Map<String, String> dict, String source) {
		for (Entry<String, String> d : dict.entrySet()) {
			source = source.replaceAll(d.getValue(), d.getKey());
		}
		return source;
	}
	
	/**
	 * return first not null or empty value of an string array
	 * @param strings
	 * @return
	 */
	public static String firstNotNullEmpty(String...strings) {
		for (String str : strings) {
			if (!StringUtil.isNullOrEmpty(str)) {
				return str;
			}
		}
		return null;
	}
}
