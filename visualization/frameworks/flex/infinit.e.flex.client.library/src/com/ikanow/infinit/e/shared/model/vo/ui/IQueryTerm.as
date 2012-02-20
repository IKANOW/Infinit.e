package com.ikanow.infinit.e.shared.model.vo.ui
{
	
	public interface IQueryTerm
	{
		function get _id():String;
		function set _id( value:String ):void;
		
		function get level():int;
		function set level( value:int ):void;
		
		function get logicOperator():String;
		function set logicOperator( value:String ):void;
	}
}
