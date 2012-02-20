package com.ikanow.infinit.e.shared.model.vo.ui
{
	import flash.events.EventDispatcher;
	import mx.collections.ArrayCollection;
	
	[Bindable]
	public class ColumnSelectorItem extends EventDispatcher implements ISelectable
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var type:String;
		
		public var _id:String;
		
		public var description:String;
		
		public var count:int;
		
		public var selected:Boolean;
		
		//======================================
		// constructor 
		//======================================
		
		public function ColumnSelectorItem( type:String, _id:String, description:String, count:int = 0 )
		{
			super();
			this.type = type;
			this._id = _id;
			this.description = description;
			this.count = count;
		}
	}
}
