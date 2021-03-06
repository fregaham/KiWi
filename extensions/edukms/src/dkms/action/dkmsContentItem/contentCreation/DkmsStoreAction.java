/*
	Copyright ¬© dkms, artaround
 */
package dkms.action.dkmsContentItem.contentCreation;

import java.util.Date;
import java.util.LinkedList;

import kiwi.model.content.ContentItem;
import kiwi.model.user.User;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.core.Events;
import org.jfree.util.Log;

import dkms.action.dkmsContentItem.DkmsBlogManager;
import dkms.action.dkmsContentItem.DkmsCombinedComponentManager;
import dkms.action.dkmsContentItem.DkmsContentItemBean;
import dkms.action.dkmsContentItem.DkmsFileManager;
import dkms.action.dkmsContentItem.DkmsSequenceComponentManager;
import dkms.action.dkmsContentItem.DkmsSlideManager;
import dkms.action.dkmsContentItem.DkmsWikiManager;
import dkms.action.dkmsContentItem.DkmsWriteModusAction;
import dkms.action.dkmsContentItem.categories.DkmsBigIdeasAction;
import dkms.datamodel.dkmsContentItem.DkmsContentItemFacade;
import dkms.datamodel.dkmsContentItem.DkmsSequenceComponentFacade;
import dkms.service.DkmsContentItemService;




@Name("dkmsStoreAction")
@Scope(ScopeType.CONVERSATION)
@Transactional
public class DkmsStoreAction {
	
	@In
	private DkmsContentItemBean dkmsContentItemBean;
	
	@In
	private DkmsContentItemService dkmsContentItemService;
	
	@In(create = true)
    private User currentUser;
	
	@In(required = false)
	private DkmsBigIdeasAction dkmsBigIdeasAction;
	

	public String finish() {
				

		Events.instance().raiseEvent("onSaveEvent");
			//momentan, wird noch gešndert
		return "/edukms/authorInterface/userAdministrationArea.xhtml";
	}	
	
	public String storeWikiContent() {

		String version = (new Date()).toString();
		
		String wikiContent = dkmsContentItemBean.getTaskContent();
		
		String wikiAuthor = currentUser.getLogin();
		
		boolean tmp = false;
		
		
		DkmsWikiManager wm = new DkmsWikiManager();
		wm.setVersion(version);
		wm.setWikiContent(wikiContent);
		//Author von altem (gešndertem) WikiContent
		wm.setWikiAuthor(dkmsContentItemBean.getAuthorName());
		
		LinkedList<DkmsWikiManager> dkmsContentItemList = dkmsContentItemBean.getDkmsWikiList();
		if(dkmsContentItemList == null){
			dkmsContentItemList = new LinkedList<DkmsWikiManager>();
		}
		
		dkmsContentItemList.add(wm);
		dkmsContentItemBean.setDkmsWikiList(dkmsContentItemList);
		dkmsBigIdeasAction.setChosenBigIdeas(dkmsContentItemBean.getBigIdeas());
		//neuer Author (der gerade eingeloggt ist und den WikiContent gešndert hat)
		dkmsContentItemBean.setAuthorName(wikiAuthor);
		dkmsContentItemBean.setDkmsContentItemType("4");
		tmp = dkmsContentItemBean.getBigIdeas().isEmpty();
		System.out.println("Show bigIdeas:" +tmp);
		Log.info("Show bigIdeas:" +tmp);
		
		return "/edukms/abcmaths/infoPlatformItemData.xhtml";
	}
	
	public String storeBlogComment() {

		String version = (new Date()).toString();
		
		String commentContent = dkmsContentItemBean.getCommentText();
		
		String commentAuthor = currentUser.getLogin();
		
		
		DkmsBlogManager bm = new DkmsBlogManager();
		bm.setVersion(version);
		bm.setCommentContent(commentContent);
		//Author von altem (gešndertem) commentContent
		bm.setCommentAuthor(commentAuthor);
		
		LinkedList<DkmsBlogManager> dkmsContentItemList = dkmsContentItemBean.getDkmsCommentList();
		if(dkmsContentItemList == null){
			dkmsContentItemList = new LinkedList<DkmsBlogManager>();
		}
		
		dkmsContentItemList.add(bm);
		dkmsContentItemBean.setDkmsCommentList(dkmsContentItemList);
		dkmsBigIdeasAction.setChosenBigIdeas(dkmsContentItemBean.getBigIdeas());
		dkmsContentItemBean.setCommentText("");
		dkmsContentItemBean.setDkmsContentItemType("5");
		
		return "/edukms/abcmaths/infoPlatformItemData.xhtml";
	}
	
	public String storeSequenceItem() {

		String taskTitle = dkmsContentItemBean.getTaskTitle();
		
		String version = (new Date()).toString();
		
		String sequenceItemContent = dkmsContentItemBean.getTaskContent();
		
		String status = dkmsContentItemBean.getSeqTaskState();
		
		DkmsSequenceComponentManager scm = new DkmsSequenceComponentManager();
		scm.setTaskTitle(taskTitle);
		scm.setVersion(version);
		scm.setSequenceContent(sequenceItemContent);
			
		LinkedList<DkmsSequenceComponentManager> dkmsContentItemList = dkmsContentItemBean.getDkmsSequenceComponentList();
		
		if ( status == "NT"){
		
			if(dkmsContentItemList == null){
				dkmsContentItemList = new LinkedList<DkmsSequenceComponentManager>();
				scm.setTaskId(0);
			}
			else {
				
				DkmsSequenceComponentManager tmp = dkmsContentItemList.getLast();
				int taskId = tmp.getTaskId() + 1;
				scm.setTaskId(taskId);
			}
			
			
			dkmsContentItemList.add(scm);
			dkmsContentItemBean.setDkmsSequenceComponentList(dkmsContentItemList);
		
		}
		
		else if (status == "CT"){
			
			String aT = dkmsContentItemBean.getActiveTask();
			int at = Integer.parseInt(aT);
			int cnt = 0;
			
			for (DkmsSequenceComponentManager sequenceComponentTmp : dkmsContentItemList) {
				if (sequenceComponentTmp.getTaskId() == at){
					sequenceComponentTmp.setSequenceContent(sequenceItemContent);
					sequenceComponentTmp.setTaskTitle(taskTitle);
					sequenceComponentTmp.setVersion(version);
					dkmsContentItemBean.getDkmsSequenceComponentList().set(cnt, sequenceComponentTmp);
				}
				cnt = cnt + 1;
			}
		}
	
				
		return "/edukms/authorInterface/sequenceController.xhtml";
	}
	
	
	public String storeSlide(){
		
		return "";
	}
	
	public String storeMovie(){
		
		return "";
	}

	public void storeCombinedComponent(DkmsContentItemFacade combinedItem){
		
		
		
		DkmsCombinedComponentManager combim = new DkmsCombinedComponentManager();
		//combim.setCombinedItem(combinedItem);
		combim.setCombinedItemTitle(combinedItem.getTitle());
		combim.setCombinedItemId(combinedItem.getKiwiIdentifier());
		combim.setCombinedItemType(combinedItem.getDkmsContentItemType());
		
//		long id = combinedItem.getId();
//		String iD = String.valueOf(id);
//		combim.setContentItemId(iD);
		
		LinkedList<DkmsCombinedComponentManager> dkmsContentItemList = dkmsContentItemBean.getDkmsCombinedComponentList();
		if(dkmsContentItemList == null){
			dkmsContentItemList = new LinkedList<DkmsCombinedComponentManager>();
		}
		
		dkmsContentItemList.add(combim);
		dkmsContentItemBean.setDkmsCombinedComponentList(dkmsContentItemList);
	
	
}
	
	public String clearCombinedComponentList(){
		
		
		
		LinkedList<DkmsCombinedComponentManager> dkmsContentItemList = dkmsContentItemBean.getDkmsCombinedComponentList();
		if(dkmsContentItemList == null){
			dkmsContentItemList = new LinkedList<DkmsCombinedComponentManager>();
		}
		
		dkmsContentItemList.clear();
		dkmsContentItemBean.setDkmsCombinedComponentList(dkmsContentItemList);
	
		return "/edukms/authorInterface/combineRessourcesController.xhtml";
	
}
	
	public String removeCombinedComponent(DkmsCombinedComponentManager item){
		
		
		
		LinkedList<DkmsCombinedComponentManager> dkmsContentItemList = dkmsContentItemBean.getDkmsCombinedComponentList();
		if(dkmsContentItemList == null){
			dkmsContentItemList = new LinkedList<DkmsCombinedComponentManager>();
		}
		
		dkmsContentItemList.remove(item);
		dkmsContentItemBean.setDkmsCombinedComponentList(dkmsContentItemList);
		
	
		return "/edukms/authorInterface/combineRessourcesController.xhtml";
	
}
	
	public String clearUploadedResourcesList(){
		
		
		
		LinkedList<DkmsFileManager> dkmsContentItemList = dkmsContentItemBean.getDkmsMediaList();
		if(dkmsContentItemList == null){
			dkmsContentItemList = new LinkedList<DkmsFileManager>();
		}
				
		
		dkmsContentItemList.clear();
		dkmsContentItemBean.setDkmsMediaList(dkmsContentItemList);
	
		return "/edukms/authorInterface/documentUploadEditor.xhtml";
	
}
	
	public String removeUploadedResourse(DkmsFileManager item){
		
		
		
		LinkedList<DkmsFileManager> dkmsContentItemList = dkmsContentItemBean.getDkmsMediaList();
		if(dkmsContentItemList == null){
			dkmsContentItemList = new LinkedList<DkmsFileManager>();
		}
		
		dkmsContentItemList.remove(item);
		dkmsContentItemBean.setDkmsMediaList(dkmsContentItemList);
		
	
		return "/edukms/authorInterface/documentUploadEditor.xhtml";
	
}
	
	public String clearImageList(){
		
		
		
		LinkedList<DkmsFileManager> dkmsContentItemList = dkmsContentItemBean.getDkmsMediaList();
		if(dkmsContentItemList == null){
			dkmsContentItemList = new LinkedList<DkmsFileManager>();
		}
				
		
		dkmsContentItemList.clear();
		dkmsContentItemBean.setDkmsMediaList(dkmsContentItemList);
	
		return "/edukms/authorInterface/imageInserter.xhtml";
	
}
	
	public String removeImage(DkmsFileManager item){
		
		
		
		LinkedList<DkmsFileManager> dkmsContentItemList = dkmsContentItemBean.getDkmsMediaList();
		if(dkmsContentItemList == null){
			dkmsContentItemList = new LinkedList<DkmsFileManager>();
		}
		
		dkmsContentItemList.remove(item);
		dkmsContentItemBean.setDkmsMediaList(dkmsContentItemList);
	
		return "/edukms/authorInterface/imageInserter.xhtml";
	
}
	
	
	public String clearSlideList(){
		
		
		
		LinkedList<DkmsSlideManager> dkmsContentItemList = dkmsContentItemBean.getDkmsSlideList();
		if(dkmsContentItemList == null){
			dkmsContentItemList = new LinkedList<DkmsSlideManager>();
		}
				
		
		dkmsContentItemList.clear();
		dkmsContentItemBean.setDkmsSlideList(dkmsContentItemList);
	
		return "/edukms/authorInterface/exerciseSheetEditor.xhtml";
	
}
	
	public String removeSlide(DkmsSlideManager item){
		
		
		
		LinkedList<DkmsSlideManager> dkmsContentItemList = dkmsContentItemBean.getDkmsSlideList();
		if(dkmsContentItemList == null){
			dkmsContentItemList = new LinkedList<DkmsSlideManager>();
		}
		
		dkmsContentItemList.remove(item);
		dkmsContentItemBean.setDkmsSlideList(dkmsContentItemList);
	
		return "/edukms/authorInterface/exerciseSheetEditor.xhtml";
	
}
	
	
	
	
}
