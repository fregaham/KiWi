/*
	Copyright © dkms, artaround
 */


package dkms.datamodel.dkmsContentItem;

import java.util.Locale;

import dkms.datamodel.dkmsContentItem.DkmsContentItemFacade;

import kiwi.model.Constants;
import kiwi.model.annotations.KiWiFacade;
import kiwi.model.annotations.RDF;
import kiwi.model.annotations.RDFType;
import kiwi.model.content.ContentItemI;


@KiWiFacade
@RDFType(Constants.DKMS_CORE + "DkmsContentItemText")
public interface DkmsContentItemTextFacade extends ContentItemI{
	
	/**
	 * the name of the DkmsContentItem
	 * @return the name
	 */
	@RDF(Constants.DKMS_CORE + "name")	
	public String getName();
	/**
	 * @param name the name to set
	 */
	public void setName(String name);

	/**
	 * @return the DkmsContentItem
	 */
	@RDF(Constants.DKMS_CORE + "dkmsContentItem")	
	public DkmsContentItemFacade getDkmsContentItem();
	/**
	 * @param dkmsContentItem the dkmsContentItem to set
	 */
	public void setDkmsContentItem(DkmsContentItemFacade dkmsContentItem);
	/**
	 * @return the language
	 */
	@RDF(Constants.DKMS_CORE + "language")	
	public Locale getLanguage() ;
	/**
	 * @param language the language to set
	 */
	public void setLanguage(Locale language) ;
	/**
	 * the description of the dkmsContentItem
	 * @return the description
	 */
	@RDF(Constants.DKMS_CORE + "description")	
	public String getDescription();
	/**
	 * @param description the description to set
	 */
	public void setDescription(String description);

}
