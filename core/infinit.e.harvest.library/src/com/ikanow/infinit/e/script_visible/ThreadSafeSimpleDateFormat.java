/*******************************************************************************
 * Copyright 2012 The Infinit.e Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.ikanow.infinit.e.script_visible;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class ThreadSafeSimpleDateFormat {
	 private DateFormat df;

	 public ThreadSafeSimpleDateFormat(String format) {
	     this.df = new SimpleDateFormat(format);
	     this.df.setTimeZone(TimeZone.getTimeZone("GMT"));
	     	// (mostly this is used twice if it's used at all so timezone doesn't actually matter)
	     	//TODO: (INF-) if no incoming time zone - eg see "format2" usage, assumes is time zone of server
	     	//  some EC2 servers are UTC, some are EST so there's some scope for confusion here
	 }

	 public synchronized String format(Date date) {
	     return df.format(date);
	 }

	 public synchronized Date parse(String string) throws ParseException {
	     return df.parse(string);
	 }
	 public synchronized void setCalendar(Calendar newCalendar) {
		 df.setCalendar(newCalendar);
	 }
}
