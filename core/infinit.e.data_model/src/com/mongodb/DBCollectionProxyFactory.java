package com.mongodb;

import java.lang.reflect.Method;

import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

public class DBCollectionProxyFactory {

	public static DBCollection get(DBCollection dbc) {
		return get(dbc.getDB(), dbc.getName());
	}
	@SuppressWarnings("deprecation")
	public static DBCollection get(DB db, String name) {
		
		//TODO: need to use code for this in the following placed:
		// (done)
		// - MongoInputFormat
		// - MongoOutputFormat
		
		Enhancer collectionEnhancer = new Enhancer();
		collectionEnhancer.setSuperclass(com.mongodb.DBCollectionImpl.class);
		MethodInterceptor collectionMi = new MethodInterceptor()
		{
			boolean _top_level = true;
			
			@Override
			public Object intercept(Object object, Method method,
					Object[] args, MethodProxy methodProxy )
					throws Throwable
			{
				if (_top_level) {
					try {
						_top_level = false;
						for (int count = 0; ; count++) {
							//DEBUG
							//System.out.println("intercepted method: " + method.toString() + ", loop=" + count + ");
							
							try {
								Object o = methodProxy.invokeSuper(object, args);
								//THIS CODE DOESN'T APPPEAR TO BE NEEDED, BUT LEAVE HERE IN CASE IT PROVES TO
								//if (o instanceof DBCursor) {
								//	o =  getCursor((DBCursor) o);
								//}							
								return o;
							}
							catch (com.mongodb.CommandFailureException e) {
								if (count < 60) {
									continue;
								}
								throw e;
							}
						}
					}
					finally {
						_top_level = true;
					}
				}
				else {
					return methodProxy.invokeSuper(object, args);
				}
			}
			
		};
		collectionEnhancer.setCallback(collectionMi);
		return (DBCollection) collectionEnhancer.create(
				new Class[]{com.mongodb.DBApiLayer.class, String.class}, 
				new Object[]{db, name});
	}

	//DO THE SAME FOR DBCURSOR (BASICALLY ONLY CARE ABOUT next()/hasNext())
	//(ACTUALLY DOESN'T SEEM TO BE NEEDED)
	protected static DBCursor getCursor(DBCursor from) {
		Enhancer dbcursorEnhancer = new Enhancer();
		dbcursorEnhancer.setSuperclass(com.mongodb.DBCursor.class);
		MethodInterceptor collectionMi = new MethodInterceptor() {
			boolean _top_level = true;
			
			@Override
			public Object intercept(Object object, Method method,
					Object[] args, MethodProxy methodProxy )
					throws Throwable
			{
				if (_top_level) {
					try {
						_top_level = false;
						for (int count = 0; ; count++) {
							//DEBUG
							//System.out.println("intercepted method: " + method.toString() + ", loop=" + count + ");
							
							try {
								Object o = methodProxy.invokeSuper(object, args);
								return o;
							}
							catch (com.mongodb.CommandFailureException e) {
								if (count < 60) {
									continue;
								}
								throw e;
							}
						}
					}
					finally {
						_top_level = true;
					}
				}
				else {
					return methodProxy.invokeSuper(object, args);
				}
			}
		};
		dbcursorEnhancer.setCallback(collectionMi);
		return (DBCursor) dbcursorEnhancer.create(
				new Class[]{DBCollection.class, DBObject.class, DBObject.class, ReadPreference.class}, 
				new Object[]{from.getCollection(), from.getQuery(), from.getKeysWanted(), from.getReadPreference()});
	}
}
