

function getTreemapData(response) {
var data = [];
    data.push(getSourceMetaTypes(response));
    data.push(getSourceMetaTags(response));
    data.push(getTimes(response));
    data.push(getPeople(response));
    data.push(getPlaces(response));
    console.log(data);
    return data;
}


function updateCount(exists, term) {
    var count = 0;
    for(var i=0, l=exists.length; i < l;   i) {
        if (exists[i].term.toLowerCase() == term.toLowerCase())
            return true;
        
        i++;
    }
    return false;
}

function termExistsAverage(exists, term, count) {
    var result = false;
    var totalCount = 1;
    for(var i=0, l=exists.length; i < l;   i) {
        if (exists[i].term.toLowerCase() == term.toLowerCase()) {
            totalCount++;
            exists[i].count = parseInt((parseInt(exists[i].count) + parseInt(count))/ totalCount); 
            result = true;
        }
        
        i++;
    }
    return result;
    
}

function termExists(exists, term, count) {
    var result = false;
    for(var i=0, l=exists.length; i < l;   i) {
        if (exists[i].term.toLowerCase() == term.toLowerCase()) {
            exists[i].count = parseInt(exists[i].count) + parseInt(count); 
            result = true;
        }
        
        i++;
    }
    return result;
    
}


function getSourceMetaTags(response) {
  var string = '{"Tags": {';
  var tags = [];
  var exists = [];
  // Loop through the items
  for(var i=0, l=response.sourceMetaTags.length; i < l;   i)
  {
    
    if (termExists(exists, response.sourceMetaTags[i].term.toLowerCase(), response.sourceMetaTags[i].count) == false)
        exists.push({term: response.sourceMetaTags[i].term.toLowerCase(), count:response.sourceMetaTags[i].count});
    
    i++;
  }
  
  
  for(var i=0, l=exists.length; i < l;   i) {
    
    string = string + '"' + exists[i].term.toLowerCase() + '":' + exists[i].count;
    if (i < l - 1 )
        string = string + ",";
        
    i++;
  }
  
  string = string + "}}"
  tags = jQuery.parseJSON(string);

  return tags;
}

function getSourceMetaTypes(response) {
  var string = '{"Media": {';
  var media = [];
  // Loop through the items
  for(var i=0, l=response.sourceMetaTypes.length; i < l;   i)
  {
        
    string = string + '"' + response.sourceMetaTypes[i].term.toLowerCase() + '":' + response.sourceMetaTypes[i].count;
    if (i < l - 1 )
        string = string + ",";
    
    /*{
        "Media": {
            "News": 5443,
            "Imagery": 4545
        }
    }*/
    i++;
  }
  string = string + "}}"
  media = jQuery.parseJSON(string);
  
  return media;
 
}


function getPeople(response, group) {
  var string = '{"People": {';
  var people = [];
 
  // Loop through the items
  for(var i=0, l=response.entities.length; i < l;   i)
  { 
        if (response.entities[i].type.toLowerCase() == "person" ) {      
          string = string + '"' + response.entities[i].disambiguous_name.toLowerCase() + '":' + parseInt(parseFloat(response.entities[i].significance)/4) + ',';
        }
        
      i++;
  }
  
  string = string.substring(0, string.length - 1);
  string = string + "}}"
  console.log(string);
  people = jQuery.parseJSON(string);
  
  return people;
}

function getPlaces(response, group) {
  var string = '{"Places": {';
  var places = [];
 
  // Loop through the items
  for(var i=0, l=response.entities.length; i < l;   i)
  {
        var place = response.entities[i].type.toLowerCase();
        if ( place == "city" || place == "continent" || place == "country" || place == "stateorcountry") {      
          string = string + '"' + response.entities[i].actual_name.toLowerCase() + '":' + parseInt(parseFloat(response.entities[i].significance)/4) + ',';
        }
        
      i++;
  }
  
  string = string.substring(0, string.length - 1);
  string = string + "}}"
  console.log(string);
  places = jQuery.parseJSON(string);
  
  return places;
}



function getTimes(response) {
  var string = '{"Times": {';
  var times = [];
  var today = new Date()
  var exists = [];
  
  // Loop through the items
  for(var i=0, l=response.times.length; i < l;   i)
  {
    
    var date = new Date(parseInt(response.times[i].time) )
    console.log(date);
    console.log(date.toDateString);
    
    if (today.getFullYear() == date.getFullYear()) {
      if ((today.getDay() == date.getDay()) && ( today.getMonth() == date.getMonth())) {
    
        if (termExists(exists, "In the last day", response.times[i].count) == false)
            exists.push({term: "In the last day", count:response.times[i].count});
        
      } else if ((today.getDay != date.getDay()) && (today.getMonth() == date.getMonth())) {
        if (today.getDay() - date.getDay() <= 7 ) {
            if (termExists(exists, "In the last week", response.times[i].count) == false)
                exists.push({term: "In the last week", count:response.times[i].count});
        } else {
          if (termExists(exists, "In the last month", response.times[i].count) == false)
                exists.push({term: "In the last month", count:response.times[i].count});
        }
      } else {
        if ((today.getMonth() >=1) && (today.getMonth() <=3 ) && ((date.getMonth() >=1) && (date.getMonth() <=3))) {
          if (termExists(exists, "In the last quarter", response.times[i].count) == false)
                exists.push({term: "In the last quarter", count:response.times[i].count});
        } else if ((today.getMonth() >=4) && (today.getMonth() <=6) && ((date.getMonth() >=4) && (date.getMonth() <=6))) {
          if (termExists(exists, "In the last quarter", response.times[i].count) == false)
                exists.push({term: "In the last quarter", count:response.times[i].count});
        } else if ((today.getMonth() >=7) && (today.getMonth() <=9) && ((date.getMonth() >=7) && (date.getMonth() <=9))) {
          if (termExists(exists, "In the last quarter", response.times[i].count) == false)
                exists.push({term: "In the last quarter", count:response.times[i].count});
        } else if ((today.getMonth() >=10) && (today.getMonth() <=12) && ((date.getMonth() >=10) && (date.getMonth() <=12))) {
          if (termExists(exists, "In the last quarter", response.times[i].count) == false)
                exists.push({term: "In the last quarter", count:response.times[i].count});
        } else {
          if (termExists(exists, "In the last year", response.times[i].count) == false)
                exists.push({term: "In the last year", count:response.times[i].count});
        }
      }
      
    } else if (today.getFullYear() - date.getFullYear() <= 10 ) {
      if (termExists(exists, "In the last decade", response.times[i].count) == false)
                exists.push({term: "In the last decade", count:response.times[i].count});
    }
    
    i++;
  }
 
  // Loop through the items
  for(var i=0, l=exists.length; i < l;   i) {
    string = string + '"' + exists[i].term + '":' + exists[i].count;
    if (i < l - 1 )
        string = string + ",";
    
    /*{
        "Media": {
            "News": 5443,
            "Imagery": 4545
        }
    }*/
    i++;
  }
  string = string + "}}"
  times = jQuery.parseJSON(string);
  
  return times;
 
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



var data = {
    response: {
        documents: {
            Document1: 3330,
            Document2: 3580,
            Document3: 3432,
            Document4: 4343
        },
        entities: {
            entity1: 4343,
            entity2: 3433,
            entity3: 9084,
            entity4: 5343,
            entity5: 3432
        },
        events: {
            event1: 4343,
            event2: 3433,
            event3: 9084,
            event4: 5343,
            event5: 3432,
            event6: 3423
        },
        mediaTypes: {
            news:4380,
            video:8500,
            social: 4332,
            blogs: 4322
        }
    }
}


var flare = {
  analytics: {
    cluster: {
      AgglomerativeCluster: 3938,
      CommunityStructure: 3812,
      HierarchicalCluster: 6714,
      MergeEdge: 743
    },
    graph: {
      BetweennessCentrality: 3534,
      LinkDistance: 5731,
      MaxFlowMinCut: 7840,
      ShortestPaths: 5914,
      SpanningTree: 3416
    },
    optimization: {
      AspectRatioBanker: 7074
    }
  },
  animate: {
    Easing: 17010,
    FunctionSequence: 5842,
    interpolate: {
      ArrayInterpolator: 1983,
      ColorInterpolator: 2047,
      DateInterpolator: 1375,
      Interpolator: 8746,
      MatrixInterpolator: 2202,
      NumberInterpolator: 1382,
      ObjectInterpolator: 1629,
      PointInterpolator: 1675,
      RectangleInterpolator: 2042
    },
    ISchedulable: 1041,
    Parallel: 5176,
    Pause: 449,
    Scheduler: 5593,
    Sequence: 5534,
    Transition: 9201,
    Transitioner: 19975,
    TransitionEvent: 1116,
    Tween: 6006
  },
  data: {
    converters: {
      Converters: 721,
      DelimitedTextConverter: 4294,
      GraphMLConverter: 9800,
      IDataConverter: 1314,
      JSONConverter: 2220
    },
    DataField: 1759,
    DataSchema: 2165,
    DataSet: 586,
    DataSource: 3331,
    DataTable: 772,
    DataUtil: 3322
  },
  display: {
    DirtySprite: 8833,
    LineSprite: 1732,
    RectSprite: 3623,
    TextSprite: 10066
  },
  flex: {
    FlareVis: 4116
  },
  physics: {
    DragForce: 1082,
    GravityForce: 1336,
    IForce: 319,
    NBodyForce: 10498,
    Particle: 2822,
    Simulation: 9983,
    Spring: 2213,
    SpringForce: 1681
  },
  query: {
    AggregateExpression: 1616,
    And: 1027,
    Arithmetic: 3891,
    Average: 891,
    BinaryExpression: 2893,
    Comparison: 5103,
    CompositeExpression: 3677,
    Count: 781,
    DateUtil: 4141,
    Distinct: 933,
    Expression: 5130,
    ExpressionIterator: 3617,
    Fn: 3240,
    If: 2732,
    IsA: 2039,
    Literal: 1214,
    Match: 3748,
    Maximum: 843,
    methods: {
      add: 593,
      and: 330,
      average: 287,
      count: 277,
      distinct: 292,
      div: 595,
      eq: 594,
      fn: 460,
      gt: 603,
      gte: 625,
      iff: 748,
      isa: 461,
      lt: 597,
      lte: 619,
      max: 283,
      min: 283,
      mod: 591,
      mul: 603,
      neq: 599,
      not: 386,
      or: 323,
      orderby: 307,
      range: 772,
      select: 296,
      stddev: 363,
      sub: 600,
      sum: 280,
      update: 307,
      variance: 335,
      where: 299,
      xor: 354,
      _: 264
    },
    Minimum: 843,
    Not: 1554,
    Or: 970,
    Query: 13896,
    Range: 1594,
    StringUtil: 4130,
    Sum: 791,
    Variable: 1124,
    Variance: 1876,
    Xor: 1101
  },
  scale: {
    IScaleMap: 2105,
    LinearScale: 1316,
    LogScale: 3151,
    OrdinalScale: 3770,
    QuantileScale: 2435,
    QuantitativeScale: 4839,
    RootScale: 1756,
    Scale: 4268,
    ScaleType: 1821,
    TimeScale: 5833
  },
  util: {
    Arrays: 8258,
    Colors: 10001,
    Dates: 8217,
    Displays: 12555,
    Filter: 2324,
    Geometry: 10993,
    heap: {
      FibonacciHeap: 9354,
      HeapNode: 1233
    },
    IEvaluable: 335,
    IPredicate: 383,
    IValueProxy: 874,
    math: {
      DenseMatrix: 3165,
      IMatrix: 2815,
      SparseMatrix: 3366
    },
    Maths: 17705,
    Orientation: 1486,
    palette: {
      ColorPalette: 6367,
      Palette: 1229,
      ShapePalette: 2059,
      SizePalette: 2291
    },
    Property: 5559,
    Shapes: 19118,
    Sort: 6887,
    Stats: 6557,
    Strings: 22026
  },
  vis: {
    axis: {
      Axes: 1302,
      Axis: 24593,
      AxisGridLine: 652,
      AxisLabel: 636,
      CartesianAxes: 6703
    },
    controls: {
      AnchorControl: 2138,
      ClickControl: 3824,
      Control: 1353,
      ControlList: 4665,
      DragControl: 2649,
      ExpandControl: 2832,
      HoverControl: 4896,
      IControl: 763,
      PanZoomControl: 5222,
      SelectionControl: 7862,
      TooltipControl: 8435
    },
    data: {
      Data: 20544,
      DataList: 19788,
      DataSprite: 10349,
      EdgeSprite: 3301,
      NodeSprite: 19382,
      render: {
        ArrowType: 698,
        EdgeRenderer: 5569,
        IRenderer: 353,
        ShapeRenderer: 2247
      },
      ScaleBinding: 11275,
      Tree: 7147,
      TreeBuilder: 9930
    },
    events: {
      DataEvent: 2313,
      SelectionEvent: 1880,
      TooltipEvent: 1701,
      VisualizationEvent: 1117
    },
    legend: {
      Legend: 20859,
      LegendItem: 4614,
      LegendRange: 10530
    },
    operator: {
      distortion: {
        BifocalDistortion: 4461,
        Distortion: 6314,
        FisheyeDistortion: 3444
      },
      encoder: {
        ColorEncoder: 3179,
        Encoder: 4060,
        PropertyEncoder: 4138,
        ShapeEncoder: 1690,
        SizeEncoder: 1830
      },
      filter: {
        FisheyeTreeFilter: 5219,
        GraphDistanceFilter: 3165,
        VisibilityFilter: 3509
      },
      IOperator: 1286,
      label: {
        Labeler: 9956,
        RadialLabeler: 3899,
        StackedAreaLabeler: 3202
      },
      layout: {
        AxisLayout: 6725,
        BundledEdgeRouter: 3727,
        CircleLayout: 9317,
        CirclePackingLayout: 12003,
        DendrogramLayout: 4853,
        ForceDirectedLayout: 8411,
        IcicleTreeLayout: 4864,
        IndentedTreeLayout: 3174,
        Layout: 7881,
        NodeLinkTreeLayout: 12870,
        PieLayout: 2728,
        RadialTreeLayout: 12348,
        RandomLayout: 870,
        StackedAreaLayout: 9121,
        TreeMapLayout: 9191
      },
      Operator: 2490,
      OperatorList: 5248,
      OperatorSequence: 4190,
      OperatorSwitch: 2581,
      SortOperator: 2023
    },
    Visualization: 16540
  }
};