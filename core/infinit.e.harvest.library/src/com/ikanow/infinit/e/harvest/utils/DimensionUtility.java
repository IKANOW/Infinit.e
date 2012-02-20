package com.ikanow.infinit.e.harvest.utils;

import com.ikanow.infinit.e.data_model.InfiniteEnums.EntityType;
import com.ikanow.infinit.e.data_model.store.document.EntityPojo;
import com.ikanow.infinit.e.data_model.store.document.EntityPojo.Dimension;

public class DimensionUtility {

	public static EntityPojo.Dimension getDimensionByType(String type)
	{
		EntityType et = EntityType.valueOf(type.toLowerCase());
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
			default:
				dimension = Dimension.What;
				break;
		}
		return dimension;
	}
}
