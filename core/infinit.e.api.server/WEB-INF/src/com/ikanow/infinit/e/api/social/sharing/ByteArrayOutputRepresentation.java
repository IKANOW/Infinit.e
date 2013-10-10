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
package com.ikanow.infinit.e.api.social.sharing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.restlet.data.MediaType;
import org.restlet.representation.OutputRepresentation;

public class ByteArrayOutputRepresentation extends OutputRepresentation 
{
	ByteArrayOutputStream os;
	
	public ByteArrayOutputRepresentation(MediaType mediaType) 
	{
		super(mediaType);
	}
	
	public void setOutputBytes(byte[] bytes) throws IOException
	{
		os = new ByteArrayOutputStream();
		os.write(bytes);
	}
	
	public void setOutputStream(ByteArrayOutputStream stream) throws IOException
	{
		os = stream;
	}

	@Override
	public void write(OutputStream out) throws IOException 
	{
		os.writeTo(out);
	}

	
}
