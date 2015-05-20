/*******************************************************************************
 * Copyright 2015, The Infinit.e Open Source Project.
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
package com.ikanow.infinit.e.data_model.utils;

import org.apache.log4j.Logger;


public class ModifiedChangeListener implements IChangeListener {
	private static final Logger logger = Logger.getLogger(ModifiedChangeListener.class);
	protected boolean modified = false;
	
	public boolean isModified() {
		return modified;
	}

	public void setModified(boolean modified) {
		this.modified = modified;
	}

	@Override
	public void onChange(String where, Object what) {
		if(logger.isDebugEnabled()){
			logger.debug("onChange"+where+":"+what);
		}
		setModified(true);
	}
	
	/**
	 * Convenience function so null check does not need to be performed. 
	 * @param changeListener
	 * @param where
	 * @param what
	 */
	public static void notify(IChangeListener changeListener){
		if(changeListener!=null){
			changeListener.onChange(null, null);
		}
	}

	/**
	 * Convenience function so null check does not need to be performed. 
	 * @param changeListener
	 * @param where
	 * @param what
	 */
	public static void notify(IChangeListener changeListener,String where, Object what){
		if(changeListener!=null){
			changeListener.onChange(where, what);
		}
	}

	/**
	 * Convenience function so null check does not need to be performed. 
	 * @param changeListener
	 * @param what
	 */
	public static void notify(IChangeListener changeListener,Object what){
		if(changeListener!=null){
			String where = Thread.currentThread().getStackTrace()[2].getMethodName(); 
			changeListener.onChange(where, what);
		}
	}
	
}
