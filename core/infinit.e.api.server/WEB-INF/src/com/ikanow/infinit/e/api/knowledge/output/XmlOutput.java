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
package com.ikanow.infinit.e.api.knowledge.output;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;

import com.fasterxml.jackson.xml.XmlMapper;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;

/**
 * Class used to turn response object into xml.  Used for XML Interface.
 * Uses xstream object to create xml response type
 * 
 * @author cmorgan
 *
 */
public class XmlOutput {
	
@SuppressWarnings("unused")
private static final Logger logger = Logger.getLogger(XmlOutput.class);
	
	/**
	 * Public function used to return ResponsePojo object as xml string representation
	 * @param rp
	 * @return
	 */
	public String getFeeds(ResponsePojo rp) {
		String xml = null;
		XmlMapper mapper = new XmlMapper();
		AnnotationIntrospector ai = new JaxbAnnotationIntrospector();
		// make deserializer use JAXB annotations (only)
		mapper.getDeserializationConfig().setAnnotationIntrospector(ai);
	    // make serializer use JAXB annotations (only)
	    mapper.getSerializationConfig().setAnnotationIntrospector(ai);
		try {
			xml = mapper.writeValueAsString(rp);
		} catch (Exception ex) {
			
		}
		return xml;
	}

}
