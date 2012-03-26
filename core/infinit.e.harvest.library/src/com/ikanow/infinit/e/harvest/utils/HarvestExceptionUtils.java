/*******************************************************************************
 * Copyright 2012, The Infinit.e Open Source Project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
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
