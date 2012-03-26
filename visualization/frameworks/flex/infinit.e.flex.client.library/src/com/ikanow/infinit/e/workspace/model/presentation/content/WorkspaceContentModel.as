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
package com.ikanow.infinit.e.workspace.model.presentation.content
{
	import com.ikanow.infinit.e.shared.event.WorkspaceEvent;
	import com.ikanow.infinit.e.shared.model.presentation.base.PresentationModel;
	import com.ikanow.infinit.e.shared.model.vo.Widget;
	import mx.collections.ArrayCollection;
	
	/**
	 *  Workspace Content Presentation Model
	 */
	public class WorkspaceContentModel extends PresentationModel
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Bindable]
		[Inject]
		public var navigator:WorkspaceContentNavigator;
		
		[Bindable]
		[Inject( "workspaceManager.selectedWidgets", bind = "true" )]
		public var selectedWidgets:ArrayCollection;
		
		
		//======================================
		// public methods 
		//======================================
		
		public function maximizeWidget( widget:Widget ):void
		{
			dispatcher.dispatchEvent( new WorkspaceEvent( WorkspaceEvent.MAXIMIZE_WIDGET, widget ) );
		}
		
		public function minimizeWidgets():void
		{
			dispatcher.dispatchEvent( new WorkspaceEvent( WorkspaceEvent.MINIMIZE_WIDGETS ) );
		}
	}
}

