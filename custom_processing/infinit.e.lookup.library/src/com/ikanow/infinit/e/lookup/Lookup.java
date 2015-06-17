package com.ikanow.infinit.e.lookup;



import com.google.common.collect.MapMaker;
import com.ikanow.infinit.e.data_model.api.BaseApiPojo;
import com.ikanow.infinit.e.data_model.custom.InfiniteMongoConfigUtil;
import com.ikanow.infinit.e.data_model.store.MongoDbConnection;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo;
import com.ikanow.infinit.e.data_model.store.social.person.PersonCommunityPojo;
import com.ikanow.infinit.e.data_model.store.social.person.PersonPojo;
import com.mongodb.*;
import com.mongodb.hadoop.util.MongoConfigUtil;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.TaskInputOutputContext;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import lookup.testing.InfiniteHadoopTestUtils;

import java.util.*;

/**
 * Created by yuri on 12/15/14.
 */
public class Lookup {

    public interface ConfigurationGetter{
        public Configuration getConfiguration();
    }

    private static boolean TEST = false;
    private static ConfigurationGetter get_config;
    public static void prepareForTest(ConfigurationGetter getConfig){
        TEST = true;
        get_config = getConfig;
    }


    static Logger _logger = Logger.getLogger("Lookup_logger");

    private MongoDbConnection con = null;
    private String dbName;
    private DB db;
    private String outputKeyType;

    private DBCollection col;

    private LookupConfig _config;

    private DBObject constantSimpleQuery;

    private Map<String, Map<String, Object>> _cache;
    private Set<String> _negativeCache;

    private TaskInputOutputContext<?, ?, ?, ?> context;

    public static class LookupConfig extends BaseApiPojo {
        public enum Condition {must_exist, may_exist, not_exists, exactly_one, more_than_one};
        public enum Relation {equal, not_equal, in, not_in};

        //public SimpleQuery simpleQuery;
        public String customJobId;

        public Condition condition;

        public String key;
        public LookupConfig.Relation relation;
        public Object value;
        public Boolean valueIsConstant;
        public transient String valueS;

        public List<JoinImport> imports;

        public void verify() {
            if (customJobId == null){
                throw new RuntimeException("No Custom Job Id Specified");
            }

            if (key == null || key.trim().length() == 0) {
                key = "key";
            }

            if (relation == null) {
                relation = LookupConfig.Relation.equal;
            }

            if (valueIsConstant == null) {
                valueIsConstant = false;
            }

            if (!valueIsConstant) {
                if (value == null) {
                    throw new RuntimeException("Subst value in Simple Query is empty");
                }
                if (!(value instanceof String)) {
                    throw new RuntimeException("Invalid subst value in Simple Query: should be String determining path in Mongo Record");
                }

                valueS = value.toString();
            }
            if (condition == null) {
                condition = Condition.exactly_one;
            }

        }


        public static class JoinImport {
            public String field;
            public String alias;
        }

    }

    public Lookup(String configJson, TaskInputOutputContext<?, ?, ?, ?> ctx){
        this(Lookup.LookupConfig.fromApi(configJson, Lookup.LookupConfig.class), ctx);
    }


    public Lookup(LookupConfig config, TaskInputOutputContext<?, ?, ?, ?> ctx) {

        this.context = ctx;
        config.verify();

        String colName = null;
        if (TEST)
            colName = getCollectionNameTest(config.customJobId);
        else {
            colName = getCustomJobOutCollectionName(config.customJobId);
        }

        if (colName == null)
            throw new RuntimeException("Cannot set collection name ");


        col = db.getCollection(colName);

        if (config.key.equals("key")){
            if (TEST || outputKeyType.equalsIgnoreCase("com.mongodb.hadoop.io.BSONWritable")) {
                BasicDBObject testKeyRecord = (BasicDBObject) col.findOne();
                if (testKeyRecord != null){
                    BasicDBObject key = (BasicDBObject) testKeyRecord.get("key");
                    if (1 == key.size()) {
                        config.key += "." + key.keySet().iterator().next();
                    }
                    else {
                        throw new RuntimeException("Invalid key size, too complex, eg: " + key);
                    }//TOTE
                }// else would mean look up wont find anythig.
            }
            else  if (!outputKeyType.equalsIgnoreCase("org.apache.hadoop.io.Text")) {
                throw new RuntimeException("Invalid key type: " + outputKeyType);
            }//TOTEST
        }

        //ensure index
        col.createIndex(new BasicDBObject(config.key, 1));

        if (config.valueIsConstant) {
            QueryBuilder qb = QueryBuilder.start(config.key);
            switch (config.relation) {
                case equal:
                    qb.is(config.value);
                    break;
                case not_equal:
                    qb.notEquals(config.value);
                    break;
                case in:
                    qb.in(config.value);
                    break;
                case not_in:
                    qb.notIn(config.value);
                    break;
            }
            constantSimpleQuery = qb.get();
        }


        //http://docs.guava-libraries.googlecode.com/git/javadoc/com/google/common/collect/MapMaker.html
        //http://docs.guava-libraries.googlecode.com/git-history/v12.0/javadoc/com/google/common/collect/MapMaker.html#concurrencyLevel(int)
        // If soft or weak references were requested, it is possible for a key or value present in the the map to be reclaimed by the garbage collector.
        // If this happens, the entry automatically disappears from the map. A partially-reclaimed entry is never exposed to the user.
        //
        //https://weblogs.java.net/blog/2006/05/04/understanding-weak-references
        //http://jeremymanson.blogspot.de/2009/07/how-hotspot-decides-to-clear_07.html
        //http://www.javaspecialists.eu/archive/Issue098.html

        _cache = new MapMaker()
                .softValues()
                .concurrencyLevel(1)
                .makeMap();

        //http://docs.oracle.com/javase/6/docs/api/java/util/Collections.html#newSetFromMap%28java.util.Map%29
        Map<String, Boolean> tmp = new MapMaker()
                .softValues()
                .concurrencyLevel(1)
                .makeMap();

        _negativeCache = Collections.newSetFromMap(tmp);
        _config = config;
    }


    public BasicDBObject join(BasicDBObject record) {

        DBObject query = null;
        String cacheKey = null;
        if (constantSimpleQuery == null) {
            QueryBuilder qb = QueryBuilder.start(_config.key);
            Object o = getProperty(record, _config.valueS);
            if (o == null)
                return null;

            switch (_config.relation) {
                case equal:
                    qb.is(o);
                    break;
                case not_equal:
                    qb.notEquals(o);
                    break;
                case in:
                    //if (o instanceof BasicDBList) {
                    if (o instanceof List) {
                        qb.in(o);
                    } else {
                        _logger.info(" not an array value for : " + _config.key + " in " + o.toString());
                        return null;
                    }
                    break;
                case not_in:
                    //if (o instanceof BasicDBList) {
                    if (o instanceof List) {
                        qb.notIn(o);
                    } else {
                        _logger.info(" not an array value for : " + _config.key + " in " + o.toString());
                        return null;
                    }
                    break;
            }
            query = qb.get();
            cacheKey = o.toString();

        } else {
            query = constantSimpleQuery;
            cacheKey = "constantSimpleQuery"; //just same query for all records.
        }

        //System.out.println("Cache size " + _cache.size() + " Hitting cache for " + cacheKey);
        Map<String, Object> addition = _cache.get(cacheKey);
        if (addition == null) {
            if (_negativeCache.contains(cacheKey)) {
                return null;
            }

            DBCursor cursor = col.find(query);
            boolean result = processCursor(record, cursor);
            cursor.close();
            if (result) {
                _cache.put(cacheKey, getImportedFields(record));
                return record;
            } else {
                _negativeCache.add(cacheKey);
                return null;
            }

        } else {
            addImportedFields(record, addition);
            return record;
        }
    }


    static class ImportAndArray {
        LookupConfig.JoinImport imp;
        BasicDBList list;

        public ImportAndArray(LookupConfig.JoinImport imp) {
            this.imp = imp;
            this.list = new BasicDBList();
        }
    }

    private <T extends DBObject> boolean processCursor(BasicDBObject record, Iterator<T> cursor) {

        switch (_config.condition) {
            case must_exist:
            case may_exist:
            case more_than_one:
                List<ImportAndArray> listOfImports = new ArrayList<ImportAndArray>();
                for (LookupConfig.JoinImport i : _config.imports) {
                    listOfImports.add(new ImportAndArray(i));
                }
                int count = 0;
                while (cursor.hasNext()) {
                    T dbo = cursor.next();
                    for (ImportAndArray i : listOfImports) {
                        Object property = getProperty(dbo, i.imp.field);
                        if (property != null) {
                            i.list.add(property);
                        }
                    }
                    count++;
                }
                if (_config.condition == LookupConfig.Condition.more_than_one && count < 2 ||
                        _config.condition == LookupConfig.Condition.must_exist && count == 0) {
                    // what to do? Skip
                    return false;
                }

                for (ImportAndArray i : listOfImports) {
                    if (i.list.size() > 0) {
                        if (i.list.size() == 1) {
                            record.put(i.imp.alias, i.list.get(0));
                        } else {
                            record.put(i.imp.alias, i.list);
                        }
                    }
                }
                return true;

            case exactly_one:       //TESTED
                if (cursor.hasNext()) {
                    T dbo = cursor.next();
                    if (cursor.hasNext()) {
                        return false;
                    }
                    for (LookupConfig.JoinImport imp : _config.imports) {
                        Object property = getProperty(dbo, imp.field);
                        if (property != null) {
                            record.put(imp.alias, property);
                        }
                    }
                } else {
                    return false;
                }

                return true;
            /*
            case must_exist:
                if (cursor.hasNext()){
                    break;
                }
                return false;
                */
            case not_exists:
                return !cursor.hasNext();
        }

        return false;
    }

    private Map<String, Object> getImportedFields(BasicDBObject record) {
        Map<String, Object> retVal = new HashMap<String, Object>();
        for (LookupConfig.JoinImport imp : _config.imports) {
            retVal.put(imp.alias, record.get(imp.alias));
        }
        return retVal;
    }

    private void addImportedFields(BasicDBObject record, Map<String, Object> addition) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> e : addition.entrySet()) {
            record.put(e.getKey(), e.getValue());
            sb.append(e.getKey() + " : " + e.getValue().getClass() + ";");
        }
        //System.out.println("*****************************Cache hit. Found " + sb.toString());
    }

    private String arrayElement(BasicDBList list, int i) {
        Object tmp = list.get(i);
        if (tmp instanceof String) {
            return "\"" + tmp + "\"";
        } else if (tmp instanceof Number) {
            return tmp.toString();
        } else
            return null;
    }

    @SuppressWarnings("unchecked")
    private <T> T getProperty(DBObject dbo, String fieldInDotNotation) {
        final String[] keys = fieldInDotNotation.split("\\.");

        Map currentMap = dbo.toMap();
        List currentList = null;


        Object result = null;
        for (int i = 0; i < keys.length; i++) {
            if (currentMap != null) {
                result = currentMap.get(keys[i]);
            } else {
                try {
                    result = currentList.get(Integer.parseInt(keys[i]));
                } catch (Exception e) {
                    return null;    // either index not numeric or out of range. No need to throw exception (?)
                }
            }

            if (null == result) {
                return null;
            }
            if (result instanceof Collection) {
                //result = ((Collection)result).iterator().next();
            } else if (result instanceof Object[]) {
                //result = ((Object[])result)[0];
            }

            if (i + 1 < keys.length) {
                if (result instanceof DBObject) {
                    currentMap = ((DBObject) result).toMap();
                    currentList = null;
                } else if (result instanceof Map) {
                    currentMap = (Map) result;
                    currentList = null;
                } else if (result instanceof List) {
                    currentList = (List) result;
                    currentMap = null;
                } else {
                    return null;
                }
            }
        }
        return (T) result;
    }//TESTE


    private String getCollectionNameTest(String collectionName) {
        try {
            if (con == null) {
                Configuration config = get_config.getConfiguration();
                MongoURI uri = MongoConfigUtil.getInputURI(config);
                dbName = uri.getDatabase();
                List<String> hosts = uri.getHosts();
                con = new MongoDbConnection(hosts.get(0), 27017);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        db = con.getMongo().getDB(dbName);
        return collectionName;
    }

    private String getCustomJobOutCollectionName(String jobNameOrId) {
        CustomMapReduceJobPojo customJob = null;
        ObjectId jobId = null;
        BasicDBObject query = new BasicDBObject();

        try {
            jobId = new ObjectId(jobNameOrId);
            query.put(CustomMapReduceJobPojo._id_, jobId);
            customJob = CustomMapReduceJobPojo.fromDb(
                    MongoDbManager.getCustom().getLookup().findOne(query),
                    CustomMapReduceJobPojo.class);

        }
        catch (Exception e) {
            // it's a job name
            query.put(CustomMapReduceJobPojo.jobtitle_, jobNameOrId);
            customJob = CustomMapReduceJobPojo.fromDb(
                    MongoDbManager.getCustom().getLookup().findOne(query),
                    CustomMapReduceJobPojo.class);
        }

        if (customJob != null) {
            if (customJob.lastCompletionTime != null) {
                ObjectId userId = InfiniteMongoConfigUtil.getUserId(context.getConfiguration());
                boolean admin = InfiniteMongoConfigUtil.getIsAdmin(context.getConfiguration());

                if (!admin) {
                    List<ObjectId> jobCommunities = customJob.communityIds;
                    String s = "";
                    for (ObjectId i : jobCommunities){
                        s+= i.toString() + ", ";
                    }
                    _logger.info ("Job communities are " + s);

                    query = new BasicDBObject();
                    query.put(PersonPojo._id_, userId);

                    PersonPojo user = PersonPojo.fromDb(MongoDbManager.getSocial().getPerson().findOne(query), PersonPojo.class);
                    if (user == null) {
                        throw new RuntimeException("Unknown user " + userId.toString());
                    }

                    List<PersonCommunityPojo> userCommunities = user.getCommunities();
                    s = "";
                    for (PersonCommunityPojo p : userCommunities){
                        s+= p.get_id().toString() + "/" + p.getName() + ", ";
                    }
                    _logger.info ("User communities are " + s);



                    for (ObjectId jc : jobCommunities) {

                        boolean found=false;
                        for (PersonCommunityPojo c : userCommunities) {
                            if (c.get_id().equals(jc)){
                                found = true;
                                break;
                            }
                        }
                        if (!found){
                            throw new RuntimeException("User " + userId.toString() + " is not authorized to use one or more custom job communities");
                        }

                    }
                    _logger.info("User " + userId.toString() + " is authorized to use Custom output of " + jobNameOrId);
                }
                else {
                    _logger.info("User " + userId.toString() + " is an admin. Authentication bypassed");
                }



                dbName = customJob.getOutputDatabase();
                db = MongoDbManager.getDB(dbName);
                outputKeyType = customJob.outputKey;
                return customJob.outputCollection;
            }
        }
        return null;
    }


}
