/*
	Copyright © dkms, artaround
 */

package dkms.datamodel.dkmsContentItem;

import java.util.LinkedList;

import dkms.datamodel.dkmsContentItem.DkmsContentItemFacade;
import dkms.datamodel.dkmsContentItem.DkmsMultimediaTextFacade;

import kiwi.model.Constants;
import kiwi.model.annotations.KiWiFacade;
import kiwi.model.annotations.RDF;
import kiwi.model.annotations.RDFType;
import kiwi.model.content.ContentItemI;


@KiWiFacade
@RDFType(Constants.DKMS_CORE + "Movie")
public interface DkmsMovieFacade extends ContentItemI{
	
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
	 * the mime type of the media
	 * @return the mimeType
	 */
	@RDF(Constants.DKMS_CORE + "mimeType")	
	public String getMimeType() ;
	/**
	 * @param mimeType the mimeType to set
	 */
	public void setMimeType(String mimeType) ;
	/**
	 * the filename of the media
	 * @return the filename
	 */
	@RDF(Constants.DKMS_CORE + "filename")	
	public String getFilename() ;
	/**
	 * @param filename the filename to set
	 */
	public void setFilename(String filename) ;
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
	 * the width of the picture or video
	 * @return the width
	 */
	@RDF(Constants.DKMS_CORE + "width")	
	public Integer getWidth();
	/**
	 * @param width the width to set
	 */
	public void setWidth(Integer width) ;
	/**
	 * the height of the picture or video
	 * @return the height
	 */
	@RDF(Constants.DKMS_CORE + "height")	
	public Integer getHeight();
	/**
	 * @param height the height to set
	 */
	public void setHeight(Integer height) ;
}
