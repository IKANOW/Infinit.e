package com.ikanow.infinit.e.shared.model.vo.ui
{
	
	public interface INavigationItem
	{
		function get navigatorId():String;
		function set navigatorId( value:String ):void;
		
		function get id():String;
		function set id( value:String ):void;
		
		function get type():String;
		function set type( value:String ):void;
		
		function get state():String;
		function set state( value:String ):void;
		
		function get label():String;
		function set label( value:String ):void;
		
		function get description():String;
		function set description( value:String ):void;
		
		function get toolTip():String;
		function set toolTip( value:String ):void;
		
		function get styleName():String;
		function set styleName( value:String ):void;
		
		function get itemCount():int;
		function set itemCount( value:int ):void;
		
		function get icon():Class;
		function set icon( value:Class ):void;
		
		function get altIcon():Class;
		function set altIcon( value:Class ):void;
		
		function get selected():Boolean;
		function set selected( value:Boolean ):void;
		
		function get enabled():Boolean;
		function set enabled( value:Boolean ):void;
	}
}
