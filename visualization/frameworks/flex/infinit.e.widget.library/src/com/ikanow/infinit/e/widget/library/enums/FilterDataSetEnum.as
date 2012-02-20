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
package com.ikanow.infinit.e.widget.library.enums
{
	/**
	 * Enums for what data the filter should apply to.
	 */
	public final class FilterDataSetEnum
	{
		/** Applies the filter to the queried data, result will be a subset of the query **/
		public static const FILTER_GLOBAL_DATA:FilterDataSetEnum = create("FILTER_GLOBAL_DATA");
		/** Applies the filter to the filtered data, result will be a subset of the filter **/
		public static const FILTER_FILTERED_DATA:FilterDataSetEnum = create("FILTER_FILTERED_DATA");
		
		private var filterDataSet:String;
		
		private static function create(filterDataSetType:String):FilterDataSetEnum
		{
			var enum:FilterDataSetEnum = new FilterDataSetEnum();
			enum.filterDataSet = filterDataSetType;
			return enum;
		}
	}
}