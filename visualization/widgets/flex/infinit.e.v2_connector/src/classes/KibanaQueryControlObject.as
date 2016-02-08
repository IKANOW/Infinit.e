package classes
{
	import com.ikanow.infinit.e.widget.library.utility.JSONEncoder;
	
	import mx.collections.ArrayCollection;

	public class KibanaQueryControlObject
	{
		public var appendToExistingKibanaQueries:Boolean;
		public var decomposeInfiniteQuery:Boolean;
		public var termsOverride:String;
		public var applyToFilter:Boolean;
		
		public var entities:ArrayCollection;
		public var associations:ArrayCollection;

		
		public function KibanaQueryControlObject(appendToExistingKibanaQueries:Boolean, decomposeInfiniteQuery:Boolean, termsOverride:String, applyToFilter:Boolean, entities:ArrayCollection, associations:ArrayCollection  )
		{
			this.appendToExistingKibanaQueries = appendToExistingKibanaQueries;
			this.decomposeInfiniteQuery = decomposeInfiniteQuery;
			this.termsOverride = termsOverride;
			this.applyToFilter = applyToFilter;
			this.entities = entities;
			this.associations = associations;
		}
		
		public function toJson():String
		{
			try{
				return JSONEncoder.encode(this);
			}
			catch(e:Error)
			{
				trace("There was an ecoding KibanaQueryControlObject to Json");
			}
			return null;
		}
		
	}
}