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
import org.bson.BasicBSONObject;

import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.mongodb.BasicDBObject;
import com.mongodb.hadoop.io.BSONWritable;
import com.mongodb.hadoop.util.MongoTool;


public class SourceSumBSON extends MongoTool
{
	public static class TokenizerMapper extends Mapper<Object, BSONObject, Text, BSONWritable> {

        private final static BSONWritable one = new BSONWritable();
        private final Text word = new Text();

        public void map( Object key, BSONObject value, Context context ) throws IOException, InterruptedException
        {        	
        	value.removeField("associations");
        	value.removeField("entities");
        	value.removeField("metadata");
        	DocumentPojo doc = DocumentPojo.fromDb( (BasicDBObject) value, DocumentPojo.class );
        	String source = doc.getSource();
        	
        	if ( source != null )
        	{
        		one.put("countobject", new BasicDBObject("count", 1));
        		word.set(source.toString());
        		context.write( word, one);
        	}        	
        }
    }

    public static class IntSumReducer extends Reducer<Text, BSONWritable, Text, BSONWritable> 
    {
        private final BSONWritable result = new BSONWritable();    	

        public void reduce( Text key, Iterable<BSONWritable> values, Context context )
                throws IOException, InterruptedException
        {

            int sum = 0;
            for ( final BSONWritable val : values )
            {
                sum += ((Integer)((BasicBSONObject)val.get("countobject")).get("count"));
            }
            result.put( "countobject",new BasicDBObject("count", sum) );
            context.write( key, result );
        }
    }

    public static void main( String[] args ) throws Exception{
        final int exitCode = ToolRunner.run( new SourceSumBSON(), args );
        System.exit( exitCode );
    }
}
