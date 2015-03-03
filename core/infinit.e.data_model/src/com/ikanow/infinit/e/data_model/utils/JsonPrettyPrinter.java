/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ikanow.infinit.e.data_model.utils;

import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

import org.bson.types.ObjectId;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;


/**
 * @author Tom Baeyens
 */
public class JsonPrettyPrinter {

  public static String toJsonPrettyPrint(DBObject jsonObject) {
    StringBuffer jsonText = new StringBuffer();
    jsonObjectToTextFormatted(jsonObject, 0, jsonText);
    return jsonText.toString();
  }

  static String spaces = "                    ";
  
  public static String jsonObjectToTextFormatted(BasicDBObject jsonObject, int indent) {
	  StringBuffer sb = new StringBuffer();
	  jsonObjectToTextFormatted(jsonObject, indent, sb);
	  return sb.toString();
  }
  public static void jsonObjectToTextFormatted(BasicDBObject jsonObject, int indent, StringBuffer jsonText) {
    jsonText.append("{ ");
    appendNewLine(indent+2, jsonText);
    Set<String> keys = new TreeSet<String>(jsonObject.keySet());
    boolean isFirst = true;
    for (String key : keys) {
    	Object val = jsonObject.get(key);
    	if (null == val) continue;
      if (isFirst) {
        isFirst = false;
      } else {
        jsonText.append(", "); 
        appendNewLine(indent+2, jsonText);
      }
      jsonText.append("\""); 
      jsonText.append(key);
      jsonText.append("\" : ");
      jsonObjectToTextFormatted(val, indent+2, jsonText);
    }
    appendNewLine(indent, jsonText);
    jsonText.append("}");
  }

  public static String jsonObjectToTextFormatted(BasicDBList jsonList, int indent) {
	  StringBuffer sb = new StringBuffer();
	  jsonObjectToTextFormatted(jsonList, indent, sb);
	  return sb.toString();
  }
  @SuppressWarnings("rawtypes")
public static void jsonObjectToTextFormatted(Collection jsonList, int indent, StringBuffer jsonText) {
    jsonText.append("[ ");
    appendNewLine(indent+2, jsonText);
    boolean isFirst = true;
    for (Object element : jsonList) {
    	if (null == element) continue;
      if (isFirst) {
        isFirst = false;
      } else {
        jsonText.append(", "); 
        appendNewLine(indent+2, jsonText);
      }
      jsonObjectToTextFormatted(element, indent+2, jsonText);
    }
    appendNewLine(indent, jsonText);
    jsonText.append("]");
  }

  private static void appendNewLine(int indent, StringBuffer jsonText) {
    jsonText.append("\n");
    jsonText.append(spaces.substring(0,indent));
  }

  public static String jsonObjectToTextFormatted(Object jsonObject, int indent) {
	  StringBuffer sb = new StringBuffer();
	  jsonObjectToTextFormatted(jsonObject, indent, sb);
	  return sb.toString();
  }
  @SuppressWarnings("rawtypes")
public static void jsonObjectToTextFormatted(Object jsonObject, int indent, StringBuffer jsonText) {
    if (jsonObject instanceof BasicDBObject) {
      jsonObjectToTextFormatted((BasicDBObject) jsonObject, indent, jsonText);
    } else if (jsonObject instanceof Collection) {
      jsonObjectToTextFormatted((Collection) jsonObject, indent, jsonText);
    } else if (jsonObject instanceof String) {
      jsonText.append("\""); 
      jsonText.append(((String) jsonObject).replace("\n", "\\n").replace("\"", "\\\""));
      jsonText.append("\"");
    } else if (jsonObject instanceof Number) {
        jsonText.append(jsonObject);
    } else if (jsonObject instanceof Date) {
        jsonText.append("{ \"$date\" : \""+jsonObject.toString()+"\" }"); // (this isn't quite the right format, but only used for display currently)
    } else if (jsonObject instanceof Boolean) {
        jsonText.append(jsonObject);
    } else if (jsonObject instanceof ObjectId) {
      jsonText.append("{ \"$oid\" : \""+jsonObject.toString()+"\" }");
    } else {
      throw new RuntimeException("couldn't pretty print "+jsonObject.getClass().getName());
    }
  }
}