/*
	Copyright © dkms, artaround
 */

package dkms.action.dkmsContentItem.contentCreation;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;

import kiwi.model.content.ContentItem;
import kiwi.model.ontology.SKOSConcept;
import kiwi.model.user.User;

import org.apache.commons.io.FilenameUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.End;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.TransactionPropagationType;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.log.Log;
import org.richfaces.event.UploadEvent;
import org.richfaces.model.UploadItem;

import dkms.action.dkmsContentItem.DkmsContentItemBean;
import dkms.action.dkmsContentItem.DkmsFileManager;
import dkms.action.dkmsContentItem.DkmsMovieManager;
import dkms.action.dkmsContentItem.DkmsSlideManager;
import dkms.action.dkmsContentItem.categories.DkmsBigIdeasAction;
import dkms.action.dkmsContentItem.categories.DkmsRepresentationTypeAction;
import dkms.action.utils.DkmsFileService;
import dkms.action.utils.DkmsImageScaleService;
import dkms.datamodel.dkmsContentItem.DkmsContentItemFacade;
import dkms.service.DkmsContentItemService;

@Name("dkmsFileAction")
@Scope(ScopeType.CONVERSATION)
@Transactional
public class DkmsFileAction {
	@Logger
	private static Log log;
	
	//check out the new created dkmsContentItem
	@Out(scope=ScopeType.SESSION, required = false)
	private ContentItem currentContentItem;
	
	@In
	private DkmsContentItemBean dkmsContentItemBean;
	
	@In(create = true)
    private User currentUser;

	private int uploadsAvailable = 20;
	
	@In 
	private DkmsContentItemService dkmsContentItemService;

	@In(required = false)
	private DkmsBigIdeasAction dkmsBigIdeasAction;
	
	@In(required = false)
	private DkmsRepresentationTypeAction dkmsRepresentationTypeAction;
		
	/**
	 *
	 * 
	 * @param event
	 */
	public void picListener(UploadEvent event) {

		UploadItem item = event.getUploadItem();

		log.info("File: '#0' with type '#1' was uploaded", item.getFileName(),
				item.getContentType());

		String name = FilenameUtils.getName(item.getFileName());
		String type = item.getContentType();
		

		if (item.isTempFile()) {
			try {				

				File file = item.getFile();
				
				//TODO: Werte aus Properties lesen
				
				//lokalhost:
				//String repos = "D:/dkms/images/";				
				//String cache = "D:/dkms/images/cache/";
				
				//abcmaths-Server:
				String repos = "/home/abcmaths/images/";				
				String cache = "/home/abcmaths/images/cache/";

				
				
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
	
	
	public void slideListener(UploadEvent event) {

		UploadItem item = event.getUploadItem();

		log.info("File: '#0' with type '#1' was uploaded", item.getFileName(),
				item.getContentType());

		String name = FilenameUtils.getName(item.getFileName());
		String type = item.getContentType();
		

		if (item.isTempFile()) {
			try {				

				File file = item.getFile();
				
				//TODO: Werte aus Properties lesen
				
				//localhost:
//				String repos = "I:/dkms/images/";				
//				String cache = "I:/dkms/images/cache/";
				
				//abcmaths-Server:
				String repos = "/home/abcmaths/images/";				
				String cache = "/home/abcmaths/images/cache/";

				
				
				try {
					
					name = "" + new Date().getTime() + "_" + name;
					DkmsFileService.copyFile(file, new File(repos + "/" + name));
				} catch (Exception e) {
					log.error("error copy file ... " + e.getMessage());
					e.printStackTrace();
				}

				// create the slideshow image:
				//TODO: Werte aus property file auslesen
				Integer width = new Integer(500);
				Integer height = new Integer(400);
				File mini = new File(cache + "/" + width + "x" + height + "_"
						+ name);
				DkmsImageScaleService.createScaledFile(file, mini, width, height, true);				

			} catch (IOException e) {
				log.error("error reading file #0", item.getFile()
						.getAbsolutePath());
			}
		}		
		
		DkmsSlideManager slt = new DkmsSlideManager();
		slt.setMimeType(type);
		slt.setFileName(name);
		
		LinkedList<DkmsSlideManager> dkmsContentItemList = dkmsContentItemBean.getDkmsSlideList();
		if(dkmsContentItemList == null){
			dkmsContentItemList = new LinkedList<DkmsSlideManager>();
		}
		
		dkmsContentItemList.add(slt);
		dkmsContentItemBean.setDkmsSlideList(dkmsContentItemList);
	}
	
	
	public void fileListener(UploadEvent event) {

		UploadItem item = event.getUploadItem();

		log.info("File: '#0' with type '#1' was uploaded", item.getFileName(),
				item.getContentType());

		String name = FilenameUtils.getName(item.getFileName());
		String type = item.getContentType();
		

		if (item.isTempFile()) {
				

				File file = item.getFile();

				//TODO: Werte aus Properties lesen
				
				//lokalhost:
				//String repos = "D:/dkms/files/";				
				//String cache = "D:/dkms/files/cache/";
				
				//abcmaths-Server:
				String repos = "/home/abcmaths/images/";				
				String cache = "/home/abcmaths/images/cache/";			
				
				try {
					
					name = "" + new Date().getTime() + "_" + name;
					DkmsFileService.copyFile(file, new File(repos + "/" + name));
				} catch (Exception e) {
					log.error("error copy file ... " + e.getMessage());
					e.printStackTrace();
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
	
	public void movieListener(UploadEvent event) {

		UploadItem item = event.getUploadItem();

		log.info("File: '#0' with type '#1' was uploaded", item.getFileName(),
				item.getContentType());

		String name = FilenameUtils.getName(item.getFileName());
		String type = item.getContentType(); 
		

		if (item.isTempFile()) {
		
				

				File file = item.getFile();

				//TODO: Werte aus Properties lesen
				
				//lokalhost:
//				String repos = "I:/dkms/files/";				
//				String cache = "I:/dkms/files/cache/";
				
				//abcmaths-Server:
				String repos = "/home/abcmaths/images/";				
				String cache = "/home/abcmaths/images/cache/";			
				
				try {
					
					name = "" + new Date().getTime() + "_" + name;
					DkmsFileService.copyFile(file, new File(repos + "/" + name));
				} catch (Exception e) {
					log.error("error copy file ... " + e.getMessage());
					e.printStackTrace();
				}

			
		}		
		
		DkmsMovieManager movt = new DkmsMovieManager();
		movt.setMimeType(type);
		movt.setFileName(name);
		
		LinkedList<DkmsMovieManager> dkmsContentItemList = dkmsContentItemBean.getDkmsMovieList();
		if(dkmsContentItemList == null){
			dkmsContentItemList = new LinkedList<DkmsMovieManager>();
		}
		
		dkmsContentItemList.add(movt);
		dkmsContentItemBean.setDkmsMovieList(dkmsContentItemList);
	}
	
	@Transactional(TransactionPropagationType.REQUIRED)
	@End(root = true, beforeRedirect = true) 
	public String storeDkmsContentItem(){
		
		dkmsContentItemBean.setAuthorName(currentUser.getLogin());
		
		DkmsContentItemFacade dkmsContentItemFacade =  dkmsContentItemService.createDkmsContentItem(dkmsContentItemBean);
		
		if(dkmsBigIdeasAction != null){
				addBigIdeas(dkmsContentItemFacade);
		}
		if(dkmsRepresentationTypeAction != null){
			addRepresentationType(dkmsContentItemFacade);
		}
		
		
		currentContentItem = dkmsContentItemFacade.getDelegate();
		return "/edukms/authorInterface/userAdministrationArea.xhtml";
	}
	
	@Transactional(TransactionPropagationType.REQUIRED)
	@End(root = true, beforeRedirect = true) 
	public String storeInfoPlatformContentItem(){
		
		DkmsContentItemFacade dkmsContentItemFacade =  dkmsContentItemService.createDkmsContentItem(dkmsContentItemBean);
		
		if(dkmsBigIdeasAction != null){
				addBigIdeas(dkmsContentItemFacade);
		}
		if(dkmsRepresentationTypeAction != null){
			addRepresentationType(dkmsContentItemFacade);
		}
		
		currentContentItem = dkmsContentItemFacade.getDelegate();
		return "/edukms/abcmaths/home.xhtml";
	}

	/**
	 * @param dkmsContentItemFacade
	 */
	private void addBigIdeas(DkmsContentItemFacade dkmsContentItemFacade) {
		LinkedList<SKOSConcept> bigIdeas = dkmsContentItemFacade.getBigIdeas();
		if(bigIdeas == null){
			bigIdeas = new LinkedList<SKOSConcept>();
		}
		
		LinkedList<SKOSConcept> chosenBigIdeas =  dkmsBigIdeasAction.getChosenBigIdeas();
		
		for (SKOSConcept skosConcept : chosenBigIdeas) {
			bigIdeas.add(skosConcept);
		}
		
		dkmsContentItemFacade.setBigIdeas(bigIdeas);
	}
	
	private void addRepresentationType(DkmsContentItemFacade dkmsContentItemFacade) {
		LinkedList<SKOSConcept> representationType = dkmsContentItemFacade.getRepresentationType();
		if(representationType == null){
			representationType = new LinkedList<SKOSConcept>();
		}
		
		LinkedList<SKOSConcept> chosenRepresentationType =  dkmsRepresentationTypeAction.getChosenRepresentationTypes();
		
		for (SKOSConcept skosConcept : chosenRepresentationType) {
			representationType.add(skosConcept);
		}
		
		dkmsContentItemFacade.setRepresentationType(representationType);
	}


	public int getUploadsAvailable() {
		return uploadsAvailable;
	}

	public void setUploadsAvailable(int uploadsAvailable) {
		this.uploadsAvailable = uploadsAvailable;
	}

}
