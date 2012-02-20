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
					DateFormatUtils.ISO_DATE_FORMAT.getPattern(),
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
