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

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;

import com.mdimension.jchronic.Chronic;

public class DateUtility 
{
	// (odd, static initialization doesn't work; just initialize first time in utility fn)
	private static String[] _allowedDatesArray_startsWithLetter = null;
	private static String[] _allowedDatesArray_numeric_1 = null;
	private static String[] _allowedDatesArray_numeric_2 = null;
	private static String[] _allowedDatesArray_stringMonth = null;
	private static String[] _allowedDatesArray_numericMonth = null;

	public synchronized static long parseDate(String sDate) 
	{
		if (null == _allowedDatesArray_startsWithLetter) 
		{
			_allowedDatesArray_startsWithLetter = new String[] {
					DateFormatUtils.SMTP_DATETIME_FORMAT.getPattern(),
					
					"MMM d, yyyy hh:mm a",
					"MMM d, yyyy HH:mm",
					"MMM d, yyyy hh:mm:ss a",
					"MMM d, yyyy HH:mm:ss",
					"MMM d, yyyy hh:mm:ss.SS a",
					"MMM d, yyyy HH:mm:ss.SS",
					
					"EEE MMM dd HH:mm:ss zzz yyyy",
					"EEE MMM dd yyyy HH:mm:ss zzz",
					"EEE MMM dd yyyy HH:mm:ss 'GMT'Z (zzz)",					
			};					
			_allowedDatesArray_numeric_1 = new String[] {
					"yyyy-MM-dd'T'HH:mm:ss'Z'",
					DateFormatUtils.ISO_DATE_FORMAT.getPattern(),
					DateFormatUtils.ISO_DATE_TIME_ZONE_FORMAT.getPattern(),
					DateFormatUtils.ISO_DATETIME_FORMAT.getPattern(),
					DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.getPattern()
			};

			_allowedDatesArray_numeric_2 = new String[] {					
					"yyyyMMdd",
					"yyyyMMdd hh:mm a",
					"yyyyMMdd HH:mm",
					"yyyyMMdd hh:mm:ss a",
					"yyyyMMdd HH:mm:ss",
					"yyyyMMdd hh:mm:ss.SS a",
					"yyyyMMdd HH:mm:ss.SS",
					// Julian, these are unlikely
					"yyyyDDD",
					"yyyyDDD hh:mm a",
					"yyyyDDD HH:mm",
					"yyyyDDD hh:mm:ss a",
					"yyyyDDD HH:mm:ss",
					"yyyyDDD hh:mm:ss.SS a",
					"yyyyDDD HH:mm:ss.SS",
				};
			_allowedDatesArray_stringMonth = new String[] {
					"dd MMM yy",
					"dd MMM yy hh:mm a",
					"dd MMM yy HH:mm",
					"dd MMM yy hh:mm:ss a",
					"dd MMM yy HH:mm:ss",
					"dd MMM yy hh:mm:ss.SS a",
					"dd MMM yy HH:mm:ss.SS",
				};
			_allowedDatesArray_numericMonth = new String[] {
					"MM dd yy",
					"MM dd yy hh:mm a",
					"MM dd yy HH:mm",
					"MM dd yy hh:mm:ss a",
					"MM dd yy HH:mm:ss",
					"MM dd yy hh:mm:ss.SS a",
					"MM dd yy HH:mm:ss.SS",
			};
		}

// Starts with day or month:
		
		String sDateTmp = sDate;
		if (Character.isLetter(sDate.charAt(0))) {
			try {
				Date date = DateUtils.parseDate(sDate, _allowedDatesArray_startsWithLetter);			
				return date.getTime();
			}
			catch (Exception e) {} // keep going			
		}//TESTED
		else if (Character.isLetter(sDate.charAt(5))) { 
			
// month must be string, doesn't start with day though
			
			try {
				int index = sDate.indexOf(':');
				if (index > 0) {
					sDate = new StringBuffer(sDate.substring(0, index).replaceAll("[./-]", " ")).append(sDate.substring(index)).toString();
				}
				else {
					sDate = sDate.replaceAll("[ ./-]", " ");
				}				
				Date date = DateUtils.parseDate(sDate, _allowedDatesArray_stringMonth);			
				return date.getTime();
			}
			catch (Exception e) {} // keep going										
		}//TESTED
		else { 
			
// Starts with a number most likely...
			
			int n = 0;
			for (; n < 4; ++n) {
				if (!Character.isDigit(sDate.charAt(n))) {
					break;
				}
			}
			if (4 == n) {
				
// (Probably starts with a year)				
				
// One of the formal formats starting with a year				
				
				try {
					Date date = DateUtils.parseDate(sDate, _allowedDatesArray_numeric_1);			
					return date.getTime();					
				}
				catch (Exception e) {} // keep going
				
// Something more ad hoc starting with a year								
				
				try {
					int index = sDate.indexOf(':');
					if (index > 0) {
						sDate = new StringBuffer(sDate.substring(0, index).replace("-", "")).append(sDate.substring(index)).toString();
					}
					else {
						sDate = sDate.replace("-", "");
					}
					Date date = DateUtils.parseDate(sDate, _allowedDatesArray_numeric_2);			
					return date.getTime();
				}
				catch (Exception e) {} // keep going							
			}//TESTED
			
// Probably starts with a day			
			
			try {
				int index = sDate.indexOf(':');
				if (index > 0) {
					sDate = new StringBuffer(sDate.substring(0, index).replaceAll("[./-]", " ")).append(sDate.substring(index)).toString();
				}
				else {
					sDate = sDate.replaceAll("[./-]", " ");
				}	
				Date date = DateUtils.parseDate(sDate, _allowedDatesArray_numericMonth);			
				return date.getTime();
			}//TESTED
			catch (Exception e) {} // keep going							
			
		}
		sDate = sDateTmp;
		
// If we're here, nothing's worked, try "natural language processing"
		
		try {
			return Chronic.parse(sDate).getBeginCalendar().getTime().getTime();
		}//TESTED
		catch (Exception e2) {
			// Error all the way out
			throw new RuntimeException("Can't parse: " + sDate);
		}//TESTED
	}

	/**
	 * getIsoDateString
	 * @param date
	 * @return
	 */
	public static String getIsoDateString(String date)
	{
		
		
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		
		try
		{
			Date d = new Date( DateUtility.parseDate( date ) );
			return format.format( d );
		}
		catch (Exception e1)
		{
			return format.format( new Date(parseDate(date)) );
		}
	}
	
}
