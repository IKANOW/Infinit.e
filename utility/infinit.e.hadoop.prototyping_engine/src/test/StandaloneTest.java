package test;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.ToolRunner;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ikanow.infinit.e.utility.hadoop.HadoopPrototypingTool;

// Just change parameters in config/config.xml and replace "SourceSumXML" with your own class
// Needs a local mongoDB installed on localhost (or change config to point to its location)
// Note on Windows needs cygwin installed to work (should be fine on linux and Mac)

@SuppressWarnings("unused")
public class StandaloneTest 
{
	public static void main(String[] args) throws Exception { 
		//InfiniteHadoopTestUtils.loadSampleData(args[0], "127.0.0.1", 27017, true);
			// (the "true" deletes the old data before inserting from the specified file)

		// Here only 1 map/reduce is called, you can call a chain by calling runStandalone with different config files
		int exitCode = InfiniteHadoopTestUtils.runStandalone( new HadoopPrototypingTool(), "src/test/config.xml", args );
		System.exit( exitCode );
	}
}
