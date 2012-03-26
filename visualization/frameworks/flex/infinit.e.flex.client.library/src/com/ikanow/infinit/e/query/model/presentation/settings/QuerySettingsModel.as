/*******************************************************************************
 * Copyright 2012, The Infinit.e Open Source Project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
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

