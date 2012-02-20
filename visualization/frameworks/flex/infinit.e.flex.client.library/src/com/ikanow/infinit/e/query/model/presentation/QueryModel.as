package com.ikanow.infinit.e.query.model.presentation
{
	import com.ikanow.infinit.e.shared.event.QueryEvent;
	import com.ikanow.infinit.e.shared.model.presentation.base.PresentationModel;
	
	/**
	 *  Query Presentation Model
	 */
	public class QueryModel extends PresentationModel
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Bindable]
		[Inject]
		public var navigator:QueryNavigator;
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Run the advanced query
		 */
		public function runAdvancedQuery():void
		{
			dispatcher.dispatchEvent( new QueryEvent( QueryEvent.RUN_ADVANCED_QUERY ) );
		}
	}
}

