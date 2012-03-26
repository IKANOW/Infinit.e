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

public class DateUtility 
{
	// (odd, static initialization doesn't work; just initialize first time in utility fn)
	private static String[] _allowedDatesArray = null;

	public static long parseDate(String sDate) 
	{
		if (null == _allowedDatesArray) 
		{
			_allowedDatesArray = new String[]
				{
					"yyyy'-'DDD",
					"yyyy'-'MM'-'dd",
					"yyyyMMdd",
					"dd MMM yyyy",
					"dd MMM yy", 
					"MM/dd/yy",
					"MM/dd/yyyy",
					"MM.dd.yy",
					"MM.dd.yyyy",
					"dd MMM yyyy hh:mm:ss",
					"MMM d, yyyy hh:mm:ss a",
					"MMM d, yyyy HH:mm:ss",
					"yyyy-MM-dd'T'HH:mm:ss'Z'",
					"EEE MMM dd HH:mm:ss zzz yyyy",
					"EEE MMM dd yyyy HH:mm:ss zzz",
					"EEE MMM dd yyyy HH:mm:ss 'GMT'Z (zzz)",
					DateFormatUtils.ISO_DATE_FORMAT.getPattern(),
					DateFormatUtils.ISO_DATETIME_FORMAT.getPattern(),
					DateFormatUtils.ISO_DATE_TIME_ZONE_FORMAT.getPattern(),
					DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.getPattern(),
					DateFormatUtils.SMTP_DATETIME_FORMAT.getPattern()
			    };			
		}
		
		try 
		{
			Date date = DateUtils.parseDate(sDate, _allowedDatesArray);			
			return date.getTime();
		}
		catch (Exception e)
		{ 
			// Error all the way out
			throw new RuntimeException(e);
		}		
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
			//
			try
			{
				Long l = Long.valueOf( date );
				if ((l > 99999999L) && (l < 10000000000L))
				{
					Date d = new Date( l );
					return format.format( d );
				}
				else
				{
					return null;
				}
			}
			catch (Exception e2)
			{
				return null;
			}
		}
	}
	
}
