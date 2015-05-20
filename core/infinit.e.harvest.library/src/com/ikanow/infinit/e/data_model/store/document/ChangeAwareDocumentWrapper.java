/*******************************************************************************
 * Copyright 2015, The Infinit.e Open Source Project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.ikanow.infinit.e.data_model.store.document;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;

import com.google.gson.GsonBuilder;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePipelinePojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.utils.IChangeListener;
import com.ikanow.infinit.e.data_model.utils.ModifiedChangeListener;
import com.mongodb.DBObject;

public class ChangeAwareDocumentWrapper extends DocumentPojo {
	
	private DocumentPojo delegate;

	/**
	 * @return the delegate
	 */
	public DocumentPojo getDelegate() {
		return delegate;
	}

	public ChangeAwareDocumentWrapper(DocumentPojo delegate){
		this.delegate = delegate;
	}

	/**
	 * @return
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return delegate.hashCode();
	}

	/**
	 * @param obj
	 * @return
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		return delegate.equals(obj);
	}

	/**
	 * @return
	 * @see com.ikanow.infinit.e.data_model.store.BaseDbPojo#toDb()
	 */
	public DBObject toDb() {
		return delegate.toDb();
	}

	/**
	 * @return
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#getId()
	 */
	public ObjectId getId() {
		return delegate.getId();
	}

	/**
	 * @param _id
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#setId(org.bson.types.ObjectId)
	 */
	public void setId(ObjectId _id) {
		delegate.setId(_id);
		ModifiedChangeListener.notify(attributeChangeListener,_id);
	}

	/**
	 * @return
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#getTitle()
	 */
	public String getTitle() {
		return delegate.getTitle();
	}

	/**
	 * @param title
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#setTitle(java.lang.String)
	 */
	public void setTitle(String title) {
		delegate.setTitle(title);
		ModifiedChangeListener.notify(attributeChangeListener,title);
	}

	/**
	 * @return
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#getDescription()
	 */
	public String getDescription() {
		return delegate.getDescription();
	}

	/**
	 * @param description
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#setDescription(java.lang.String)
	 */
	public void setDescription(String description) {
		delegate.setDescription(description);
		ModifiedChangeListener.notify(attributeChangeListener,description);
	}

	/**
	 * @return
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#getUrl()
	 */
	public String getUrl() {
		return delegate.getUrl();
	}

	/**
	 * @return
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return delegate.toString();
	}

	/**
	 * @param url
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#setUrl(java.lang.String)
	 */
	public void setUrl(String url) {
		delegate.setUrl(url);
		ModifiedChangeListener.notify(attributeChangeListener,url);
	}

	/**
	 * @return
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#getCreated()
	 */
	public Date getCreated() {
		return delegate.getCreated();
	}

	/**
	 * @param created
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#setCreated(java.util.Date)
	 */
	public void setCreated(Date created) {
		delegate.setCreated(created);
		ModifiedChangeListener.notify(attributeChangeListener,created);
	}

	/**
	 * @return
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#getModified()
	 */
	public Date getModified() {
		return delegate.getModified();
	}

	/**
	 * @param modified
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#setModified(java.util.Date)
	 */
	public void setModified(Date modified) {
		delegate.setModified(modified);
		ModifiedChangeListener.notify(attributeChangeListener,modified);
	}

	/**
	 * @return
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#getPublishedDate()
	 */
	public Date getPublishedDate() {
		return delegate.getPublishedDate();
	}

	/**
	 * @param publishedDate
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#setPublishedDate(java.util.Date)
	 */
	public void setPublishedDate(Date publishedDate) {
		delegate.setPublishedDate(publishedDate);
		ModifiedChangeListener.notify(attributeChangeListener,publishedDate);
	}

	/**
	 * @return
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#getSource()
	 */
	public String getSource() {
		return delegate.getSource();
	}

	/**
	 * @param source
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#setSource(java.lang.String)
	 */
	public void setSource(String source) {
		delegate.setSource(source);
		ModifiedChangeListener.notify(attributeChangeListener,source);
	}

	/**
	 * @return
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#getRawSourceKey()
	 */
	public String getRawSourceKey() {
		return delegate.getRawSourceKey();
	}

	/**
	 * @return
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#getSourceKey()
	 */
	public String getSourceKey() {
		return delegate.getSourceKey();
	}

	/**
	 * @param sourceKey
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#setSourceKey(java.lang.String)
	 */
	public void setSourceKey(String sourceKey) {
		delegate.setSourceKey(sourceKey);
		ModifiedChangeListener.notify(attributeChangeListener,sourceKey);
	}

	/**
	 * @param entities
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#setEntities(java.util.List)
	 */
	public void setEntities(List<EntityPojo> entities) {
		delegate.setEntities(entities);
		// for now we do not want the document to notofy the listeners on association changes
	}

	/**
	 * @return
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#getEntities()
	 */
	public List<EntityPojo> getEntities() {
		return delegate.getEntities();
	}

	/**
	 * @return
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#getMediaType()
	 */
	public String getMediaType() {
		return delegate.getMediaType();
	}

	/**
	 * @param mediaType
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#setMediaType(java.lang.String)
	 */
	public void setMediaType(String mediaType) {
		delegate.setMediaType(mediaType);
		ModifiedChangeListener.notify(attributeChangeListener,mediaType);
	}

	/**
	 * @return
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#getFullText()
	 */
	public String getFullText() {
		return delegate.getFullText();
	}

	/**
	 * @param fullText
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#setFullText(java.lang.String)
	 */
	public void setFullText(String fullText) {
		delegate.setFullText(fullText);
		ModifiedChangeListener.notify(attributeChangeListener,fullText);
	}

	/**
	 * 
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#makeFullTextNonTransient()
	 */
	public void makeFullTextNonTransient() {
		delegate.makeFullTextNonTransient();
	}

	/**
	 * @return
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#getTempSource()
	 */
	public SourcePojo getTempSource() {
		return delegate.getTempSource();
	}

	/**
	 * @param tempSource
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#setTempSource(com.ikanow.infinit.e.data_model.store.config.source.SourcePojo)
	 */
	public void setTempSource(SourcePojo tempSource) {
		delegate.setTempSource(tempSource);
		ModifiedChangeListener.notify(attributeChangeListener,tempSource);
	}

	/**
	 * @param events
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#setAssociations(java.util.List)
	 */
	public void setAssociations(List<AssociationPojo> events) {
		delegate.setAssociations(events);
		// for now we do not want the document to notofy the listeners on association changes
	}

	/**
	 * @return
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#getAssociations()
	 */
	public List<AssociationPojo> getAssociations() {
		return delegate.getAssociations();
	}

	/**
	 * @param fieldName
	 * @param fieldVal
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#addToMetadata(java.lang.String, java.lang.Object)
	 */
	public void addToMetadata(String fieldName, Object fieldVal) {
		delegate.addToMetadata(fieldName, fieldVal);
		ModifiedChangeListener.notify(dirtyChangeListener);
	}

	/**
	 * @param fieldName
	 * @param fieldVals
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#addToMetadata(java.lang.String, java.lang.Object[])
	 */
	public void addToMetadata(String fieldName, Object[] fieldVals) {
		delegate.addToMetadata(fieldName, fieldVals);
		ModifiedChangeListener.notify(dirtyChangeListener);
	}

	/**
	 * @param metadata
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#setMetadata(java.util.LinkedHashMap)
	 */
	public void setMetadata(LinkedHashMap<String, Object[]> metadata) {
		delegate.setMetadata(metadata);
		ModifiedChangeListener.notify(attributeChangeListener,metadata);
	}

	/**
	 * @return
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#getMetadata()
	 */
	public LinkedHashMap<String, Object[]> getMetadata() {
		ModifiedChangeListener.notify(dirtyChangeListener);
		return delegate.getMetadata();
	}

	/**
	 * @return
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#getMetaData()
	 */
	public LinkedHashMap<String, Object[]> getMetaData() {
		ModifiedChangeListener.notify(dirtyChangeListener);
		return delegate.getMetaData();
	}

	/**
	 * @return
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#getTags()
	 */
	public Set<String> getTags() {
		return delegate.getTags();
	}

	/**
	 * @param tags_
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#setTags(java.util.Set)
	 */
	public void setTags(Set<String> tags_) {
		delegate.setTags(tags_);
		ModifiedChangeListener.notify(attributeChangeListener,tags_);
	}

	/**
	 * @param tags_
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#addTags(java.util.Set)
	 */
	public void addTags(Set<String> tags_) {
		delegate.addTags(tags_);
	}

	/**
	 * @param communityId
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#setCommunityId(org.bson.types.ObjectId)
	 */
	public void setCommunityId(ObjectId communityId) {
		delegate.setCommunityId(communityId);
		ModifiedChangeListener.notify(attributeChangeListener,communityId);
	}

	/**
	 * @return
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#getCommunityId()
	 */
	public ObjectId getCommunityId() {
		return delegate.getCommunityId();
	}

	/**
	 * @return
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#getDocGeo()
	 */
	public GeoPojo getDocGeo() {
		return delegate.getDocGeo();
	}

	/**
	 * @param docGeo
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#setDocGeo(com.ikanow.infinit.e.data_model.store.document.GeoPojo)
	 */
	public void setDocGeo(GeoPojo docGeo) {
		delegate.setDocGeo(docGeo);
		ModifiedChangeListener.notify(attributeChangeListener,docGeo);
	}

	/**
	 * @param locs
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#setLocs(java.util.Set)
	 */
	public void setLocs(Set<String> locs) {
		delegate.setLocs(locs);
		ModifiedChangeListener.notify(attributeChangeListener,locs);
	}

	/**
	 * @return
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#getLocs()
	 */
	public Set<String> getLocs() {
		return delegate.getLocs();
	}

	/**
	 * @param months
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#setMonths(java.util.Set)
	 */
	public void setMonths(Set<Integer> months) {
		delegate.setMonths(months);
		ModifiedChangeListener.notify(attributeChangeListener,months);
	}

	/**
	 * @return
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#getMonths()
	 */
	public Set<Integer> getMonths() {
		return delegate.getMonths();
	}

	/**
	 * @param sourceUrl
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#setSourceUrl(java.lang.String)
	 */
	public void setSourceUrl(String sourceUrl) {
		delegate.setSourceUrl(sourceUrl);
		ModifiedChangeListener.notify(attributeChangeListener,sourceUrl);
	}

	/**
	 * @return
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#getSourceUrl()
	 */
	public String getSourceUrl() {
		return delegate.getSourceUrl();
	}

	/**
	 * @return
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#getIndex()
	 */
	public String getIndex() {
		return delegate.getIndex();
	}

	/**
	 * @param index
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#setIndex(java.lang.String)
	 */
	public void setIndex(String index) {
		delegate.setIndex(index);
		ModifiedChangeListener.notify(attributeChangeListener,index);
	}

	/**
	 * @return
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#metaDataToText()
	 */
	public String metaDataToText() {
		return delegate.metaDataToText();
	}

	/**
	 * @return
	 * @deprecated
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#getHeaderEndIndex()
	 */
	public int getHeaderEndIndex() {
		return delegate.getHeaderEndIndex();
	}

	/**
	 * @param headerEndIndex
	 * @deprecated
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#setHeaderEndIndex(int)
	 */
	public void setHeaderEndIndex(int headerEndIndex) {
		delegate.setHeaderEndIndex(headerEndIndex);
		ModifiedChangeListener.notify(attributeChangeListener,headerEndIndex);
	}

	/**
	 * @return
	 * @deprecated
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#getFooterStartIndex()
	 */
	public int getFooterStartIndex() {
		return delegate.getFooterStartIndex();
	}

	/**
	 * @param footerStartIndex
	 * @deprecated
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#setFooterStartIndex(int)
	 */
	public void setFooterStartIndex(int footerStartIndex) {
		delegate.setFooterStartIndex(footerStartIndex);
		ModifiedChangeListener.notify(attributeChangeListener,footerStartIndex);
	}

	/**
	 * @param sHeaderField
	 * @deprecated
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#addToHeader(java.lang.String)
	 */
	public void addToHeader(String sHeaderField) {
		delegate.addToHeader(sHeaderField);
	}

	/**
	 * @param sFooterField
	 * @deprecated
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#addToFooter(java.lang.String)
	 */
	public void addToFooter(String sFooterField) {
		delegate.addToFooter(sFooterField);
	}

	/**
	 * @return
	 * @deprecated
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#getHeaderFields()
	 */
	public Set<String> getHeaderFields() {
		return delegate.getHeaderFields();
	}

	/**
	 * @return
	 * @deprecated
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#getFooterFields()
	 */
	public Set<String> getFooterFields() {
		return delegate.getFooterFields();
	}

	/**
	 * @return
	 * @deprecated
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#getHeader()
	 */
	public String getHeader() {
		return delegate.getHeader();
	}

	/**
	 * @return
	 * @deprecated
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#getFooter()
	 */
	public String getFooter() {
		return delegate.getFooter();
	}

	/**
	 * @return
	 * @deprecated
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#getBody()
	 */
	public String getBody() {
		return delegate.getBody();
	}

	/**
	 * @param sourceKey
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#setDuplicateFrom(java.lang.String)
	 */
	public void setDuplicateFrom(String sourceKey) {
		delegate.setDuplicateFrom(sourceKey);
		ModifiedChangeListener.notify(attributeChangeListener,sourceKey);
	}

	/**
	 * @return
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#getDuplicateFrom()
	 */
	public String getDuplicateFrom() {
		return delegate.getDuplicateFrom();
	}

	/**
	 * @param masterClone
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#setCloneFrom(com.ikanow.infinit.e.data_model.store.document.DocumentPojo)
	 */
	public void setCloneFrom(DocumentPojo masterClone) {
		delegate.setCloneFrom(masterClone);
		ModifiedChangeListener.notify(attributeChangeListener,masterClone);
	}

	/**
	 * @return
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#getCloneFrom()
	 */
	public DocumentPojo getCloneFrom() {
		return delegate.getCloneFrom();
	}

	/**
	 * @param gp
	 * @return
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#extendBuilder(com.google.gson.GsonBuilder)
	 */
	public GsonBuilder extendBuilder(GsonBuilder gp) {
		return delegate.extendBuilder(gp);
	}

	/**
	 * @return
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#getAggregateSignif()
	 */
	public Double getAggregateSignif() {
		return delegate.getAggregateSignif();
	}

	/**
	 * @param aggregateSignif
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#setAggregateSignif(java.lang.Double)
	 */
	public void setAggregateSignif(Double aggregateSignif) {
		delegate.setAggregateSignif(aggregateSignif);
		ModifiedChangeListener.notify(attributeChangeListener,aggregateSignif);
	}

	/**
	 * @return
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#getQueryRelevance()
	 */
	public Double getQueryRelevance() {
		return delegate.getQueryRelevance();
	}

	/**
	 * @param queryRelevance
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#setQueryRelevance(java.lang.Double)
	 */
	public void setQueryRelevance(Double queryRelevance) {
		delegate.setQueryRelevance(queryRelevance);
		ModifiedChangeListener.notify(attributeChangeListener,queryRelevance);
	}

	/**
	 * @return
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#getScore()
	 */
	public Double getScore() {
		return delegate.getScore();
	}

	/**
	 * @param score
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#setScore(java.lang.Double)
	 */
	public void setScore(Double score) {
		delegate.setScore(score);
		ModifiedChangeListener.notify(attributeChangeListener,score);
	}

	/**
	 * @param updateId
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#setUpdateId(org.bson.types.ObjectId)
	 */
	public void setUpdateId(ObjectId updateId) {
		delegate.setUpdateId(updateId);
		ModifiedChangeListener.notify(attributeChangeListener,updateId);
	}

	/**
	 * @return
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#getUpdateId()
	 */
	public ObjectId getUpdateId() {
		return delegate.getUpdateId();
	}

	/**
	 * @param displayUrl
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#setDisplayUrl(java.lang.String)
	 */
	public void setDisplayUrl(String displayUrl) {
		delegate.setDisplayUrl(displayUrl);
		ModifiedChangeListener.notify(attributeChangeListener,displayUrl);
	}

	/**
	 * @return
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#getDisplayUrl()
	 */
	public String getDisplayUrl() {
		return delegate.getDisplayUrl();
	}

	/**
	 * @param explain
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#setExplain(java.lang.Object)
	 */
	public void setExplain(Object explain) {
		delegate.setExplain(explain);
		ModifiedChangeListener.notify(attributeChangeListener,explain);
	}

	/**
	 * @return
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#getExplain()
	 */
	public Object getExplain() {
		return delegate.getExplain();
	}

	/**
	 * 
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#resetRawFullText()
	 */
	public void resetRawFullText() {
		delegate.resetRawFullText();
		ModifiedChangeListener.notify(attributeChangeListener,getRawFullText());
	}

	/**
	 * @return
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#getRawFullText()
	 */
	public String getRawFullText() {
		return delegate.getRawFullText();
	}

	/**
	 * @return
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#getSpawnedFrom()
	 */
	public SourcePipelinePojo getSpawnedFrom() {
		return delegate.getSpawnedFrom();
	}

	/**
	 * @param spawnedFrom
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#setSpawnedFrom(com.ikanow.infinit.e.data_model.store.config.source.SourcePipelinePojo)
	 */
	public void setSpawnedFrom(SourcePipelinePojo spawnedFrom) {
		delegate.setSpawnedFrom(spawnedFrom);
		ModifiedChangeListener.notify(attributeChangeListener,spawnedFrom);
	}

	/**
	 * @return
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#getHasDefaultUrl()
	 */
	public boolean getHasDefaultUrl() {
		return delegate.getHasDefaultUrl();
	}

	/**
	 * @param hasDefaultUrl
	 * @see com.ikanow.infinit.e.data_model.store.document.DocumentPojo#setHasDefaultUrl(boolean)
	 */
	public void setHasDefaultUrl(boolean hasDefaultUrl) {
		delegate.setHasDefaultUrl(hasDefaultUrl);
		ModifiedChangeListener.notify(attributeChangeListener,hasDefaultUrl);
	}

	// the first Listener is used to map simple attribute changes into the engine js object
	private transient IChangeListener attributeChangeListener = null;

	/**
	 * @param attributeChangeListener the attributeChangeListener to set
	 */
	public void setAttributeChangeListener(IChangeListener attributeChangeListener) {
		this.attributeChangeListener = attributeChangeListener;
	}

	// the dirtyChangeListener is used to mark the object as "changed beyond repair" in the script engine
	private transient IChangeListener dirtyChangeListener = null;


	/**
	 * @param dirtyChangeListener the dirtyChangeListener to set
	 */
	public void setDirtyChangeListener(IChangeListener dirtyChangeListener) {
		this.dirtyChangeListener = dirtyChangeListener;
	}


	
	/** 
	 * Function does not trigger any notifications on purpose.
	 * @return metadata
	 */
	@Override
	public LinkedHashMap<String, Object[]> getMetadataReadOnly()
	{
		return delegate.getMetadata();
	}

}
