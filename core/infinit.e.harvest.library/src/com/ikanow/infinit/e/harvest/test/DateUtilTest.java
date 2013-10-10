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
package com.ikanow.infinit.e.harvest.test;

import java.util.Date;

import com.ikanow.infinit.e.harvest.utils.DateUtility;

public class DateUtilTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String[] dateTests = {
				// Formal
				"2011-01-01T12:15:32Z",
				// Day
				"Wed, 13 Aug 1998 23:11:01 UTC",
				// Numeric
				"2011-189",
				"2011189 11:32:11.01",
				"20110812",
				// String month:
				"11 August 2011",
				"11.Aug.2011 11:11:00 PM",
				// Numeric month:
				"08-11-99", 
				"08/11/99 13:21:00",
				"08.11.99 13:21:00",
				// NLP
				"yesterday",
				"5 minutes ago",
				// Fail:
				"dfdgfh"				
		};
		for (String dateTest: dateTests) {
			Date date = new Date(DateUtility.parseDate(dateTest));
			System.out.println(dateTest + "->" + date.toString());
		}
		

	}

}
