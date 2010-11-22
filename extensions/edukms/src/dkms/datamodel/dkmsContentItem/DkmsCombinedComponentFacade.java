/*
	Copyright © dkms, artaround
 */

package dkms.datamodel.dkmsContentItem;



import dkms.datamodel.dkmsContentItem.DkmsContentItemFacade;
import kiwi.model.Constants;
import kiwi.model.annotations.KiWiFacade;
import kiwi.model.annotations.RDF;
import kiwi.model.annotations.RDFType;
import kiwi.model.content.ContentItemI;


@KiWiFacade
@RDFType(Constants.DKMS_CORE + "CombinedComponent")
public interface DkmsCombinedComponentFacade extends ContentItemI{
	
	@RDF(Constants.DKMS_CORE + "isMaster")
	public boolean isMaster();
	
	
	
	/**
	 * @return the dkmsContentItem
	 */
	@RDF(Constants.DKMS_CORE + "dkmsContentItem")	
	public DkmsContentItemFacade getdkmsContentItem();
	/**
	 * @param dkmsContentItem the dkmsContentItem to set
	 */
	public void setDkmsContentItem(DkmsContentItemFacade dkmsContentItem) ;
	/**
	 * @return the combinedContent
	 */
	@RDF(Constants.DKMS_CORE + "combinedItem")	
	public DkmsContentItemFacade getCombinedItem();
	/**
	 * @param dkmsContentItem the combinedItem to set
	 */
	public void setCombinedItem(DkmsContentItemFacade dkmsCombinedItem) ;
	/**
	 * the combinedItemTitle
	 * @return the combinedItemTitle
	 */
	@RDF(Constants.DKMS_CORE + "combinedItemTitle")	
	public String getCombinedItemTitle() ;
	/**
	 * @param mimeType the combinedItemTitle to set
	 */
	public void setCombinedItemTitle(String combinedItemTitle) ;
	/**
	 * the combinedItemType
	 * @return the combinedItemType
	 */
	@RDF(Constants.DKMS_CORE + "combinedItemType")	
	public String getCombinedItemType() ;
	/**
	 * @param mimeType the combinedItemType to set
	 */
	public void setCombinedItemType(String combinedItemType) ;
	/**
	 * set to true if the media content has public access
	 * @return the publicAccess
	 */
	@RDF(Constants.DKMS_CORE + "isPublicAccess")	
	public Boolean getPublicAccess() ;
	/**
	 * @param publicAccess the publicAccess to set
	 */
	public void setPublicAccess(Boolean publicAccess) ;
	/**
	 * the combinedItemId
	 * @return the combinedItemId
	 */
	@RDF(Constants.DKMS_CORE + "combinedItemId")	
	public String getCombinedItemId() ;
	/**
	 * @param combinedItemId to set
	 */
	public void setCombinedItemId(String combinedItemId) ;	
}
