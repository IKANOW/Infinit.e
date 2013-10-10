package com.ikanow.infinit.e.harvest.test;


import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import org.junit.Before;
import org.junit.Test;

import com.ikanow.infinit.e.data_model.Globals;
import com.ikanow.infinit.e.data_model.api.ApiManager;
import com.ikanow.infinit.e.data_model.api.config.SourcePojoApiMap;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.store.document.EntityPojo;
import com.ikanow.infinit.e.harvest.HarvestController;
import com.ikanow.infinit.e.harvest.enrichment.custom.JavascriptSecurityManager;

public class TestSecurityManager 
{
	private static JavascriptSecurityManager jsm;
	private static ScriptEngine engine;
	private static HarvestController harvester;	
	
	@Before
	public void setUp() throws Exception 
	{		
		Globals.setIdentity(com.ikanow.infinit.e.data_model.Globals.Identity.IDENTITY_SERVICE);
		Globals.overrideConfigLocation(System.getProperty("CONFIG_LOCATION"));
		
		ScriptEngineManager factory = new ScriptEngineManager();
		engine = factory.getEngineByName("JavaScript");		
		
		jsm = new JavascriptSecurityManager();
		//jsm.setPermissions();
	}
	
	private void harvesterSetup() throws IOException
	{
		
		harvester = new HarvestController();
		harvester.setStandaloneMode(1, false);	
	}
	
	@Test
	public void testJAVASCRIPTFileAccess()
	{
		//String file = "src/com/ikanow/infinit/e/harvest/test/testread.txt";
		String file = "C:/Users/Burch/Desktop/w4.txt";
		String ret = null;
		
		engine.put("url", file);	
		try
		{
			ret = (String)jsm.eval(engine, "var fr = new java.io.FileReader(url);\n var br = new java.io.BufferedReader(fr);\n var fullstr = '';\n var str;\n while ((str = br.readLine())!=null)\n fullstr+=str;\n br.close();\n fullstr;");
			System.out.println("testJAVASCRIPTFileAccess: " + ret);
		}
		catch (Exception ex)
		{
			return;
		}	
		fail();
	}
	
	@Test
	public void testJAVAFileAccess()
	{
		String file = "src/com/ikanow/infinit/e/harvest/test/testread.txt";		
		//Test file access in java on valid file - expected results, print out file text	
		try
		{
			BufferedReader in = new BufferedReader(new FileReader(file));
			StringBuilder sb = new StringBuilder();
			String str;
			while ((str = in.readLine()) != null)
				sb.append(str);
			in.close();	
			System.out.println("testJAVAFileAccess: " + sb.toString());
		}
		catch (Exception ex)
		{
			System.out.println("testJAVAFileAccess: " + ex.getMessage());
			fail();
		}		
	}

	
	
	@Test
	public void testJAVASCRIPTRemoteUrlAccess()
	{
		//Test doing a valid api call - expected results, print out some json
		String apicall = "importPackage(java.net);" + 
			"importPackage(java.util);" + 
			"importPackage(java.io);" +
			"importPackage(java.lang);" + 
			"importPackage(net.sf.json);" + 
			"importPackage(net.sf.json.JSONSerializer);" + 
			"var myURL = new java.net.URL('http://dev.ikanow.com/api/auth/login/ping/ping');" + 
			"var urlConnect = myURL.openConnection();\n" + 
			"var retVal = urlConnect.getInputStream();\n" + 
			"var is = new InputStreamReader(retVal);\n" +
			"var sb = new StringBuilder();\n" +
			"var br = new BufferedReader(is);\n" +
			"var read = br.readLine();\n" +
			"while ( read != null ) { sb.append(read); read = br.readLine(); }\n" +
			"var json = sb.toString();\n" +			
			"retVal.close();\n" +
			"json";		
		String ret = null;
		try
		{
			ret = (String)jsm.eval(engine, apicall);
			System.out.println("testJAVASCRIPTRemoteUrlAccess: " + ret);
		}
		catch (Exception ex)
		{
			System.out.println("testJAVASCRIPTRemoteUrlAccess: " + ex.getMessage());
			fail();
			return;
		}		
	}
	
	@Test
	public void testJAVARemoteUrlAccess()
	{
		//Test doing a valid api call - expected results, print out some json					
		try
		{
			URL myURL = new URL("http://dev.ikanow.com/api/auth/login/ping/ping");
			URLConnection urlConnect = myURL.openConnection();
			InputStream is = urlConnect.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			StringBuilder sb = new StringBuilder();
			BufferedReader br = new BufferedReader(isr);
			String read = br.readLine();
			while ( read != null )
			{
				sb.append(read);
				read = br.readLine();
			}
			System.out.println("testJAVARemoteUrlAccess: " + sb.toString());
		}
		catch (Exception ex)
		{
			fail();
			return;
		}	
	}
	
	@Test
	public void testJAVASCRIPTLocalUrlAccess()
	{
		String apicall = "importPackage(java.net);" + 
			"importPackage(java.util);" + 
			"importPackage(java.io);" +
			"importPackage(java.lang);" + 
			"importPackage(net.sf.json);" + 
			"importPackage(net.sf.json.JSONSerializer);" + 
			"var myURL = new java.net.URL('http://localhost:8184/auth/login/ping/ping');" + 
			"var urlConnect = myURL.openConnection();\n" + 
			"var retVal = urlConnect.getInputStream();\n" + 
			"var is = new InputStreamReader(retVal);\n" +
			"var sb = new StringBuilder();\n" +
			"var br = new BufferedReader(is);\n" +
			"var read = br.readLine();\n" +
			"while ( read != null ) { sb.append(read); read = br.readLine(); }\n" +
			"var json = sb.toString();\n" +			
			"retVal.close();\n" +
			"json";		
		String ret = null;
		try
		{
			ret = (String)jsm.eval(engine, apicall);
			System.out.println("testJAVASCRIPTLocalUrlAccess: " + ret);
		}
		catch (Exception ex)
		{
			return;
		}	
		fail();		
	}
	
	//This test fails because javascript attempts to use the reader to access the file
	//which i assume breaks the security process (i thought java might have used it to
	//open the file and we'd get away with it).
	/*@Test
	public void testJAVASCRIPTScript()
	{
		//Test doing a valid api call - expected results, print out some json
		String file = "src/com/ikanow/infinit/e/harvest/test/testjavascript.js";	
		String ret = null;
		try
		{
			ret = (String)jsm.eval(engine, new FileReader(file));
			System.out.println("testJAVASCRIPTScript: " + ret);
		}
		catch (Exception ex)
		{
			System.out.println("testJAVASCRIPTScript: " + ex.getMessage());
			fail();
			return;
		}		
	}*/
	
	//We can't turn off the thread manipulation because things like
	//url.openConnection fail then, not worrying about it currently.
	/*@Test
	public void testJAVASCRIPTStopThread()
	{		
		try
		{
			jsm.eval(engine, "java.lang.Thread.currentThread().stop();");				
		}
		catch (Exception ex)
		{			
			return;
		}	
		fail();
	}*/
	
	/*@Test
	public void testJAVAChangeSecurityManager()
	{				
		try
		{
			System.setSecurityManager(new SocketSecurityManager());
		}
		catch (Exception ex)
		{		
			System.out.println("testJAVAChangeSecurityManager: " + ex.getMessage());
			fail();
		}		
	}
	
	@Test
	public void testJAVASCRIPTChangeSecurityManager()
	{		
		//im not sure why you can change the security manager, you should not be allowed to
		String ret = null;
		try
		{
			ret = (String)jsm.eval(engine, "java.lang.System.setSecurityManager(new com.ikanow.infinit.e.harvest.enrichment.custom.SocketSecurityManager());");
			System.out.println("testJAVASCRIPTChangeSecurityManager: " + ret);
		}
		catch (Exception ex)
		{			
			return;
		}	
		fail();
	}*/
	
	@Test
	public void testJAVASCRIPTExec()
	{		
		String ret = null;
		try
		{
			ret = (String)jsm.eval(engine, "java.lang.Runtime.getRuntime().exec('dir');");
			System.out.println("testJAVASCRIPTExec: " + ret);
		}
		catch (Exception ex)
		{			
			return;
		}	
		fail();
	}
	
	@Test
	public void testStructuredSourceTest() throws IOException
	{
		harvesterSetup();
		try
		{
			String file = "src/com/ikanow/infinit/e/harvest/test/teststructuredsource.json";				
			BufferedReader in = new BufferedReader(new FileReader(file));
			StringBuilder sb = new StringBuilder();
			String str;
			while ((str = in.readLine()) != null)
				sb.append(str);
			in.close();
			SourcePojo source = ApiManager.mapFromApi(sb.toString(), SourcePojo.class, new SourcePojoApiMap(null));
			
			harvester.setStandaloneMode(1, false);
			List<DocumentPojo> toAdd = new LinkedList<DocumentPojo>();
			List<DocumentPojo> toUpdate = new LinkedList<DocumentPojo>();
			List<DocumentPojo> toRemove = new LinkedList<DocumentPojo>();
			harvester.harvestSource(source, toAdd, toUpdate, toRemove);
			
			//make sure there is an entity named 'TEST_ENTITY'
			boolean foundTestEntity = false;
			for (DocumentPojo doc: toAdd) 
			{
				for ( EntityPojo ent : doc.getEntities())
				{
					if ( ent.getDisambiguatedName().equals("TEST_ENTITY"))
					{
						foundTestEntity = true;
						break;
					}
				}								
			}			
			assertTrue(foundTestEntity);
			
		}
		catch(Exception ex)
		{
			System.out.println("testStructuredSourceTest: " + ex.getMessage());
			fail();
		}
	}
	
	//TODO (INF-2118): This didn't work, amongst other things it stopped regexes from working
//	@Test
//	public void testReflection()
//	{	
//		String js =
//			"var person = java.lang.Class.forName(\"com.ikanow.infinit.e.data_model.store.social.person.PersonPojo\");" +
//			"person.getDeclaredField(\"_id\").setAccessible(true);";
//		String ret = null;
//		try
//		{
//			ret = (String)jsm.eval(engine, js);
//			System.out.println("testReflection: " + ret);
//			fail();	//should have triggered security manager
//		}
//		catch (Exception ex)
//		{
//			//this should reach here
//		}	
//	}
	
	@Test
	public void testLocalJavaAfterLocalJavascriptTest()
	{
		//first make a local call that will trigger the sec manager
		String apicall = "importPackage(java.net);" + 
		"importPackage(java.util);" + 
		"importPackage(java.io);" +
		"importPackage(java.lang);" + 
		"importPackage(net.sf.json);" + 
		"importPackage(net.sf.json.JSONSerializer);" + 
		"var myURL = new java.net.URL('http://localhost:8184/auth/login/ping/ping');" + 
		"var urlConnect = myURL.openConnection();\n" + 
		"var retVal = urlConnect.getInputStream();\n" + 
		"var is = new InputStreamReader(retVal);\n" +
		"var sb = new StringBuilder();\n" +
		"var br = new BufferedReader(is);\n" +
		"var read = br.readLine();\n" +
		"while ( read != null ) { sb.append(read); read = br.readLine(); }\n" +
		"var json = sb.toString();\n" +			
		"retVal.close();\n" +
		"json";		
		String ret = null;
		try
		{
			ret = (String)jsm.eval(engine, apicall);
			System.out.println("testLocalJavaAfterLocalJavascriptTest: " + ret);
			fail();	//should have triggered security manager
		}
		catch (Exception ex)
		{
			//this should reach here
		}	
		//now that the security man is on, try calling a local java call to make sure it still works
		//Test doing a valid api call - expected results, print out some json					
		try
		{
			URL myURL = new URL("http://localhost:8184/auth/login/ping/ping");
			URLConnection urlConnect = myURL.openConnection();
			InputStream is = urlConnect.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			StringBuilder sb = new StringBuilder();
			BufferedReader br = new BufferedReader(isr);
			String read = br.readLine();
			while ( read != null )
			{
				sb.append(read);
				read = br.readLine();
			}
			System.out.println("testLocalJavaAfterLocalJavascriptTest: " + sb.toString());
			//should make it through successfully
		}
		catch (Exception ex)
		{
			fail();
			return;
		}	
	}
	
	@Test
	public void testUnstructuredSourceTest() throws IOException
	{		
		harvesterSetup();
		try
		{
			String file = "src/com/ikanow/infinit/e/harvest/test/testunstructuredsource.json";				
			BufferedReader in = new BufferedReader(new FileReader(file));
			StringBuilder sb = new StringBuilder();
			String str;
			while ((str = in.readLine()) != null)
				sb.append(str);
			in.close();
			SourcePojo source = ApiManager.mapFromApi(sb.toString(), SourcePojo.class, new SourcePojoApiMap(null));
			
			harvester.setStandaloneMode(1, false);
			List<DocumentPojo> toAdd = new LinkedList<DocumentPojo>();
			List<DocumentPojo> toUpdate = new LinkedList<DocumentPojo>();
			List<DocumentPojo> toRemove = new LinkedList<DocumentPojo>();
			harvester.harvestSource(source, toAdd, toUpdate, toRemove);
			
			//make sure there is an entity named 'TEST_ENTITY'
			boolean foundTestEntity = false;
			for (DocumentPojo doc: toAdd) 
			{
				for (Object meta : doc.getMetadata().get("test") )
				{
					if ( ((String) meta).equals("TEST_ENTITY"))
					{
						foundTestEntity = true;
						break;
					}						
				}							
			}			
			assertTrue(foundTestEntity);
			
		}
		catch(Exception ex)
		{
			System.out.println("testUnstructuredSourceTest: " + ex.getMessage());
			fail();
		}
	}
	
	@Test
	public void testCacheSourceTest() throws IOException
	{		
		harvesterSetup();
		try
		{
			String file = "src/com/ikanow/infinit/e/harvest/test/testcachesource.json";				
			BufferedReader in = new BufferedReader(new FileReader(file));
			StringBuilder sb = new StringBuilder();
			String str;
			while ((str = in.readLine()) != null)
				sb.append(str);
			in.close();
			SourcePojo source = ApiManager.mapFromApi(sb.toString(), SourcePojo.class, new SourcePojoApiMap(null));
						
			List<DocumentPojo> toAdd = new LinkedList<DocumentPojo>();
			List<DocumentPojo> toUpdate = new LinkedList<DocumentPojo>();
			List<DocumentPojo> toRemove = new LinkedList<DocumentPojo>();
			harvester.harvestSource(source, toAdd, toUpdate, toRemove);
			
			//make sure there is an entity named 'TEST_ENTITY'
			boolean foundTestEntity = false;
			boolean foundTestMeta = false;
			for (DocumentPojo doc: toAdd) 
			{
				for ( EntityPojo ent : doc.getEntities())
				{
					if ( ent.getDisambiguatedName().equals("frances2"))
					{
						foundTestEntity = true;
						break;
					}
				}	
				
				for (Object meta : doc.getMetadata().get("test") )
				{
					if ( ((String) meta).equals("frances2"))
					{
						foundTestMeta = true;
						break;
					}						
				}	
			}			
			assertTrue(foundTestEntity);
			assertTrue(foundTestMeta);						
		}
		catch(Exception ex)
		{
			System.out.println("testCacheSourceTest: " + ex.getMessage());
			fail();
		}
	}
	
	@Test
	public void testStructuredSourceRemoteCallTest() throws IOException
	{
		harvesterSetup();
		try
		{
			String file = "src/com/ikanow/infinit/e/harvest/test/teststructuredsourceremotecall.json";				
			BufferedReader in = new BufferedReader(new FileReader(file));
			StringBuilder sb = new StringBuilder();
			String str;
			while ((str = in.readLine()) != null)
				sb.append(str);
			in.close();
			SourcePojo source = ApiManager.mapFromApi(sb.toString(), SourcePojo.class, new SourcePojoApiMap(null));
			
			harvester.setStandaloneMode(1, false);
			List<DocumentPojo> toAdd = new LinkedList<DocumentPojo>();
			List<DocumentPojo> toUpdate = new LinkedList<DocumentPojo>();
			List<DocumentPojo> toRemove = new LinkedList<DocumentPojo>();
			harvester.harvestSource(source, toAdd, toUpdate, toRemove);
			
			//make sure there is NOT an entity named 'TEST_ENTITY', should fail the js with sec error
			boolean foundTestEntity = false;
			for (DocumentPojo doc: toAdd) 
			{
				for ( EntityPojo ent : doc.getEntities())
				{
					if ( ent.getDisambiguatedName().equals("TEST_ENTITY"))
					{
						foundTestEntity = true;
						break;
					}
				}								
			}			
			assertTrue(!foundTestEntity);
			
		}
		catch(Exception ex)
		{
			System.out.println("testStructuredSourceTest: " + ex.getMessage());
			fail();
		}
	}
}
