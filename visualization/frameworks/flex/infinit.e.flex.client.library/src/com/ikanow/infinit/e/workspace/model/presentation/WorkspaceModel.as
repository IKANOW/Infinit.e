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
package com.ikanow.infinit.e.workspace.model.presentation
{
	import com.ikanow.infinit.e.shared.model.presentation.base.PresentationModel;
	import com.ikanow.infinit.e.shared.model.vo.Widget;
	import com.ikanow.infinit.e.shared.view.component.common.InfDragImageList;
	import mx.collections.ArrayCollection;
	import mx.events.DragEvent;
	
	/**
	 *  Workspace Presentation Model
	 */
	public class WorkspaceModel extends PresentationModel
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Bindable]
		[Inject]
		public var navigator:WorkspaceNavigator;
		
		[Bindable]
		[Inject( "workspaceManager.selectedWidgets", bind = "true" )]
		public var selectedWidgets:ArrayCollection;
		
		[Bindable]
		[Inject( "workspaceManager.workspaceIsFull", bind = "true" )]
		public var workspaceIsFull:Boolean;
		
		
		//======================================
		// public methods 
		//======================================
		
		public function handleDragEnter( event:DragEvent ):void
		{
			if ( ( event.dragInitiator as InfDragImageList ) != null )
			{
				var dragObj:Vector.<Object> = event.dragSource.dataForFormat( "itemsByIndex" ) as Vector.<Object>;
				var widget:Widget = dragObj[ 0 ] as Widget;
				
				navigator.showLayoutView();
				navigator.closeWidgetDrawer();
			}
		}
	}
}

