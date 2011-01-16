/*
	Copyright Â© dkms, artaround
 */
package dkms.action.dkmsContentItem;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;

import kiwi.api.content.ContentItemService;
import kiwi.api.entity.KiWiEntityManager;
import kiwi.model.user.User;

import org.apache.commons.io.FilenameUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.End;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.log.Log;
import org.richfaces.event.UploadEvent;
import org.richfaces.model.UploadItem;

import dkms.action.dkmsContentItem.categories.DkmsBigIdeasAction;
import dkms.action.dkmsContentItem.categories.DkmsRepresentationTypeAction;
import dkms.action.utils.DkmsFileService;
import dkms.action.utils.DkmsImageScaleService;
import dkms.action.utils.DkmsPropertiesReader;
import dkms.datamodel.dkmsContentItem.DkmsContentItemFacade;
import dkms.service.DkmsContentItemService;

@Name("dkmsWriteModusAction")
@Scope(ScopeType.CONVERSATION)
@Transactional
public class DkmsWriteModusAction {
	
	@In
	private DkmsContentItemFacade selectedDkmsContentItem;
	
	@In(create = true)
    private User currentUser;
	
	@In
	private ContentItemService contentItemService;
	
	@In(create = true)
	private DkmsContentItemBean dkmsContentItemBean;
	
	@In(required = false)
	private DkmsBigIdeasAction dkmsBigIdeasAction;
	
	@In(required = false)
	private DkmsRepresentationTypeAction dkmsRepresentationTypeAction;
	
	@In
    private KiWiEntityManager kiwiEntityManager;
	
	@In
	private DkmsContentItemService dkmsContentItemService;
	
	@Logger
	private static Log log;
	
	private int uploadsAvailable = 5;
	
	
	public int getUploadsAvailable() {
		return uploadsAvailable;
	}

	public void setUploadsAvailable(int uploadsAvailable) {
		this.uploadsAvailable = uploadsAvailable;
	}
	
	
	
	@End
	public String changeDkmsContentItem() {
		
		selectedDkmsContentItem.setBigIdeas(dkmsBigIdeasAction.getChosenBigIdeas());
		//selectedDkmsContentItem.setRepresentationType(dkmsRepresentationTypeAction.getChosenRepresentationTypes());
		if (dkmsContentItemBean.getDkmsContentItemName().equals("")){
			dkmsContentItemBean.setDkmsContentItemName("No Title");
		}
		dkmsContentItemBean.setDkmsContentItemType(selectedDkmsContentItem.getDkmsContentItemType());
		dkmsContentItemService.updateDkmsContentItem(selectedDkmsContentItem, dkmsContentItemBean);
		return "/edukms/authorInterface/userAdministrationArea.xhtml";
		
	}
	@End
	public String changeWikiContentItem() {
		
		selectedDkmsContentItem.setBigIdeas(dkmsBigIdeasAction.getChosenBigIdeas());
		if (dkmsContentItemBean.getDkmsContentItemName().equals("")){
			dkmsContentItemBean.setDkmsContentItemName("No Title");
		}
		//selectedDkmsContentItem.setRepresentationType(dkmsRepresentationTypeAction.getChosenRepresentationTypes());
		dkmsContentItemBean.setDkmsContentItemType(selectedDkmsContentItem.getDkmsContentItemType());
		dkmsContentItemService.updateDkmsContentItem(selectedDkmsContentItem, dkmsContentItemBean);
		return "/edukms/abcmaths/searchKMS.xhtml";
		
	}
	@End
	public String changeBlogContentItem() {
		
		selectedDkmsContentItem.setBigIdeas(dkmsBigIdeasAction.getChosenBigIdeas());
		if (dkmsContentItemBean.getDkmsContentItemName().equals("")){
			dkmsContentItemBean.setDkmsContentItemName("No Title");
		}
		//selectedDkmsContentItem.setRepresentationType(dkmsRepresentationTypeAction.getChosenRepresentationTypes());
		dkmsContentItemBean.setDkmsContentItemType(selectedDkmsContentItem.getDkmsContentItemType());
		dkmsContentItemService.updateDkmsContentItem(selectedDkmsContentItem, dkmsContentItemBean);
		return "/edukms/abcmaths/searchKMS.xhtml";
		
	}
	
	
	@End
	public String storeComment() {
		
		String version = (new Date()).toString();
		
		String commentContent = dkmsContentItemBean.getCommentText();
		
		String commentAuthor = currentUser.getLogin();
		
		
		DkmsBlogManager bm = new DkmsBlogManager();
		bm.setVersion(version);
		bm.setCommentContent(commentContent);
		//Author von altem (geändertem) commentContent
		bm.setCommentAuthor(commentAuthor);
		
		LinkedList<DkmsBlogManager> dkmsContentItemList = dkmsContentItemBean.getDkmsCommentList();
		if(dkmsContentItemList == null){
			dkmsContentItemList = new LinkedList<DkmsBlogManager>();
		}
		
		dkmsContentItemList.add(bm);
		dkmsContentItemBean.setDkmsCommentList(dkmsContentItemList);
		dkmsContentItemBean.setCommentText("");		
		
		selectedDkmsContentItem.setBigIdeas(dkmsBigIdeasAction.getChosenBigIdeas());
		selectedDkmsContentItem.setRepresentationType(dkmsRepresentationTypeAction.getChosenRepresentationTypes());
		dkmsContentItemBean.setDkmsContentItemType(selectedDkmsContentItem.getDkmsContentItemType());
		dkmsContentItemService.updateDkmsContentItem(selectedDkmsContentItem, dkmsContentItemBean);
		
//		if (dkmsContentItemBean.getDkmsContentItemType().equals("1")){
//			return "/edukms/abcmaths/exerciseSheetPresentation.xhtml";
//		}
//		else if (dkmsContentItemBean.getDkmsContentItemType().equals("2")){
//			return "/edukms/abcmaths/sequencePresentation.xhtml";
//		}
//		else if (dkmsContentItemBean.getDkmsContentItemType().equals("3")){
//			return "/edukms/abcmaths/documentPresentation.xhtml";
//		}
//		else if (dkmsContentItemBean.getDkmsContentItemType().equals("4")){
//			return "/edukms/abcmaths/wikiPresentation.xhtml";
//		}
//		else if (dkmsContentItemBean.getDkmsContentItemType().equals("5")){
//			return "/edukms/abcmaths/blogPresentation.xhtml";
//		}
//		else if (dkmsContentItemBean.getDkmsContentItemType().equals("6")){
//			return "/edukms/abcmaths/combinedResourcesPresentation.xhtml";
//		}
		return "/edukms/abcmaths/searchKMS.xhtml";
		
	}	
	
	
	
	
	public String trashDkmsContentItem() {
		
		selectedDkmsContentItem.setBigIdeas(dkmsBigIdeasAction.getChosenBigIdeas());	
		selectedDkmsContentItem.setRepresentationType(dkmsRepresentationTypeAction.getChosenRepresentationTypes());
		dkmsContentItemBean.setDkmsContentItemType(selectedDkmsContentItem.getDkmsContentItemType());
		dkmsContentItemBean.setTrashState(true);
		dkmsContentItemService.updateDkmsContentItem(selectedDkmsContentItem, dkmsContentItemBean);
		return "/edukms/authorInterface/userAdministrationArea.xhtml";
	}	
	
	public String reactivateDkmsContentItem() {
		
		selectedDkmsContentItem.setBigIdeas(dkmsBigIdeasAction.getChosenBigIdeas());				
		selectedDkmsContentItem.setRepresentationType(dkmsRepresentationTypeAction.getChosenRepresentationTypes());
		dkmsContentItemBean.setDkmsContentItemType(selectedDkmsContentItem.getDkmsContentItemType());
		dkmsContentItemBean.setTrashState(false);
		dkmsContentItemService.updateDkmsContentItem(selectedDkmsContentItem, dkmsContentItemBean);
		return "/edukms/authorInterface/userAdministrationArea.xhtml";
	}	
		
}
