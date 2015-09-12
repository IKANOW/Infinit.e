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
		//TODO replace cursor with cursor + check (Actually anything iterable?)
		
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(com.mongodb.DBCollectionImpl.class);
		MethodInterceptor mi = new MethodInterceptor()
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
								return methodProxy.invokeSuper(object, args);
							}
							catch (com.mongodb.MongoException e) {
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
		enhancer.setCallback(mi);
		return (DBCollection) enhancer.create(
				new Class[]{com.mongodb.DBApiLayer.class, String.class}, 
				new Object[]{db, name});
	}
	
}
