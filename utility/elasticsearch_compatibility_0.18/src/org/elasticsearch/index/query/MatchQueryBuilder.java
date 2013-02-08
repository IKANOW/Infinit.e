package org.elasticsearch.index.query;

public class MatchQueryBuilder extends org.elasticsearch.index.query.TextQueryBuilder {

    public static enum Type {
        BOOLEAN,
        PHRASE,
        PHRASE_PREFIX
    }
	public MatchQueryBuilder(String name, Object text) {
		super(name, text);
	}
	public MatchQueryBuilder type(Type type) {
		super.type((TextQueryBuilder.Type.values()[type.ordinal()]));
		return this;
	}

}
