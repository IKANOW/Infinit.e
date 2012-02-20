package examples;

import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.util.ToolRunner;
import org.bson.BSONObject;

import com.mongodb.hadoop.util.MongoTool;


public class WordCountXML extends MongoTool
{
	public static class TokenizerMapper extends Mapper<Object, BSONObject, Text, IntWritable> {

        private final static IntWritable one = new IntWritable( 1 );
        private final Text word = new Text();

        public void map( Object key, BSONObject value, Context context ) throws IOException, InterruptedException{

            System.out.println( "key: " + key );
            System.out.println( "value: " + value );

            final StringTokenizer itr = new StringTokenizer( value.get( "x" ).toString() );
            while ( itr.hasMoreTokens() ){
                word.set( itr.nextToken() );
                context.write( word, one );
            }
        }
    }

    public static class IntSumReducer extends Reducer<Text, IntWritable, Text, IntWritable> {

        private final IntWritable result = new IntWritable();

        public void reduce( Text key, Iterable<IntWritable> values, Context context )
                throws IOException, InterruptedException{

            int sum = 0;
            for ( final IntWritable val : values ){
                sum += val.get();
            }
            result.set( sum );
            context.write( key, result );
        }
    }

    static{
        // Load the XML config defined in hadoop-local.xml
        //Configuration.addDefaultResource( "config/hadoop-local.xml" );    
        //Configuration.addDefaultResource( "/tmp/config/harvester-feed.xml" );        
    }

    public static void main( String[] args ) throws Exception{
        final int exitCode = ToolRunner.run( new WordCountXML(), args );
        System.exit( exitCode );
    }
}
