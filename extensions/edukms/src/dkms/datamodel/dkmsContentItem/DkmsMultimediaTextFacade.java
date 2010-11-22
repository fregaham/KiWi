/*
	Copyright © dkms, artaround
 */

package dkms.datamodel.dkmsContentItem;

import kiwi.model.content.ContentItemI;
import java.util.Locale;

import dkms.datamodel.dkmsContentItem.DkmsMultimediaFacade;

import kiwi.model.Constants;
import kiwi.model.annotations.KiWiFacade;
import kiwi.model.annotations.RDF;
import kiwi.model.annotations.RDFType;


@KiWiFacade
@RDFType(Constants.DKMS_CORE + "MultimediaText")
public interface DkmsMultimediaTextFacade extends ContentItemI{

	/**
	 * @return the description
	 */
	@RDF(Constants.DKMS_CORE + "description")	
	public String getDescription() ;
	/**
	 * @param description the description to set
	 */
	public void setDescription(String description);
	/**
	 * @return the language
	 */
	@RDF(Constants.DKMS_CORE + "language")	
	public Locale getLanguage();
	/**
	 * @param language the language to set
	 */
	public void setLanguage(Locale language) ;
	/**
	 * @return the mediaContent
	 */
	@RDF(Constants.DKMS_CORE + "multimedia")	
	public DkmsMultimediaFacade getMultimedia() ;
	/**
	 * @param mediaContent the mediaContent to set
	 */
	public void setMultimedia(DkmsMultimediaFacade mediaContent) ;

}
