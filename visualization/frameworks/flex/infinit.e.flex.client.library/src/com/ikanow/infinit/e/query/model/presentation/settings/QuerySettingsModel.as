package com.ikanow.infinit.e.query.model.presentation.settings
{
	import com.ikanow.infinit.e.shared.model.presentation.base.PresentationModel;
	import mx.collections.ArrayCollection;
	
	/**
	 *  Query Settings Presentation Model
	 */
	public class QuerySettingsModel extends PresentationModel
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Bindable]
		[Inject]
		public var navigator:QuerySettingsNavigator;
		
		[Bindable]
		/**
		 * The collection of query terms
		 */
		public var queryTerms:ArrayCollection;
		
		
		//======================================
		// public methods 
		//======================================
		
		[Inject( "queryManager.queryTerms", bind = "true" )]
		/**
		 * Query Terms Collection
		 * @param value
		 */
		public function setQueryTerms( value:ArrayCollection ):void
		{
			queryTerms = value;
		}
	}
}

