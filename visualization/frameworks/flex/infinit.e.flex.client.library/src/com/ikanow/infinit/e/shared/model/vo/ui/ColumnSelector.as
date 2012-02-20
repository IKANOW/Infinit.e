package com.ikanow.infinit.e.shared.model.vo.ui
{
	import flash.events.EventDispatcher;
	import mx.collections.ArrayCollection;
	
	[Bindable]
	public class ColumnSelector extends EventDispatcher implements ISelectable
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var _id:String;
		
		public var dataField:String;
		
		public var description:String;
		
		public var selected:Boolean;
		
		public var dataProvider:ArrayCollection = new ArrayCollection();
		
		public var dataProviderEnabled:Boolean = true;
		
		public var filterType:String;
		
		//======================================
		// constructor 
		//======================================
		
		public function ColumnSelector( _id:String, dataField:String, description:String, filterType:String, selected:Boolean )
		{
			super();
			this._id = _id;
			this.dataField = dataField;
			this.description = description;
			this.filterType = filterType;
			this.selected = selected;
		}
	}
}
