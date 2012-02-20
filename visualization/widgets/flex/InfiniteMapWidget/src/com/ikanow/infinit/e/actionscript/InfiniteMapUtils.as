package com.ikanow.infinit.e.actionscript
{
	import mx.collections.ArrayCollection;

	public class InfiniteMapUtils
	{
		public static var allowedOntologyData:ArrayCollection = new ArrayCollection();
		
		public static function allowOntologyType(ontology_type:String):Boolean
		{
			if ( ontology_type == null )
				ontology_type = "point";
			for each (var ont:Object in allowedOntologyData )
			{
				if ( ont.data == ontology_type )
				{
					return ont.toggled;
				} 					
			}
			return false;
		}
	}
}