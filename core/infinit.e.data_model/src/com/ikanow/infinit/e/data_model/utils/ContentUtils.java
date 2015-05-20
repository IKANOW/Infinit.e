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

import java.text.Normalizer;
import java.util.regex.Pattern;

public class ContentUtils {

	public static final Pattern DIACRITICS_AND_FRIENDS
	= Pattern.compile("[\\p{InCombiningDiacriticalMarks}\\p{IsLm}\\p{IsSk}]+");

	public static final Pattern CONTAINS_NON_ASCII
	= Pattern.compile("[^\\p{ASCII}]");
	
	public static String stripDiacritics(String str) {
		// First check if I need to do anything:
		if (str==null || !CONTAINS_NON_ASCII.matcher(str).find()) {
			return str;
		}
		str = Normalizer.normalize(str, Normalizer.Form.NFD).replace("\u0131", "i");
		//TODO (INF-2447): this quick hack fixes problem with "ıstanbul" (other 6 turkish chars seem to work fine)	
		
		str = DIACRITICS_AND_FRIENDS.matcher(str).replaceAll("");
		
		return str;
	}
}
