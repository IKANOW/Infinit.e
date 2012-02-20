package com.ikanow.infinit.e.widget.library.utility
{
	public class InfiniteIcons
	{
		/**
		 * function to get an icon based on the category
		 * 
		 * @param type The type of the entity
		 * 
		 * @return The icon of the entity
		 */
		public static function getIcon(type:String):String
		{
			type = type.toLowerCase();
			switch (type)
			{
				case "person": 					return "person.png"; 	break;
				case "personvictim":			return "person.png"; 	break;
				case "personsuspect":			return "person.png"; 	break;
				case "criminalactivity":		return "needle.png";	break;
				case "place":					return "city.png"; 		break;
				case "city":					return "city.png"; 		break;
				case "region":					return "globe.png";		break;
				case "country": 				return "globe.png";		break;
				case "continent": 				return "globe.png";		break;
				case "stateorcounty":			return "globe.png";		break;
				case "provinceorstate":			return "globe.png";		break;
				case "company":					return "desk.png";		break;
				case "organization":			return "desk.png";		break;
				case "healthcondition":			return "firstaid.png";	break;
				case "drug":					return "needle.png";	break;
				case "sport":					return "soccer.png";	break;
				case "sportingevent":			return "soccer.png";	break;
				case "facility":				return "facility.png"; 	break;
				case "geographicfeature":		return "mountain.png";	break;
				case "entertainmentaward":		return "award.png";		break;
				case "movie":					return "movie.png";		break;
				case "televisionstation":		return "movie.png";		break;
				case "televisionshow": 			return "movie.png";		break;
				case "musicgroup":				return "music.png";		break;
				case "radiostation":			return "music.png";		break;
				case "financialmarketindex":	return "stocks.png";	break;
				case "automobile":				return "car.png";		break;
				case "printmedia":				return "news.png";		break;
				case "holiday":					return "holiday.png";	break;
				case "naturaldisaster":			return "storm.png";		break;
				case "fieldterminology":		return "misc.png";		break;
				case "technology":				return "misc.png";		break;
				case "product":					return "misc.png";		break;
				default:						return "misc.png";		break;
			}
		}
	}
}