/*
	Copyright © dkms, artaround
 */

package dkms.action.dkmsContentItem.contentCreation;

import java.io.Serializable;
import java.util.Date;
import java.util.LinkedList;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;

import dkms.action.dkmsContentItem.DkmsContentItemBean;
import dkms.action.dkmsContentItem.DkmsMovieManager;
import dkms.action.dkmsContentItem.DkmsSequenceComponentManager;
import dkms.action.dkmsContentItem.categories.DkmsBigIdeasAction;
import dkms.action.dkmsContentItem.categories.DkmsRepresentationTypeAction;


@Name("dkmsBasicAction")
@Scope(ScopeType.CONVERSATION)
public class DkmsBasicAction implements Serializable {

	private static final long serialVersionUID = 7482822342778616612L;
	
	@In(required = false)
	private DkmsBigIdeasAction dkmsBigIdeasAction;
	
	@In(required = false)
	private DkmsRepresentationTypeAction dkmsRepresentationTypeAction;
	
	@In(create = true)
	private DkmsContentItemBean dkmsContentItemBean;
	
	@Logger
    private Log log;
    
	
    public String newExerciseSheet(){    	
    	dkmsContentItemBean.clear();
    	dkmsContentItemBean.setContentItemState("N");
    	return "/edukms/authorInterface/exerciseSheetEditor.xhtml";
    }
    
    public String newWikiEntry(){    	
    	dkmsContentItemBean.clear();
    	dkmsContentItemBean.setContentItemState("N");
    	return "/edukms/abcmaths/wikiEditor.xhtml";
    }
    
    public String newBlogEntry(){    	
    	dkmsContentItemBean.clear();
    	dkmsContentItemBean.setContentItemState("N");
    	return "/edukms/abcmaths/blogEditor.xhtml";
    }
    
    public String newSequence(){    	
    	dkmsContentItemBean.clear();
    	dkmsContentItemBean.setContentItemState("N");
    	return "/edukms/authorInterface/sequenceController.xhtml";
    }
    
    public String newResourceCombination(){    	
    	dkmsContentItemBean.clear();
    	dkmsContentItemBean.setContentItemState("N");
    	dkmsContentItemBean.setContentItemStateBuffer("6");
    	return "/edukms/authorInterface/combineRessourcesController.xhtml";
    }
    
    public String newDocument(){    
    	dkmsContentItemBean.clear();
    	dkmsContentItemBean.setContentItemState("N");
    	return "/edukms/authorInterface/documentUploadEditor.xhtml";
    }
    
    public String adminDocuments(){    
    	dkmsContentItemBean.clear();
    	return "/edukms/authorInterface/userAdministrationArea.xhtml";
    }
    
    public String goToCreateResource(){    
    	
    	return "/edukms/authorInterface/createResource.xhtml";
    }
    
    
    
	
	public String goToSearchContentPage(){
		return "/edukms/abcmaths/searchKMS.xhtml";
	}
	
	public String goToAuthorInterface(){
		return "/edukms/authorInterface/home.xhtml";
	}
	
	public String showDkmsContentItemsLastUploaded(){
		return "/edukms/abcmaths/lastUploadedResult.xhtml";
	}

	
	public String goToHome(){
		dkmsContentItemBean.setDkmsContentItemName("Abort");
		return "/edukms/authorInterface/home.seam";
		
	}
	
	public String goToAbcmathsHome(){
		dkmsContentItemBean.setDkmsContentItemName("Abort");
		return "/edukms/abcmaths/home.seam";
		
	}
	
	public String goToAdminArea(){
		return "/edukms/authorInterface/userAdministrationArea.seam";
		
	}
	
	public String reloadExerciseSheetEditor(){
		return "/edukms/authorInterface/exerciseSheetEditor.seam";
		
	}
	
	public String reloadSequenceEditor(){
		return "/edukms/authorInterface/sequenceEditor.seam";
		
	}
	
	public String reloadExerciseSheetChangeEditor(){
		return "/edukms/authorInterface/exerciseSheetChangeEditor.seam";
		
	}
	
		
	public String reloadWikiEditor(){
		return "/edukms/abcmaths/wikiEditor.seam";
		
	}	
	
	public String reloadWikiChangeEditor(){
		return "/edukms/abcmaths/wikiChangeEditor.seam";
		
	}	
	
	public String reloadImageInserter(){
		return "/edukms/authorInterface/imageInserter.seam";
		
	}	
	
	
	@Begin(join=true)
	public String nextStepSheet() {
		dkmsContentItemBean.setDkmsContentItemType("1");
		dkmsContentItemBean.setLastUpdate((new Date()).toString());
		dkmsBigIdeasAction.setChosenBigIdeas(dkmsContentItemBean.getBigIdeas());
		return "/edukms/authorInterface/contentItemData.xhtml";
	}
	
	
	
	
	public String insertPicIntoSheet(String filename){
		String temp;
		String text;
		String imgInsertion; 
		if (dkmsContentItemBean.getDkmsContentItemType().equals("1")){
		imgInsertion = "<img src='" + filename + " ' /	>";			
		temp = dkmsContentItemBean.getExerciseSheetContent();
		text = temp + imgInsertion;
		dkmsContentItemBean.setExerciseSheetContent(text);	
		return "/edukms/authorInterface/exerciseSheetEditor.xhtml";
		} 
		else return "";
	
	}
	
	public String insertPicIntoTaskContent(String filename){
		String temp;
		String text;
		String imgInsertion;
		imgInsertion = "<img src='" + filename + " ' /	>";
		temp = dkmsContentItemBean.getTaskContent();
		text = temp + imgInsertion;
		dkmsContentItemBean.setTaskContent(text);	
		return "/edukms/authorInterface/sequenceEditor.xhtml";
	}
	
	public String insertPicIntoWikiContent(String filename){
		String temp;
		String text;
		String imgInsertion;
		imgInsertion = "<img src='" + filename + " ' /	>";
		temp = dkmsContentItemBean.getTaskContent();
		text = temp + imgInsertion;
		dkmsContentItemBean.setTaskContent(text);	
		return "/edukms/abcmaths/wikiEditor.xhtml";
	}
	
	public String insertMovieIntoSheet(){
		String filename= "";
		String filetype="";
		String temp;
		String text;
		String movieInsertion;
		
		LinkedList<DkmsMovieManager> ll = new LinkedList<DkmsMovieManager>();
		ll = dkmsContentItemBean.getDkmsMovieList();
		if (ll.size() > 0) {
			DkmsMovieManager mf = ll.getFirst();	
			filename = "../../images/" + mf.getFileName();
			filetype = mf.getMimeType();
		}
		if (filetype.equals("application/octet-stream")){
			movieInsertion = "<object data='" + filename + "' type='application/x-shockwave-flash'><param name='movie' value='" + filename + "'></object>  ";
			temp = dkmsContentItemBean.getExerciseSheetContent();
			text = temp + movieInsertion;
			dkmsContentItemBean.setExerciseSheetContent(text);	
		}
		return "/edukms/authorInterface/exerciseSheetEditor.xhtml";
	}
	
	public String insertMovieIntoTaskContent(){
		String filename= "";
		String filetype="";
		String temp;
		String text;
		String movieInsertion;
		
		LinkedList<DkmsMovieManager> ll = new LinkedList<DkmsMovieManager>();
		ll = dkmsContentItemBean.getDkmsMovieList();
		if (ll.size() > 0) {
			DkmsMovieManager mf = ll.getFirst();	
			filename = "../../images/" + mf.getFileName();
			filetype = mf.getMimeType();
		}
		if (filetype.equals("application/octet-stream")){
			movieInsertion = "<object data='" + filename + "' type='application/x-shockwave-flash'><param name='movie' value='" + filename + "'></object>  ";
			temp = dkmsContentItemBean.getTaskContent();
			text = temp + movieInsertion;
			dkmsContentItemBean.setTaskContent(text);	
		}
		return "/edukms/authorInterface/sequenceEditor.xhtml";
	}
	
	
	public String insertMovieIntoWikiContent(){
		String filename= "";
		String filetype="";
		String temp;
		String text;
		String movieInsertion;
		
		LinkedList<DkmsMovieManager> ll = new LinkedList<DkmsMovieManager>();
		ll = dkmsContentItemBean.getDkmsMovieList();
		if (ll.size() > 0) {
			DkmsMovieManager mf = ll.getFirst();	
			filename = "../../images/" + mf.getFileName();
			filetype = mf.getMimeType();
		}
		if (filetype.equals("application/octet-stream")){
			movieInsertion = "<object data='" + filename + "' type='application/x-shockwave-flash'><param name='movie' value='" + filename + "'></object>  ";
			temp = dkmsContentItemBean.getTaskContent();
			text = temp + movieInsertion;
			dkmsContentItemBean.setTaskContent(text);	
		}
		return "/edukms/abcmaths/wikiEditor.xhtml";
	}
	
		
	@Begin(join=true)
	public String nextStepSequence() {
		dkmsContentItemBean.setDkmsContentItemType("2");
		dkmsContentItemBean.setLastUpdate((new Date()).toString());	
		dkmsBigIdeasAction.setChosenBigIdeas(dkmsContentItemBean.getBigIdeas());
		return "/edukms/authorInterface/contentItemData.xhtml";
	}
	
	public String nextStepChangeSequence() {
		dkmsContentItemBean.setContentItemState("C");
		dkmsContentItemBean.setLastUpdate((new Date()).toString());
		dkmsBigIdeasAction.setChosenBigIdeas(dkmsContentItemBean.getBigIdeas());
		return "/edukms/authorInterface/contentItemData.xhtml";
	}
	
	@Begin(join=true)
	public String nextStepDocument() {
		dkmsContentItemBean.setDkmsContentItemType("3");
		dkmsContentItemBean.setLastUpdate((new Date()).toString());
		dkmsBigIdeasAction.setChosenBigIdeas(dkmsContentItemBean.getBigIdeas());
		return "/edukms/authorInterface/contentItemData.xhtml";
	}
	
	
	public String nextStepDocumentUploadWizard() {
		
		return "/edukms/authorInterface/documentUploader.xhtml";
	}
	
	public String nextStepDocumentUpload() {
		
		return "/edukms/authorInterface/documentUploadEditor.xhtml";
	}
	
	@Begin(join=true)
	public String nextStepWiki() {
		dkmsContentItemBean.setDkmsContentItemType("4");
		dkmsContentItemBean.setDkmsContentItemName(dkmsContentItemBean.getDkmsContentItemName() + " (Wiki)" );
		dkmsContentItemBean.setLastUpdate((new Date()).toString());
		dkmsBigIdeasAction.setChosenBigIdeas(dkmsContentItemBean.getBigIdeas());
		return "/edukms/abcmaths/infoPlatformItemData.xhtml";
	}
	
	@Begin(join=true)
	public String nextStepBlog() {
		dkmsContentItemBean.setDkmsContentItemType("5");
		dkmsContentItemBean.setDkmsContentItemName(dkmsContentItemBean.getDkmsContentItemName() + " (Blog)" );
		dkmsContentItemBean.setLastUpdate((new Date()).toString());
		dkmsBigIdeasAction.setChosenBigIdeas(dkmsContentItemBean.getBigIdeas());
		return "/edukms/abcmaths/infoPlatformItemData.xhtml";
	}
	
	@Begin(join=true)
	public String nextStepCombinedResource() {
		dkmsContentItemBean.setDkmsContentItemType("6");
		dkmsContentItemBean.setLastUpdate((new Date()).toString());
		dkmsBigIdeasAction.setChosenBigIdeas(dkmsContentItemBean.getBigIdeas());
		return "/edukms/authorInterface/contentItemData.xhtml";
	}
	
	
	public String nextToSequenceEditor(DkmsSequenceComponentManager selectedItem) {
		
		
		dkmsContentItemBean.setDkmsContentItemType("2");
		dkmsContentItemBean.setTaskContent(selectedItem.getSequenceContent());
		dkmsContentItemBean.setTaskTitle(selectedItem.getTaskTitle());
		dkmsContentItemBean.setSeqTaskState("CT");	
		int at = selectedItem.getTaskId();
		String aT = Integer.toString(at);
		dkmsContentItemBean.setActiveTask(aT);
		
		return "/edukms/authorInterface/sequenceEditor.xhtml";
	}
	
	@Begin(join=true)
	public String newSequenceTask() {
		
		dkmsContentItemBean.setTaskContent("");
		dkmsContentItemBean.setTaskTitle("");
		dkmsContentItemBean.setSeqTaskState("NT");		
		
		return "/edukms/authorInterface/sequenceEditor.xhtml";
	}
	
		
	public String nextToContentItemChangeData() {
		dkmsContentItemBean.setLastUpdate((new Date()).toString());
		return "/edukms/authorInterface/contentItemChangeData.xhtml";
	}	
	
	public String nextToSequenceController() {
		return "/edukms/authorInterface/sequenceController.xhtml";
	}
	
	public String nextToSequenceChangeController() {
		return "/edukms/authorInterface/sequenceChangeController.xhtml";
	}
	
	public String nextForChange() {
		return "/edukms/authorInterface/exerciseSheetChangeData.xhtml";
	}
	
	@Begin(join=true)
	public String nextToSimpleResImageUploader() {
		dkmsContentItemBean.setDkmsContentItemType("1");
		return "/edukms/authorInterface/imageUploader.xhtml";
	}
	
	@Begin(join=true)
	public String nextToSegmentedResImageUploader() {
		dkmsContentItemBean.setDkmsContentItemType("2");
		return "/edukms/authorInterface/imageUploader.xhtml";
	}
	
	@Begin(join=true)
	public String nextToWikiImageUploader() {
		dkmsContentItemBean.setDkmsContentItemType("4");
		return "/edukms/authorInterface/imageUploader.xhtml";
	}
	
	public String nextToImageInserter() {
		return "/edukms/authorInterface/imageInserter.xhtml";
	}
	
	@Begin(join=true)
	public String nextToSimpleResMovieWizard() {
		dkmsContentItemBean.setDkmsContentItemType("1");
		return "/edukms/authorInterface/movieWizard.xhtml";
	}
	
	@Begin(join=true)
	public String nextToSegmentedResMovieWizard() {
		dkmsContentItemBean.setDkmsContentItemType("2");
		return "/edukms/authorInterface/movieWizard.xhtml";
	}
	
	@Begin(join=true)
	public String nextToWikiMovieWizard() {
		dkmsContentItemBean.setDkmsContentItemType("4");
		return "/edukms/authorInterface/movieWizard.xhtml";
	}
	
	public String updateViewStatus(DkmsSequenceComponentManager item) {
		
		LinkedList<DkmsSequenceComponentManager> dkmsContentItemList = dkmsContentItemBean.getDkmsSequenceComponentList();
		int cnt = 0;
		
		for (DkmsSequenceComponentManager sequenceComponentTmp : dkmsContentItemList) {
			if (sequenceComponentTmp.equals(item)){
				if (sequenceComponentTmp.getViewStatus()==1){
					sequenceComponentTmp.setViewStatus(0);
					dkmsContentItemBean.getDkmsSequenceComponentList().set(cnt, sequenceComponentTmp);
				}else{
					sequenceComponentTmp.setViewStatus(1);
					dkmsContentItemBean.getDkmsSequenceComponentList().set(cnt, sequenceComponentTmp);
				}
			}
			cnt = cnt + 1;			
		}
		return "/edukms/authorInterface/sequenceController.xhtml";
	}
	
	
	
}
