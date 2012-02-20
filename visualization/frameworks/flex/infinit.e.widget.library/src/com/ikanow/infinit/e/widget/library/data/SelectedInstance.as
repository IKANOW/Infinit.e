/**
 * <p>Infinit.e</p>
 *
 * <p>Copyright (c) 2011 IKANOW, llc.</p>
 * <p>http://www.ikanow.com</p> 
 *
 * <p>NOTICE:  IKANOW permits you to use this this file in accordance with the terms of the license agreement 
 * accompanying it.  For information about the licensing and copyright of this Plug-In please contact IKANOW, llc. 
 * at support&#64;ikanow.com.</p>
 *
 * <p>http://www.ikanow.com/terms-conditions/</p>
 * 
 */
package com.ikanow.infinit.e.widget.library.data
{
	import mx.collections.ArrayCollection;

	/**
	 *  DEPRICATED: This class builds selected instances from a module to pass onto other modules in the
	 * environment
	*/
	
	public class SelectedInstance
	{
		private var feedid:String;
		private var entities_disambig_name:ArrayCollection = null;
		
		/**
		 * Constructor
		 * 
		 * @param _feedid The id of the current feed
		 * @param _entitieslist The array collection of entities for the current feed
		*/
		public function SelectedInstance(_feedid:String="", _entitieslist:ArrayCollection=null)
		{
			feedid = _feedid;
			entities_disambig_name = _entitieslist;
		}
		
		/**
		 * function to set the feed id of the current feed
		 * 
		 * @param _feedid The id of the current feed
		*/
		public function setFeedID(_feedid:String):void
		{
			feedid = _feedid;			
		}
		
		/**
		 * function to add entities to the list
		 * 
		 * @param _entitieslist The array collection of entities for the current feed
		*/
		public function addEntityList(_entitieslist:ArrayCollection):void
		{
			entities_disambig_name = _entitieslist;
		}
		
		/**
		 * function to add entities individually to the list
		 * 
		 * @param _entity_disambiguous_name The name of hte entity
		*/
		public function addEntityItem(_entity_disambiguous_name:String):void
		{
			entities_disambig_name.addItem(_entity_disambiguous_name);
		}
		
		/**
		 * function to get the feed id of the current item
		 * 
		 * @return The current item feed id
		*/
		public function getfeedID():String
		{
			return feedid;
		}
		
		/**
		 * function to get the entities for the current item
		 * 
		 * @return The entities for the current item
		*/
		public function getEntities():ArrayCollection
		{
			return entities_disambig_name;
		}
	}
}