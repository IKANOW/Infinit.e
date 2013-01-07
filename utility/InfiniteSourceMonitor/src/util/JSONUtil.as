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
package util
{
	import com.adobe.serialization.json.*;
	
	/**
	 * Converts model representations between a serialized and deserialized state.
	 *
	 * <p>Makes extensive use of reflection to instantiate fully-qualified object types based on
	 * the Class-specific decoder method called.</p>
	 *
	 */
	public class JSONUtil
	{
		
		//======================================
		// public static methods 
		//======================================
		
		/**
		 * Deserializes a json string into an actionscript object.
		 * Fields can then be accessed using object.field or object[field]
		 * calls.
		 *
		 * @param jsonData The json string to be deserialized.
		 * @return Object created from the json string.
		 */
		public static function decode( jsonData:String ):Object
		{
			var jd:JSONDecoder;
			var value:Object;
			
			try
			{
				jd = new JSONDecoder( jsonData, true );
				value = jd.getValue();
			}
			catch ( e:Error )
			{
				trace( "JSON decode error: " + e.message );
			}
			
			return value;
		}
		
		/**
		 * Serializes an actionscript object into a json string.
		 *
		 * @param asObject The actionscript object to be serialized.
		 * @return json String created from the actionscript object.
		 */
		public static function encode( asObject:Object ):String
		{
			var je:JSONEncoder = new JSONEncoder( asObject );
			return je.getString();
		}
		
		public static function formatJson( input:String ):String
		{
			var quote:String;
			var output:String = "";
			var depth:int = 0;
			var escaped:Boolean = false;
			
			for ( var i:int = 0; i < input.length; i++ )
			{
				var ch:String = input.charAt( i );
				
				// (Handle escaping at the top)
				if ( ch == '\\' )
				{
					if ( quote != null ) // (else ignore)
					{
						escaped = !escaped;
					}
				}
				
				switch ( ch )
				{
					case '{':
					case '[':
						output += ch;
						if ( quote == null )
						{
							output += "\n";
							depth++;
							output += indent( depth );
						}
						break;
					case '}':
					case ']':
						if ( quote != null )
							output += ch;
						else
						{
							output += "\n"
							depth--;
							output += indent( depth );
							output += ch;
						}
						break;
					case '"':
						output += ch;
						if ( quote == '"' && !escaped )
						{
							quote = null;
						}
						else if ( quote == null )
							quote = ch;
						break;
					case '\'':
						output += ch;
						if ( quote == '\'' && !escaped )
						{
							quote = null;
						}
						else if ( quote == null )
							quote = ch;
						break;
					case ',':
						output += ch;
						if ( quote == null )
						{
							output += "\n";
							output += indent( depth );
						}
						break;
					case ':':
						if ( quote != null )
							output += ch;
						else
							output += ": ";
						break;
					case null:
						break;
					default:
						if ( quote != null || ch.toString() != " " )
							output += ch;
						break;
				}
				
				// Escaped state only persists for 1-char after the \
				if ( ch != '\\' )
				{
					escaped = false;
				}
			}
			return output;
		}
		
		public static function indent( depth:int ):String
		{
			var toReturn:String = "";
			
			for ( var i:int = 0; i < depth; i++ )
			{
				toReturn += "\t";
			}
			return toReturn;
		}
	}

}
