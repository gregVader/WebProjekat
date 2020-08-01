/***********************************************************************
  * Module:  CommentCsvConverter.java
 * Author:  Geri
 * Purpose: Defines the Class CommentCsvConverter
 ***********************************************************************/

package repository.csv.converter;


import java.util.StringJoiner;

import beans.Comment;
import beans.User;

public class CommentCsvConverter implements ICsvConverter<Comment> {
   private String delimiter = "~";
   private String newLine = "`";
   
   public String toCsv(Comment entity){
	   StringJoiner joiner = new StringJoiner(delimiter);
	   
	   joiner.add(String.valueOf(entity.getId()));
	   joiner.add(entity.getText().replace("\n", newLine));
	   joiner.add(String.valueOf(entity.getRating()));
	   joiner.add(String.valueOf(entity.isDeleted()));
	   joiner.add(String.valueOf(entity.isApproved()));
	   joiner.add(String.valueOf(entity.getUser() == null ? "" : entity.getUser().getId()));
	   
      return joiner.toString();
   }
   
   public Comment fromCsv(String entityCsv) {
	  String[] tokens = entityCsv.split(delimiter);
	  
	  long id = Long.valueOf(tokens[0]);
	  String text = tokens[1].replace(newLine, "\n");
	  int rating = Integer.valueOf(tokens[2]);
	  boolean deleted = Boolean.valueOf(tokens[3]);
	  boolean approved = Boolean.valueOf(tokens[4]);
	  User user = new User(Long.valueOf(tokens[5])); 
	  
	  Comment retVal = new Comment(id, text, rating, deleted, approved, user);
	  
      return retVal;
   }

}