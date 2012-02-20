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
	@SuppressWarnings("deprecation")
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
