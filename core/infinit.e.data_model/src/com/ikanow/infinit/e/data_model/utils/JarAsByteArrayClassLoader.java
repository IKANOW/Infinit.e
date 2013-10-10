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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class JarAsByteArrayClassLoader extends SecureClassLoader {

	private final byte[] jarBytes;

	public JarAsByteArrayClassLoader(byte[] jarBytes, ClassLoader parent) {
		super(parent);
		this.jarBytes = jarBytes;
	}

	@Override
	protected Class<?> findClass(final String name) throws ClassNotFoundException {
		InputStream in = getResourceAsStream(name.replace('.', '/') + ".class");
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			JarAsByteArrayClassLoader.copyTo(in, out);
			byte[] bytes = out.toByteArray();
			Class<?> clazz = defineClass(name, bytes, 0, bytes.length);
			if (null == clazz) {
				throw new ClassNotFoundException(name);
			}
			return clazz;
		}
		catch (Exception e) {
			throw new ClassNotFoundException(name, e);
		}
	}
	
	@Override
	public URL getResource(String name) {
		return null;
	}

	@Override
	public InputStream getResourceAsStream(String name) {
		try {
			JarInputStream jis = new JarInputStream(new ByteArrayInputStream(jarBytes)); 
			JarEntry entry;
			while ((entry = jis.getNextJarEntry()) != null) {
				if (entry.getName().equals(name)) {
					return jis;
				}
			}
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static InputStream copyTo(InputStream in, OutputStream out) throws IOException {
		List<Byte> outbytes = new ArrayList<Byte>();
		for (int ch = -1; (ch = in.read()) != -1;) {
			outbytes.add((byte)ch);
			out.write(ch);
		}
		byte[] byteArray = new byte[outbytes.size()];
		for (int i = byteArray.length - 1; i >= 0; i--) {
			byteArray[i] = outbytes.get(i);
		}
		return new ByteArrayInputStream(byteArray);
	}
}
