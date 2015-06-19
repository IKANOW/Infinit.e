import urllib2
import simplejson as json

indexes = '_status'
url = 'http://localhost:9200/'
max_index_size = 2000000000

request = urllib2.urlopen(url+indexes)
response = request.read()

indexes = json.loads(response)

for i in indexes['indices']:
    for shard in indexes['indices'][i]['shards']:
        #print json.dumps(shard, sort_keys=True, indent=4 * ' ')
        shardindex = json.dumps(shard, sort_keys=True, indent=4 * ' ')
        size = json.dumps(indexes['indices'][i]['shards'][shard][0]['index']['size_in_bytes'], sort_keys=True, indent=4 * ' ')
        print i + " " + shardindex + " " + str(size)
        if int(size) > int(max_index_size):
            # Index + Shard
            print i + shardindex + " is greater than " + str(max_index_size) + " @ " + str(size)

    #print json.dumps(indexes['indices'][i], sort_keys=True, indent=4 * ' ')


