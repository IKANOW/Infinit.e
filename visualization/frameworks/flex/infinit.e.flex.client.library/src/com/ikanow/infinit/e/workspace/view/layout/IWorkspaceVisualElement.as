package com.ikanow.infinit.e.workspace.view.layout
{
	import mx.core.IVisualElement;
	
	public interface IWorkspaceVisualElement extends IVisualElement
	{
		function get positionIndex():int;
		function set positionIndex( value:int ):void;
		
		function get maximized():Boolean;
		function set maximized( value:Boolean ):void;
	}
}
