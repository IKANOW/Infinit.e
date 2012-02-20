
              
              
            function initializeDocuments(response) {
                var documents = [];
                for(var i=0, l=response.data.length; i < l;   i) {
                    
                    var title = response.data[i].title;
                    var description = response.data[i].description;
                    var url = response.data[i].url
            
                    documents.push({title: title, description: description, url: url});
            
                   i++; 
                }
                
                return documents;
            }
            
            function initializeEntities(response) {
                var entities = [];
                for(var i=0, l=response.data.length; i < l;   i) {
                    
                    for(var e=0, el=response.data[i].entities.length; e < el;   e) {
                        
                        if (response.data[i].entities[e].disambiguous_name != null) {
                            if (entityExists(entities, response.data[i].entities[e].disambiguous_name) == false)
                                entities.push({type: response.data[i].entities[e].type, value: response.data[i].entities[e].disambiguous_name});
                          
                        } 
                            if (entityExists(entities, response.data[i].entities[e].actual_name) == false)
                                entities.push({type: response.data[i].entities[e].type, value: response.data[i].entities[e].actual_name});
                         
                    e++;
                    }
                   i++; 
                }
                
                return entities;
            }
            
            
            function entityExists(entities, value) {
                for (var i=0, l=entities.length; i < l; i) {
                    if (entities[i].value == value) {
                        return true;
                    }
                    i++;
                }
                return false;
            }
            
           
             
            
            
            
            
          