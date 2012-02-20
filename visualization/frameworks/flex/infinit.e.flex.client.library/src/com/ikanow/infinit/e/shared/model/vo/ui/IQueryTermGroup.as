package com.ikanow.infinit.e.shared.model.vo.ui
{
	import mx.collections.ArrayCollection;
	
	public interface IQueryTermGroup
	{
		function get _id():String;
		function set _id( value:String ):void;
		
		function get children():ArrayCollection;
		function set children( value:ArrayCollection ):void;
	}
}
