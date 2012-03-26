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
package com.ikanow.infinit.e.data_model.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class ThreadSafeSimpleDateFormat {
	 private DateFormat df;

	 public ThreadSafeSimpleDateFormat(String format) {
	     this.df = new SimpleDateFormat(format);
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
