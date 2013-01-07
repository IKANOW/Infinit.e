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
package com.ikanow.infinit.e.community.model.presentation.list
{
	import com.ikanow.infinit.e.shared.event.CommunityEvent;
	import com.ikanow.infinit.e.shared.model.presentation.base.PresentationModel;
	import com.ikanow.infinit.e.shared.model.vo.Community;
	import com.ikanow.infinit.e.shared.model.vo.Source;
	import com.ikanow.infinit.e.shared.model.vo.ui.DialogControl;
	import com.ikanow.infinit.e.shared.util.FilterUtil;
	
	import flash.utils.setTimeout;
	
	import mx.collections.ArrayCollection;
	import mx.events.CollectionEvent;
	
	/**
	 *  Source Monitor List Presentation Model
	 */
	public class SourceMonitorListModel extends PresentationModel
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Bindable]
		/**
		 * The selected source
		 */
		public var selectedSource:Source;
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Public Selected Source
		 * @param value
		 */
		[Inject( "communityManager.selectedSource", bind = "true" )]
		public function setSource( value:Source ):void
		{
			selectedSource = value;
		}
		
		//======================================
		// protected methods 
		//======================================
	}
}

