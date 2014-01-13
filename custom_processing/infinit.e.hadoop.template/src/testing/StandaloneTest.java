/*******************************************************************************
 * Copyright 2012 The Infinit.e Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package testing;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.ToolRunner;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ikanow.infinit.e.hadoop.processing.InfiniteProcessingEngine;

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
		int exitCode = InfiniteHadoopTestUtils.runStandalone( new InfiniteProcessingEngine(), "example_standalone_config/config.xml", args );
		System.exit( exitCode );
	}
}
