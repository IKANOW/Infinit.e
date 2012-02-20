package com.ikanow.infinit.e.api.social.sharing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.restlet.data.MediaType;
import org.restlet.resource.OutputRepresentation;

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
