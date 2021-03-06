/***********************************************************************
 * Module:  ReservationService.java
 * Author:  Geri
 * Purpose: Defines the Class ReservationService
 ***********************************************************************/

package service;

import repository.ReservationRepository;
import repository.abstractrepository.IApartmentRepository;
import repository.abstractrepository.IDateCollectionRepository;
import repository.abstractrepository.IPricingCalendarRepository;
import repository.abstractrepository.IUserRepository;
import beans.User;
import beans.enums.DayOfWeek;
import beans.enums.ReservationStatus;
import beans.enums.UserType;
import dto.BookingDatesDTO;
import dto.ReservationDTO;
import exceptions.BadRequestException;
import exceptions.DatabaseException;
import exceptions.InvalidUserException;
import beans.Apartment;
import beans.Comment;
import beans.DateCollection;
import beans.DateRange;
import beans.PricingCalendar;
import beans.Reservation;
import java.util.*;
import java.util.stream.Collectors;

public class ReservationService {
   private ReservationRepository reservationRepository;
   private IDateCollectionRepository dateCollectionRepository;
   private IPricingCalendarRepository pricingCalendarRepository;
   private IApartmentRepository apartmentRepository;
   private IUserRepository userRepository;
   
   //Constructors 
   public ReservationService(ReservationRepository reservationRepository, IDateCollectionRepository dateCollectionRepository, IPricingCalendarRepository pricingCalendarRepository, IApartmentRepository apartmentRepository, IUserRepository userRepository) {
	   super();
	   this.reservationRepository = reservationRepository;
	   this.dateCollectionRepository = dateCollectionRepository;
	   this.pricingCalendarRepository = pricingCalendarRepository;
	   this.apartmentRepository = apartmentRepository;
	   this.userRepository = userRepository;
   }

//Methods
   
   /** Returns a list of Reservations by guest<br><br>
    *  
    *  <b>Called by:</b> guest, admin or host<br>
    *  
    * @throws DatabaseException 
    * @throws InvalidUserException
    */
   public List<Reservation> getReservationByGuest(User guest) throws DatabaseException, InvalidUserException {
      if(guest.getUserType() != UserType.undefined)
      {
    	  List<Reservation> allReservations = reservationRepository.getAllEager();
    	  List<Reservation> retVal = new ArrayList<Reservation>();
    	  
    	  for(Reservation reservation: allReservations)
    	  {
    		  if(reservation.getGuest().getId() == guest.getId())
    		  {
    			  retVal.add(reservation);
    		  }
    	  }
    	  
    	  return retVal;
      }
      throw new InvalidUserException();
   }
   
   /** Changes status <b>reservation status</b> of reservation <br><br>
    *  
    *  <b>Called by:</b> host or guest<br>
    *  
    * @throws DatabaseException 
 * @throws BadRequestException 
    */
   
   public void cancelReservation(long reservationId, User user) throws DatabaseException, BadRequestException {
	   if(user.getUserType() == UserType.guest) {		   
		   Reservation reservation = reservationRepository.getEager(reservationId);
		   
		   if(reservation.getReservationStatus() == ReservationStatus.accepted || reservation.getReservationStatus() == ReservationStatus.created) {
			   
			   reservation.setReservationStatus(ReservationStatus.cancelled);
			   
			   DateCollection dc = dateCollectionRepository.getByApartmentId(reservation.getApartment().getId());
			   dc.removeBooking(reservation.getDateRange());
			   
			   dateCollectionRepository.update(dc);
			   reservationRepository.update(reservation);
		   }
		   else {
			   throw new BadRequestException("Reservation status was previously not 'created' or 'accepted'");
		   }
	   }
	   else {		   
		   throw new InvalidUserException();
	   }
   }
   
   
   /** Returns a list of reservation by <b>host</b> <br><br>
    *  
    *  <b>Called by:</b> host or admin<br>
    *  
    * @throws DatabaseException 
    * @throws InvalidUserException
    */
   public List<Reservation> getReservationByHost(User host, UserType userType) throws InvalidUserException, DatabaseException {
	   if(userType == UserType.admin || userType == UserType.host)
	   {		  
		   List<Reservation> reservations = reservationRepository.getAllEager();
		   List<Reservation> retVal = new ArrayList<Reservation>();
		   
		   for(Reservation reservation: reservations)
		   {
			   if(reservation.getApartment().getHost().getId() == host.getId())
			   {
				   retVal.add(reservation);
			   }
		   }
		   return retVal;
	   }
	   throw new InvalidUserException();
   }
   
   
   /** Changes status <b>reservation status</b> to <b>accepted</b> <br><br>
    *  
    *  <b>Called by:</b> host<br>
    *  
    * @throws DatabaseException 
 * @throws BadRequestException 
    */
   public void acceptReservation(long reservationId, User user) throws DatabaseException, BadRequestException {
	   if(user.getUserType() == UserType.host) {		   
		   Reservation reservation = reservationRepository.getEager(reservationId);
		   
		   if(reservation.getReservationStatus() == ReservationStatus.created) {
			   
			   reservation.setReservationStatus(ReservationStatus.accepted);
			   reservationRepository.update(reservation);
		   }
		   else {
			   throw new BadRequestException("Reservation status was previously not 'created'");
		   }
	   }
	   else {		   
		   throw new InvalidUserException();
	   }
   }
   
   
   /** Changes status <b>reservation status</b> to <b>rejected</b> <br><br>
    *  
    *  <b>Called by:</b> host<br>
    *  
    * @throws DatabaseException 
    * @throws InvalidUserException
 * @throws BadRequestException 
    */
   public void rejectReservation(long reservationId, User user) throws DatabaseException, BadRequestException {
	   if(user.getUserType() == UserType.host) {		   
		   Reservation reservation = reservationRepository.getEager(reservationId);
		   
		   if(reservation.getReservationStatus() == ReservationStatus.accepted || reservation.getReservationStatus() == ReservationStatus.created) {
			   
			   reservation.setReservationStatus(ReservationStatus.rejected);
			   
			   DateCollection dc = dateCollectionRepository.getByApartmentId(reservation.getApartment().getId());
			   dc.removeBooking(reservation.getDateRange());
			   
			   dateCollectionRepository.update(dc);
			   reservationRepository.update(reservation);
		   }
		   else {
			   throw new BadRequestException("Reservation status was previously not 'created' or 'accepted'");
		   }
	   }
	   else {		   
		   throw new InvalidUserException();
	   }
	   
   }
   
   /** Changes status <b>reservation status</b> to <b>finished</b> <br><br>
    *  
    *  <b>Called by:</b> host<br>
    *  
    * @throws DatabaseException 
 * @throws BadRequestException 
    */
   public void finishReservation(long reservationId, User user) throws DatabaseException, BadRequestException {
	   if(user.getUserType() == UserType.host) {		   
		   Reservation reservation = reservationRepository.getEager(reservationId);
		   
		   if(reservation.getReservationStatus() == ReservationStatus.created || reservation.getReservationStatus() == ReservationStatus.accepted) {
			   
			   if(reservation.getDateRange().getEnd().before(new Date())) {
				   reservation.setReservationStatus(ReservationStatus.finished);
				   reservationRepository.update(reservation);				   
			   }
			   else {
				   throw new BadRequestException("Reservation is not completed.");
			   }
		   }
		   else {
			   throw new BadRequestException("Reservation status was previously not 'created' or 'accepted'");
		   }
	   }
	   else {		   
		   throw new InvalidUserException();
	   }
   }
   
   public List<Reservation> getAll() throws DatabaseException {
      return reservationRepository.getAllEager();
   }
   
   /** Creates a new <b>reservation</b> <br><br>
    *  
    *  <b>Called by:</b> guest<br>
    *  
    * @throws DatabaseException 
    * @throws InvalidUserException
    * @throws BadRequestException 
    */
   public void create(ReservationDTO reservation, User user) throws InvalidUserException, DatabaseException, BadRequestException {
      if(user.getUserType() == UserType.guest) {
    	  DateCollection dc = dateCollectionRepository.getByApartmentId(reservation.getApartmentId());
    	  dc.addBooking(reservation.getDateRange());
    	  dateCollectionRepository.update(dc);
    	  
    	  Apartment apartment = apartmentRepository.getById(reservation.getApartmentId());
    	  
    	  PricingCalendar calendar = pricingCalendarRepository.getAll().get(0);
    	  double totalPrice = calendar.getTotalPrice(reservation.getDateRange(), apartment.getPricePerNight());
    	  
    	  User guest = userRepository.getById(user.getId());
    	  
    	  Reservation res = new Reservation(apartment, guest, reservation.getDateRange().getStart(), reservation.getNights(), totalPrice, reservation.getMessage(), false, ReservationStatus.created, new Comment(0));
    	  reservationRepository.create(res);
      }
      else {    	  
    	  throw new InvalidUserException();
      }
   }
   
   public List<Date> getAvailableDatesByApartment(long apartmentId, User user) throws InvalidUserException, DatabaseException{
	   if(user.getUserType() != UserType.undefined) {
		   DateCollection dc = dateCollectionRepository.getByApartmentId(apartmentId);
		   if(user.getUserType() == UserType.guest)
			   return dc.getAvailableBookingDates();
		   else
			   return dc.getAvailableDates();
	   }
	   else {
		   throw new InvalidUserException();
	   }

   }
   
   public List<Date> getUnavailableDatesByApartment(long apartmentId, User user) throws InvalidUserException, DatabaseException{
	   if(user.getUserType() != UserType.undefined) {
		   DateCollection dc = dateCollectionRepository.getByApartmentId(apartmentId);
		   if(user.getUserType() == UserType.guest)
			   return dc.getUnavailableBookingDates();
		   else
			   return dc.getUnavailableDates();
	   }
	   else {
		   throw new InvalidUserException();
	   }

   }

   public PricingCalendar getPricingCalendar(User user) throws InvalidUserException, DatabaseException {
	   if(user.getUserType() == UserType.admin) {
		   List<PricingCalendar> list = pricingCalendarRepository.getAll();
		   return list.get(0);
	   }
	   else {
		   throw new InvalidUserException();
	   }
   }

   public void updatePricingCalendar(PricingCalendar pricingCalendar, User user) throws InvalidUserException, DatabaseException {
	   if(user.getUserType() == UserType.admin) {
		   pricingCalendarRepository.update(pricingCalendar);
	   }
	   else
		   throw new InvalidUserException();
   }
   
   public List<Reservation> getReservationsByApartment(long apartmentId, User user) throws InvalidUserException, DatabaseException
   {
	   if(user.getUserType() != UserType.undefined || user != null)
	   {
		   List<Reservation> allReservations = reservationRepository.getAllEager();
		   List<Reservation> retVal = new ArrayList<Reservation>();
		   if(user.getUserType() == UserType.admin || user.getUserType() == UserType.host)
		   {
			   for(Reservation res: allReservations)
			   {
				   if(res.getApartment().getId() == apartmentId)
					   retVal.add(res);
			   }
		   }
		   else if(user.getUserType() == UserType.guest)
		   {
			   for(Reservation res: allReservations)
			   {
				   if(res.getApartment().getId() == apartmentId && res.getGuest().getId() == user.getId())
					   retVal.add(res);
			   }
		   }
		   
		   for(Reservation res: retVal)
		   {
			   res.getGuest().getAccount().setPassword("");
		   }
		   
		   return retVal;
	   }
	   else
		   throw new InvalidUserException();
   }

   public BookingDatesDTO getBookingDatesInfo(long apartmentId, User user) throws InvalidUserException, DatabaseException {
	   if(user.getUserType() != UserType.undefined) {
		   DateCollection dc = dateCollectionRepository.getByApartmentId(apartmentId);
		   return new BookingDatesDTO(dc.getCheckInDays(), dc.getCheckOutDays(), dc.getCheckInOutDays());
	   }
	   else {
		   throw new InvalidUserException();
	   }
   }

	public double getTotalPrice(User user, long apartmentId, DateRange dateRange) throws BadRequestException, DatabaseException {
		if(user.getUserType() == UserType.guest) {
			DateCollection dc = dateCollectionRepository.getByApartmentId(apartmentId);
			
			dc.addBooking(dateRange);
			
			PricingCalendar cal = pricingCalendarRepository.getAll().get(0);
			Apartment a = apartmentRepository.getById(apartmentId);
			return cal.getTotalPrice(dateRange, a.getPricePerNight());
		}
		else {
			throw new InvalidUserException();
		}
	}

	public List<Reservation> getReservations(User user) throws DatabaseException, InvalidUserException {
		switch(user.getUserType()) {
		case admin: {
			return reservationRepository.getAllEager();
		}
		case host:{
			return reservationRepository.getAllEager().stream().filter(res -> res.getApartment().getHost().equals(user)).collect(Collectors.toList());
		}
		case guest:{
			return reservationRepository.getAllEager().stream().filter(res -> res.getGuest().equals(user)).collect(Collectors.toList());
		}
		default:
			throw new InvalidUserException();
		}
	}

}