/***********************************************************************
 * Module:  AvailableDateCollectionRepository.java
 * Author:  Geri
 * Purpose: Defines the Class AvailableDateCollectionRepository
 ***********************************************************************/

package repository;

import java.util.List;
import java.util.NoSuchElementException;

import beans.Apartment;
import beans.DateCollection;
import exceptions.DatabaseException;
import exceptions.EntityNotFoundException;
import repository.abstractrepository.IApartmentRepository;
import repository.abstractrepository.IDateCollectionRepository;
import repository.csv.CSVRepository;
import repository.csv.IEagerCsvRepository;
import repository.csv.stream.ICsvStream;
import repository.sequencer.LongSequencer;

public class DateCollectionRepository extends CSVRepository<DateCollection> implements IDateCollectionRepository, IEagerCsvRepository<DateCollection> {
   
	private IApartmentRepository apartmentRepository;
	
	public DateCollectionRepository(ICsvStream<DateCollection> stream, LongSequencer sequencer, IApartmentRepository apartmentRepository) throws DatabaseException {
		super("Date Collection", stream, sequencer);
		this.apartmentRepository = apartmentRepository;
	}
	
	private void bind(List<DateCollection> dateCollections) throws DatabaseException
	{
		for(DateCollection dateCollection: dateCollections)
		{
			dateCollection.setApartment(getApartmentById(dateCollection.getApartment()));
		}
	}
	
	public DateCollection getEager(long id) throws DatabaseException {
		DateCollection dateCollection = getById(id);
		
		dateCollection.setApartment(getApartmentById(dateCollection.getApartment()));
		
		return dateCollection;
	}

	public List<DateCollection> getAllEager() throws DatabaseException {
		List<DateCollection> dateCollections = getAll();
		
		bind(dateCollections);
		
		return dateCollections;
	}

	private Apartment getApartmentById(Apartment apartment) throws DatabaseException {
		return apartment == null? null : apartmentRepository.getById(apartment.getId());
	}
	
	
	@Override
	public void deleteByApartment(long apartmentId) throws DatabaseException {
		DateCollection dc = getByApartmentId(apartmentId);
		
		dc.delete();
		update(dc);
	}

	@Override
	public DateCollection getByApartmentId(long apartmentId) throws DatabaseException {
		try {
			return getAll().stream().filter(dc -> dc.getApartment().getId() == apartmentId).findFirst().get();			
		}catch(NoSuchElementException e) {
			throw new EntityNotFoundException(String.format(this.notFoundError, this.entityName, "apartmentId", apartmentId));
		}
	}
}