package examples;

import java.io.IOException;

import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.util.ToolRunner;
import org.bson.BSONObject;

import com.mongodb.hadoop.util.MongoTool;


public class SourceSumXML extends MongoTool
{
	public static class TokenizerMapper extends Mapper<Object, BSONObject, Text, IntWritable> {

        private final static IntWritable one = new IntWritable( 1 );
        private final Text word = new Text();

        public void map( Object key, BSONObject value, Context context ) throws IOException, InterruptedException
        {
        	Object source = value.get("source");        	
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
