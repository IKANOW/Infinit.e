//get all Feed sources to run between midnight and noon that aren't turned off
var MIN_SEARCH_CYCLE = 14400;
var MAX_SEARCH_CYCLE = 86400;
var today = new Date();
var START_DATE;
var END_DATE;

//only run if the variable was passed in
if ( isMidNoon != null )
{
        if ( isMidNoon )
        {
                //handles dates from midnight tomorrow to noon tomorrow
                START_DATE = new Date(today.getFullYear(), today.getMonth(), today.getDate()+1,0,0,0,0).getTime();
                END_DATE = new Date(today.getFullYear(), today.getMonth(), today.getDate()+1,12,0,0,0).getTime();  
        }
        else
        {
                //handles dates from noon today to midnight tomorrow
                var START_DATE = new Date(today.getFullYear(), today.getMonth(), today.getDate(),12,0,0,0).getTime();
                var END_DATE = new Date(today.getFullYear(), today.getMonth(), today.getDate()+1,0,0,0,0).getTime();
        }
        var sources = db.source.find({extractType: "Feed", searchCycle_secs : { $gt : MIN_SEARCH_CYCLE} });

        //cycle through sources and adjust their time by +/- 1/4 searchCycle (max of 1d)
        sources.forEach(distribute_harvest_time);
}

function distribute_harvest_time(doc)
{
        if ( willRun(doc) )
        {
                //adjust search cycle so its not greater than the max (so we dont move it too far)
                var searchCycle = doc.searchCycle_secs;
                if ( searchCycle > MAX_SEARCH_CYCLE )
                {
                        searchCycle = MAX_SEARCH_CYCLE;
                }

                //get a random number between -1 and 1
                var random = -1 + Math.random() * 2;

                var timeToAdjust = searchCycle*.25*random*1000;

                //update harvest time by timeToAdjust
                var lastHarvest = doc.harvest.harvested;
                var newHarvest = new Date( lastHarvest.getTime() + timeToAdjust );
                //save new harvest date back over source harvest
                db.source.update({_id:doc._id},{$set:{"harvest.harvested":newHarvest}});
        }
}

function willRun(doc)
{
        var lastHarvest = doc.harvest.harvested;
        var nextHarvestTime = new Date( lastHarvest.getTime() + (doc.searchCycle_secs*1000)).getTime();
        if ( nextHarvestTime >= START_DATE && nextHarvestTime < END_DATE )
                return true;
        else
                return false;
}