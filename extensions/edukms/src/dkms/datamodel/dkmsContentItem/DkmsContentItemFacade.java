/*
	Copyright Â© dkms, artaround
 */

package dkms.datamodel.dkmsContentItem;

import java.util.Date;
import java.util.LinkedList;

import org.hibernate.validator.Range;

import dkms.datamodel.dkmsContentItem.DkmsContentItemTextFacade;
import dkms.datamodel.dkmsContentItem.DkmsMultimediaFacade;

import kiwi.model.Constants;
import kiwi.model.annotations.KiWiFacade;
import kiwi.model.annotations.RDF;
import kiwi.model.annotations.RDFType;
import kiwi.model.content.ContentItemI;
import kiwi.model.ontology.SKOSConcept;



@KiWiFacade
@RDFType(Constants.DKMS_CORE + "DkmsContentItem")
public interface DkmsContentItemFacade extends ContentItemI{
	
	@RDF(Constants.DKMS_CORE + "authorName")
	public String getAuthorName();
	
	public String setAuthorName(String authorName);
	
	@RDF(Constants.DKMS_CORE + "bigIdeas")
	public LinkedList<SKOSConcept> getBigIdeas();
	
	public String setBigIdeas(LinkedList<SKOSConcept> bigIdeas);
	
	@RDF(Constants.DKMS_CORE + "representationType")
	public LinkedList<SKOSConcept> getRepresentationType();
	
	public String setRepresentationType(LinkedList<SKOSConcept> representationType);
	
	@RDF(Constants.DKMS_CORE + "isPublic")
	public boolean getPublicAccess();

	public void setPublicAccess(boolean publicAccess);
	
	@RDF(Constants.DKMS_CORE + "description")
	public String getDescription();
	
	public String setDescription(String description);
	
	@RDF(Constants.DKMS_CORE + "sellingState")
	public boolean getSellingState();
	
	public String setSellingState(boolean sellingState);
	
	@RDF(Constants.DKMS_CORE + "location")
	public String getLocation();
	
	public String setLocation(String location);
	
	@RDF(Constants.DKMS_CORE + "taskContent")
	public String getTaskContent();
	
	public String setTaskContent(String taskContent);
	
	@RDF(Constants.DKMS_CORE + "taskTitle")
	public String getTaskTitle();
	
	public String setTaskTitle(String taskTitle);
	
	@RDF(Constants.DKMS_CORE + "exerciseSheetContent")
	public String getExerciseSheetContent();
	
	public String setExerciseSheetContent(String exerciseSheetContent);	
	
	@RDF(Constants.DKMS_CORE + "activeTask")
	public String getActiveTask();
	
	public String setActiveTask(String activeTask);
	
	@RDF(Constants.DKMS_CORE + "tmpContentItem")
	public String getTmpContentItem();
	
	public String setTmpContentItem(String tmpContentItem);
	
	@RDF(Constants.DKMS_CORE + "contentItemStateBuffer")
	public String getContentItemStateBuffer();
	
	public String setContentItemStateBuffer(String contentItemStateBuffer);
	
	@RDF(Constants.DKMS_CORE + "contentItemBuffer")
	public DkmsContentItemFacade getContentItemBuffer();
	
	public DkmsContentItemFacade setContentItemBuffer(DkmsContentItemFacade contentItemBuffer);
	
	
	@RDF(Constants.DKMS_CORE + "contentItemIdentifier")
	public String getContentItemIdentifier();
	
	public String setContentItemIdentifier(String contentItemIdentifier);
	
	@RDF(Constants.DKMS_CORE + "seqTaskState")
	public String getSeqTaskState();
	
	public String setSeqTaskState(String seqTaskState);
	
	@RDF(Constants.DKMS_CORE + "contentItemState")
	public String getContentItemState();
	
	public String setContentItemState(String contentItemState);
	
	@RDF(Constants.DKMS_CORE + "commentText")
	public String getCommentText();
	
	public String setCommentText(String commentText);
	
	@RDF(Constants.DKMS_CORE + "dkmsContentItemType")
	public String getDkmsContentItemType();
	
	public String setDkmsContentItemType(String dkmsContentItemType);
	
	@RDF(Constants.DKMS_CORE + "lastUpdate")
	public String getLastUpdate();
	
	public String setLastUpdate(String lastUpdate);
	
	@RDF(Constants.DKMS_CORE + "customerIsMember")
	public boolean getCustomerIsMember();
	
	public void setCustomerIsMember(boolean customerIsMember);
	
	@RDF(Constants.DKMS_CORE + "trashState")
	public boolean getTrashState();
	
	public void setTrashState(boolean trashState);
	
	
	//Alle Bilder und Videos die zu einem bestimmten ContentItem gehören
	@RDF(Constants.DKMS_CORE + "dkmsMediaList")
	LinkedList<DkmsMultimediaFacade> getDkmsMediaList();	
	void setDkmsMediaList(LinkedList<DkmsMultimediaFacade> dkmsMultimediaFacades);
	
	//Alle WikiEinträge die zu einem bestimmten ContentItem gehören
	@RDF(Constants.DKMS_CORE + "dkmsWikiList")
	LinkedList<DkmsWikiFacade> getDkmsWikiList();	
	void setDkmsWikiList(LinkedList<DkmsWikiFacade> dkmsWikiFacades);
	
	//Alle BlogEinträge die zu einem bestimmten ContentItem gehören
	@RDF(Constants.DKMS_CORE + "dkmsCommentList")
	LinkedList<DkmsBlogFacade> getDkmsCommentList();	
	void setDkmsCommentList(LinkedList<DkmsBlogFacade> dkmsBlogFacades);
	
	//Alle Slides die zu einem bestimmten ContentItem gehören
	@RDF(Constants.DKMS_CORE + "dkmsSlideList")
	LinkedList<DkmsSlideFacade> getDkmsSlideList();	
	void setDkmsSlideList(LinkedList<DkmsSlideFacade> dkmsSlideFacades);
	
	//Alle Movies die zu einem bestimmten ContentItem gehören
	@RDF(Constants.DKMS_CORE + "dkmsMovieList")
	LinkedList<DkmsMovieFacade> getDkmsMovieList();	
	void setDkmsMovieList(LinkedList<DkmsMovieFacade> dkmsMovieFacades);
	
	//Alle Sequenz Komponenten die zu einem bestimmten ContentItem gehören
	@RDF(Constants.DKMS_CORE + "dkmsSequenceComponentList")
	LinkedList<DkmsSequenceComponentFacade> getDkmsSequenceComponentList();	
	void setDkmsSequenceComponentList(LinkedList<DkmsSequenceComponentFacade> dkmsSequenceComponentFacades);
	
	//Alle Combined ContentItems die zu einem bestimmten ContentItem gehören
	@RDF(Constants.DKMS_CORE + "dkmsCombinedComponentList")
	LinkedList<DkmsCombinedComponentFacade> getDkmsCombinedComponentList();	
	void setDkmsCombinedComponentList(LinkedList<DkmsCombinedComponentFacade> dkmsCombinedComponentFacades);

	/**
	 * @return the viewCounter
	 */
	@RDF(Constants.DKMS_CORE + "viewCounter")	
	public Integer getViewCounter();
	/**
	 * @param viewCounter the viewCounter to set
	 */
	public void setViewCounter(Integer viewCounter);
	
	//TODO Donation
	
	/** translations of the artwork */
	@RDF(Constants.DKMS_CORE + "dkmsContentItemTextList")
	LinkedList<DkmsContentItemTextFacade> getDkmsContentItemTextList();	
	void setDkmsContentItemTextList(LinkedList<DkmsContentItemTextFacade> multimediaFacades);
	
		
	/**
	 * The longitude of this point of interest. Maps to geo:long of the geo ontology
	 * in the triple store.
	 * 
	 * @return
	 */
	@Range(min=-180, max=180)
	@RDF(Constants.NS_GEO+"long")
	public double getLongitude();
	public void setLongitude(double longitude);

	/**
	 * The latitude of this point of interest. Maps to geo:lat of the geo ontology 
	 * in the triple store.
	 * @return
	 */
	@Range(min=-90, max=90)
	@RDF(Constants.NS_GEO+"lat")
	public double getLatitude();
	public void setLatitude(double latitude);

}