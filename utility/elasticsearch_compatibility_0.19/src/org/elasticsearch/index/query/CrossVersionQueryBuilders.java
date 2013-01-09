package org.elasticsearch.index.query;

public class CrossVersionQueryBuilders {

	  /**
     * Creates a text query with type "PHRASE" for the provided field name and text.
     *
     * @param name The field name.
     * @param text The query text (to be analyzed).
     */
    public static MatchQueryBuilder matchPhraseQuery(String name, Object text) {
        return QueryBuilders.matchPhraseQuery(name, text);
    }
}
