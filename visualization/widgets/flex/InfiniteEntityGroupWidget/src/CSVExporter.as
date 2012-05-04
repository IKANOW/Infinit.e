package
{
	import mx.collections.ArrayCollection;
	import system.data.Set;
	import system.data.sets.HashSet;
	
	public class CSVExporter
	{
		
		//======================================
		// public static properties 
		//======================================
		
		public static const CSV_ALL:int = 0;
		
		public static const CSV_DOCS:int = 1;
		
		public static const CSV_ENTS:int = 2;
		
		public static const CSV_ASSOCS:int = 3;
		
		//======================================
		// constructor 
		//======================================
		
		public function CSVExporter()
		{
		
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		public function createCSV( docs:ArrayCollection, crossDimensional:int ):String
		{
			var csvOutput:String = "";
			
			if ( crossDimensional == CSV_ALL )
			{
				csvOutput = "\"ID\",\"URL\",\"Title\",\"Source\",\"Tags\",\"MediaType\",\"CommunityID\",\"Description\",\"Published Date\",\"Entity Disambiguated Name\",\"Entity Actual Name\",\"Entity Type\",\"Entity Dimension\",\"Entity Doc Count\",\"Entity Frequency\",\"Entity Total Frequency\"\n";
				
				for each ( var doc:Object in docs )
				{
					var docOutput:String = "\"" + doc._id + "\",\"" + doc.url + "\",\"" + prepareCSVClean( doc.title ) + "\",\"";
					docOutput += doc.source + "\",\"" + doc.tags + "\",\"";
					docOutput += doc.mediaType + "\",\"" + doc.communityId + "\",\"";
					docOutput += prepareCSVClean( doc.description ) + "\",\"" + doc.publishedDate + "\",";
					var hadEnt:Boolean = false;
					
					for each ( var ent:Object in doc.entities )
					{
						csvOutput += docOutput + "\"" + prepareCSVClean( ent.disambiguated_name ) + "\",\"" + prepareCSVClean( ent.actual_name ) + "\",\"" + ent.type + "\",\"" + ent.dimension + "\",\"" + ent.doccount + "\",\"" + ent.frequency + "\",\"" + ent.totalfrequency + "\"\n";
						hadEnt = true;
					}
					
					if ( !hadEnt )
						csvOutput += docOutput + "\n";
				}
			}
			else if ( crossDimensional == CSV_DOCS )
			{
				csvOutput = "\"ID\",\"URL\",\"Title\",\"Source\",\"Tags\",\"MediaType\",\"CommunityID\",\"Description\",\"Published Date\",\"Entity Count\",\"Association Count\"\n";
				
				for each ( var doc1:Object in docs )
				{
					var docOutput1:String = "\"" + doc1._id + "\",\"" + doc1.url + "\",\"" + prepareCSVClean( doc1.title ) + "\",\"";
					docOutput1 += doc1.source + "\",\"" + doc1.tags + "\",\"";
					docOutput1 += doc1.mediaType + "\",\"" + doc1.communityId + "\",\"";
					docOutput1 += prepareCSVClean( doc1.description ) + "\",\"" + doc1.publishedDate + "\",\"";
					docOutput1 += doc1.entities.length + "\",\"" + doc1.associations.length + "\"";
					csvOutput += docOutput1 + "\n";
				}
			}
			else if ( crossDimensional == CSV_ENTS )
			{
				csvOutput = "\"Entity Disambiguated Name\",\"Entity Actual Name\",\"Entity Type\",\"Entity Dimension\",\"Entity Doc Count\",\"Entity Frequency\",\"Entity Total Frequency\"\n";
				var entName:Set = new HashSet();
				
				for each ( var doc2:Object in docs )
				{
					for each ( var ent1:Object in doc2.entities )
					{
						if ( !entName.contains( ent1.index ) )
						{
							entName.add( ent1.index );
							csvOutput += "\"" + prepareCSVClean( ent1.disambiguated_name ) + "\",\"" + prepareCSVClean( ent1.actual_name ) +
								"\",\"" + ent1.type + "\",\"" + ent1.dimension + "\",\"" + ent1.doccount + "\",\"" + ent1.frequency + "\",\"" +
								ent1.totalfrequency + "\"\n";
						}
					}
				}
			}
			else //CSV_ASSOCS
			{
				csvOutput = "\"Entity 1\",\"Index 1\",\"Verb\",\"Verb Category\",\"Entity 2\",\"Index 2\",\"Association Type\"\n";
				var assocName:Set = new HashSet();
				
				for each ( var doc3:Object in docs )
				{
					for each ( var assoc:Object in doc3.associations )
					{
						var tempName:String = buildAssocName( assoc );
						
						if ( !assocName.contains( tempName ) )
						{
							assocName.add( tempName );
							csvOutput += "\"" + prepareCSVClean( assoc.entity1 ) + "\",\"" + prepareCSVClean( assoc.entity1_index ) + "\",\"" +
								prepareCSVClean( assoc.verb ) + "\",\"" + prepareCSVClean( assoc.verb_category ) + "\",\"" +
								prepareCSVClean( assoc.entity2 ) + "\",\"" + prepareCSVClean( assoc.entity2_index ) + "\",\"" +
								prepareCSVClean( assoc.assoc_type ) + "\"\n";
						}
					}
				}
			}
			return csvOutput;
		}
		
		//======================================
		// private methods 
		//======================================
		
		private function buildAssocName( assoc:Object ):String
		{
			var assocName:String = "";
			
			if ( assoc.entity1_index != null )
			{
				assocName += assoc.entity1_index + "|";
			}
			
			if ( assoc.verb != null )
			{
				assocName += assoc.verb + "|";
			}
			
			if ( assoc.entity2_index != null )
			{
				assocName += assoc.entity2_index + "";
			}
			return assocName;
		}
		
		private function prepareCSVClean( field:Object ):String
		{
			if ( ( field as String ) != null )
				return field.replace( new RegExp( '"', "g" ), '\"\"' ).replace( new RegExp( '\n', "g" ), '  ' );
			else
				return "";
		}
	}
}
