package org.elasticsearch.index.query;

import java.util.Map;

public class CrossVersionQueryBuilders 
{

	  /**
     * Creates a text query with type "PHRASE" for the provided field name and text.
     *
     * @param name The field name.
     * @param text The query text (to be analyzed).
     */
    public static MatchQueryBuilder matchPhraseQuery(String name, Object text) {
        return QueryBuilders.matchPhraseQuery(name, text);
    }
    
    public static CustomFiltersScoreQueryBuilder customFiltersScoreQuery(BaseQueryBuilder queryObj)
    {
    	return new CustomFiltersScoreQueryBuilder( QueryBuilders.functionScoreQuery(queryObj) );
    }

	public static CustomScoreQueryBuilder customScoreQuery(BaseQueryBuilder queryObj) 
	{		
		return new CustomScoreQueryBuilder( QueryBuilders.functionScoreQuery(queryObj) );
	}
	
	public static CustomScoreQueryBuilder customScoreQueryBuilderScript(BaseQueryBuilder queryObj, String script, String lang, Map<String, Object> params)
	{
		return new CustomScoreQueryBuilder( QueryBuilders.functionScoreQuery(queryObj) ).script(script, lang, params);
	}
}
