package org.elasticsearch.common.codec.digest;

public class DigestUtils {

	public static Object md5Hex(String string) {
		return org.elasticsearch.common.Digest.md5Hex(string);
	}

}
