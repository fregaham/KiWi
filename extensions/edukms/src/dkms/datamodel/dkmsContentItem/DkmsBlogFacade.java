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
@RDFType(Constants.DKMS_CORE + "Blog")
public interface DkmsBlogFacade extends ContentItemI{
	
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
	 * the commentAuthor
	 * @return the commentAuthor
	 */
	@RDF(Constants.DKMS_CORE + "commentAuthor")	
	public String getCommentAuthor() ;
	/**
	 * @param commentAuthor to set
	 */
	public void setCommentAuthor(String commentAuthor) ;
	/**
	 * the commentContent
	 * @return the commentContent
	 */
	@RDF(Constants.DKMS_CORE + "commentContent")	
	public String getCommentContent() ;
	/**
	 * @param commentContent to set
	 */
	public void setCommentContent(String commentContent) ;
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
