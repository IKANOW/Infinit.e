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
package com.ikanow.infinit.e.data_model.utils;

import com.ikanow.infinit.e.data_model.InfiniteEnums.EntityType;
import com.ikanow.infinit.e.data_model.store.document.EntityPojo;
import com.ikanow.infinit.e.data_model.store.document.EntityPojo.Dimension;

public class DimensionUtility {

	public static EntityPojo.Dimension getDimensionByType(String type)
	{
		EntityType et = null;
		try {
			et = EntityType.valueOf(type.toLowerCase());
		}
		catch (Exception e) {
			return Dimension.What;
		}
		EntityPojo.Dimension dimension = null;
		switch (et)
		{
			case city:
			case continent:
			case country:
			case geographicfeature:
			case provinceorstate:
			case region:
			case stateorcounty:
				dimension = Dimension.Where;
				break;	
			case anniversary:
			case automobile:
			case currency:
			case drug:
			case emailaddress:
			case entertainmentaward:
			case entertainmentawardevent:
			case facility:
			case faxnumber:
			case fieldterminology:
			case financialmarketindex:			
			case healthcondition:
			case holiday:
			case industryterm:
			case marketindex:
			case medicalcondition:
			case medicaltreatment:
			case movie:			
			case musicalbum:
			case naturaldisaster:
			case naturalfeature:
			case operatingsystem:
			case phonenumber:
			case politicalevent:
			case printmedia:
			case product:
			case programminglanguage:
			case publishedmedium:
			case radioprogram:
			case sport:		
			case sportingevent:
			case sportsevent:
			case sportsgame:
			case sportsleague:
			case technology:
			case televisionshow:
			case tvshow:
			case tvstation:
			case url:
				dimension = Dimension.What;
				break;
			case company:						
			case musicgroup:
			case organization:
			case person:
			case position:
			case radiostation:
			case televisionstation:
				dimension = Dimension.Who;
				break;
			case date:
				dimension = Dimension.When;
			default:
				dimension = Dimension.What;
				break;
		}
		return dimension;
	}
}
