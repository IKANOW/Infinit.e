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
package com.ikanow.infinit.e.harvest.enrichment.script;

import java.util.Date;
import java.util.LinkedHashMap;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

// TODO comment in for jdkversion >= 1.8 
//import jdk.nashorn.api.scripting.ScriptObjectMirror;




import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.document.ChangeAwareDocumentWrapper;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.utils.IkanowSecurityManager;
import com.ikanow.infinit.e.harvest.DefaultHarvestContext;
import com.ikanow.infinit.e.harvest.HarvestContext;
import com.ikanow.infinit.e.harvest.enrichment.custom.JavaScriptUtils;
import com.ikanow.infinit.e.harvest.enrichment.script.CompiledScriptFactory;
import com.ikanow.infinit.e.harvest.enrichment.script.ScriptEngineContextAttributeNotifier;
import com.ikanow.infinit.e.harvest.enrichment.script.ScriptCallbackNotifier;
import com.ikanow.infinit.e.harvest.extraction.document.DuplicateManager;
import com.ikanow.infinit.e.harvest.extraction.document.HarvestStatus;
import com.mongodb.BasicDBObject;
import com.mongodb.util.JSON;



@SuppressWarnings("unused")
public class JavaScriptInternalObjectTest {
	private static final Logger logger = Logger.getLogger(JavaScriptInternalObjectTest.class);

	public JavaScriptInternalObjectTest(){
	}

	
//	static final String SCRIPT = "var foo = 'Hello World!';\n" + "result.setWords(foo.split(' '));foo;";
	private static String SCRIPT = "var _doc = eval('(' + document + ')'); \n";
	// returns 
	private static String SCRIPT1 = "var x = 'test'; x;";
	// returns com.sun.script.javascript.ExternalScriptable
	private static String SCRIPT2 = "var ff = function() { var x = 'test'; x; return this;};ff();";
	// returns sun.org.mozilla.javascript.internal.InterpretedFunction
	private static String SCRIPT3 = "var ff = function() { var x = 'test'; x; return this;}; ff;";
	private static String JSONOBJECT = "";
	private static String SCRIPT4 = "var title = _doc.title;print('title:'+title);print('meta.lastname:'+_doc.metadata.lastname[0]);print('_doc.metadata.csv[0].type:'+_doc.metadata.csv[0].type)";
	//this does not work
	private static String SCRIPT_JOIN = "var cArray = _doc.metadata.correlations_arrayList[0]; var joinStr = cArray.join('and'); print('join:'+joinStr);";
	// this works, built-in array
	//private static String SCRIPT_JOIN = "var cArray = [];cArray.push('a_0');cArray.push('a_1');cArray.push('a_2'); var joinStr = cArray.join('and'); print('join:'+joinStr);";
	//private static String SCRIPT_JOIN = "var doc2 = Java.from(_doc); var cArray = _doc2.metadata.correlations_array; var joinStr = cArray.join('and'); print('join:'+joinStr);";
	private static String SCRIPT_EVAL = "var _doc = eval('(' + document + ')'); print(_doc.metadata['json'].length);\n";
	private static String SCRIPT_DIRECT_ASSIGN = "var _doc = document; print(_doc.metadata['json'].length);\n";
	private static String SCRIPT_SETMEMBER = "_doc.metadata['newMember'] = ['a','b']; \n";
	private static String RETURN_JSON = "JSON.stringify(_doc)";
	private static String SCRIPT_ASSIGN_ARRAY = "var _doc = document;var _metadataArray = Java.from(_doc.metadata['correlations_array'])";
	private static String SCRIPT_DOC_2JSON = "var _json = JSON.stringify(_docPojo.title); _json;\n";
	private static String SCRIPT_PRINT_DOC_2JSON = "print(JSON.stringify(_doc));";

	DocumentPojo doc = null;
	ScriptEngineManager manager = null;
	ScriptEngine scriptEngine = null;
	String docStr = null;
	String documentJson1 = null;
	DocumentPojo doc1 = null;
	CompiledScriptFactory compiledScriptFactory = null;

	protected void setup(){
		doc = new DocumentPojo();
		try {
			HarvestContext context = new DefaultHarvestContext(){

				@Override
				public IkanowSecurityManager getSecurityManager() {
					return new IkanowSecurityManager(true);
				}
				
			};

		compiledScriptFactory = new CompiledScriptFactory(new SourcePojo(),context);
    	// COMPILED_SCRIPT initialization 
        this.compiledScriptFactory.executeCompiledScript(CompiledScriptFactory.GLOBAL);

		manager = new ScriptEngineManager();
		scriptEngine = compiledScriptFactory.getEngine();
		GsonBuilder gb = new GsonBuilder();
		Gson g = gb.create();
		doc.setTitle("DocumentPojo_Title");
		doc.setUrl("http://www.google.com");
		doc.setDescription("DocumentPojo_Description");
		LinkedHashMap<String, Object[]> metadata =  new LinkedHashMap<String, Object[]>();
		//CSV csv0 = new CSV();
		//csv0.type="csv0_type";
		//csv0.commentName="commentName0";
		//CSV csv1 = new CSV();
		//csv1.type="csv1_type";
		//csv1.commentName="commentName1";
		//metadata.put("csv", new Object[]{csv0,csv1});
		metadata.put("lastname", new Object[]{"lastname_val"});
		metadata.put("programlist", new Object[]{"programlist_0"});
		// _doc.metadata.json[0].correlations_array
		metadata.put("correlations_array", new Object[]{"c_0","c_1","c_2"});
		//ArrayList<String> cal = new ArrayList<String>();
		//cal.add("a_0");
		//cal.add("a_1");
		//cal.add("a_2");
		//metadata.put("correlations_arrayList", new Object[]{cal});
		doc.setMetadata(metadata);
		docStr = g.toJson(doc);
			documentJson1 = IOUtils.toString(JavaScriptInternalObjectTest.class.getResourceAsStream("../custom/data/document2.json"));		
			doc1 = DocumentPojo.fromDb((BasicDBObject)JSON.parse(documentJson1), DocumentPojo.class);		
		} catch (Exception e) {
			logger.error(e);
		}
		
	}
	
	public void testMapAccessibility(){
		ScriptContext context = scriptEngine.getContext();
		context.setAttribute("_doc", doc,ScriptContext.GLOBAL_SCOPE);
//		context.setAttribute("result", result, ScriptContext.ENGINE_SCOPE);
		Object retval = null;
		try {
			String script = SCRIPT4;
			//retval = scriptEngine.eval(script);
			//System.out.println("Evaluated retval:"+retval);
			Compilable compilingEngine = (Compilable)scriptEngine;

			CompiledScript cs = compilingEngine.compile(script);
			retval = cs.eval(context);			
			System.out.println("Compiled retval:"+retval);
		} catch (ScriptException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Context:"+context);	
	}
	
	public void testArrayJoin(){
		// test for TypeError: [Ljava.lang.Object;@4b2c5e02 has no such function "join" in <eval> at line number 1

		ScriptContext context = scriptEngine.getContext();
		context.setAttribute("_doc", doc,ScriptContext.ENGINE_SCOPE);
		Object retval = null;
		try {
			String script = SCRIPT_EVAL;
			Compilable compilingEngine = (Compilable)scriptEngine;
			CompiledScript cs = compilingEngine.compile(script);
			retval = cs.eval(context);
			System.out.println("Compiled retval:"+retval);
		} catch (ScriptException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Context:"+context);	
	}

	public void testDocumentObjectWrapping(){
		// test for TypeError: [Ljava.lang.Object;@4b2c5e02 has no such function "join" in <eval> at line number 1
		// TODOcomment in for jdkno >= JDK1.8
/*		ScriptContext context = scriptEngine.getContext();
		//context.setAttribute("document", documentJson1,ScriptContext.ENGINE_SCOPE);
		context.setAttribute("document", doc,ScriptContext.ENGINE_SCOPE);
		Object retval = null;
		try {
			//String script = SCRIPT_EVAL;
			//String script = SCRIPT_DIRECT_ASSIGN;
			String script = SCRIPT_ASSIGN_ARRAY;
			Compilable compilingEngine = (Compilable)scriptEngine;
			CompiledScript cs = compilingEngine.compile(script);
			retval = cs.eval(context);
			System.out.println("Compiled retval:"+retval);
			Object _doc =context.getAttribute("_doc");
			Object metadataArray = (ScriptObjectMirror)context.getAttribute("_metadataArray");
			
			Object unwrapDoc = ScriptObjectMirror.unwrap(_doc, null);
	*/		
			/*ScriptObjectMirror _metadata = (ScriptObjectMirror)_doc.getMember("metadata");
			ScriptObjectMirror json = (ScriptObjectMirror)_metadata.getMember("json");
			ScriptObjectMirror json0 = (ScriptObjectMirror)json.getSlot(0);
			ScriptObjectMirror correlationsArrray = (ScriptObjectMirror)json0.getMember("correlations_array");
			// add some new stuff
			_metadata.put("stringMember", "stringvalue"); 
			Object strMember = _metadata.get("stringMember");
			System.out.println(strMember.getClass());
			//System.out.println("Internal _doc :"+_doc);
			CompiledScript cs2 = compilingEngine.compile(SCRIPT_SETMEMBER);
			cs2.eval(context);
			CompiledScript cs3 = compilingEngine.compile(RETURN_JSON);
			String retJson = (String)cs3.eval(context);
			Object newMember = _metadata.get("newMember");
			System.out.println(newMember.getClass());
			System.out.println(retJson); */
/*		} catch (ScriptException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Context:"+context);
		*/	
	}
	
	public void testDocumentObjectToJavascript(){
			// test for TypeError: [Ljava.lang.Object;@4b2c5e02 has no such function "join" in <eval> at line number 1

			ScriptContext context = compiledScriptFactory.getScriptContext();
			context.setAttribute("_debug", true,ScriptContext.ENGINE_SCOPE);
			context.setAttribute("_dirtyDoc", true,ScriptContext.ENGINE_SCOPE);
			ChangeAwareDocumentWrapper caDoc1 = new ChangeAwareDocumentWrapper(doc1);
			
			ScriptCallbackNotifier attributeChangeListener = new ScriptCallbackNotifier(compiledScriptFactory,JavaScriptUtils.setDocumentAttributeScript,JavaScriptUtils.docAttributeName,JavaScriptUtils.docAttributeValue);
			caDoc1.setAttributeChangeListener(attributeChangeListener);
			ScriptEngineContextAttributeNotifier dirtyChangeListener = new ScriptEngineContextAttributeNotifier(compiledScriptFactory,"_dirtyDoc",true);
			caDoc1.setDirtyChangeListener(dirtyChangeListener);
			context.setAttribute("_docPojo", caDoc1.getDelegate(),ScriptContext.ENGINE_SCOPE);
//			context.setAttribute("caDocPojo", caDoc1,ScriptContext.ENGINE_SCOPE);
			Object retval = null;
			try {
				String script  = JavaScriptUtils.getCheckDirtyDoc();
				Compilable compilingEngine = (Compilable)scriptEngine;
				CompiledScript csOm2js = compilingEngine.compile(script);
				retval = csOm2js.eval(context);
				CompiledScript csPrintJson = compilingEngine.compile(SCRIPT_PRINT_DOC_2JSON);
				retval = csPrintJson.eval(context);
				caDoc1.setTitle("Modified Title");
				caDoc1.setPublishedDate(new Date());
				retval = csPrintJson.eval(context);
				// this should trigger the new dirty
				caDoc1.getMetadata().put("newM",new Object[]{"m1","m2"});
				retval = csOm2js.eval(context);
				
				retval = csPrintJson.eval(context);
				//System.out.println("Compiled retval:"+retval);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				logger.error("Caught an exception:",e);
			}
		}
			
	
	public static void main(String[] args) {
		JavaScriptInternalObjectTest test= new JavaScriptInternalObjectTest();
		test.setup();
		//test.testMapAccessibility();
		//test.testArrayJoin();
		test.testDocumentObjectToJavascript();
	}

}
