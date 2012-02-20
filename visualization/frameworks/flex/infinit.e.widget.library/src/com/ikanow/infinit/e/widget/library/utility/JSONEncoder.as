/**
 * <p>Infinit.e</p>
 *
 * <p>Copyright (c) 2011 IKANOW, llc.</p>
 * <p>http://www.ikanow.com</p> 
 *
 * <p>NOTICE:  IKANOW permits you to use this this file in accordance with the terms of the license agreement 
 * accompanying it.  For information about the licensing and copyright of this Plug-In please contact IKANOW, llc. 
 * at customer&#64;ikanow.com.</p>
 *
 * <p>THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.</p>
 * 
 */
package com.ikanow.infinit.e.widget.library.utility
{
	import com.adobe.serialization.json.JSONEncoder;

	/**
	 * Helper class used to decoded JSON returned from
	 * infinit.e API calls.
	 */
	public class JSONEncoder
	{
		/**
		 * Deserializes a json string into an actionscript object.
		 * Fields can then be accessed using object.field or object[field]
		 * calls.
		 * 
		 * @param jsonData The json string to be deserialized.
		 * @return Object created from the json string.
		 */
		public static function encode(jsonObject:Object):String
		{
			var je:com.adobe.serialization.json.JSONEncoder = new com.adobe.serialization.json.JSONEncoder(jsonObject);
			return je.getString();			
		}				
	}
}