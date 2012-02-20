package com.ikanow.infinit.e.shared.model.constant.types
{
	import mx.collections.ArrayCollection;
	
	/**
	 * Query Operator Type Constants
	 */
	public class QueryOperatorTypes
	{
		
		//======================================
		// public static properties 
		//======================================
		
		public static const AND:String = "AND";
		
		public static const OR:String = "OR";
		
		public static const NOT:String = "NOT";
		
		public static const AND_NOT:String = "AND NOT";
		
		/**
		 * returns a collection fo the operator types
		 * @return The collection of operator types
		 */
		public static function get types():ArrayCollection
		{
			return new ArrayCollection( [ AND, OR, NOT ] );
		}
	}
}

