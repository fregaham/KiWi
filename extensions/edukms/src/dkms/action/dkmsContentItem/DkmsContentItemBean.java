/*
	Copyright © dkms, artaround
 */

package dkms.action.dkmsContentItem;

import java.io.Serializable;
import java.util.Date;
import java.util.LinkedList;

import kiwi.model.ontology.SKOSConcept;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.log.Log;

import dkms.action.dkmsContentItem.categories.DkmsBigIdeasAction;
import dkms.action.dkmsContentItem.categories.DkmsRepresentationTypeAction;
import dkms.datamodel.dkmsContentItem.DkmsBlogFacade;
import dkms.datamodel.dkmsContentItem.DkmsCombinedComponentFacade;
import dkms.datamodel.dkmsContentItem.DkmsContentItemFacade;
import dkms.datamodel.dkmsContentItem.DkmsContentItemTextFacade;
import dkms.datamodel.dkmsContentItem.DkmsMovieFacade;
import dkms.datamodel.dkmsContentItem.DkmsMultimediaFacade;
import dkms.datamodel.dkmsContentItem.DkmsSequenceComponentFacade;
import dkms.datamodel.dkmsContentItem.DkmsSlideFacade;
import dkms.datamodel.dkmsContentItem.DkmsWikiFacade;

@Name("dkmsContentItemBean")
@Scope(ScopeType.CONVERSATION)
@Transactional
public class DkmsContentItemBean implements Serializable {

	private static final long serialVersionUID = 1L;
	
	@Begin(join=true)
	@Create
	public void begin(){
	
	}
	
	@In(create= true)
	@Out(required = false)
	private DkmsBigIdeasAction dkmsBigIdeasAction;
	
	@In(create= true)
	@Out(required = false)
	private DkmsRepresentationTypeAction dkmsRepresentationTypeAction;
	
	@Logger
	private static Log log;
	
	 //1==exerciseSheet  2==exerciseSequence  3== uploadDocument   4==wikiContent
    private String dkmsContentItemType;    
    
	private String activeTask;
	
	private String tmpContentItem;
	
	private String contentItemIdentifier;
	
	private String lastUpdate;
	
	

	//NT == new Task    CT == changeTask 
	private String seqTaskState;
	
	//N == New ContentItem, C == Change Content Item
	private String contentItemState;
	
	private String dkmsContentItemName, authorName, description, exerciseSheetContent, 
			taskContent, taskTitle, commentText;   

	private double longitude, latitude;   
	
	private LinkedList<SKOSConcept> bigIdeas;
	
	private LinkedList<SKOSConcept> representationType;
	
	private LinkedList<DkmsContentItemTextFacade> dkmsContentItemTextList;
	
	private boolean publicAccess, trashState, customerIsMember;
	
	private LinkedList<DkmsFileManager> dkmsMediaList;
	
	private LinkedList<DkmsWikiManager> dkmsWikiList;
	
	private LinkedList<DkmsBlogManager> dkmsCommentList;
	
	private LinkedList<DkmsSlideManager> dkmsSlideList;	

	private LinkedList<DkmsMovieManager> dkmsMovieList;
	
	private LinkedList<DkmsSequenceComponentManager> dkmsSequenceComponentList;	

	private LinkedList<DkmsCombinedComponentManager> dkmsCombinedComponentList;
	
	public void clear(){
		Date d = new Date();
		dkmsContentItemName = "";
		authorName  = "";
		bigIdeas  = null;
		representationType  = null;
		dkmsBigIdeasAction = null;
		dkmsRepresentationTypeAction = null;
		description  = "";
		taskContent  = "";
		taskTitle = "";
		exerciseSheetContent = "";
		activeTask  = "";
		seqTaskState  = "";
		contentItemState = "";
	    tmpContentItem = "";
		longitude = 0;
		latitude = 0;
		trashState = false;
		dkmsMediaList = null;
		dkmsWikiList = null;
		dkmsCommentList = null;
		dkmsSlideList = null;
		dkmsMovieList = null;
		dkmsSequenceComponentList = null;
		dkmsCombinedComponentList = null;
		contentItemIdentifier = "";
		lastUpdate = "";
		
		
	}
	

	public void init(DkmsContentItemFacade selectedDkmsContentItem){
		this.dkmsContentItemName = selectedDkmsContentItem.getTitle();
		
		LinkedList<DkmsMultimediaFacade> ll = selectedDkmsContentItem.getDkmsMediaList();
		dkmsMediaList = new LinkedList<DkmsFileManager>();
		for (DkmsMultimediaFacade dkmsMultimediaFacade : ll) {
			dkmsMediaList.add(new DkmsFileManager(dkmsMultimediaFacade.getMimeType(), dkmsMultimediaFacade.getFilename()));
		}
		
		LinkedList<DkmsWikiFacade> ll2 = selectedDkmsContentItem.getDkmsWikiList();
		dkmsWikiList = new LinkedList<DkmsWikiManager>();
		for (DkmsWikiFacade dkmsWikiFacade : ll2) {
			dkmsWikiList.add(new DkmsWikiManager(dkmsWikiFacade.getVersion(), dkmsWikiFacade.getWikiContent(), dkmsWikiFacade.getWikiAuthor()));
		}
		
		
		LinkedList<DkmsBlogFacade> ll3 = selectedDkmsContentItem.getDkmsCommentList();
		dkmsCommentList = new LinkedList<DkmsBlogManager>();
		for (DkmsBlogFacade dkmsBlogFacade : ll3) {
			dkmsCommentList.add(new DkmsBlogManager(dkmsBlogFacade.getVersion(), dkmsBlogFacade.getCommentContent(), dkmsBlogFacade.getCommentAuthor()));
		}
		
		LinkedList<DkmsSlideFacade> ll4 = selectedDkmsContentItem.getDkmsSlideList();
		dkmsSlideList = new LinkedList<DkmsSlideManager>();
		for (DkmsSlideFacade dkmsSlideFacade : ll4) {
			dkmsSlideList.add(new DkmsSlideManager(dkmsSlideFacade.getMimeType(), dkmsSlideFacade.getFilename()));
		}
		
		LinkedList<DkmsMovieFacade> ll5 = selectedDkmsContentItem.getDkmsMovieList();
		dkmsMovieList = new LinkedList<DkmsMovieManager>();
		for (DkmsMovieFacade dkmsMovieFacade : ll5) {
			dkmsMovieList.add(new DkmsMovieManager(dkmsMovieFacade.getMimeType(), dkmsMovieFacade.getFilename()));
		}
		
		LinkedList<DkmsSequenceComponentFacade> ll6 = selectedDkmsContentItem.getDkmsSequenceComponentList();
		dkmsSequenceComponentList = new LinkedList<DkmsSequenceComponentManager>();
		for (DkmsSequenceComponentFacade dkmsSequenceComponentFacade : ll6) {
			dkmsSequenceComponentList.add(new DkmsSequenceComponentManager(dkmsSequenceComponentFacade.getTaskId(), dkmsSequenceComponentFacade.getTaskTitle(), dkmsSequenceComponentFacade.getVersion(), dkmsSequenceComponentFacade.getSequenceContent()));
		}
		
		LinkedList<DkmsCombinedComponentFacade> ll7 = selectedDkmsContentItem.getDkmsCombinedComponentList();
		dkmsCombinedComponentList = new LinkedList<DkmsCombinedComponentManager>();
		for (DkmsCombinedComponentFacade dkmsCombinedComponentFacade : ll7) {
			dkmsCombinedComponentList.add(new DkmsCombinedComponentManager(dkmsCombinedComponentFacade.getCombinedItemTitle(), dkmsCombinedComponentFacade.getCombinedItemId(), dkmsCombinedComponentFacade.getCombinedItemType()));
		}
		
				
		this.dkmsContentItemTextList = selectedDkmsContentItem.getDkmsContentItemTextList();
		this.trashState = selectedDkmsContentItem.getSellingState();
		this.publicAccess = selectedDkmsContentItem.getPublicAccess();
		this.authorName = selectedDkmsContentItem.getAuthorName();		
		this.bigIdeas = selectedDkmsContentItem.getBigIdeas();
		this.representationType = selectedDkmsContentItem.getRepresentationType();
		this.description = selectedDkmsContentItem.getDescription();
		this.taskContent = selectedDkmsContentItem.getTaskContent();
		this.taskTitle = selectedDkmsContentItem.getTaskTitle();
		this.exerciseSheetContent = selectedDkmsContentItem.getExerciseSheetContent();		
		this.activeTask = selectedDkmsContentItem.getActiveTask();
		this.seqTaskState = selectedDkmsContentItem.getSeqTaskState();		
		this.contentItemState = selectedDkmsContentItem.getContentItemState();	
		this.tmpContentItem = selectedDkmsContentItem.getTmpContentItem();
		this.contentItemIdentifier = selectedDkmsContentItem.getContentItemIdentifier();
		this.lastUpdate = selectedDkmsContentItem.getLastUpdate();
		
		
		if(this.bigIdeas != null)
		{
			dkmsBigIdeasAction.setChosenConcepts(this.bigIdeas);
		}
		else
		{
			dkmsBigIdeasAction = null;
		}
		
		if(this.representationType != null)
		{
			dkmsRepresentationTypeAction.setChosenConcepts(this.representationType);
		}
		else
		{
			dkmsRepresentationTypeAction = null;
		}
	}
	
		
	


	public void check(){
		log.info(latitude);
		log.info(longitude);
	}
	
	public String getDkmsContentItemName() {
		return dkmsContentItemName;
	}

	public void setDkmsContentItemName(String dkmsContentItemName) {
		this.dkmsContentItemName = dkmsContentItemName;
	}

	public String getAuthorName() {
		return authorName;
	}

	public void setAuthorName(String authorName) {
		this.authorName = authorName;
	}

	public LinkedList<SKOSConcept> getBigIdeas() {
		return bigIdeas;
	}

	public void setBigIdeas(LinkedList<SKOSConcept> bigIdeas) {
		this.bigIdeas = bigIdeas;
	}
	
	public LinkedList<SKOSConcept> getRepresentationType() {
		return representationType;
	}

	public void setRepresentationType(LinkedList<SKOSConcept> representationType) {
		this.representationType = representationType;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getTaskContent() {
		return taskContent;
	}

	public void setTaskContent(String taskContent) {
		this.taskContent = taskContent;
	}

	public void setExerciseSheetContent(String exerciseSheetContent) {
		this.exerciseSheetContent = exerciseSheetContent;
	}	
	
	public String getExerciseSheetContent() {
		return exerciseSheetContent;
	}

	public void setTaskTitle(String taskTitle) {
		this.taskTitle = taskTitle;
	}
	
	public String getTaskTitle() {
		return taskTitle;
	}
	
	public String getActiveTask() {
		return activeTask;
	}

	public void setActiveTask(String activeTask) {
		this.activeTask = activeTask;
	}
	
	public String getSeqTaskState() {
		return seqTaskState;
	}

	public void setSeqTaskState(String seqTaskState) {
		this.seqTaskState = seqTaskState;
	}	
	
	public String getContentItemState() {
		return contentItemState;
	}

	public void setContentItemState(String contentItemState) {
		this.contentItemState = contentItemState;
	}	
		
	public LinkedList<DkmsContentItemTextFacade> getDkmsContentItemTextList() {
		return dkmsContentItemTextList;
	}

	public void setDkmsContentItemTextList(LinkedList<DkmsContentItemTextFacade> dkmsContentItemTextList) {
		this.dkmsContentItemTextList = dkmsContentItemTextList;
	}
	
	public LinkedList<DkmsWikiManager> getDkmsWikiList() {
		return dkmsWikiList;
	}

	public void setDkmsWikiList(LinkedList<DkmsWikiManager> dkmsWikiList) {
		this.dkmsWikiList = dkmsWikiList;
	}
	
	public LinkedList<DkmsBlogManager> getDkmsCommentList() {
		return dkmsCommentList;
	}


	public void setDkmsCommentList(LinkedList<DkmsBlogManager> dkmsCommentList) {
		this.dkmsCommentList = dkmsCommentList;
	}

	public boolean getPublicAccess() {
		return publicAccess;
	}

	public void setPublicAccess(boolean publicAccess) {
		this.publicAccess = publicAccess;
	}
	
	public boolean getCustomerIsMember() {
		return customerIsMember;
	}

	public void setCustomerIsMember(boolean customerIsMember) {
		this.customerIsMember = customerIsMember;
	}

	public boolean getTrashState() {
		return trashState;
	}

	public void setTrashState(boolean trashState) {
		this.trashState = trashState;
	}

	public LinkedList<DkmsFileManager> getDkmsMediaList() {
		return dkmsMediaList;
	}

	public void setDkmsMediaList(
			LinkedList<DkmsFileManager> dkmsMediaList) {
		this.dkmsMediaList = dkmsMediaList;
	}
	
	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public void setDkmsContentItemType(String dkmsContentItemType) {
		this.dkmsContentItemType = dkmsContentItemType;
	}

	public String getDkmsContentItemType() {
		return dkmsContentItemType;
	}
	
	
	public String getCommentText() {
		return commentText;
	}
	
	public void setCommentText(String commentText) {
		this.commentText = commentText;
	}
	
	public LinkedList<DkmsSlideManager> getDkmsSlideList() {
		return dkmsSlideList;
	}


	public void setDkmsSlideList(LinkedList<DkmsSlideManager> dkmsSlideList) {
		this.dkmsSlideList = dkmsSlideList;
	}


	public LinkedList<DkmsMovieManager> getDkmsMovieList() {
		return dkmsMovieList;
	}


	public void setDkmsMovieList(LinkedList<DkmsMovieManager> dkmsMovieList) {
		this.dkmsMovieList = dkmsMovieList;
	}


	public LinkedList<DkmsSequenceComponentManager> getDkmsSequenceComponentList() {
		return dkmsSequenceComponentList;
	}


	public void setDkmsSequenceComponentList(LinkedList<DkmsSequenceComponentManager> dkmsSequenceComponentList) {
		this.dkmsSequenceComponentList = dkmsSequenceComponentList;
	}
	
	public LinkedList<DkmsCombinedComponentManager> getDkmsCombinedComponentList() {
		return dkmsCombinedComponentList;
	}


	public void setDkmsCombinedComponentList(
			LinkedList<DkmsCombinedComponentManager> dkmsCombinedComponentList) {
		this.dkmsCombinedComponentList = dkmsCombinedComponentList;
	}
	
	public DkmsBigIdeasAction getDkmsBigIdeasAction() {
		return dkmsBigIdeasAction;
	}

	public void setDkmsBigIdeasAction(DkmsBigIdeasAction dkmsBigIdeasAction) {
		this.dkmsBigIdeasAction = dkmsBigIdeasAction;
	}
	
	public String getTmpContentItem() {
		return tmpContentItem;
	}

	public void setTmpContentItem(String tmpContentItem) {
		this.tmpContentItem = tmpContentItem;
	}
	
	public String getContentItemIdentifier() {
		return contentItemIdentifier;
	}

	public void setContentItemIdentifier(String contentItemIdentifier) {
		this.contentItemIdentifier = contentItemIdentifier;
	}

	public String getLastUpdate() {
		return lastUpdate;
	}


	public void setLastUpdate(String lastUpdate) {
		this.lastUpdate = lastUpdate;
	}
	
	
}
