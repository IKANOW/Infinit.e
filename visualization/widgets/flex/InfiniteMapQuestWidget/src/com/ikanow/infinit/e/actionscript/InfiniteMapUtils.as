/*******************************************************************************
 * Copyright 2012, The Infinit.e Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.ikanow.infinit.e.actionscript
{
	import mx.collections.ArrayCollection;

	public class InfiniteMapUtils
	{
		public static var allowedOntologyData:ArrayCollection = new ArrayCollection();
		
		public static function allowOntologyType(ontology_type:String):Boolean
		{
			if ( ontology_type == null )
				ontology_type = "point";
			for each (var ont:Object in allowedOntologyData )
			{
				if ( ont.data == ontology_type )
				{
					return ont.toggled;
				} 					
			}
			return false;
		}
	}
}
