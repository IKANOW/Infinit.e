package com.ikanow.infinit.e.data_model.store.social.sharing;

import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.List;

import org.bson.types.ObjectId;

import com.google.gson.reflect.TypeToken;
import com.ikanow.infinit.e.data_model.store.BaseDbPojo;

public class SharePojo extends BaseDbPojo
{
	// Standard static function for readability
	@SuppressWarnings("unchecked")
	static public TypeToken<List<SharePojo>> listType() { return new TypeToken<List<SharePojo>>(){}; }
	
	// Private class fields
	private ObjectId _id = null;
	private Date created = null;
	private Date modified = null;
	private ShareOwnerPojo owner = null;
	private String type = null;
	private String title = null;
	private String description = null;
	private String mediaType = null;
	private List<String> tags = null;
	private String share = null;
	private DocumentLocationPojo documentLocation = null;
	private List<ShareCommunityPojo> communities = null;
	private byte[] binaryData = null;
	private ObjectId binaryId = null; 
	private ByteArrayOutputStream binaryStream = null;
	
	/**
	 * @param _id the _id to set
	 */
	public void set_id(ObjectId _id) {
		this._id = _id;
	}
	/**
	 * @return the _id
	 */
	public ObjectId get_id() {
		return _id;
	}
	
	/**
	 * @param created the created to set
	 */
	public void setCreated(Date created) {
		this.created = created;
	}
	/**
	 * @return the created
	 */
	public Date getCreated() {
		return created;
	}

	/**
	 * @param modified the modified to set
	 */
	public void setModified(Date modified) {
		this.modified = modified;
	}
	/**
	 * @return the modified
	 */
	public Date getModified() {
		return modified;
	}

	/**
	 * @param shareOwner the shareOwner to set
	 */
	public void setOwner(ShareOwnerPojo owner) {
		this.owner = owner;
	}
	/**
	 * @return the shareOwner
	 */
	public ShareOwnerPojo getOwner() {
		return owner;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}
	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}
	
	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}
	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param tags the tags to set
	 */
	public void setTags(List<String> tags) {
		this.tags = tags;
	}
	/**
	 * @return the tags
	 */
	public List<String> getTags() {
		return tags;
	}
	/**
	 * @param share the share to set
	 */
	public void setShare(String share) {
		this.share = share;
	}
	/**
	 * @return the share
	 */
	public String getShare() {
		return share;
	}
	/**
	 * @param shareLocation the shareLocation to set
	 */
	public void setDocumentLocation(DocumentLocationPojo documentLocation) {
		this.documentLocation = documentLocation;
	}
	/**
	 * @return the shareLocation
	 */
	public DocumentLocationPojo getDocumentLocation() {
		return documentLocation;
	}
	/**
	 * @param communities the communities to set
	 */
	public void setCommunities(List<ShareCommunityPojo> communities) {
		this.communities = communities;
	}
	/**
	 * @return the communities
	 */
	public List<ShareCommunityPojo> getCommunities() {
		return communities;
	}





	/**
	 * Share.ShareOwnerPojo
	 * @author craigvitter
	 */
	public static class ShareOwnerPojo
	{
		private ObjectId _id = null;
		private String email = null;
		private String displayName = null;
		/**
		 * @param _id the _id to set
		 */
		public void set_id(ObjectId _id) {
			this._id = _id;
		}
		/**
		 * @return the _id
		 */
		public ObjectId get_id() {
			return _id;
		}
		/**
		 * @param email the email to set
		 */
		public void setEmail(String email) {
			this.email = email;
		}
		/**
		 * @return the email
		 */
		public String getEmail() {
			return email;
		}
		/**
		 * @param displayName the displayName to set
		 */
		public void setDisplayName(String displayName) {
			this.displayName = displayName;
		}
		/**
		 * @return the displayName
		 */
		public String getDisplayName() {
			return displayName;
		}
	}
	
	
	/**
	 * Share.DocumentLocationPojo
	 * @author craigvitter
	 */
	public static class DocumentLocationPojo
	{
		private ObjectId _id = null;
		private String database = null;
		private String collection = null;
		/**
		 * @param _id the _id to set
		 */
		public void set_id(ObjectId _id) {
			this._id = _id;
		}
		/**
		 * @return the _id
		 */
		public ObjectId get_id() {
			return _id;
		}
		/**
		 * @param database the database to set
		 */
		public void setDatabase(String database) {
			this.database = database;
		}
		/**
		 * @return the database
		 */
		public String getDatabase() {
			return database;
		}
		/**
		 * @param collection the collection to set
		 */
		public void setCollection(String collection) {
			this.collection = collection;
		}
		/**
		 * @return the collection
		 */
		public String getCollection() {
			return collection;
		}
	}
	

	/**
	 * Share.CommunityPojo
	 * @author craigvitter
	 */
	public static class ShareCommunityPojo
	{
		private ObjectId _id = null;
		private String name = null;
		private String comment = null;
		
		/**
		 * @param _id the _id to set
		 */
		public void set_id(ObjectId _id) {
			this._id = _id;
		}
		/**
		 * @return the _id
		 */
		public ObjectId get_id() {
			return _id;
		}
		/**
		 * @param name the name to set
		 */
		public void setName(String name) {
			this.name = name;
		}
		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}
		/**
		 * @param comment the comment to set
		 */
		public void setComment(String comment) {
			this.comment = comment;
		}
		/**
		 * @return the comment
		 */
		public String getComment() {
			return comment;
		}
	}

	public void setBinaryData(byte[] binaryData) {
		this.binaryData = binaryData;
	}
	
	
	public byte[] getBinaryData() {
		return binaryData;
	}
	
	
	public void setMediaType(String mediaType) {
		this.mediaType = mediaType;
	}
	
	
	public String getMediaType() {
		return mediaType;
	}
	public void setBinaryId(ObjectId binaryId) {
		this.binaryId = binaryId;
	}
	public ObjectId getBinaryId() {
		return binaryId;
	}
	public void setBinaryStream(ByteArrayOutputStream binaryStream) {
		this.binaryStream = binaryStream;
	}
	public ByteArrayOutputStream getBinaryStream() {
		return binaryStream;
	}
}
