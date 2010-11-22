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
@RDFType(Constants.DKMS_CORE + "Wiki")
public interface DkmsWikiFacade extends ContentItemI{
	
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
	 * the version
	 * @return the version
	 */
	@RDF(Constants.DKMS_CORE + "version")	
	public String getVersion() ;
	/**
	 * @param mimeType the version to set
	 */
	public void setVersion(String version) ;
	/**
	 * the wikiAuthor
	 * @return the wikiAuthor
	 */
	@RDF(Constants.DKMS_CORE + "wikiAuthor")	
	public String getWikiAuthor() ;
	/**
	 * @param wikiAuthor to set
	 */
	public void setWikiAuthor(String wikiAuthor) ;
	/**
	 * the wikiContent
	 * @return the wikiContent
	 */
	@RDF(Constants.DKMS_CORE + "wikiContent")	
	public String getWikiContent() ;
	/**
	 * @param wikiContent to set
	 */
	public void setWikiContent(String wikiContent) ;
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
		
}
