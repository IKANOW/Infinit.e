package com.ikanow.infinit.e.harvest.utils;

public class HarvestExceptionUtils {

	public static StringBuffer createExceptionMessage(Exception e) {
		return createExceptionMessage(null, e);
	}
	public static StringBuffer createExceptionMessage(String prefix, Exception e) {
		StackTraceElement[] st = e.getStackTrace();
		StringBuffer errMessage = new StringBuffer();
		if (null != prefix) {
			errMessage.append(prefix).append(':');
		}
		errMessage.append((e.getMessage()==null?"NullPointerException":e.getMessage())).append(':');
		if (st.length > 0) {
			errMessage.append(st[0].getClassName()).append('.').append(st[0].getMethodName()).append(':').append(st[0].getLineNumber());
		}						
		return errMessage;
	}
	
}
