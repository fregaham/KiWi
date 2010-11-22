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
	
	
	
	
	public void listener(UploadEvent event) {

		UploadItem item = event.getUploadItem();

		log.info("File: '#0' with type '#1' was uploaded", item.getFileName(),
				item.getContentType());

		String name = FilenameUtils.getName(item.getFileName());
		String type = item.getContentType();
		

		if (item.isTempFile()) {
			try {

				File file = item.getFile();

				String repos = DkmsPropertiesReader
						.getProperty(DkmsPropertiesReader.FILE_REPOSITORY);
				String cache = DkmsPropertiesReader.getProperty(DkmsPropertiesReader.FILE_CACHE);

				try {
					name = "" + new Date().getTime() + "_" + name;
					DkmsFileService.copyFile(file, new File(repos + "/" + name));
				} catch (Exception e) {
					log.error("error copy file ... " + e.getMessage());
					e.printStackTrace();
				}

				// create the mini thumbnail image:
				//TODO: Werte aus property file auslesen
				Integer width = new Integer(100);
				Integer height = new Integer(75);
				File mini = new File(cache + "/" + width + "x" + height + "_"
						+ name);
				DkmsImageScaleService.createScaledFile(file, mini, width, height, true);
				
				// create the thumbnail image:
				width = new Integer(DkmsPropertiesReader
						.getProperty(DkmsPropertiesReader.THUMB_SIZE_X));
				height = new Integer(DkmsPropertiesReader
						.getProperty(DkmsPropertiesReader.THUMB_SIZE_Y));
				File thumb = new File(cache + "/" + width + "x" + height + "_"
						+ name);
				DkmsImageScaleService.createScaledFile(file, thumb, width, height, true);
				
				// create the mini preview image:
				width = new Integer(280);
				height = new Integer(150);
				File miniprev = new File(cache + "/" + width + "x" + height + "_"
						+ name);
				DkmsImageScaleService.createScaledFile(file, miniprev, width, height, true);

				// create the preview image:
				width = new Integer(DkmsPropertiesReader
						.getProperty(DkmsPropertiesReader.PREVIEW_SIZE_X));
				height = new Integer(DkmsPropertiesReader
						.getProperty(DkmsPropertiesReader.PREVIEW_SIZE_Y));
				File prev = new File(cache + "/" + width + "x" + height + "_"
						+ name);
				DkmsImageScaleService.createScaledFile(file, prev, width, height, true);

				// create the detail image:
				width = new Integer(DkmsPropertiesReader
						.getProperty(DkmsPropertiesReader.DETAIL_SIZE_X));
				height = new Integer(DkmsPropertiesReader
						.getProperty(DkmsPropertiesReader.DETAIL_SIZE_Y));
				File detail = new File(cache + "/" + width + "x" + height + "_"
						+ name);
				DkmsImageScaleService.createScaledFile(file, detail, width, height, true);

			} catch (IOException e) {
				log.error("error reading file #0", item.getFile()
						.getAbsolutePath());
			}
		}
		
		DkmsFileManager mt = new DkmsFileManager();
		mt.setMimeType(type);
		mt.setFileName(name);
		
		LinkedList<DkmsFileManager> dkmsContentItemList = dkmsContentItemBean.getDkmsMediaList();
		if(dkmsContentItemList == null){
			dkmsContentItemList = new LinkedList<DkmsFileManager>();
		}
		
		dkmsContentItemList.add(mt);
		dkmsContentItemBean.setDkmsMediaList(dkmsContentItemList);
	}
	
	
	@End
	public String changeDkmsContentItem() {
		
		selectedDkmsContentItem.setBigIdeas(dkmsBigIdeasAction.getChosenBigIdeas());
		selectedDkmsContentItem.setRepresentationType(dkmsRepresentationTypeAction.getChosenRepresentationTypes());
		dkmsContentItemBean.setDkmsContentItemType(selectedDkmsContentItem.getDkmsContentItemType());
		dkmsContentItemService.updateDkmsContentItem(selectedDkmsContentItem, dkmsContentItemBean);
		return "/edukms/authorInterface/userAdministrationArea.xhtml";
		
	}
	
	@End
	public String changeWikiContentItem() {
		
		selectedDkmsContentItem.setBigIdeas(dkmsBigIdeasAction.getChosenBigIdeas());
		selectedDkmsContentItem.setRepresentationType(dkmsRepresentationTypeAction.getChosenRepresentationTypes());
		dkmsContentItemBean.setDkmsContentItemType(selectedDkmsContentItem.getDkmsContentItemType());
		dkmsContentItemService.updateDkmsContentItem(selectedDkmsContentItem, dkmsContentItemBean);
		return "/edukms/abcmaths/lastUploadedResult.xhtml";
		
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
		return "/edukms/abcmaths/lastUploadedResult.xhtml";
		
	}	
	
	
	public String changeBlogContentItem() {
		
		selectedDkmsContentItem.setBigIdeas(dkmsBigIdeasAction.getChosenBigIdeas());
		selectedDkmsContentItem.setRepresentationType(dkmsRepresentationTypeAction.getChosenRepresentationTypes());
		dkmsContentItemBean.setDkmsContentItemType(selectedDkmsContentItem.getDkmsContentItemType());
		dkmsContentItemService.updateDkmsContentItem(selectedDkmsContentItem, dkmsContentItemBean);
		return "/edukms/abcmaths/blogPresentation.xhtml";
		
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
