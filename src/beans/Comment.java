/***********************************************************************
 * Module:  Comment.java
 * Author:  Geri
 * Purpose: Defines the Class Comment
 ***********************************************************************/

package beans;

import beans.interfaces.IDeletable;
import beans.interfaces.IIdentifiable;

public class Comment implements IDeletable, IIdentifiable {
   private long id;
   private String text;
   private int rating;
   private boolean deleted;
   private boolean approved;
   
   private User user;
   private long apartmentId;
   
//Constructors
	public Comment(long id, String text, int rating, boolean deleted, boolean approved, User user, long apartmentId) {
		super();
		this.id = id;
		this.text = text;
		this.rating = rating;
		this.deleted = deleted;
		this.approved = approved;
		this.user = user;
		this.apartmentId = apartmentId;
	}
	
	public Comment() {
		super();
	}
	
	public Comment(String text, int rating, boolean deleted, boolean approved, User user, long apartmentId) {
		super();
		this.text = text;
		this.rating = rating;
		this.deleted = deleted;
		this.approved = approved;
		this.user = user;
		this.apartmentId = apartmentId;
	}
	
	public Comment(long id)
	{
		this.id = id;
	}
	
//Getters and Setters
	public String getText() {
		return text;
	}
	
	public void setText(String text) {
		this.text = text;
	}
	
	public int getRating() {
		return rating;
	}
	
	public void setRating(int rating) {
		this.rating = rating;
	}
	
	public boolean isDeleted() {
		return deleted;
	}
	
	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}
	
	public boolean isApproved() {
		return approved;
	}
	
	public void setApproved(boolean approved) {
		this.approved = approved;
	}
	
	public User getUser() {
		return user;
	}
	
	public void setUser(User user) {
		this.user = user;
	}
	
	public long getApartmentId() {
		return apartmentId;
	}

	public void setApartmentId(long apartmentId) {
		this.apartmentId = apartmentId;
	}

	public long getId() {
	    return this.id;
	 }
	 
	 public void setId(long id) {
	    this.id = id;
	 }
  
//Methods
	 public void delete() {
	      this.setDeleted(true);
	  }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Comment other = (Comment) obj;
		if (id != other.id)
			return false;
		return true;
	}
	  
}