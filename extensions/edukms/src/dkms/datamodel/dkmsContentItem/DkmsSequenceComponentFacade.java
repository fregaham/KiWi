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
@RDFType(Constants.DKMS_CORE + "SequenceComponent")
public interface DkmsSequenceComponentFacade extends ContentItemI{
	
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
	 * @return the sequenceContent
	 */
	@RDF(Constants.DKMS_CORE + "sequenceItem")	
	public DkmsContentItemFacade getSequenceItem();
	/**
	 * @param dkmsContentItem the sequenceItem to set
	 */
	public void setSequenceItem(DkmsContentItemFacade dkmsSequenceItem) ;
	/**
	 * the taskTitle
	 * @return the taskTitle
	 */
	@RDF(Constants.DKMS_CORE + "taskTitle")	
	public String getTaskTitle() ;
	/**
	 * @param  the taskTitle to set
	 */
	public void setTaskTitle(String taskTitle) ;
	/**
	 * the version
	 * @return the version
	 */
	@RDF(Constants.DKMS_CORE + "version")	
	public String getVersion() ;
	/**
	 * @param  the version to set
	 */
	public void setVersion(String version) ;
	/**
	 * the taskId
	 * @return the taskId
	 */
	@RDF(Constants.DKMS_CORE + "taskId")	
	public int getTaskId() ;
	/**
	 * @param  the taskId to set
	 */
	public void setTaskId(int taskId) ;
	/**
	 * the sequenceContent
	 * @return the sequenceContent
	 */
	@RDF(Constants.DKMS_CORE + "sequenceContent")	
	public String getSequenceContent() ;
	/**
	 * @param sequenceContent to set
	 */
	public void setSequenceContent(String sequenceContent) ;
	/**
	 * the viewStatus
	 * @return the viewStatus
	 */
	@RDF(Constants.DKMS_CORE + "viewStatus")	
	public int getViewStatus() ;
	/**
	 * @param  the taskId to set
	 */
	public void setViewStatus(int viewStatus) ;
			
}
