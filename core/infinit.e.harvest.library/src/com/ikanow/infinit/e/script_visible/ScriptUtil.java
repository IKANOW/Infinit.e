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
package com.ikanow.infinit.e.script_visible;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.SimpleTimeZone;

import javax.script.ScriptException;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ikanow.infinit.e.harvest.enrichment.custom.JavaScriptUtils;
import com.ikanow.infinit.e.harvest.enrichment.script.CompiledScriptFactory;

public class ScriptUtil {
	private static final Logger logger = Logger.getLogger(ScriptUtil.class);

    private static ThreadSafeSimpleDateFormat _format = new ThreadSafeSimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    static{
    	_format.setCalendar(new GregorianCalendar(new SimpleTimeZone(0, "UTC")));
    }
    
	private static GsonBuilder gb = new GsonBuilder();
	
	private static Gson _gson = null;
	
	static {
		_gson = gb.create();
	} 

	public static String format(Date d){
		String ds = null;
		if(ds==null){
			ds = _format.format(d);
		}
		return ds;
	}
	public static boolean isObjectArray(Object o){
		return o!=null && (o instanceof Object[]);
	}
	
	/**
	 *  Helper function convering object to json for comparison. 
	 * @param o - Anyobject converted to json
	 * @return String json representation
	 */
	public static String toJson(Object o){
		return _gson.toJson(o);
	}
	
	public static Map<String, Object> extractFields(Object target){
		Map<String,Object> fieldMap = new HashMap<String,Object>();
		if(target!=null){
			Field[] fields = target.getClass().getDeclaredFields();
			for (int i = 0; i < fields.length; i++) {
				Field field = fields[i];
				int modifiers = field.getModifiers();
				String key = field.getName();
				if(!Modifier.isTransient(modifiers) && !Modifier.isStatic(modifiers)){
					field.setAccessible(true);
					try {
						Object value = field.get(target);
						fieldMap.put(key, value);
					} catch (Exception e) {
						logger.error("extractFieldNames caught exception getting value from:"+key,e);
					}
				}
			} // for
		} // if
		return fieldMap; 
	}

	/**
	 *  Helper function to debug black box engine.
	 * @param compiledScriptFactory
	 */
	public static void printEngineState(CompiledScriptFactory compiledScriptFactory) {
		try {
			compiledScriptFactory.executeCompiledScript(JavaScriptUtils.printState);
		} catch (ScriptException e1) {			
			e1.printStackTrace();
		}
		
	}
}
