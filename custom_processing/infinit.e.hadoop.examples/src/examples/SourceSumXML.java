/*******************************************************************************
 * Copyright 2012, The Infinit.e Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package examples;

import java.io.IOException;

import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.util.ToolRunner;
import org.bson.BSONObject;

import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.mongodb.BasicDBObject;
import com.mongodb.hadoop.util.MongoTool;


public class SourceSumXML extends MongoTool
{
	public static class TokenizerMapper extends Mapper<Object, BSONObject, Text, IntWritable> {

        private final static IntWritable one = new IntWritable( 1 );
        private final Text word = new Text();

        public void map( Object key, BSONObject value, Context context ) throws IOException, InterruptedException
        {
        	// Document access methods:        	
        	// 1. Pure BSON version
        	//Object source = value.get("source");
        	// 2. BSON with safe field names
        	//Object source = value.get(DocumentPojo.source_);
        	// 3. Data model abstraction
        	//DocumentPojo doc = DocumentPojo.fromDb( (BasicDBObject) value, DocumentPojo.class );
        	//String source = doc.getSource();
        	// 4. Data model abstraction with faster deserialization
        	value.removeField("associations");
        	value.removeField("entities");
        	value.removeField("metadata");
        	DocumentPojo doc = DocumentPojo.fromDb( (BasicDBObject) value, DocumentPojo.class );
        	String source = doc.getSource();
        	
        	if ( source != null )
        	{
        		word.set(source.toString());
        		context.write( word, one);
        	}
        }
    }

    public static class IntSumReducer extends Reducer<Text, IntWritable, Text, IntWritable> 
    {
        private final IntWritable result = new IntWritable();

        public void reduce( Text key, Iterable<IntWritable> values, Context context )
                throws IOException, InterruptedException
        {

            int sum = 0;
            for ( final IntWritable val : values )
            {
                sum += val.get();
            }
            result.set( sum );
            context.write( key, result );
        }
    }

    public static void main( String[] args ) throws Exception{
        final int exitCode = ToolRunner.run( new SourceSumXML(), args );
        System.exit( exitCode );
    }
}
