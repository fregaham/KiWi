/*
	Copyright © dkms, artaround
 */

package dkms.datamodel.timeTable;

import java.util.Date;

import kiwi.model.Constants;
import kiwi.model.annotations.KiWiFacade;
import kiwi.model.annotations.RDF;
import kiwi.model.annotations.RDFType;
import kiwi.model.content.ContentItemI;


@KiWiFacade
@RDFType(Constants.DKMS_CORE + "DkmsContentItem")
public interface DkmsEventFacade extends ContentItemI{	
	
	/**
	 * @return the street
	 */
	@RDF(Constants.DKMS_CORE + "street")	
	public String getStreet();
	/**
	 * @param street the street to set
	 */
	public void setStreet(String street) ;
	/**
	 * @return the postcode
	 */
	@RDF(Constants.DKMS_CORE + "postcode")	
	public String getPostcode();
	/**
	 * @param postcode the postcode to set
	 */
	public void setPostcode(String postcode);
	/**
	 * @return the city
	 */
	@RDF(Constants.DKMS_CORE + "city")	
	public String getCity();
	/**
	 * @param city the city to set
	 */
	public void setCity(String city);
	/**
	 * @return the isoCountryCode3
	 */
	@RDF(Constants.DKMS_CORE + "countryCode")	
	public String getIsoCountryCode3();
	/**
	 * @param isoCountryCode3 the isoCountryCode3 to set
	 */
	public void setIsoCountryCode3(String isoCountryCode3);
	/**
	 * @return the name
	 */
	@RDF(Constants.DKMS_CORE + "name")		
	public String getName();
	/**
	 * @param name the name to set
	 */
	public void setName(String name) ;
	/**
	 * @return the datetime
	 */
	@RDF(Constants.DKMS_CORE + "datetime")		
	public Date getDatetime();
	/**
	 * @param datetime the datetime to set
	 */
	public void setDatetime(Date datetime);

}
