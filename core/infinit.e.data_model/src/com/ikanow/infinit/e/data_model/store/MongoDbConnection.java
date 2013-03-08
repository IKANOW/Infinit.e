/*******************************************************************************
 * Copyright 2012 The Infinit.e Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.ikanow.infinit.e.data_model.store;

import java.net.UnknownHostException;

import com.ikanow.infinit.e.data_model.utils.PropertiesManager;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.mongodb.ReadPreference;


/**
 * Classed used to process all incoming query requests to API
 * 
 * @author cmorgan
 *
 */
public class MongoDbConnection {
	
	/** 
	  * Private Class Variables
	  */
	private Mongo mongo;
	private String server = "localhost";
	private int port = 27017;
	private boolean successful = true;
	
	/** 
	  * Get the server
	  */
	public String getServer() {
		return server;
	}
	/** 
	  * Get the port
	  */
	public int getPort() {
		return port;
	}
	/** 
	  * Findout if the connection is established
	  */
	public boolean isSuccessful() {
		if ( mongo != null) {
			this.successful = true;
		}
		else {
			this.successful = false;
		}
		return this.successful;
	}

	/** 
	 * Get the Mongo Object
	 */
	public Mongo getMongo() {
		return mongo;
	}

	/** 
	  * Class Constructor used to establish the mongo object
	 * @throws MongoException 
	 * @throws UnknownHostException 
	  */
	public MongoDbConnection() throws UnknownHostException, MongoException {
		// Test to see of the mongo object is populated
		if ( mongo == null ) 
			mongo = new Mongo();	
	}
	/**
	 * Class Constructor used to establish the mongo object
	 * 
	 * @param  server	the server location ( example localhost )
	 * @throws MongoException 
	 * @throws UnknownHostException 
	 */
	public MongoDbConnection(String server) throws UnknownHostException, MongoException {
		this.server = server;
		mongo = new Mongo(this.server);
	}
	/**
	 * Class Constructor used to establish the mongo object
	 * 
	 * @param  server	the server location ( example localhost )
	 * @param  port		the port number ( example 27017
	 * @throws MongoException 
	 * @throws UnknownHostException 
	 */
	public MongoDbConnection(String server, int port) throws UnknownHostException, MongoException {
		this.server = server;
		this.port = port;
		mongo = new Mongo(this.server, this.port);
	}
	
	/**
	 * Class Constructor used to establish the mongo object
	 * 
	 * @param  server	the server location ( example localhost )
	 * @param  port		the port number ( example 27017
	 * @throws MongoException 
	 * @throws UnknownHostException 
	 */
	public MongoDbConnection(PropertiesManager properties) throws UnknownHostException, MongoException {
		this.server = properties.getDatabaseServer();
		this.port = properties.getDatabasePort();
		mongo = new Mongo(this.server, this.port);
		
		if (properties.getDistributeAllDbReadsAcrossSlaves()) {
			mongo.setReadPreference(ReadPreference.SECONDARY);
		}
	}
	
	/*
	 * 
	 */
	public void CloseConnection() {
		if ( mongo != null )
			mongo.close();
	}
	
	
	
	
}
