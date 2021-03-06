package rest;

import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.put;
import static spark.Spark.delete;
import static spark.Spark.staticFiles;
import static spark.Spark.webSocket;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.security.Key;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import beans.Amenity;
import beans.Apartment;
import beans.Comment;
import beans.PricingCalendar;

import beans.DateRange;
import beans.Reservation;
import beans.User;
import beans.enums.UserType;
import dto.ApartmentDTO;
import dto.ApartmentEditDTO;
import dto.ApartmentFilterDTO;
import dto.BookingDatesDTO;
import dto.CommentDTO;
import dto.ErrorMessageDTO;
import dto.ReservationDTO;
import dto.TokenDTO;
import dto.UserDTO;
import dto.UserFilterDTO;
import exceptions.BadRequestException;
import exceptions.DatabaseException;
import exceptions.InvalidDateException;
import exceptions.InvalidPasswordException;
import exceptions.InvalidUserException;
import exceptions.NotUniqueException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import repository.csv.converter.UserCsvConverter;
import spark.Request;
import utils.AppResources;
import utils.StringResources;
import ws.WsHandler;

public class SparkAppMain {

	private static Gson g;

	/**
	 * Kljuc za potpisivanje JWT tokena.
	 * Biblioteka: https://github.com/jwtk/jjwt
	 */
	static Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
	static AppResources resources;
	static UserCsvConverter userConverter = new UserCsvConverter();
	private static int minutesUntilTokenExpires = 60;

	public static void main(String[] args) throws IOException, DatabaseException {
		
		g = getGson();

		
		try {
			resources = new AppResources();
		} catch (DatabaseException e1) {
			e1.printStackTrace();
			System.out.println("Server resources failed to load");
			return;
		}
		
		port(8088);

		webSocket("/ws", WsHandler.class);

		staticFiles.externalLocation(new File("./static").getCanonicalPath());
		
		get("/rest/vazduhbnb/test", (req, res) -> {
			return "Works";
		});
		
		
		post("/rest/vazduhbnb/login", (req, res) -> {
			res.type("application/json");
			String payload = req.body();
			UserDTO u = g.fromJson(payload, UserDTO.class);
			
			User loggedInUser = null;
			
			try{
				loggedInUser = resources.userService.login(u);
				
				String jws = getJwtToken(loggedInUser);
				
				System.out.println("User " + loggedInUser.getName() + " " + loggedInUser.getSurname() + " (" + loggedInUser.getUserType() + ") logged in.");
				System.out.println("Returned JWT: " + jws);
				
				return g.toJson(new TokenDTO(jws, loggedInUser.getAccount().getUsername(), loggedInUser.getUserType()), TokenDTO.class);
			}catch(InvalidUserException ex) {
				res.status(403); //forbidden (blocked user)
				return g.toJson(new ErrorMessageDTO(ex.getMessage()), ErrorMessageDTO.class);
			}catch(BadRequestException ex) {
				res.status(400);
				return g.toJson(new ErrorMessageDTO(ex.getMessage()), ErrorMessageDTO.class);
			}catch(DatabaseException ex) {
				res.status(500);
				return g.toJson(new ErrorMessageDTO(StringResources.INTERNAL_SERVER_ERROR), ErrorMessageDTO.class);
			}
			
		});
				
		get("/rest/vazduhbnb/reservations", (req, res) -> {
			res.type("application/json");
			
			User loggedInUser = getLoggedInUser(req);
			if(loggedInUser == null) {
				res.status(401);
				return g.toJson(new ErrorMessageDTO(StringResources.USER_NOT_LOGGED_IN), ErrorMessageDTO.class);
			}
			
			try {
				List<Reservation> reservations = resources.reservationService.getReservations(loggedInUser);
				res.status(200);
				return g.toJson(reservations);
			}catch(InvalidUserException e) {
				res.status(403);
				return g.toJson(new ErrorMessageDTO(StringResources.ACCESS_DENIED), ErrorMessageDTO.class);
			}catch(DatabaseException e) {
				res.status(500);
				return g.toJson(new ErrorMessageDTO(StringResources.INTERNAL_SERVER_ERROR), ErrorMessageDTO.class);
			}
		});
		
		put("/rest/vazduhbnb/cancelReservation", (req, res) -> {
			User loggedInUser = getLoggedInUser(req);
			if(loggedInUser == null) {
				res.status(401);
				return g.toJson(new ErrorMessageDTO(StringResources.USER_NOT_LOGGED_IN), ErrorMessageDTO.class);
			}
			
			long reservationId = g.fromJson(req.body(), Long.class);
			
			try {				
				resources.reservationService.cancelReservation(reservationId, loggedInUser);
				res.status(200);
				return "OK";
			}catch(InvalidUserException e) {
				res.status(403);
				return g.toJson(new ErrorMessageDTO(StringResources.ACCESS_DENIED), ErrorMessageDTO.class);				
			}catch(BadRequestException e) {
				res.status(400);
				return g.toJson(new ErrorMessageDTO(e.getMessage()), ErrorMessageDTO.class);
			}catch(DatabaseException e) {
				res.status(500);
				return g.toJson(new ErrorMessageDTO(StringResources.INTERNAL_SERVER_ERROR), ErrorMessageDTO.class);				
			}
		});
		
		put("/rest/vazduhbnb/rejectReservation", (req, res) -> {
			User loggedInUser = getLoggedInUser(req);
			if(loggedInUser == null) {
				res.status(401);
				return g.toJson(new ErrorMessageDTO(StringResources.USER_NOT_LOGGED_IN), ErrorMessageDTO.class);
			}
			
			long reservationId = g.fromJson(req.body(), Long.class);
			
			try {				
				resources.reservationService.rejectReservation(reservationId, loggedInUser);
				res.status(200);
				return "OK";
			}catch(InvalidUserException e) {
				res.status(403);
				return g.toJson(new ErrorMessageDTO(StringResources.ACCESS_DENIED), ErrorMessageDTO.class);				
			}catch(BadRequestException e) {
				res.status(400);
				return g.toJson(new ErrorMessageDTO(e.getMessage()), ErrorMessageDTO.class);
			}catch(DatabaseException e) {
				res.status(500);
				return g.toJson(new ErrorMessageDTO(StringResources.INTERNAL_SERVER_ERROR), ErrorMessageDTO.class);				
			}
		});
		
		put("/rest/vazduhbnb/acceptReservation", (req, res) -> {
			User loggedInUser = getLoggedInUser(req);
			if(loggedInUser == null) {
				res.status(401);
				return g.toJson(new ErrorMessageDTO(StringResources.USER_NOT_LOGGED_IN), ErrorMessageDTO.class);
			}
			
			long reservationId = g.fromJson(req.body(), Long.class);
			
			try {				
				resources.reservationService.acceptReservation(reservationId, loggedInUser);
				res.status(200);
				return "OK";
			}catch(InvalidUserException e) {
				res.status(403);
				return g.toJson(new ErrorMessageDTO(StringResources.ACCESS_DENIED), ErrorMessageDTO.class);				
			}catch(BadRequestException e) {
				res.status(400);
				return g.toJson(new ErrorMessageDTO(e.getMessage()), ErrorMessageDTO.class);
			}catch(DatabaseException e) {
				res.status(500);
				return g.toJson(new ErrorMessageDTO(StringResources.INTERNAL_SERVER_ERROR), ErrorMessageDTO.class);				
			}
		});
		
		put("/rest/vazduhbnb/finishReservation", (req, res) -> {
			User loggedInUser = getLoggedInUser(req);
			if(loggedInUser == null) {
				res.status(401);
				return g.toJson(new ErrorMessageDTO(StringResources.USER_NOT_LOGGED_IN), ErrorMessageDTO.class);
			}
			
			long reservationId = g.fromJson(req.body(), Long.class);
			
			try {				
				resources.reservationService.finishReservation(reservationId, loggedInUser);
				res.status(200);
				return "OK";
			}catch(InvalidUserException e) {
				res.status(403);
				return g.toJson(new ErrorMessageDTO(StringResources.ACCESS_DENIED), ErrorMessageDTO.class);				
			}catch(BadRequestException e) {
				res.status(400);
				return g.toJson(new ErrorMessageDTO(e.getMessage()), ErrorMessageDTO.class);
			}catch(DatabaseException e) {
				res.status(500);
				return g.toJson(new ErrorMessageDTO(StringResources.INTERNAL_SERVER_ERROR), ErrorMessageDTO.class);				
			}
		});
		
		post("/rest/vazduhbnb/comment", (req, res) -> {
			res.type("application/json");
			
			User loggedInUser = getLoggedInUser(req);
			if(loggedInUser == null) {
				res.status(401);
				return g.toJson(new ErrorMessageDTO(StringResources.USER_NOT_LOGGED_IN), ErrorMessageDTO.class);
			}
			
			CommentDTO comment = g.fromJson(req.body(), CommentDTO.class);
			
			try {
				Comment newComment = resources.commentService.create(comment, loggedInUser);
				res.status(200);
				return g.toJson(newComment, Comment.class);
			}catch(InvalidUserException e) {
				res.status(403);
				return g.toJson(new ErrorMessageDTO(StringResources.ACCESS_DENIED), ErrorMessageDTO.class);				
			}catch(BadRequestException e) {
				res.status(400);
				return g.toJson(new ErrorMessageDTO(e.getMessage()), ErrorMessageDTO.class);
			}catch(DatabaseException e) {
				res.status(500);
				return g.toJson(new ErrorMessageDTO(StringResources.INTERNAL_SERVER_ERROR), ErrorMessageDTO.class);				
			}
		});
		
		post("/rest/vazduhbnb/register", (request, response) -> {
			
			response.type("application/json");
			String payload = request.body();
			
			UserDTO u = g.fromJson(payload, UserDTO.class);
			
			try {
				User registeredUser = resources.userService.register(u);
				response.status(200); // OK
				String jws = getJwtToken(registeredUser);
				System.out.println("Registered user: " + registeredUser.getAccount().getUsername() + " with JWTToken: " + jws);
				
				return g.toJson(new TokenDTO(jws, registeredUser.getAccount().getUsername(), registeredUser.getUserType()), TokenDTO.class);
			} catch(NotUniqueException ex) {
				response.status(409); // username is not unique
				return g.toJson(new ErrorMessageDTO(ex.getMessage()), ErrorMessageDTO.class);
			} catch(BadRequestException ex) {
				response.status(400); // data is invalid
				return g.toJson(new ErrorMessageDTO(ex.getMessage()), ErrorMessageDTO.class);
			} catch (DatabaseException ex) {
				response.status(500); // server-side error
				return g.toJson(new ErrorMessageDTO(StringResources.INTERNAL_SERVER_ERROR), ErrorMessageDTO.class);
			}
		});
		
		get("/rest/vazduhbnb/apartments", (req, res) -> {
			res.type("application/json");
			
			System.out.println(req.body());
			
			String payload = req.queryParams("filter");
			ApartmentFilterDTO filter = g.fromJson(payload, ApartmentFilterDTO.class);
			User loggedInUser = getLoggedInUser(req);
			
			try {
				List<Apartment> apartments = resources.apartmentService.find(filter, loggedInUser);
				
				res.status(200);
				return g.toJson(apartments);
			}catch(BadRequestException e) {
				res.status(400);
				return g.toJson(new ErrorMessageDTO(e.getMessage()), ErrorMessageDTO.class);
			}catch(DatabaseException e) {
				res.status(500);
				return g.toJson(new ErrorMessageDTO(StringResources.INTERNAL_SERVER_ERROR), ErrorMessageDTO.class);
			}

		});
		
		get("/rest/vazduhbnb/filteredUsers", (request, response) -> {
			response.type("application/json");
			
			String payload = request.queryParams("filter");
			UserFilterDTO filter = g.fromJson(payload, UserFilterDTO.class);
			User loggedinUser = getLoggedInUser(request);
			
			try{
				List<User> users = resources.userService.find(filter, loggedinUser);
				
				return g.toJson(users);
			}catch(DatabaseException ex){
				response.status(500);
				return g.toJson(new ErrorMessageDTO(ex.getMessage()), ErrorMessageDTO.class);
			}catch(InvalidUserException ex){
				response.status(403);
				return g.toJson(new ErrorMessageDTO(StringResources.ACCESS_DENIED), ErrorMessageDTO.class);
			}
		});
		
		post("/rest/vazduhbnb/reservation", (req, res) -> {
			User loggedInUser = getLoggedInUser(req);
			if(loggedInUser == null) {
				res.status(401);
				return g.toJson(new ErrorMessageDTO(StringResources.USER_NOT_LOGGED_IN), ErrorMessageDTO.class);
			}
			
			ReservationDTO reservation = g.fromJson(req.body(), ReservationDTO.class);
			
			try {
				resources.reservationService.create(reservation, loggedInUser);
				res.status(200);
				return "OK";
			}catch(InvalidUserException e) {
				res.status(403);
				return g.toJson(new ErrorMessageDTO(StringResources.ACCESS_DENIED), ErrorMessageDTO.class);
			}catch(BadRequestException e) {
				res.status(400);
				return g.toJson(new ErrorMessageDTO(e.getMessage()), ErrorMessageDTO.class);
			}catch(DatabaseException e) {
				res.status(500);
				return g.toJson(new ErrorMessageDTO(StringResources.INTERNAL_SERVER_ERROR), ErrorMessageDTO.class);
			}
		});
		
		post("/rest/vazduhbnb/apartment", (request, response) -> {
			response.type("application/json");
			
			User loggedInUser = getLoggedInUser(request);
			if(loggedInUser == null) {
				response.status(401);
				return g.toJson(new ErrorMessageDTO(StringResources.USER_NOT_LOGGED_IN), ErrorMessageDTO.class);
			}
			
			String payload = request.body();
			ApartmentDTO apartment = g.fromJson(payload, ApartmentDTO.class);
			
			try{
				resources.apartmentService.create(apartment, loggedInUser);
				response.status(200);
				return "OK";
			}catch(DatabaseException e){
				response.status(500);
				return g.toJson(new ErrorMessageDTO(StringResources.INTERNAL_SERVER_ERROR), ErrorMessageDTO.class);
			}catch(InvalidUserException e) {
				response.status(403);
				return g.toJson(new ErrorMessageDTO(StringResources.ACCESS_DENIED), ErrorMessageDTO.class);
			}catch(BadRequestException e) {
				response.status(400);
				return g.toJson(new ErrorMessageDTO(e.getMessage()), ErrorMessageDTO.class);
			}
		});
		
		put("/rest/vazduhbnb/apartment", (req, res) -> {
			res.type("application/json");
			
			User loggedInUser = getLoggedInUser(req);
			if(loggedInUser == null) {
				res.status(401);
				return g.toJson(new ErrorMessageDTO(StringResources.USER_NOT_LOGGED_IN), ErrorMessageDTO.class);
			}
			
			ApartmentEditDTO apartmentEdit = g.fromJson(req.body(), ApartmentEditDTO.class);
			
			try {				
				resources.apartmentService.update(apartmentEdit.getApartment(), apartmentEdit.getDates(), loggedInUser);
				res.status(200);
				return "OK";
			}catch(InvalidUserException e) {
				res.status(403);
				return g.toJson(new ErrorMessageDTO(StringResources.ACCESS_DENIED), ErrorMessageDTO.class);
			}catch(BadRequestException e) {
				res.status(400);
				return g.toJson(new ErrorMessageDTO(e.getMessage()), ErrorMessageDTO.class);
			}catch(DatabaseException e) {
				res.status(500);
				return g.toJson(new ErrorMessageDTO(StringResources.INTERNAL_SERVER_ERROR), ErrorMessageDTO.class);
			}
		});
		
		put("/rest/vazduhbnb/approve", (req, res) ->{
			res.type("application/json");
			
			Comment comment = g.fromJson(req.body(), Comment.class);
			boolean toApprove = Boolean.valueOf(req.queryParams("approve"));
			long apartmentId = Long.valueOf(req.queryParams("apartment"));
			
			User loggedInUser = getLoggedInUser(req);
			
			if(loggedInUser == null) {
				res.status(401);
				return g.toJson(new ErrorMessageDTO(StringResources.USER_NOT_LOGGED_IN), ErrorMessageDTO.class);
			}
			
			try {
				resources.commentService.approveComment(comment, toApprove, loggedInUser, apartmentId);
				res.status(200);
				return toApprove ? "approved" : "disapproved";
			}catch(InvalidUserException e) {
				res.status(403);
				return g.toJson(new ErrorMessageDTO(StringResources.ACCESS_DENIED), ErrorMessageDTO.class);
			}catch(DatabaseException e) {
				res.status(500);
				return g.toJson(new ErrorMessageDTO(StringResources.INTERNAL_SERVER_ERROR), ErrorMessageDTO.class);
			}
		});
		
		put("/rest/vazduhbnb/activate", (req, res) ->{
			res.type("application/json");
			
			User loggedInUser = getLoggedInUser(req);
			if(loggedInUser == null) {
				res.status(401);
				return g.toJson(new ErrorMessageDTO(StringResources.USER_NOT_LOGGED_IN), ErrorMessageDTO.class);
			}
			
			boolean toActivate = Boolean.valueOf(req.queryParams("activate"));		
			Apartment apartment = g.fromJson(req.body(), Apartment.class);
			
			try {				
				resources.apartmentService.activateApartment(apartment, loggedInUser, toActivate);
				res.status(200);
				return "Apartment " + (toActivate ? "activated" : "deactivated") + ".";
			}catch(InvalidUserException e) {
				res.status(403);
				return g.toJson(new ErrorMessageDTO(StringResources.ACCESS_DENIED), ErrorMessageDTO.class);
			}catch(DatabaseException e) {
				res.status(500);
				return g.toJson(new ErrorMessageDTO(StringResources.INTERNAL_SERVER_ERROR), ErrorMessageDTO.class);
			}
		});
		
		delete("rest/vazduhbnb/apartment", (req, res) ->{
			
			User loggedInUser = getLoggedInUser(req);
			if(loggedInUser == null) {
				res.status(401);
				return g.toJson(new ErrorMessageDTO(StringResources.USER_NOT_LOGGED_IN), ErrorMessageDTO.class);
			}
			
			String payload = req.body();
			Apartment apartment = g.fromJson(payload, Apartment.class);
			
			try {				
				resources.apartmentService.delete(apartment, loggedInUser);
				res.status(200);
				return "Apartment deleted";
			}catch(InvalidUserException e) {
				res.status(403);
				return g.toJson(new ErrorMessageDTO(StringResources.ACCESS_DENIED), ErrorMessageDTO.class);
			}catch(BadRequestException e) {
				res.status(400);
				return g.toJson(new ErrorMessageDTO(e.getMessage()), ErrorMessageDTO.class);
			}catch(DatabaseException e) {
				res.status(500);
				return g.toJson(new ErrorMessageDTO(StringResources.INTERNAL_SERVER_ERROR), ErrorMessageDTO.class);
			}
			
		});
		
		get("/rest/vazduhbnb/apartmentComments",(req, res) -> {
			res.type("application/json");
			
			User loggedInUser = getLoggedInUser(req);
			if(loggedInUser == null) {
				loggedInUser = new User();
				loggedInUser.setUserType(UserType.undefined);
			}
			
			long apartmentId = Long.valueOf(req.queryParams("apartment"));
			
			try {
				List<Comment> comments = resources.commentService.getCommentsByApartment(apartmentId, loggedInUser);
				res.status(200);
				return g.toJson(comments);
			}catch(InvalidUserException e) {
				res.status(403);
				return g.toJson(new ErrorMessageDTO(StringResources.ACCESS_DENIED), ErrorMessageDTO.class);
			}catch(DatabaseException e) {
				res.status(500);
				return g.toJson(new ErrorMessageDTO(StringResources.INTERNAL_SERVER_ERROR), ErrorMessageDTO.class);				
			}
			
		});
		
		get("/rest/vazduhbnb/availabledates", (req, res) -> {
			res.type("application/json");
			
			User loggedInUser = getLoggedInUser(req);
			if(loggedInUser == null) {
				res.status(401);
				return g.toJson(new ErrorMessageDTO(StringResources.USER_NOT_LOGGED_IN), ErrorMessageDTO.class);
			}
			
			long apartmentId = Long.valueOf(req.queryParams("apartment"));
			
			try {
				List<Date> dates = resources.reservationService.getAvailableDatesByApartment(apartmentId, loggedInUser);
				res.status(200);
				return g.toJson(dates);
			}catch(InvalidUserException e) {
				res.status(401);
				return g.toJson(new ErrorMessageDTO(StringResources.USER_NOT_LOGGED_IN), ErrorMessageDTO.class);
			}catch(DatabaseException e) {
				res.status(500);
				return g.toJson(new ErrorMessageDTO(StringResources.INTERNAL_SERVER_ERROR), ErrorMessageDTO.class);
			}
		});
		
		get("/rest/vazduhbnb/unavailabledates", (req, res) -> {
			res.type("application/json");
			
			User loggedInUser = getLoggedInUser(req);
			if(loggedInUser == null) {
				res.status(401);
				return g.toJson(new ErrorMessageDTO(StringResources.USER_NOT_LOGGED_IN), ErrorMessageDTO.class);
			}
			
			long apartmentId = Long.valueOf(req.queryParams("apartment"));
			
			try {
				List<Date> dates = resources.reservationService.getUnavailableDatesByApartment(apartmentId, loggedInUser);
				res.status(200);
				return g.toJson(dates);
			}catch(InvalidUserException e) {
				res.status(401);
				return g.toJson(new ErrorMessageDTO(StringResources.USER_NOT_LOGGED_IN), ErrorMessageDTO.class);
			}catch(DatabaseException e) {
				res.status(500);
				return g.toJson(new ErrorMessageDTO(StringResources.INTERNAL_SERVER_ERROR), ErrorMessageDTO.class);
			}
		});
		
		get("/rest/vazduhbnb/bookingdatesinfo", (req, res) -> {
			res.type("application/json");
			
			User loggedInUser = getLoggedInUser(req);
			if(loggedInUser == null) {
				res.status(401);
				return g.toJson(new ErrorMessageDTO(StringResources.USER_NOT_LOGGED_IN), ErrorMessageDTO.class);
			}
			
			long apartmentId = Long.valueOf(req.queryParams("apartment"));
			
			try {
				BookingDatesDTO dates = resources.reservationService.getBookingDatesInfo(apartmentId, loggedInUser);
				res.status(200);
				return g.toJson(dates, BookingDatesDTO.class);
			}catch(InvalidUserException e) {
				res.status(401);
				return g.toJson(new ErrorMessageDTO(StringResources.USER_NOT_LOGGED_IN), ErrorMessageDTO.class);
			}catch(DatabaseException e) {
				res.status(500);
				return g.toJson(new ErrorMessageDTO(StringResources.INTERNAL_SERVER_ERROR), ErrorMessageDTO.class);
			}
		});
		
		get("/rest/vazduhbnb/totalPrice", (req, res) -> {
			res.type("application/json");
			
			User loggedInUser = getLoggedInUser(req);
			if(loggedInUser == null) {
				res.status(401);
				return g.toJson(new ErrorMessageDTO(StringResources.USER_NOT_LOGGED_IN), ErrorMessageDTO.class);
			}
			
			long apartmentId = Long.valueOf(req.queryParams("apartment"));
			DateRange dateRange = g.fromJson(req.queryParams("dates"), DateRange.class);
			
			try {
				double totalPrice = resources.reservationService.getTotalPrice(loggedInUser, apartmentId, dateRange);
				res.status(200);
				return totalPrice;
			}catch(InvalidUserException e) {
				res.status(403);
				return g.toJson(new ErrorMessageDTO(StringResources.ACCESS_DENIED), ErrorMessageDTO.class);
			}catch(InvalidDateException e) {
				res.status(400);
				return g.toJson(new ErrorMessageDTO(e.getMessage()), ErrorMessageDTO.class);
			}catch(DatabaseException e) {
				res.status(500);
				return g.toJson(new ErrorMessageDTO(StringResources.INTERNAL_SERVER_ERROR), ErrorMessageDTO.class);
			}
		});
		
		get("/rest/vazduhbnb/profile", (request, response)->{
			User loggedinUser = getLoggedInUser(request);
			
			if(loggedinUser == null){
				response.status(401);
				return g.toJson(new ErrorMessageDTO(StringResources.USER_NOT_LOGGED_IN), ErrorMessageDTO.class);
			}
			
			try{
				return g.toJson(resources.userService.getById(loggedinUser.getId()), User.class);
			} catch(DatabaseException ex) {
				response.status(500);
				return g.toJson(new ErrorMessageDTO(StringResources.INTERNAL_SERVER_ERROR), ErrorMessageDTO.class);
			}
			
		});
		
		put("/rest/vazduhbnb/updateUser", (request, response)->{
			response.type("application/json");
			String payload = request.body();
			
			UserDTO updatedUser = g.fromJson(payload, UserDTO.class);
			
			try {
				User user = resources.userService.update(updatedUser);
				return g.toJson(resources.userService.getById(user.getId()), User.class);
			}
			catch(DatabaseException ex){
				response.status(500);
				return g.toJson(new ErrorMessageDTO(StringResources.INTERNAL_SERVER_ERROR), ErrorMessageDTO.class );
			}
			catch(InvalidPasswordException ex){
				response.status(409);
				return g.toJson(new ErrorMessageDTO(ex.getMessage()), ErrorMessageDTO.class);
			}
			catch(BadRequestException ex){
				response.status(400);
				return g.toJson(new ErrorMessageDTO(ex.getMessage()), ErrorMessageDTO.class);
			}
		});
		
		put("/rest/vazduhbnb/amenity", (request, response)->{
			response.type("application/json");
			
			User loggedInUser = getLoggedInUser(request);
			
			if(loggedInUser == null){
				response.status(401);
				return g.toJson(new ErrorMessageDTO(StringResources.USER_NOT_LOGGED_IN), ErrorMessageDTO.class);
			}

			String payload = request.body();
			Amenity updatedAmenity = g.fromJson(payload, Amenity.class);

			try{
				resources.amenityService.update(updatedAmenity, loggedInUser);
				return "OK";
			}
			catch(InvalidUserException ex) {
				response.status(403);				
				return g.toJson(new ErrorMessageDTO(StringResources.ACCESS_DENIED), ErrorMessageDTO.class);
			}
			catch(DatabaseException ex){
				response.status(500);
				return g.toJson(new ErrorMessageDTO(StringResources.INTERNAL_SERVER_ERROR), ErrorMessageDTO.class);
			}
		});
		
		delete("/rest/vazduhbnb/amenity", (request, response)->{
			response.type("application/json");
			
			User loggedInUser = getLoggedInUser(request);
			
			if(loggedInUser == null){
				response.status(401);
				return g.toJson(new ErrorMessageDTO(StringResources.USER_NOT_LOGGED_IN), ErrorMessageDTO.class);
			}
			
			String payload = request.body();
			Amenity deletedAmenity = g.fromJson(payload, Amenity.class);
			
			try {
				resources.amenityService.delete(deletedAmenity, loggedInUser);
				response.status(200);
				return "OK";
			}
			catch(InvalidUserException ex) {
				response.status(403);				
				return g.toJson(new ErrorMessageDTO(StringResources.ACCESS_DENIED), ErrorMessageDTO.class);
			}
			catch(DatabaseException ex){
				response.status(500);
				return g.toJson(new ErrorMessageDTO(StringResources.INTERNAL_SERVER_ERROR), ErrorMessageDTO.class);
			}
		});
		
		post("/rest/vazduhbnb/amenity", (request, response)->{
			response.type("application/json");
			
			User loggedInUser = getLoggedInUser(request);
			
			if(loggedInUser == null){
				response.status(401);
				return g.toJson(new ErrorMessageDTO(StringResources.USER_NOT_LOGGED_IN), ErrorMessageDTO.class);
			}
			
			String payload = request.body();
			String amenityName = g.fromJson(g.toJson(payload), String.class);
			
			try {
				Amenity amenity = resources.amenityService.create(amenityName, loggedInUser);
				response.status(200);
				return g.toJson(amenity);
			}
			catch(InvalidUserException ex) {
				response.status(403);				
				return g.toJson(new ErrorMessageDTO(StringResources.ACCESS_DENIED), ErrorMessageDTO.class);				
			}
			catch(DatabaseException ex) {
				response.status(500);
				return g.toJson(new ErrorMessageDTO(StringResources.INTERNAL_SERVER_ERROR), ErrorMessageDTO.class);
			}
		});
		
		get("/rest/vazduhbnb/amenities", (request, response) -> {
			response.type("application/json");
			
			try{
				List<Amenity> amenities = resources.amenityService.getAll();
				return g.toJson(amenities);
			}
			catch(DatabaseException ex){
				response.status(500);
				return g.toJson(new ErrorMessageDTO(StringResources.INTERNAL_SERVER_ERROR), ErrorMessageDTO.class);
			}
		});
		
		get("/rest/vazduhbnb/userType", (request, response)->{
			response.type("application/json");
			
			User loggedinUser = getLoggedInUser(request);
			response.status(200);
			if(loggedinUser != null)
				return g.toJson(loggedinUser.getUserType());
			return g.toJson(UserType.undefined);
		});
		
		get("/rest/vazduhbnb/apartmentsByUsertype", (request, response) -> {
			response.type("application/json");
			
			User loggedinUser = getLoggedInUser(request);
			if(loggedinUser == null){
				response.status(401);
				return g.toJson(StringResources.USER_NOT_LOGGED_IN);
			}
			
			List<Reservation> reservations = new ArrayList<Reservation>();
			List<Apartment> apartments = new ArrayList<Apartment>();
			try {
				if(loggedinUser.getUserType() == UserType.admin){
					reservations = 	resources.reservationService.getAll();
				}
				else if(loggedinUser.getUserType() == UserType.guest){
					reservations = resources.reservationService.getReservationByGuest(loggedinUser);
				}
				else if(loggedinUser.getUserType() == UserType.host){
					reservations = resources.reservationService.getReservationByHost(loggedinUser, loggedinUser.getUserType());
				}
				
				for(Reservation res: reservations){
					if(!apartments.contains(res.getApartment()))
						apartments.add(res.getApartment());
				}
								
				return g.toJson(apartments);
			}catch(DatabaseException ex){
				response.status(500);
				return g.toJson(new ErrorMessageDTO(StringResources.INTERNAL_SERVER_ERROR), ErrorMessageDTO.class);
			}catch(InvalidUserException ex){
				response.status(403);
				return g.toJson(new ErrorMessageDTO(StringResources.ACCESS_DENIED), ErrorMessageDTO.class);
			}			
		});
		
		get("/rest/vazduhbnb/getAllUsers", (request, response) -> {
			response.type("application/json");
			
			User user = getLoggedInUser(request);
			if(user == null) {
				response.status(401);
				return g.toJson(StringResources.USER_NOT_LOGGED_IN);
			}
			
			try {
				List<User> users = resources.userService.getAll(user);
				response.status(200);
				return g.toJson(users);
			}
			catch(InvalidUserException ex) {
				response.status(403);
				return g.toJson(new ErrorMessageDTO(StringResources.ACCESS_DENIED));
			}
			catch(DatabaseException ex) {
				response.status(500);
				return g.toJson(new ErrorMessageDTO(StringResources.INTERNAL_SERVER_ERROR), ErrorMessageDTO.class);
			}
		});
		
		put("/rest/vazduhbnb/reservationsByApartment", (request, response) -> {
			response.type("application/json");
			
			String payload = request.body();
			
			Apartment apartment = g.fromJson(payload, Apartment.class);
			User loggedinUser = getLoggedInUser(request);
			
			try{
				List<Reservation> reservations = resources.reservationService.getReservationsByApartment(apartment.getId(), loggedinUser);
				return g.toJson(reservations);
			}catch(InvalidUserException ex){
				response.status(403);
				return g.toJson(new ErrorMessageDTO(StringResources.ACCESS_DENIED), ErrorMessageDTO.class);
			}catch(DatabaseException ex) {
				response.status(500);
				return g.toJson(new ErrorMessageDTO(StringResources.INTERNAL_SERVER_ERROR), ErrorMessageDTO.class);
			}
		});
		
		put("/rest/vazduhbnb/blockUser", (request, response) ->{
			response.type("application/json");
			
			String payload = request.body();
			
			User loggedInUser = getLoggedInUser(request);
			if(loggedInUser == null) {
				response.status(401);
				return g.toJson(new ErrorMessageDTO(StringResources.USER_NOT_LOGGED_IN), ErrorMessageDTO.class);
			}
			
			User user = g.fromJson(payload, User.class);
			
			if(user == null) {
				response.status(400);
				return g.toJson(new ErrorMessageDTO(StringResources.USER_NOT_FOUND), ErrorMessageDTO.class);
			}
			
			try {
				resources.userService.blockUser(user, loggedInUser);
				response.status(200);
				return "OK";
			}
			catch(InvalidUserException ex) {				
				response.status(403);
				return g.toJson(new ErrorMessageDTO(StringResources.ACCESS_DENIED), ErrorMessageDTO.class);
			}
			catch(DatabaseException ex) {
				response.status(500);
				return g.toJson(new ErrorMessageDTO(StringResources.INTERNAL_SERVER_ERROR), ErrorMessageDTO.class);
			}
		});
		
		put("/rest/vazduhbnb/unblockUser", (request, response) ->{
			response.type("application/json");
			
			String payload = request.body();
			
			User loggedInUser = getLoggedInUser(request);
			if(loggedInUser == null) {
				response.status(401);
				return g.toJson(new ErrorMessageDTO(StringResources.USER_NOT_LOGGED_IN), ErrorMessageDTO.class);
			}
			
			User user = g.fromJson(payload, User.class);
			
			if(user == null) {
				response.status(400);
				return g.toJson(new ErrorMessageDTO(StringResources.USER_NOT_FOUND), ErrorMessageDTO.class);
			}
			
			try {
				resources.userService.unblockUser(user, loggedInUser);
				response.status(200);
				return "OK";
			}
			catch(InvalidUserException ex) {				
				response.status(403);
				return g.toJson(new ErrorMessageDTO(StringResources.ACCESS_DENIED), ErrorMessageDTO.class);
			}
			catch(DatabaseException ex) {
				response.status(500);
				return g.toJson(new ErrorMessageDTO(StringResources.INTERNAL_SERVER_ERROR), ErrorMessageDTO.class);
			}
		});
		
		get("/rest/vazduhbnb/pricingCalendar", (req, res) -> {
			res.type("application/json");
			
			User loggedInUser = getLoggedInUser(req);
			if(loggedInUser == null) {
				res.status(401);
				return g.toJson(new ErrorMessageDTO(StringResources.USER_NOT_LOGGED_IN), ErrorMessageDTO.class);
			}
			
			try {				
				PricingCalendar cal = resources.reservationService.getPricingCalendar(loggedInUser);
				res.status(200);
				return g.toJson(cal, PricingCalendar.class);
			}catch(InvalidUserException e) {
				res.status(403);
				return g.toJson(new ErrorMessageDTO(StringResources.ACCESS_DENIED), ErrorMessageDTO.class);
			}catch(DatabaseException e) {				
				res.status(500);
				return g.toJson(new ErrorMessageDTO(StringResources.INTERNAL_SERVER_ERROR), ErrorMessageDTO.class);
			}
		});
		
		put("/rest/vazduhbnb/pricingCalendar", (req, res) -> {
			res.type("application/json");
			
			User loggedInUser = getLoggedInUser(req);
			if(loggedInUser == null) {
				res.status(401);
				return g.toJson(new ErrorMessageDTO(StringResources.USER_NOT_LOGGED_IN), ErrorMessageDTO.class);
			}
			
			PricingCalendar pricingCalendar = g.fromJson(req.body(), PricingCalendar.class);
			
			try {
				resources.reservationService.updatePricingCalendar(pricingCalendar, loggedInUser);
				res.status(200);
				return "OK";
			}catch(InvalidUserException e) {
				res.status(403);
				return g.toJson(new ErrorMessageDTO(StringResources.ACCESS_DENIED), ErrorMessageDTO.class);
			}catch(DatabaseException e) {				
				res.status(500);
				return g.toJson(new ErrorMessageDTO(StringResources.INTERNAL_SERVER_ERROR), ErrorMessageDTO.class);
			}
		});
		
		get("/rest/vazduhbnb/getLoggedInUser", (request, response)->{
			response.type("application/json");
			User user = getLoggedInUser(request);
			return g.toJson(user);
		});
		
		get("/*", (request, response) -> {
			response.status(404);
			response.redirect("/404.html");
			return "";
		});
		
	}

	private static Gson getGson() {
		GsonBuilder builder = new GsonBuilder(); 

		// Register an adapter to manage the date types as long values 
		builder.registerTypeAdapter(Date.class, new JsonDeserializer<Date>() { 
		   public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		      return new Date(json.getAsJsonPrimitive().getAsLong()); 
		   }
		});
		
		builder.registerTypeAdapter(Date.class, new JsonSerializer<Date>() {

			@Override
			public JsonElement serialize(Date src, Type type, JsonSerializationContext context) {
				return src == null ? null : new JsonPrimitive(src.getTime());
			}
			
		});
		
		return builder.create();
	}
	
	private static User getLoggedInUser(Request request) {
		String auth = request.headers("Authorization");
		if ((auth != null) && (auth.contains("Bearer "))) {
			String jwt = auth.substring(auth.indexOf("Bearer ") + 7);
			try {
			    Jws<Claims> claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(jwt);
				return userConverter.fromCsv(claims.getBody().getSubject());
			} catch (Exception e) {
				return null;
			}
		}
		return null;
	}
	
	private static String getJwtToken(User user) {
		JwtBuilder jwtBuilder = Jwts.builder();
		jwtBuilder.setSubject(userConverter.toCsv(user));
		
		jwtBuilder.setExpiration(new Date(new Date().getTime() + 1000*minutesUntilTokenExpires*60L));
		jwtBuilder.setIssuedAt(new Date());
		
		return jwtBuilder.signWith(key).compact();
	}
}
