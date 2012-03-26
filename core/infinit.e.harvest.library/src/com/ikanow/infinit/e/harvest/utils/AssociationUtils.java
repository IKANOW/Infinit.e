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
package com.ikanow.infinit.e.harvest.utils;

import com.ikanow.infinit.e.data_model.store.document.AssociationPojo;

public class AssociationUtils 
{
	/**
	 * Return the type of event based on following criteria,
	 * event can be either Event, Fact, or Summary
	 * 
	 *  Event: Must contain at least 2 disambigous entities
	 *  Fact: Generic Relation
	 *  Summary: Anything else
	 * 
	 * @param event
	 * @return
	 */
	public static String getAssocType(AssociationPojo event)
	{
		// Count disambiguous entities
		int disambig_count = 0;
		if ( event.getEntity1_index() != null ) disambig_count++;
		if ( event.getEntity2_index() != null ) disambig_count++;
		if ( event.getGeo_index() != null ) disambig_count++;
		
		if ( disambig_count > 1 && ( event.getVerb_category().equals("generic relations") || event.getVerb_category().equals("career") ) )
			return "Fact";
		else if ( disambig_count > 1 )
			return "Event";
		else
			return "Summary";
	}
	
	public static void convertEventPojoToLowerCase(AssociationPojo event)
	{
		if ( null != event.getEntity1() )
			event.setEntity1(event.getEntity1().toLowerCase());
		if ( null != event.getEntity1_index() )
			event.setEntity1_index(event.getEntity1_index().toLowerCase());
		if ( null != event.getEntity2() )
			event.setEntity2(event.getEntity2().toLowerCase());
		if ( null != event.getEntity2_index() )
			event.setEntity2_index(event.getEntity2_index().toLowerCase());
		if ( null != event.getVerb() )
			event.setVerb(event.getVerb().toLowerCase());
		if ( null != event.getVerb_category() )
			event.setVerb_category(event.getVerb_category().toLowerCase());
	}
}
