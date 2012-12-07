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
		if (!CONTAINS_NON_ASCII.matcher(str).find()) {
			return str;
		}
		str = Normalizer.normalize(str, Normalizer.Form.NFD);
		str = DIACRITICS_AND_FRIENDS.matcher(str).replaceAll("");
		
		return str;
	}
}
