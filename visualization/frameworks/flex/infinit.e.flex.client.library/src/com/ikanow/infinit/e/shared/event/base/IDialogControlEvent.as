package com.ikanow.infinit.e.shared.event.base
{
	import com.ikanow.infinit.e.shared.model.vo.ui.DialogControl;
	
	public interface IDialogControlEvent
	{
		function get dialogControl():DialogControl;
		function set dialogControl( value:DialogControl ):void;
	}
}
