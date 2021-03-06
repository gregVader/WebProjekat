/***********************************************************************
 * Module:  UserService.java
 * Author:  Geri
 * Purpose: Defines the Class UserService
 ***********************************************************************/

package service;

import repository.AccountRepository;
import repository.UserRepository;
import specification.ISpecification;
import specification.filterconverter.UserFilterConverter;
import dto.UserDTO;
import beans.Account;
import beans.Reservation;
import beans.User;
import beans.enums.Gender;
import beans.enums.UserType;
import dto.UserFilterDTO;
import exceptions.BadRequestException;
import exceptions.DatabaseException;
import exceptions.EntityNotFoundException;
import exceptions.InvalidPasswordException;
import exceptions.InvalidUserException;
import exceptions.NotUniqueException;

import java.util.*;
import java.util.stream.Collectors;

public class UserService {
   private UserRepository userRepository;
   private AccountRepository accountRepository;
   private ReservationService reservationService;
   
   private String fieldError = "Empty fields: %s";
   private String passwordFieldError = "Passwords don't match";
   private String oldPasswordError = "Invalid password";
   private String notUniqueError = "Username '%s' is not unique!";
   private String blockedUserError = "User '%s' is blocked";
   
   
//Constructors
   public UserService(UserRepository userRepository, AccountRepository accountRepository, ReservationService reservationService) {
	super();
	this.userRepository = userRepository;
	this.accountRepository = accountRepository;
	this.reservationService = reservationService;
   }

//Methods
   public User register(UserDTO user) throws BadRequestException, DatabaseException {
	   validateUsername(user);
	   validatePassword(user);
	   validatePersonalInfoFields(user);
      
      User createdUser = new User(new Account(user.getUsername(), user.getPassword(), false), user.getName(), user.getSurname(), false, false, user.getGender(), UserType.guest);
       return userRepository.create(createdUser);
   }
   
   private void validatePersonalInfoFields(UserDTO user) throws BadRequestException {
	   boolean hasName, hasSurname, hasGender;
	   
	   hasName = !user.getName().isEmpty();
	   hasSurname = !user.getSurname().isEmpty();
	   hasGender = !(user.getGender() == Gender.undefined);
	   
	   if(!hasName || !hasSurname || !hasGender) {
		   
		   String emptyFields = (!hasName ? "name " : "") + (!hasSurname ? "surname " : "") + (!hasGender ? "gender " : "");
		   
		   throw new BadRequestException(String.format(fieldError, emptyFields));
	   }
   }
   
   private void validateUsername(UserDTO user) throws DatabaseException, BadRequestException {
	   
	   if(user.getUsername().isEmpty())
		   throw new BadRequestException(String.format(fieldError, "username"));
	   
	   if(!isUsernameUnique(user.getUsername()))
		   throw new NotUniqueException(String.format(notUniqueError, user.getUsername()));
	
   }

   private void validatePassword(UserDTO user) throws BadRequestException {
	   
	   if(user.getPassword().isEmpty() || user.getControlPassword().isEmpty()) {
		   String emptyFields = ((user.getPassword().isEmpty() ? "password " : "") + (user.getControlPassword().isEmpty() ? "controlPassword " : ""));
		   throw new BadRequestException(String.format(fieldError, emptyFields));
	   }
	   
	   if(!user.getPassword().equals(user.getControlPassword())) {
		   throw new BadRequestException(passwordFieldError);
	   }
   }
   
   public void delete(long id) throws DatabaseException
   {
	   userRepository.delete(id);
   }
   
   public User login(UserDTO user) throws BadRequestException, DatabaseException {
	   
	   if(user.getUsername().isEmpty() || user.getPassword().isEmpty())
		   throw new BadRequestException();
	   
	   User logInUser = null;
	   
	   try{
		   logInUser = userRepository.getByUsername(user.getUsername());
	   }catch(EntityNotFoundException e) {
		   throw new BadRequestException();
	   }
	   
	   if(!logInUser.getAccount().getPassword().equals(user.getPassword()))
		   throw new BadRequestException(passwordFieldError);
	   
	   if(logInUser.isBlocked())
		   throw new InvalidUserException(String.format(blockedUserError, user.getUsername()));
	   
	   return logInUser;
   }
   
   public User getById(long id) throws DatabaseException {
      User user = userRepository.getEager(id);
      user.getAccount().setPassword("");
      return user;
   }
   
   public User update(UserDTO user) throws BadRequestException, DatabaseException {
	   
      validatePersonalInfoFields(user);
            
      User updateUser = userRepository.getByUsername(user.getUsername());
      updateUser.setName(user.getName());
      updateUser.setSurname(user.getSurname());
      updateUser.setGender(user.getGender());
      userRepository.update(updateUser);
      
      if(!user.getOldPassword().isEmpty()) {
    	  if(user.getOldPassword().equals(updateUser.getAccount().getPassword())){
    		  validatePassword(user);
    		  updateUser.getAccount().setPassword(user.getPassword());
    		  accountRepository.update(updateUser.getAccount());
    	  }
    	  else {
    		  throw new InvalidPasswordException(oldPasswordError);
    	  }
      }
       
      return updateUser;
   }
   
   private boolean isUsernameUnique(String username) {
	   return userRepository.isUsernameUnique(username);
   }

   public List<User> getAll(User loggedInUser) throws DatabaseException, InvalidUserException { 
	   switch(loggedInUser.getUserType()) {
	   case admin: {
		   List<User> users = userRepository.getAllEager();
		      for(User user : users)
		    	  user.getAccount().setPassword("");
		      
		   return users;
	   }
	   case host: {
		   return getGuestsByHost(loggedInUser, loggedInUser.getUserType());
	   }
	   default:
		   throw new InvalidUserException();
	   }
   }
   
   public List<User> getGuestsByHost(User host, UserType userType) throws InvalidUserException, DatabaseException {
	   
	   if(userType != UserType.host)
		   throw new InvalidUserException();
	   
      List<Reservation> reservations = reservationService.getReservationByHost(host, userType);
      
      List<User> users = new ArrayList<User>();
      
      if(!reservations.isEmpty())
    	  users = reservations.stream().map(Reservation::getGuest).distinct().collect(Collectors.toList());
      
      return users;
   }
   
   public List<User> find(UserFilterDTO filter, User thisUser) throws DatabaseException, InvalidUserException {
	  List<User> retVal = new ArrayList<User>();
	  ISpecification<User> specification = UserFilterConverter.getSpecification(filter);
	    
	  if(thisUser.getUserType() == UserType.admin)
		  retVal = userRepository.find(specification);
	  else if(thisUser.getUserType() == UserType.host);
	  	  retVal = getGuestsByHost(thisUser, thisUser.getUserType()).stream().filter(user -> specification.isSatisfiedBy(user)).collect(Collectors.toList());
	   
      return retVal;
   }
   
   public User blockUser(User user, User loggedInUser) throws DatabaseException, InvalidUserException {
	   if(loggedInUser.getUserType() == UserType.admin) {		   
		   User blockedUser = userRepository.getEager(user.getId());
		   blockedUser.block();
		   userRepository.update(blockedUser);
		   
		   blockedUser.getAccount().setPassword("");
		   return blockedUser;
	   }
	   else {
		   throw new InvalidUserException();
	   }
   }
   
   public User unblockUser(User user, User loggedInUser) throws DatabaseException, InvalidUserException {
	   if(loggedInUser.getUserType() == UserType.admin) {		   
		   User blockedUser = userRepository.getEager(user.getId());
		   blockedUser.setBlocked(false);
		   userRepository.update(blockedUser);
		   
		   blockedUser.getAccount().setPassword("");
		   return blockedUser;
	   }
	   else {
		   throw new InvalidUserException();
	   }
   }

}