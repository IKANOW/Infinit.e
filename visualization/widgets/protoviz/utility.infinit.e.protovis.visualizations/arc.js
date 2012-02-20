// This file contains the weighted network of coappearances of characters in
// Victor Hugo's novel "Les Miserables". Nodes represent characters as indicated
// by the labels, and edges connect any pair of characters that appear in the
// same chapter of the book. The values on the edges are the number of such
// coappearances. The data on coappearances were taken from D. E. Knuth, The
// Stanford GraphBase: A Platform for Combinatorial Computing, Addison-Wesley,
// Reading, MA (1993).
//
// The group labels were transcribed from "Finding and evaluating community
// structure in networks" by M. E. J. Newman and M. Girvan.

var mydata ;
var mynodes;
var mylinks;


var infinite = {
  nodes:[
         {nodeName:"news", group:1},
         {nodeName:"imagery", group:1},
         {nodeName:"social", group:1},
         {nodeName:"video", group:1},
         {nodeName:"discussion", group:1},
         {nodeName:"taylor swift", group:2},
         {nodeName:"selena gomez", group:2},
         {nodeName:"israel", group:2},
         {nodeName:"president", group:2},
         {nodeName:"palin", group:2},
         {nodeName:"the washington post company", group:2},
         {nodeName:"piper", group:2},
         {nodeName:"the washington post", group:2},
         {nodeName:"lady liberty", group:2},
         {nodeName:"iman al-obeidi", group:2},
         {nodeName:"united states", group:2},
         {nodeName:"she was assaulted by gaddafi troops, is reportedly on her way to us", group:2},
         {nodeName:"her way", group:2},
         {nodeName:"rachel nicholas", group:2},
         {nodeName:"cleveland", group:2},
         {nodeName:"person travel", group:3},
         {nodeName:"generic relations", group:3},
         {nodeName:"family relation", group:3},
         {nodeName:"quotation", group:3},
         {nodeName:"taylor swift", group:4},
         {nodeName:"selena gomez", group:4},
         {nodeName:"israel", group:4},
         {nodeName:"president", group:4},
         {nodeName:"palin", group:4},
         {nodeName:"the washington post company", group:4},
         {nodeName:"piper", group:4},
         {nodeName:"the washington post", group:4},
         {nodeName:"lady liberty", group:4},
         {nodeName:"iman al-obeidi", group:4},
         {nodeName:"united states", group:4},
         {nodeName:"she was assaulted by gaddafi troops, is reportedly on her way to us", group:4},
         {nodeName:"her way", group:4},
         {nodeName:"rachel nicholas", group:4},
         {nodeName:"cleveland", group:4}
         ],
    links:[
           {source:1, target:0, value:1},
           {source:2, target:0, value:8},
           {source:3, target:0, value:10},
           {source:3, target:2, value:6},
           {source:4, target:0, value:1},
           {source:5, target:0, value:1},
           {source:6, target:0, value:1},
           {source:7, target:0, value:1},
           {source:8, target:0, value:2},
           {source:9, target:0, value:1},
           {source:11, target:10, value:1}
           ]
    }

var miserables = {
  nodes:[
    {nodeName:"Myriel", group:1},
    {nodeName:"Napoleon", group:1},
    {nodeName:"Mlle. Baptistine", group:1},
    {nodeName:"Mme. Magloire", group:1},
    {nodeName:"Countess de Lo", group:1},
    {nodeName:"Geborand", group:1},
    {nodeName:"Champtercier", group:1},
    {nodeName:"Cravatte", group:1},
    {nodeName:"Count", group:1},
    {nodeName:"Old Man", group:1},
    {nodeName:"Labarre", group:2},
    {nodeName:"Valjean", group:2},
    {nodeName:"Marguerite", group:3},
    {nodeName:"Mme. de R", group:2},
    {nodeName:"Isabeau", group:2},
    {nodeName:"Gervais", group:2},
    {nodeName:"Tholomyes", group:3},
    {nodeName:"Listolier", group:3},
    {nodeName:"Fameuil", group:3},
    {nodeName:"Blacheville", group:3},
    {nodeName:"Favourite", group:3},
    {nodeName:"Dahlia", group:3},
    {nodeName:"Zephine", group:3},
    {nodeName:"Fantine", group:3},
    {nodeName:"Mme. Thenardier", group:4},
    {nodeName:"Thenardier", group:4},
    {nodeName:"Cosette", group:5},
    {nodeName:"Javert", group:4},
    {nodeName:"Fauchelevent", group:0},
    {nodeName:"Bamatabois", group:2},
    {nodeName:"Perpetue", group:3},
    {nodeName:"Simplice", group:2},
    {nodeName:"Scaufflaire", group:2},
    {nodeName:"Woman 1", group:2},
    {nodeName:"Judge", group:2},
    {nodeName:"Champmathieu", group:2},
    {nodeName:"Brevet", group:2},
    {nodeName:"Chenildieu", group:2},
    {nodeName:"Cochepaille", group:2},
    {nodeName:"Pontmercy", group:4},
    {nodeName:"Boulatruelle", group:6},
    {nodeName:"Eponine", group:4},
    {nodeName:"Anzelma", group:4},
    {nodeName:"Woman 2", group:5},
    {nodeName:"Mother Innocent", group:0},
    {nodeName:"Gribier", group:0},
    {nodeName:"Jondrette", group:7},
    {nodeName:"Mme. Burgon", group:7},
    {nodeName:"Gavroche", group:8},
    {nodeName:"Gillenormand", group:5},
    {nodeName:"Magnon", group:5},
    {nodeName:"Mlle. Gillenormand", group:5},
    {nodeName:"Mme. Pontmercy", group:5},
    {nodeName:"Mlle. Vaubois", group:5},
    {nodeName:"Lt. Gillenormand", group:5},
    {nodeName:"Marius", group:8},
    {nodeName:"Baroness T", group:5},
    {nodeName:"Mabeuf", group:8},
    {nodeName:"Enjolras", group:8},
    {nodeName:"Combeferre", group:8},
    {nodeName:"Prouvaire", group:8},
    {nodeName:"Feuilly", group:8},
    {nodeName:"Courfeyrac", group:8},
    {nodeName:"Bahorel", group:8},
    {nodeName:"Bossuet", group:8},
    {nodeName:"Joly", group:8},
    {nodeName:"Grantaire", group:8},
    {nodeName:"Mother Plutarch", group:9},
    {nodeName:"Gueulemer", group:4},
    {nodeName:"Babet", group:4},
    {nodeName:"Claquesous", group:4},
    {nodeName:"Montparnasse", group:4},
    {nodeName:"Toussaint", group:5},
    {nodeName:"Child 1", group:10},
    {nodeName:"Child 2", group:10},
    {nodeName:"Brujon", group:4},
    {nodeName:"Mme. Hucheloup", group:8}
  ],
  links:[
    {source:1, target:0, value:1},
    {source:2, target:0, value:8},
    {source:3, target:0, value:10},
    {source:3, target:2, value:6},
    {source:4, target:0, value:1},
    {source:5, target:0, value:1},
    {source:6, target:0, value:1},
    {source:7, target:0, value:1},
    {source:8, target:0, value:2},
    {source:9, target:0, value:1},
    {source:11, target:10, value:1},
    {source:11, target:3, value:3},
    {source:11, target:2, value:3},
    {source:11, target:0, value:5},
    {source:12, target:11, value:1},
    {source:13, target:11, value:1},
    {source:14, target:11, value:1},
    {source:15, target:11, value:1},
    {source:17, target:16, value:4},
    {source:18, target:16, value:4},
    {source:18, target:17, value:4},
    {source:19, target:16, value:4},
    {source:19, target:17, value:4},
    {source:19, target:18, value:4},
    {source:20, target:16, value:3},
    {source:20, target:17, value:3},
    {source:20, target:18, value:3},
    {source:20, target:19, value:4},
    {source:21, target:16, value:3},
    {source:21, target:17, value:3},
    {source:21, target:18, value:3},
    {source:21, target:19, value:3},
    {source:21, target:20, value:5},
    {source:22, target:16, value:3},
    {source:22, target:17, value:3},
    {source:22, target:18, value:3},
    {source:22, target:19, value:3},
    {source:22, target:20, value:4},
    {source:22, target:21, value:4},
    {source:23, target:16, value:3},
    {source:23, target:17, value:3},
    {source:23, target:18, value:3},
    {source:23, target:19, value:3},
    {source:23, target:20, value:4},
    {source:23, target:21, value:4},
    {source:23, target:22, value:4},
    {source:23, target:12, value:2},
    {source:23, target:11, value:9},
    {source:24, target:23, value:2},
    {source:24, target:11, value:7},
    {source:25, target:24, value:13},
    {source:25, target:23, value:1},
    {source:25, target:11, value:12},
    {source:26, target:24, value:4},
    {source:26, target:11, value:31},
    {source:26, target:16, value:1},
    {source:26, target:25, value:1},
    {source:27, target:11, value:17},
    {source:27, target:23, value:5},
    {source:27, target:25, value:5},
    {source:27, target:24, value:1},
    {source:27, target:26, value:1},
    {source:28, target:11, value:8},
    {source:28, target:27, value:1},
    {source:29, target:23, value:1},
    {source:29, target:27, value:1},
    {source:29, target:11, value:2},
    {source:30, target:23, value:1},
    {source:31, target:30, value:2},
    {source:31, target:11, value:3},
    {source:31, target:23, value:2},
    {source:31, target:27, value:1},
    {source:32, target:11, value:1},
    {source:33, target:11, value:2},
    {source:33, target:27, value:1},
    {source:34, target:11, value:3},
    {source:34, target:29, value:2},
    {source:35, target:11, value:3},
    {source:35, target:34, value:3},
    {source:35, target:29, value:2},
    {source:36, target:34, value:2},
    {source:36, target:35, value:2},
    {source:36, target:11, value:2},
    {source:36, target:29, value:1},
    {source:37, target:34, value:2},
    {source:37, target:35, value:2},
    {source:37, target:36, value:2},
    {source:37, target:11, value:2},
    {source:37, target:29, value:1},
    {source:38, target:34, value:2},
    {source:38, target:35, value:2},
    {source:38, target:36, value:2},
    {source:38, target:37, value:2},
    {source:38, target:11, value:2},
    {source:38, target:29, value:1},
    {source:39, target:25, value:1},
    {source:40, target:25, value:1},
    {source:41, target:24, value:2},
    {source:41, target:25, value:3},
    {source:42, target:41, value:2},
    {source:42, target:25, value:2},
    {source:42, target:24, value:1},
    {source:43, target:11, value:3},
    {source:43, target:26, value:1},
    {source:43, target:27, value:1},
    {source:44, target:28, value:3},
    {source:44, target:11, value:1},
    {source:45, target:28, value:2},
    {source:47, target:46, value:1},
    {source:48, target:47, value:2},
    {source:48, target:25, value:1},
    {source:48, target:27, value:1},
    {source:48, target:11, value:1},
    {source:49, target:26, value:3},
    {source:49, target:11, value:2},
    {source:50, target:49, value:1},
    {source:50, target:24, value:1},
    {source:51, target:49, value:9},
    {source:51, target:26, value:2},
    {source:51, target:11, value:2},
    {source:52, target:51, value:1},
    {source:52, target:39, value:1},
    {source:53, target:51, value:1},
    {source:54, target:51, value:2},
    {source:54, target:49, value:1},
    {source:54, target:26, value:1},
    {source:55, target:51, value:6},
    {source:55, target:49, value:12},
    {source:55, target:39, value:1},
    {source:55, target:54, value:1},
    {source:55, target:26, value:21},
    {source:55, target:11, value:19},
    {source:55, target:16, value:1},
    {source:55, target:25, value:2},
    {source:55, target:41, value:5},
    {source:55, target:48, value:4},
    {source:56, target:49, value:1},
    {source:56, target:55, value:1},
    {source:57, target:55, value:1},
    {source:57, target:41, value:1},
    {source:57, target:48, value:1},
    {source:58, target:55, value:7},
    {source:58, target:48, value:7},
    {source:58, target:27, value:6},
    {source:58, target:57, value:1},
    {source:58, target:11, value:4},
    {source:59, target:58, value:15},
    {source:59, target:55, value:5},
    {source:59, target:48, value:6},
    {source:59, target:57, value:2},
    {source:60, target:48, value:1},
    {source:60, target:58, value:4},
    {source:60, target:59, value:2},
    {source:61, target:48, value:2},
    {source:61, target:58, value:6},
    {source:61, target:60, value:2},
    {source:61, target:59, value:5},
    {source:61, target:57, value:1},
    {source:61, target:55, value:1},
    {source:62, target:55, value:9},
    {source:62, target:58, value:17},
    {source:62, target:59, value:13},
    {source:62, target:48, value:7},
    {source:62, target:57, value:2},
    {source:62, target:41, value:1},
    {source:62, target:61, value:6},
    {source:62, target:60, value:3},
    {source:63, target:59, value:5},
    {source:63, target:48, value:5},
    {source:63, target:62, value:6},
    {source:63, target:57, value:2},
    {source:63, target:58, value:4},
    {source:63, target:61, value:3},
    {source:63, target:60, value:2},
    {source:63, target:55, value:1},
    {source:64, target:55, value:5},
    {source:64, target:62, value:12},
    {source:64, target:48, value:5},
    {source:64, target:63, value:4},
    {source:64, target:58, value:10},
    {source:64, target:61, value:6},
    {source:64, target:60, value:2},
    {source:64, target:59, value:9},
    {source:64, target:57, value:1},
    {source:64, target:11, value:1},
    {source:65, target:63, value:5},
    {source:65, target:64, value:7},
    {source:65, target:48, value:3},
    {source:65, target:62, value:5},
    {source:65, target:58, value:5},
    {source:65, target:61, value:5},
    {source:65, target:60, value:2},
    {source:65, target:59, value:5},
    {source:65, target:57, value:1},
    {source:65, target:55, value:2},
    {source:66, target:64, value:3},
    {source:66, target:58, value:3},
    {source:66, target:59, value:1},
    {source:66, target:62, value:2},
    {source:66, target:65, value:2},
    {source:66, target:48, value:1},
    {source:66, target:63, value:1},
    {source:66, target:61, value:1},
    {source:66, target:60, value:1},
    {source:67, target:57, value:3},
    {source:68, target:25, value:5},
    {source:68, target:11, value:1},
    {source:68, target:24, value:1},
    {source:68, target:27, value:1},
    {source:68, target:48, value:1},
    {source:68, target:41, value:1},
    {source:69, target:25, value:6},
    {source:69, target:68, value:6},
    {source:69, target:11, value:1},
    {source:69, target:24, value:1},
    {source:69, target:27, value:2},
    {source:69, target:48, value:1},
    {source:69, target:41, value:1},
    {source:70, target:25, value:4},
    {source:70, target:69, value:4},
    {source:70, target:68, value:4},
    {source:70, target:11, value:1},
    {source:70, target:24, value:1},
    {source:70, target:27, value:1},
    {source:70, target:41, value:1},
    {source:70, target:58, value:1},
    {source:71, target:27, value:1},
    {source:71, target:69, value:2},
    {source:71, target:68, value:2},
    {source:71, target:70, value:2},
    {source:71, target:11, value:1},
    {source:71, target:48, value:1},
    {source:71, target:41, value:1},
    {source:71, target:25, value:1},
    {source:72, target:26, value:2},
    {source:72, target:27, value:1},
    {source:72, target:11, value:1},
    {source:73, target:48, value:2},
    {source:74, target:48, value:2},
    {source:74, target:73, value:3},
    {source:75, target:69, value:3},
    {source:75, target:68, value:3},
    {source:75, target:25, value:3},
    {source:75, target:48, value:1},
    {source:75, target:41, value:1},
    {source:75, target:70, value:1},
    {source:75, target:71, value:1},
    {source:76, target:64, value:1},
    {source:76, target:65, value:1},
    {source:76, target:66, value:1},
    {source:76, target:63, value:1},
    {source:76, target:62, value:1},
    {source:76, target:48, value:1},
    {source:76, target:58, value:1}
  ]
};

var arrayNodes = new Array();
var jsonNodes = [];
var jsonLinks = [];
var max = 25;

function getThings(response, group) {
  var array = new Array();
  var count = 1;
  // Loop through the items
  for(var i=0, l=response.data.length; i < l;   i)
  {
    if ( count == max)
      break;
    
    for(var e=0, el=response.data[i].entities.length; e < el; e) {
      if (existsInArray(array, response.data[i].entities[e].disambiguous_name.toLowerCase()) == false ) {
        var thing = response.data[i].entities[e].type.toLowerCase();
        if ( thing == "facility" || thing == "organization" || thing == "printmedia" || thing == "technology" || thing == "company" ) {
          count++;
          array.push(response.data[i].entities[e].disambiguous_name.toLowerCase());
        }
      }
      e++;
    }
   
    i++;
  }  
    array.sort();
    toNodes(array, group);   
}


function getPlaces(response, group) {
  var array = new Array();
  var count = 1;
  // Loop through the items
  for(var i=0, l=response.data.length; i < l;   i)
  {
    if ( count >= max)
      break;
    
    for(var e=0, el=response.data[i].entities.length; e < el; e) {
      if ( count >= max)
        break;
    
      if (existsInArray(array, response.data[i].entities[e].disambiguous_name.toLowerCase()) == false ) {
        var place = response.data[i].entities[e].type.toLowerCase();
        if ( place == "city" || place == "continent" || place == "country" || place == "stateorcountry" ) {
          count++;
          array.push(response.data[i].entities[e].disambiguous_name.toLowerCase());
        }
      }
      e++;
    }
   
    for(var e=0, el=response.data[i].events.length; e < el; e) {
      
      if ( count >= max)
        break;
    
      if (response.data[i].events[e].entity1 != null && response.data[i].events[e].entity1_index !=null) {
        if (existsInArray(array, response.data[i].events[e].entity1.toLowerCase()) == false ) {
          var place = response.data[i].events[e].entity1_index.toLowerCase();
          if (place.indexOf("country") > 0 || place.indexOf("city") > 0 || place.indexOf("continent") > 0) {
            count++;
            array.push(response.data[i].events[e].entity1.toLowerCase());
          }
        }
      }
      e++;
    }
    
    
    for(var e=0, el=response.data[i].events.length; e < el; e) {
      
      if ( count >= max)
        break;
      
      if (response.data[i].events[e].entity2 != null && response.data[i].events[e].entity2_index !=null) {
        if (existsInArray(array, response.data[i].events[e].entity2.toLowerCase()) == false )
          var place = response.data[i].events[e].entity2_index.toLowerCase();
          if (place.indexOf("country") > 0 || place.indexOf("city") > 0 || place.indexOf("continent") > 0) {
            count++;
            array.push(response.data[i].events[e].entity2.toLowerCase());
        }
      }
      e++;
    }
    
  
    i++;
  }  
    array.sort();
    toNodes(array, group);
    
}

function getLocation(value) {
  var location = -1;
  for(var i=0, l=arrayNodes.length; i < l;   i) {
    if (value.toLowerCase() == arrayNodes[i].toLowerCase()) {
      return i;
    }
    i++;
  }
  return location;
}
function existsInJsonNodes(source, target) {
  var exists = false;
  for(var i=0, l=jsonNodes.length; i < l; i) {
    if (jsonNodes[i].source == source && jsonNodes[i].target == target) { 
      return true;
    }
    i++;
  }
  return exists;
  
}
// SOMETHING FUNKY GOING HERE
function existsInJsonLinks(sourceLocation, targetLocation) {
  var exists = false;
  if ( jsonLinks.length > 0 ) {
    for(var i=0, l=jsonLinks.length; i < l; i) {
      
      if (jsonLinks[i].source == sourceLocation && jsonLinks[i].target == targetLocation) {
        if (jsonLinks[i].value <= 50 ) {
          jsonLinks[i].value = jsonLinks[i].value + 1;
        }
        return true;
      }
      /*else {
        jsonLinks.push({source: sourceLocation, target: targetLocation, value: 1});
      }*/
      i++;
    }
    jsonLinks.push({source: sourceLocation, target: targetLocation, value: 1});
  } else {
    jsonLinks.push({source: sourceLocation, target: targetLocation, value: 1});
  }

  return exists;
  
}

function getMediaLinks(response) {
  var sourceLocation = 10;
  var targetLocation = 10;
  
  for(var i=0, l=response.data.length; i < l;   i)
    {
      var mediaType = response.data[i].mediaType.toLowerCase();
      sourceLocation = getLocation(mediaType);
      
      for(var e=0, el=response.data[i].entities.length; e < el; e) {
        targetLocation = getLocation(response.data[i].entities[e].disambiguous_name.toLowerCase());
        if (sourceLocation > 0 && targetLocation > 0) {
          existsInJsonLinks(sourceLocation, targetLocation);
        }
        e++;
      }
      
      
      for(var e=0, el=response.data[i].events.length; e < el; e) {
        targetLocation = getLocation(response.data[i].events[e].verb_category.toLowerCase());
        if (sourceLocation > 0 && targetLocation > 0) {
          existsInJsonLinks(sourceLocation, targetLocation);
        }
        e++;
      }
      
      i++;
    }
}

function whenEventOccured(publishedDate) {
  var today = new Date()
  
  if (today.getFullYear() == publishedDate.getFullYear()) {
    if ((today.getDay() == publishedDate.getDay()) && ( today.getMonth() == publishedDate.getMonth())) {
      return "In the last day";
    } else if ((today.getDay != publishedDate.getDay()) && (today.getMonth() == publishedDate.getMonth())) {
      if (today.getDay() - publishedDate.getDay() <= 7 ) {
        return "In the last week";
      } else {
        return "In the last month";
      }
    } else {
      if ((today.getMonth() >=1) && (today.getMonth() <=3 ) && ((publishedDate.getMonth() >=1) && (publishedDate.getMonth() <=3))) {
        return "In the last quarter";
      } else if ((today.getMonth() >=4) && (today.getMonth() <=6) && ((publishedDate.getMonth() >=4) && (publishedDate.getMonth() <=6))) {
        return "In the last quarter";
      } else if ((today.getMonth() >=7) && (today.getMonth() <=9) && ((publishedDate.getMonth() >=7) && (publishedDate.getMonth() <=9))) {
        return "In the last quarter";
      } else if ((today.getMonth() >=10) && (today.getMonth() <=12) && ((publishedDate.getMonth() >=10) && (publishedDate.getMonth() <=12))) {
        return "In the last quarter";
      } else {
        return "In the last year";
      }
    }
    
  } else if (today.getFullYear() - publishedDate.getFullYear() <= 10 ) {
    return "In the last decade";
  }
}

function getTimeLinks(response) {
  var sourceLocation;
  var targetLocation;
  
  for(var i=0, l=response.data.length; i < l;   i)
      {
        var publishedDate = new Date(response.data[i].publishedDate);
        var when = whenEventOccured(publishedDate);
        sourceLocation = getLocation(when);
        for(var e=0, el=response.data[i].events.length; e < el; e) {
          if (response.data[i].events[e].entity1 != null && response.data[i].events[e].entity1_index !=null) {
              if (response.data[i].events[e].entity1_index.toLowerCase().indexOf("person") > 0) {
                targetLocation = getLocation(response.data[i].events[e].entity1.toLowerCase());
                              
                if (sourceLocation > 0 && targetLocation > 0 ) {
                  existsInJsonLinks(sourceLocation, targetLocation)
                }
              }
          }
          e++;
        }
      i++;  
      }  
}

function getPeopleLinks(response) {
  var sourceLocation;
  var targetLocation;
    
  for(var i=0, l=response.data.length; i < l;   i)
    {
    for(var e=0, el=response.data[i].events.length; e < el; e) {
        if (response.data[i].events[e].entity1 != null && response.data[i].events[e].entity1_index !=null) {
            if (response.data[i].events[e].entity1_index.toLowerCase().indexOf("person") > 0) {
              sourceLocation = getLocation(response.data[i].events[e].entity1.toLowerCase());
              targetLocation = getLocation(response.data[i].events[e].verb_category.toLowerCase());
                            
              if (sourceLocation > 0 && targetLocation > 0 ) {
                existsInJsonLinks(sourceLocation, targetLocation)
              }
            }
        }
        e++;
    }
    i++;
  }
  
  for(var i=0, l=response.data.length; i < l;   i)
    {
    for(var e=0, el=response.data[i].events.length; e < el; e) {
        if (response.data[i].events[e].entity1 != null && response.data[i].events[e].entity1_index !=null && response.data[i].events[e].entity2 !=null) {
            if (response.data[i].events[e].entity1_index.toLowerCase().indexOf("person") > 0) {
              sourceLocation = getLocation(response.data[i].events[e].entity1.toLowerCase());
              targetLocation = getLocation(response.data[i].events[e].entity2.toLowerCase());
                            
              if (sourceLocation > 0 && targetLocation > 0 ) {
                existsInJsonLinks(sourceLocation, targetLocation)
              }
            }
        }
        e++;
    }
    i++;
  }
}

function getEventLinks(response) {
  var sourceLocation;
  var targetLocation;
    
  for(var i=0, l=response.data.length; i < l;   i)
    {
      for(var e=0, el=response.data[i].events.length; e < el; e) {
        if (response.data[i].events[e].entity1 != null && response.data[i].events[e].entity1_index !=null && response.data[i].events[e].entity2 !=null) {
            if (response.data[i].events[e].entity1_index.toLowerCase().indexOf("person") > 0) {
              sourceLocation = getLocation(response.data[i].events[e].verb_category.toLowerCase());
              targetLocation = getLocation(response.data[i].events[e].entity2.toLowerCase());
                            
              if (sourceLocation > 0 && targetLocation > 0 ) {
                existsInJsonLinks(sourceLocation, targetLocation)
              }
            }
        }
        e++;
    }
      i++;
    }
}

function getPeople(response, group) {
  var array = new Array();
  var count = 0;
  // Loop through the items
  for(var i=0, l=response.data.length; i < l;   i)
  {
    
    if ( count >= max )
      break;
    
    for(var e=0, el=response.data[i].entities.length; e < el; e) {
      if ( count >= max)
        break;
      
      if (existsInArray(array, response.data[i].entities[e].disambiguous_name.toLowerCase()) == false ) {
        if (response.data[i].entities[e].type.toLowerCase() == "person" ) {
          count++;
          array.push(response.data[i].entities[e].disambiguous_name.toLowerCase());
        }
      }
      e++;
    }
   
    for(var e=0, el=response.data[i].events.length; e < el; e) {
      if ( count >= max)
        break;
      
      if (response.data[i].events[e].entity1 != null && response.data[i].events[e].entity1_index !=null) {
        if (existsInArray(array, response.data[i].events[e].entity1.toLowerCase()) == false ) {
          if (response.data[i].events[e].entity1_index.toLowerCase().indexOf("person") > 0) {
            count++;
            array.push(response.data[i].events[e].entity1.toLowerCase());
          }
        }
      }
      e++;
    }
    
    
    for(var e=0, el=response.data[i].events.length; e < el; e) {
      if ( count >= max)
        break;
      
      if (response.data[i].events[e].entity2 != null && response.data[i].events[e].entity2_index !=null) {
        if (existsInArray(array, response.data[i].events[e].entity2.toLowerCase()) == false )
          if (response.data[i].events[e].entity2_index.toLowerCase().indexOf("person") > 0){
          count++;
          array.push(response.data[i].events[e].entity2.toLowerCase());
        }
      }
      e++;
    }
    
  
    i++;
  }  
    array.sort();
    toNodes(array, group);
    return array;
}
/*
function getEntities(response, group, list, includeOrIngore) {
  var array = new Array();
  // Loop through the items
  for(var i=0, l=response.data.length; i < l;   i)
  {
    
    for(var e=0, el=response.data[i].entities.length; e < el; e) {
      if (existsInArray(array, response.data[i].entities[e].disambiguous_name.toLowerCase()) == false ) {
        if (list == null ) {
          array.push(response.data[i].entities[e].disambiguous_name.toLowerCase());
        }
        else {
          if (list != null && includeOrIgnore == true) {
            if (existsInArray(list, response.data[i].entities[e].type.toLowerCase()) {
              array.push(response.data[i].entities[e].disambiguous_name.toLowerCase());
            }
          }
        }
      }
    
      e++;
    }
    
    for(var e=0, el=response.data[i].events.length; e < el; e) {
      
      if (response.data[i].events[e].entity1 != null) {
        
        if (existsInArray(array, response.data[i].events[e].entity1.toLowerCase()) == false ) {
          if (list == null) {
            array.push(response.data[i].events[e].entity1.toLowerCase());
          }
          else {
            if (list != null && includeOrIgnore == true) {
              if (existsInArray(list, response.data[i].events[e].entity1.toLowerCase())) {
                array.push(response.data[i].events[e].entity1.toLowerCase());
              }
            }
          }
        }
        
      }
      e++;
    }
    
    for(var e=0, el=response.data[i].events.length; e < el; e) {
      if (response.data[i].events[e].entity2 != null) {
        
        if (existsInArray(array, response.data[i].events[e].entity2.toLowerCase()) == false )
          array.push(response.data[i].events[e].entity2.toLowerCase());
          
      }
      e++;
    }
             
    i++;
  }
  array.sort();
  toNodes(array, group); 
}
*/

function getEvents(response, group) {
  // build up array
  var array = new Array();
  var count = 1;
  // Loop through the items
  for(var i=0, l=response.data.length; i < l;   i)
  {
    if ( count >= max)
      break;
    
    for(var e=0, el=response.data[i].events.length; e < el; e) {
   
      if (existsInArray(array, response.data[i].events[e].verb_category.toLowerCase()) == false ) {
        count++;
        array.push(response.data[i].events[e].verb_category.toLowerCase());
      }
      e++;
    }
                
    i++;
  }
  array.sort();
  toNodes(array, group); 
}

function getVerbs(response, group) {
  // build up array
  var array = new Array();
  // Loop through the items
  for(var i=0, l=response.data.length; i < l;   i)
  {
    
    for(var e=0, el=response.data[i].events.length; e < el; e) {
   
      if (response.data[i].events[e].verb != null) {
        if (existsInArray(array, response.data[i].events[e].verb.toLowerCase()) == false )
          array.push(response.data[i].events[e].verb.toLowerCase());
      }
      e++;
    }
                
    i++;
  }
  array.sort();
  toNodes(array, group); 
}

function getTime(group) {
  var array = new Array();
  
  array.push("In the last day");
  array.push("In the last week");
  array.push("In the last month");
  array.push("In the last quarter");
  array.push("In the last year");
  array.push("In the last decade");
  
  toNodes(array,group);
  
}

function getSourceMetaTypes(response, group) {
  var array = new Array();
  // Loop through the items
  for(var i=0, l=response.data.length; i < l;   i)
  {
      if (existsInArray(array, response.data[i].mediaType.toLowerCase()) == false )
        array.push(response.data[i].mediaType.toLowerCase());
      
      i++;
  }

  array.sort();
  toNodes(array, group); 
}

function getNodes(response) {
  
  //getVerbs(response, 3);
  
  getPlaces(response, 1);
  getThings(response, 2);
  getEvents(response, 3);
  getSourceMetaTypes(response, 4);
  getPeople(response, 5);
  getTime(6);
  

  console.log(jsonNodes);
  return jsonNodes;
}

function getLinks(response) {
  getTimeLinks(response);
  getPeopleLinks(response);
  getEventLinks(response);
  getMediaLinks(response);
  console.log(jsonLinks);
  return jsonLinks;
}

function toNodes(array, group) {
  for (var i=0, l=array.length; i < l; i) {
   arrayNodes.push(array[i].toLowerCase());
   jsonNodes.push({nodeName: array[i].toLowerCase(), group: group});
   i++ 
  }
}

function existsInArray(array, value) {
  var exists = false;
  for (var i=0, l=array.length; i < l; i) {
    if (array[i].toLowerCase() == value.toLowerCase())
      exists = true; 
    i++;
  }
  return exists;
}


/*
function getSourceMetaTypesLinks(response, nodes) {
  // build up the json
  var map = new Map;
  // Loop through the metatypes
  for(var i=0, l=response.data.length; i < l;   i)
  {
      var mediaType = response.data[i].mediaType.toLowerCase();
      var source = getTarget(response, mediaType);
      
      map.put(response.sourceMetaTypes[i].term.toLowerCase() , group);

      // Process each image
      console.log(response.sourceMetaTypes[i].term.toLowerCase()  + " " + response.sourceMetaTypes[i].count );
      
      i++;
  }
}

function getTarget(nodes, value) {
  for(var i=0, l=nodes.length; i < l;   i) {
    if ( nodes[i].nodeName.toLowerCase() == value.toLowerCase() ) {
      return i;
    }
    i++;
  }
}
*/


        