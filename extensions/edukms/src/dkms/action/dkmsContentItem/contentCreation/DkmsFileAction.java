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
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;
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
	public void picListener(FileUploadEvent event) {

		UploadedFile item = event.getFile();

		log.info("File: '#0' with type '#1' was uploaded", item.getFileName(),
				item.getContentType());

		String name = FilenameUtils.getName(item.getFileName());
		String type = item.getContentType();

		log.info("File successfully read!");
				
				//TODO: Werte aus Properties lesen
				
				//lokalhost:
				//String repos = "C:/images/";				
				//String cache = "C:/images/cache/";
				
				//abcmaths-Server:
				String repos = "/home/abcmaths/images/";				
				String cache = "/home/abcmaths/images/cache/";

				
				
				File file = null;
				// store the orig in file repos:
				try {
					// create unique filename with timestamp+filename:
					name = "" + new Date().getTime() + "_" + name;
					file = DkmsFileService.copyFile(item.getContents(), new File(repos
							+ "/" + name));
				} catch (Exception e) {
					log.error("error copy file ... " + e.getMessage());
					e.printStackTrace();
				}

				try {
				// create shaped image:
				//TODO: Werte aus property file auslesen
				Integer width = new Integer(650);
				Integer height = new Integer(2000);
				File mini = new File(cache + "/" + width + "x" + height + "_"
						+ name);
				DkmsImageScaleService.createScaledFile(file, mini, width, height, true);				

				} catch (Exception e) {
					e.printStackTrace();
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
	
	
	public void slideListener(FileUploadEvent event) {

		UploadedFile item = event.getFile();

		log.info("File: '#0' with type '#1' was uploaded", item.getFileName(),
				item.getContentType());

		String name = FilenameUtils.getName(item.getFileName());
		String type = item.getContentType();

		log.info("File successfully read!");
				
				//TODO: Werte aus Properties lesen
				
				//localhost:
				//String repos = "C:/images/";				
				//String cache = "C:/images/cache/";
				
				//abcmaths-Server:
				String repos = "/home/abcmaths/images/";				
				String cache = "/home/abcmaths/images/cache/";

				
				
				File file = null;
				// store the orig in file repos:
				try {
					// create unique filename with timestamp+filename:
					name = "" + new Date().getTime() + "_" + name;
					file = DkmsFileService.copyFile(item.getContents(), new File(repos
							+ "/" + name));
				} catch (Exception e) {
					log.error("error copy file ... " + e.getMessage());
					e.printStackTrace();
				}

				try {

				// create the slideshow image:
				//TODO: Werte aus property file auslesen
				Integer width = new Integer(500);
				Integer height = new Integer(400);
				File mini = new File(cache + "/" + width + "x" + height + "_"
						+ name);
				DkmsImageScaleService.createScaledFile(file, mini, width, height, true);				

				} catch (Exception e) {
					e.printStackTrace();
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
	
	
	public void fileListener(FileUploadEvent event) {

		UploadedFile item = event.getFile();

		log.info("File: '#0' with type '#1' was uploaded", item.getFileName(),
				item.getContentType());

		String name = FilenameUtils.getName(item.getFileName());
		String type = item.getContentType();

		log.info("File successfully read!");

				//TODO: Werte aus Properties lesen
				
				//lokalhost:
				//String repos = "C:/images/";				
				//String cache = "C:/images/cache/";
				
				//abcmaths-Server:
				String repos = "/home/abcmaths/images/";				
				String cache = "/home/abcmaths/images/cache/";			
				
				File file = null;
				// store the orig in file repos:
				try {
					// create unique filename with timestamp+filename:
					name = "" + new Date().getTime() + "_" + name;
					file = DkmsFileService.copyFile(item.getContents(), new File(repos
							+ "/" + name));
				} catch (Exception e) {
					log.error("error copy file ... " + e.getMessage());
					e.printStackTrace();
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
	
	public void movieListener(FileUploadEvent event) {

		UploadedFile item = event.getFile();

		log.info("File: '#0' with type '#1' was uploaded", item.getFileName(),
				item.getContentType());

		String name = FilenameUtils.getName(item.getFileName());
		String type = item.getContentType();

		log.info("File successfully read!");

				//TODO: Werte aus Properties lesen
				
				//lokalhost:
				//String repos = "C:/images/";				
				//String cache = "C:/images/cache/";
				
				//abcmaths-Server:
				String repos = "/home/abcmaths/images/";				
				String cache = "/home/abcmaths/images/cache/";			
				
				File file = null;
				// store the orig in file repos:
				try {
					// create unique filename with timestamp+filename:
					name = "" + new Date().getTime() + "_" + name;
					file = DkmsFileService.copyFile(item.getContents(), new File(repos
							+ "/" + name));
				} catch (Exception e) {
					log.error("error copy file ... " + e.getMessage());
					e.printStackTrace();
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
		
		//dkmsContentItemBean.setAuthorName(currentUser.getLogin());
		dkmsContentItemBean.setAuthorName(currentUser.getFirstName() + " " + currentUser.getLastName());
		if (dkmsContentItemBean.getDkmsContentItemName().equals("")){
			dkmsContentItemBean.setDkmsContentItemName("No Title");
		}
		
		DkmsContentItemFacade dkmsContentItemFacade =  dkmsContentItemService.createDkmsContentItem(dkmsContentItemBean);
		dkmsContentItemFacade.setAuthor(currentUser);
		
		
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
		
		//dkmsContentItemBean.setAuthorName(currentUser.getLogin());
		dkmsContentItemBean.setAuthorName(currentUser.getFirstName() + " " + currentUser.getLastName());
		if (dkmsContentItemBean.getDkmsContentItemName().equals("")){
			dkmsContentItemBean.setDkmsContentItemName("No Title");
		}
		
		DkmsContentItemFacade dkmsContentItemFacade =  dkmsContentItemService.createDkmsContentItem(dkmsContentItemBean);
		dkmsContentItemFacade.setAuthor(currentUser);
		
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
