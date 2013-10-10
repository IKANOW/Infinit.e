package com.ikanow.infinit.e.widget.library.utility
{
	import mx.rpc.xml.SimpleXMLEncoder;
	import flash.xml.XMLDocument;
	import flash.xml.XMLNode;
	
	public class XmlEncoder
	{
		public static function encode(obj:Object):XML {
			var qName:QName = new QName("root");
			var xmlDocument:XMLDocument = new XMLDocument();
			var simpleXMLEncoder:SimpleXMLEncoder = new SimpleXMLEncoder(xmlDocument);
			var xmlNode:XMLNode = simpleXMLEncoder.encodeValue(obj, qName, xmlDocument);
			var xml:XML = new XML(xmlDocument.toString());
			// trace(xml.toXMLString());
			return xml;
		}
	}
}