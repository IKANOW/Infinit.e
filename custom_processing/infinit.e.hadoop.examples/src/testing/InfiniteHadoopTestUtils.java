package testing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.gson.GsonBuilder;
import com.ikanow.infinit.e.data_model.api.BaseApiPojo;
import com.ikanow.infinit.e.data_model.api.knowledge.DocumentPojoApiMap;
import com.ikanow.infinit.e.data_model.store.MongoDbConnection;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.mongodb.MongoException;

public class InfiniteHadoopTestUtils {

//////////////////////////////////////////////////////////////////////////////////////////////////

	// Run Hadoop inside Eclipse
	
	public static int runStandalone(Tool runner, String configPath, String [] args) throws Exception
	{
		Configuration config = new Configuration();
		config.set("mapred.job.tracker", "local");
		config.set("fs.default.name", "local");
		// Now read in via XML
		File fXmlFile = new File(configPath);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(fXmlFile);
		NodeList nList = doc.getElementsByTagName("property");
		
		for (int temp = 0; temp < nList.getLength(); temp++) {			 
			Node nNode = nList.item(temp);
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {				   
				Element eElement = (Element) nNode;	
				String name = getTagValue("name", eElement);
				String value = getTagValue("value", eElement);
				if ((null != name) && (null != value)) {
					config.set(name, value);
				}
			}
		}
		return ToolRunner.run( config, runner, args );		
	}

//////////////////////////////////////////////////////////////////////////////////////////////////

	// Load document results from the GUI into a (local) mongoDB
	
	public static void loadSampleData(String dataPath, String mongoServer, int mongoPort, boolean resetBeforeLoading) throws MongoException, IOException
	{
		String json = readFile(dataPath);
		MongoDbConnection mongoConnection = new MongoDbConnection(mongoServer, mongoPort);
		
		if (resetBeforeLoading) {
			mongoConnection.getMongo().getDB("test").getCollection("docs").drop();
		}
		QueryResults res = QueryResults.fromApi(json, QueryResults.class);
		List<DocumentPojo> documents = res.documents;
		if (null == documents) {
			documents = res.data; // (maybe it was a direct JSON call not a GUI save file)
		}		
		for (DocumentPojo doc: documents) {
			mongoConnection.getMongo().getDB("test").getCollection("docs").insert(doc.toDb());
		}
		
	}//TESTED
	
	public static class QueryResults extends BaseApiPojo
	{
		@Override
		public GsonBuilder extendBuilder(GsonBuilder gp) { // Extend the builder to apply standard document deserialization
			return new DocumentPojoApiMap().extendBuilder(gp);
		}
		public List<DocumentPojo> documents; // (from GUI save)
		public List<DocumentPojo> data; // (from direct JSON call to rest)
		
		//(and lots of other stuff that will get discarded for now)
	}
		
//////////////////////////////////////////////////////////////////////////////////////////////////

	// Utilities

	private static String getTagValue(String sTag, Element eElement) {
		NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();		 
		Node nValue = (Node) nlList.item(0);
		if (null != nValue) {
			return nValue.getNodeValue();
		}
		else {
			return null;
		}
	}
	private static String readFile( String file ) throws IOException {
		BufferedReader reader = new BufferedReader( new FileReader (file));
		String line  = null;
		StringBuilder stringBuilder = new StringBuilder();
		String ls = System.getProperty("line.separator");
		while( ( line = reader.readLine() ) != null ) {
			stringBuilder.append( line );
			stringBuilder.append( ls );
		}
		return stringBuilder.toString();
	}
}
