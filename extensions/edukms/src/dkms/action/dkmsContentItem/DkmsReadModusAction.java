/*
	Copyright Â© dkms, artaround
 */
package dkms.action.dkmsContentItem;

import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import kiwi.api.entity.KiWiEntityManager;
import kiwi.api.equity.EquityService;
import kiwi.model.content.ContentItem;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Events;
import org.jboss.seam.log.Log;

import dkms.action.utils.DkmsPropertiesReader;
import dkms.datamodel.dkmsContentItem.DkmsContentItemFacade;
import dkms.datamodel.dkmsContentItem.DkmsMultimediaFacade;
import dkms.service.DkmsContentItemService;
import dkms.service.DkmsImageService;

@Name("dkmsReadModusAction")
@Scope(ScopeType.CONVERSATION)
public class DkmsReadModusAction {

	@In(create = true)
	private DkmsImageService dkmsImageService;
	
	@In(required = false)
	private DkmsContentItemService dkmsContentItemService;

	@Logger
	private Log log;

	@In
	private EquityService equityService;
	
	private List<DkmsContentItemFacade> dkmsContentItems;
	private List<DkmsContentItemFacade> allDkmsContentItems;
	
	private List<DkmsContentItemFacade> resultList;
	
	@In(create = true)
	private DkmsContentItemBean dkmsContentItemBean;
	
	@In
    private KiWiEntityManager kiwiEntityManager;
	
	@Out(required = false)
	private DkmsContentItemFacade selectedDkmsContentItem;
	
	/**
	 * @param dkmsContentItem
	 * @return Html Link
	 */
	
	
	public String editExerciseSheetContent(DkmsContentItemFacade dkmsContentItem){
		 dkmsContentItemBean.init(dkmsContentItem);	
		 selectedDkmsContentItem = dkmsContentItem;
		 selectedDkmsContentItem.setContentItemState("C");
		 dkmsContentItemBean.setContentItemState("C");
		 return "/edukms/authorInterface/exerciseSheetEditor.xhtml";
	}
	
	
	public String editWikiContent(){
		 dkmsContentItemBean.setContentItemState("C");
		 return "/edukms/abcmaths/wikiEditor.xhtml";
	}
	
	
	public String insertBlogComment(){	
		 dkmsContentItemBean.setContentItemState("C");
		 return "/edukms/abcmaths/blogInsertionEditor.xhtml";
	}
	
	
	public String insertComment(){	
		 dkmsContentItemBean.setContentItemState("C");
		 return "/edukms/abcmaths/commentEditor.xhtml";
	}
	
	
	public String editSequence(DkmsContentItemFacade dkmsContentItem){
		 dkmsContentItemBean.init(dkmsContentItem);	
		 selectedDkmsContentItem = dkmsContentItem;
		 selectedDkmsContentItem.setContentItemState("C");
		 dkmsContentItemBean.setContentItemState("C");
		
		 return "/edukms/authorInterface/sequenceController.xhtml";
	}
	
	public String editDocument(DkmsContentItemFacade dkmsContentItem){
		 dkmsContentItemBean.init(dkmsContentItem);	
		 selectedDkmsContentItem = dkmsContentItem;
		 selectedDkmsContentItem.setContentItemState("C");
		 dkmsContentItemBean.setContentItemState("C");
		
		 return "/edukms/authorInterface/documentUploadEditor.xhtml";
	}
	
	public String editCombinedResource(DkmsContentItemFacade dkmsContentItem){
		 dkmsContentItemBean.init(dkmsContentItem);	
		 selectedDkmsContentItem = dkmsContentItem;
		 selectedDkmsContentItem.setContentItemState("C");
		 dkmsContentItemBean.setContentItemState("C");		 	 
		 return "/edukms/authorInterface/combineRessourcesController.xhtml";
	}
	
	
	
	public String exitSequenceChangeController(){
		
		 return "/edukms/authorInterface/userAdministrationArea.xhtml";
	}
	
	@Begin(join=true)
	public String showExerciseSheetDetails(DkmsContentItemFacade dkmsContentItem){
		 dkmsContentItemBean.init(dkmsContentItem);
		 dkmsContentItemBean.setContentItemStateBuffer("1");
		 selectedDkmsContentItem = dkmsContentItem;
		 return "/edukms/authorInterface/exerciseSheetShowDetails.xhtml";
	}
	
	@Begin(join=true)
	public String showExerciseSheetFromCombined(DkmsContentItemFacade contentItem, String identifier){
		dkmsContentItemBean.init(contentItem);
		dkmsContentItemBean.setTmpContentItem(identifier);
		dkmsContentItemBean.setContentItemStateBuffer("6");
		selectedDkmsContentItem = contentItem;		 
		 	 
		return "/edukms/authorInterface/exerciseSheetShowDetails.xhtml";
	}
	
	@Begin(join=true)
	public String showExerciseSheetPresentation(DkmsContentItemFacade dkmsContentItem){
		 dkmsContentItemBean.init(dkmsContentItem);	
		 selectedDkmsContentItem = dkmsContentItem;
		 return "/edukms/abcmaths/exerciseSheetPresentation.xhtml";
	}
	
	@Begin(join=true)
	public String showWikiPresentation(DkmsContentItemFacade dkmsContentItem){
		 dkmsContentItemBean.init(dkmsContentItem);	
		 selectedDkmsContentItem = dkmsContentItem;
		 return "/edukms/abcmaths/wikiPresentation.xhtml";
	}
	
	@Begin(join=true)
	public String showBlogPresentation(DkmsContentItemFacade dkmsContentItem){
		 dkmsContentItemBean.init(dkmsContentItem);	
		 selectedDkmsContentItem = dkmsContentItem;
		 return "/edukms/abcmaths/blogPresentation.xhtml";
	}
	
	@Begin(join=true)
	public String showSequenceDetails(DkmsContentItemFacade dkmsContentItem){
		 dkmsContentItemBean.init(dkmsContentItem);	
		 dkmsContentItemBean.setContentItemStateBuffer("2");
		 selectedDkmsContentItem = dkmsContentItem;
		 return "/edukms/authorInterface/sequenceShowDetails.xhtml";
	}
	
	
	@Begin(join=true)
	public String showSequenceFromCombined(DkmsContentItemFacade contentItem, String identifier){
		dkmsContentItemBean.init(contentItem);
		dkmsContentItemBean.setTmpContentItem(identifier);
		dkmsContentItemBean.setContentItemStateBuffer("6");
		selectedDkmsContentItem = contentItem;		 
		 	 
		 return "/edukms/authorInterface/sequenceShowDetails.xhtml";
	}
		
	public String showSequenceChangeDetails(){
		 
		 return "/edukms/authorInterface/sequenceShowChangeDetails.xhtml";
	}
	
	@Begin(join=true)
	public String showSequencePresentation(DkmsContentItemFacade dkmsContentItem){
		 dkmsContentItemBean.init(dkmsContentItem);	
		 selectedDkmsContentItem = dkmsContentItem;
		 return "/edukms/abcmaths/sequencePresentation.xhtml";
	}
	
	@Begin(join=true)
	public String showCombinedResourcesPresentation(DkmsContentItemFacade dkmsContentItem){
		 dkmsContentItemBean.init(dkmsContentItem);	
		 selectedDkmsContentItem = dkmsContentItem;
		 return "/edukms/abcmaths/combinedResourcesPresentation.xhtml";
	}
	
	@Begin(join=true)
	public String showDocumentDetails(DkmsContentItemFacade dkmsContentItem){
		 dkmsContentItemBean.init(dkmsContentItem);	
		 dkmsContentItemBean.setContentItemStateBuffer("3");
		 selectedDkmsContentItem = dkmsContentItem;
		 return "/edukms/authorInterface/documentShowDetails.xhtml";
	}
	
	@Begin(join=true)
	public String showCombinedResourceDetails(DkmsContentItemFacade dkmsContentItem){
		 dkmsContentItemBean.init(dkmsContentItem);	
		 selectedDkmsContentItem = dkmsContentItem;
		 return "/edukms/authorInterface/combinedResourceShowDetails.xhtml";
	}
	
	
	@Begin(join=true)
	public String showDocumentFromCombined(DkmsContentItemFacade contentItem, String identifier){
		dkmsContentItemBean.init(contentItem);
		dkmsContentItemBean.setTmpContentItem(identifier);
		dkmsContentItemBean.setContentItemStateBuffer("6");
		selectedDkmsContentItem = contentItem;		 
		 	 
		return "/edukms/authorInterface/documentShowDetails.xhtml";
	}
		
	@Begin(join=true)
	public String showDocumentPresentation(DkmsContentItemFacade dkmsContentItem){
		 dkmsContentItemBean.init(dkmsContentItem);	
		 selectedDkmsContentItem = dkmsContentItem;
		 return "/edukms/abcmaths/documentPresentation.xhtml";
	}
	
	public double getEquityValue(ContentItem ci){
		return equityService.getContentItemEquity(ci);
	}
	
	
	public String getQuickResources(String queryType){
		
		if (queryType.equals("1")){
		
			List<DkmsContentItemFacade> dkmsContentItemList = dkmsContentItemService.getAllDkmsContentItems();
			resultList = new LinkedList<DkmsContentItemFacade>();
			
			for (DkmsContentItemFacade cifTmp : dkmsContentItemList) {
				
				if (cifTmp.getTrashState() == false) resultList.add(cifTmp);
			
			}
			
		
		}
		
		if (queryType.equals("2")){
			
			List<DkmsContentItemFacade> dkmsContentItemList = dkmsContentItemService.getNewestDkmsContentItems();
			resultList = new LinkedList<DkmsContentItemFacade>();
			
			for (DkmsContentItemFacade cifTmp : dkmsContentItemList) {
				
				if (cifTmp.getTrashState() == false) resultList.add(cifTmp);
			
			}
			
		
		}
		
		if (queryType.equals("3")){
			
			List<DkmsContentItemFacade> dkmsContentItemList = dkmsContentItemService.getAllWikiResources();
			resultList = new LinkedList<DkmsContentItemFacade>();
			
			for (DkmsContentItemFacade cifTmp : dkmsContentItemList) {
				
				if (cifTmp.getTrashState() == false) resultList.add(cifTmp);
			
			}
			
		
		}
		
		if (queryType.equals("4")){
			
			List<DkmsContentItemFacade> dkmsContentItemList = dkmsContentItemService.getAllBlogResources();
			resultList = new LinkedList<DkmsContentItemFacade>();
			
			for (DkmsContentItemFacade cifTmp : dkmsContentItemList) {
				
				if (cifTmp.getTrashState() == false) resultList.add(cifTmp);
			
			}
			
		
		}
		
		return "/edukms/abcmaths/quickSearchResult.xhtml";
	}
	
	public String showCombinedSimpleResource(String combinedItemId, String identifier){
		 
		
		List<DkmsContentItemFacade> dkmsContentItemList = dkmsContentItemService.getAllDkmsContentItems();
		
		String tmpCI = selectedDkmsContentItem.getKiwiIdentifier();
		
		for (DkmsContentItemFacade cifTmp : dkmsContentItemList) {
			if (cifTmp.getKiwiIdentifier().equals(combinedItemId)){
				
				dkmsContentItemBean.init(cifTmp);
				dkmsContentItemBean.setTmpContentItem(identifier);
				selectedDkmsContentItem = cifTmp;
							
			}
		}			
		 
		return "/edukms/abcmaths/combiSimpleResPresentation.xhtml";
	}
	
	public String showCombinedSegmentedResource(String combinedItemId, String identifier){
		 
		
		List<DkmsContentItemFacade> dkmsContentItemList = dkmsContentItemService.getAllDkmsContentItems();
		
		//String tmpCI = selectedDkmsContentItem.getKiwiIdentifier();
		
		for (DkmsContentItemFacade cifTmp : dkmsContentItemList) {
			if (cifTmp.getKiwiIdentifier().equals(combinedItemId)){
				
				dkmsContentItemBean.init(cifTmp);
				dkmsContentItemBean.setTmpContentItem(identifier);
				//selectedDkmsContentItem = cifTmp;
							
			}
		}			
		 
		return "/edukms/abcmaths/combiSegmentedResPresentation.xhtml";
	}
	
	public String showCombinedUploadedResource(String combinedItemId, String identifier){
		 
		
		List<DkmsContentItemFacade> dkmsContentItemList = dkmsContentItemService.getAllDkmsContentItems();
		
		String tmpCI = selectedDkmsContentItem.getKiwiIdentifier();
		
		for (DkmsContentItemFacade cifTmp : dkmsContentItemList) {
			if (cifTmp.getKiwiIdentifier().equals(combinedItemId)){
				
				dkmsContentItemBean.init(cifTmp);
				dkmsContentItemBean.setTmpContentItem(identifier);
				selectedDkmsContentItem = cifTmp;
							
			}
		}			
		 
		return "/edukms/abcmaths/combiUploadedResPresentation.xhtml";
	}
		
	
	
	public String goToCombinedResource(String identifier){

		List<DkmsContentItemFacade> dkmsContentItemList = dkmsContentItemService.getAllDkmsContentItems();
		
		String tmpCI = identifier;
		
		for (DkmsContentItemFacade cifTmp : dkmsContentItemList) {
			if (cifTmp.getKiwiIdentifier().equals(identifier)){
				
				dkmsContentItemBean.init(cifTmp);
				selectedDkmsContentItem = cifTmp;
								
			}
		}			
		
		return "/edukms/abcmaths/combinedResourcesPresentation.xhtml";
	}
	
	
	
	public String goToCombinedResourceController(String identifier){ 
		
		List<DkmsContentItemFacade> dkmsContentItemList = dkmsContentItemService.getAllDkmsContentItems();
		
		for (DkmsContentItemFacade cifTmp : dkmsContentItemList) {
			if (cifTmp.getKiwiIdentifier().equals(identifier)){
				
				dkmsContentItemBean.init(cifTmp);
				selectedDkmsContentItem = cifTmp;
								
			}
		}			
		
		//dkmsContentItemBean.setContentItemState("6");
		//selectedDkmsContentItem.setContentItemState("6");
    	return "/edukms/authorInterface/combineRessourcesController.xhtml";
    }
	
	
	
	
	public String showScrollPerspective(){
		
		String content = "";
		
		LinkedList<DkmsSequenceComponentManager> scTmp =  dkmsContentItemBean.getDkmsSequenceComponentList();
		
		if(scTmp != null){
			for (DkmsSequenceComponentManager sequenceComponentTmp : scTmp) {
				if (sequenceComponentTmp.getViewStatus()==0){
					content = content + "<b>" + sequenceComponentTmp.getTaskTitle() + "</b><br/>";
					content = content + sequenceComponentTmp.getSequenceContent() + "<br/><br/>";
				}
			}			
		}	
		 dkmsContentItemBean.setTaskContent(content);
		 return "/edukms/abcmaths/sequenceScrollPresentation.xhtml";
	}
	
	public String showCombiScrollPerspective(){
		
		String content = "";
		
		LinkedList<DkmsSequenceComponentManager> scTmp =  dkmsContentItemBean.getDkmsSequenceComponentList();
		
		if(scTmp != null){
			for (DkmsSequenceComponentManager sequenceComponentTmp : scTmp) {
				
				content = content + "<b>" + sequenceComponentTmp.getTaskTitle() + "</b><br/>";
				content = content + sequenceComponentTmp.getSequenceContent() + "<br/><br/>";
				
			}			
		}	
		 dkmsContentItemBean.setTaskContent(content);
		 return "/edukms/abcmaths/combiSegmentResScrollPresentation.xhtml";
	}	
	
	
	
	
	public String showTabPerspective(){
		 
		 return "/edukms/abcmaths/sequencePresentation.xhtml";
	}
	
	public String showCombiTabPerspective(){
		 
		 return "/edukms/abcmaths/combiSegmentedResPresentation.xhtml";
	}
	
	
	public String exitExerciseSheetDetails(){
		 return "/edukms/authorInterface/userAdministrationArea.xhtml";
	}
	
	public String selectDkmsContentItem(DkmsContentItemFacade dkmsContentItem){
		 dkmsContentItemBean.init(dkmsContentItem);	
		 selectedDkmsContentItem = dkmsContentItem;
		 return "/edukms/pages/frontend/dkmsContentItemDetails.xhtml";
	}
	
	public String trashDkmsContentItem(DkmsContentItemFacade dkmsContentItem){
		 dkmsContentItemBean.init(dkmsContentItem);	
		 selectedDkmsContentItem = dkmsContentItem;
		 return "/edukms/authorInterface/trashConfirmation.xhtml";
	}
	
	public String reactivateDkmsContentItem(DkmsContentItemFacade dkmsContentItem){
		 dkmsContentItemBean.init(dkmsContentItem);	
		 selectedDkmsContentItem = dkmsContentItem;
		 return "/edukms/authorInterface/reactivateConfirmation.xhtml";
	}
	
	public String deleteDkmsContentItem(DkmsContentItemFacade dkmsContentItem){
		// diese Funktion wird erst beim Entleeren des Papierkorbs ausgeführt
		dkmsContentItemBean.init(dkmsContentItem);	
		selectedDkmsContentItem = dkmsContentItem;
		kiwiEntityManager.remove(selectedDkmsContentItem.getDelegate());
		dkmsContentItemService.getDkmsContentItems().remove(selectedDkmsContentItem);
		return "/edukms/authorInterface/trashContainer.xhtml";		
	}
	
	public String previewExerciseSheet() {
		
		Events.instance().raiseEvent("onSaveEvent");

		return "/edukms/authorInterface/exerciseSheetPreview.xhtml";
	}
	
	public String exitExerciseSheetPreview() {
		

		return "/edukms/authorInterface/exerciseSheetEditor.xhtml";
	}
	
	public String previewExerciseChangeSheet() {
		

		return "/edukms/authorInterface/exerciseSheetChangePreview.xhtml";
	}
	
	public String exitExerciseSheetChangePreview() {
		

		return "/edukms/authorInterface/exerciseSheetChangeEditor.xhtml";
	}
	
	
	
	public String previewSequence() {
		
		Events.instance().raiseEvent("onSaveEvent");

		return "/edukms/authorInterface/sequencePreview.xhtml";
	}	
	
	public String exitSequencePreview() {
		

		return "/edukms/authorInterface/sequenceController.xhtml";
	}
	
	public String exitSequenceChangePreview() {
		

		return "/edukms/authorInterface/sequenceChangeController.xhtml";
	}
	
	public String exitTrashConfirmation(){
		 
		 return "/edukms/authorInterface/userAdministrationArea.xhtml";
	}
	
	public String goToTrashContainer(){
		 
		 return "/edukms/authorInterface/trashContainer.xhtml";
	}
	
	public String goToResultList(){
		 
		 return "/edukms/abcmaths/searchKMS.xhtml";
	}
	
	public String buyDkmsContentItem(){
		 
		 return "/edukms/pages/frontend/buyArtwork.xhtml";
	}
	
	
		
	public List<String> getDkmsContentItemThumbURLs() {

		List<String> filenames = new Vector<String>();
		List<DkmsContentItemFacade> artworks = getDkmsContentItems();
		for (DkmsContentItemFacade artwork : artworks) {
			List<DkmsMultimediaFacade> multimedias = artwork
					.getDkmsMediaList();
			for (DkmsMultimediaFacade multimedia : multimedias) {
				// FIXME: if (multimedia.isMaster()){
				String url = dkmsImageService.createImageURL(multimedia.getFilename(),
						new Integer(DkmsPropertiesReader
								.getProperty(DkmsPropertiesReader.THUMB_SIZE_X)),
						new Integer(DkmsPropertiesReader
								.getProperty(DkmsPropertiesReader.THUMB_SIZE_Y)));
				if (url != null) {
					filenames.add(url);
				}
				// }
			}
		}

		return filenames;

	}

	/**
	 * return the height of the wrapper div of a single artwork thumbnail
	 * 
	 * @return the height of the wrapper div of a single artwork thumbnail
	 */
	public String getDkmsContentItemWrapHeight() {
		Integer y = new Integer(DkmsPropertiesReader
				.getProperty(DkmsPropertiesReader.THUMB_SIZE_Y));
		y = (y * 120) / 100;
		return "" + y;
	}

	/**
	 * 
	 * 
	 * @return the width of the wrapper div of a single artwork thumbnail
	 */
	public String getDkmsContentItemWrapWidth() {
		Integer x = new Integer(DkmsPropertiesReader
				.getProperty(DkmsPropertiesReader.THUMB_SIZE_X));
		x = (x * 120) / 100;
		return "" + x;
	}

	/**
	 * 
	 * 
	 * @return the height of the wrapper div of a single artwork thumbnail
	 */
	public String getDkmsContentItemPrevHeight() {
		Integer y = new Integer(DkmsPropertiesReader
				.getProperty(DkmsPropertiesReader.PREVIEW_SIZE_Y));
		return "" + y;
	}

	/**
	 * 
	 * 
	 * @return the width of the wrapper div of a single artwork thumbnail
	 */
	public String getDkmsContentItemPrevWidth() {
		Integer x = new Integer(DkmsPropertiesReader
				.getProperty(DkmsPropertiesReader.PREVIEW_SIZE_X));
		return "" + x;
	}

	// TODO: implement query for the master image
	// //@End
	public String getMasterFileNameFromArtwork(DkmsContentItemFacade a) {
		LinkedList<DkmsMultimediaFacade> ll = new LinkedList<DkmsMultimediaFacade>();
		ll = a.getDkmsMediaList();

		if (ll.size() > 0) {
			DkmsMultimediaFacade mf = ll.getFirst();
			DkmsFileManager mTmp = new DkmsFileManager(mf.getMimeType(), mf.getFilename());
			return getMultimediaFilename(mTmp);
		}
		return null;
	}

	public String getPreviewMasterFileNameFromArtwork(DkmsContentItemFacade a) {
		LinkedList<DkmsMultimediaFacade> ll = new LinkedList<DkmsMultimediaFacade>();
		ll = a.getDkmsMediaList();

		if (ll.size() > 0) {
			DkmsMultimediaFacade mf = ll.getFirst();
			DkmsFileManager mTmp = new DkmsFileManager(mf.getMimeType(), mf.getFilename());	
			return getPreviewMultimediaFilename(mTmp);
		}
		return null;

	}

	public String getThumbMasterFileNameFromArtwork(DkmsContentItemFacade a) {

		log.info(a == null ? "null value" : "ok");

		LinkedList<DkmsMultimediaFacade> ll = new LinkedList<DkmsMultimediaFacade>();
		ll = a.getDkmsMediaList();		
		if (ll.size() > 0) {
			DkmsMultimediaFacade mf = ll.getFirst();
			DkmsFileManager mTmp = new DkmsFileManager(mf.getMimeType(), mf.getFilename());	
			return getThumbMultimediaFilename(mTmp);
		}
		return null;

	}

	//TODO: Pfad aus Property File und Server!!!
	public String getOriginalMultimediaFilename(DkmsFileManager multimedia) {
		//localhost
		//String path = "http://localhost:8080//KiWi/images/";	
		//abcmaths Server
		String path = "http://abcmaths.sbg.ac.at/images/";	

		
		return path + multimedia.getFileName();
	}
	
	public String getOriginalMultimediaFilenameShort(DkmsFileManager multimedia){		
		
		return multimedia.getFileName();
	}
	
	//TODO: Pfad aus Property File und Server!!!
	public String getSlideshowMultimediaFilename(DkmsSlideManager multimedia) {
		
		Integer width = new Integer(500);
		Integer height = new Integer(400);
		
		//localhost
		//String path = "http://localhost:8080//KiWi/images/";	
		//abcmaths Server
		String path = "http://abcmaths.sbg.ac.at/images/cache";	
        String pic = "/" + width + "x" + height + "_" + multimedia.getFileName();
		
		return path + pic;
	}
	
	
	
	//
	public String getMultimediaFilename(DkmsFileManager multimedia) {
		Integer width = new Integer(DkmsPropertiesReader
				.getProperty(DkmsPropertiesReader.DETAIL_SIZE_X));
		Integer height = new Integer(DkmsPropertiesReader
				.getProperty(DkmsPropertiesReader.DETAIL_SIZE_Y));
		return dkmsImageService.createImageURL(multimedia.getFileName(), width, height);
	}
	
	//
	public String getPreviewMultimediaFilename(DkmsFileManager multimedia) {
		Integer width = new Integer(DkmsPropertiesReader
				.getProperty(DkmsPropertiesReader.PREVIEW_SIZE_X));
		Integer height = new Integer(DkmsPropertiesReader
				.getProperty(DkmsPropertiesReader.PREVIEW_SIZE_Y));
		return dkmsImageService.createImageURL(multimedia.getFileName(), width, height);
	}
	//
	public String getThumbMultimediaFilename(DkmsFileManager multimedia) {
		Integer width = new Integer(DkmsPropertiesReader
				.getProperty(DkmsPropertiesReader.THUMB_SIZE_X));
		Integer height = new Integer(DkmsPropertiesReader
				.getProperty(DkmsPropertiesReader.THUMB_SIZE_Y));
		return dkmsImageService.createImageURL(multimedia.getFileName(), width, height);
	}

//	public String getMiniMultimediaFilename(DkmsFileManager multimedia) {
//		// TODO Werte von Property File auslesen
//		Integer width = new Integer(100);
//		Integer height = new Integer(75);
//		return dkmsImageService.createImageURL(multimedia.getFileName(), width, height);
//	}
	
	public String getShapedMultimediaFilename(DkmsFileManager multimedia) {
		// TODO Werte und path von Property File auslesen
		Integer width = new Integer(650);
		Integer height = new Integer(2000);
		
		//localhost
		//String path = "http://localhost:8080//KiWi/images/cache";	
		//abcmaths Server
		String path = "http://abcmaths.sbg.ac.at/images/cache/";	

		
		return path + width + "x" + height + "_"  + multimedia.getFileName();
		
		}
	
	public String getMiniMultimediaPicname(DkmsFileManager multimedia) {
		// TODO Werte und path von Property File auslesen
		Integer width = new Integer(100);
		Integer height = new Integer(75);
		
		//localhost
		//String path = "http://localhost:8080//KiWi/images/cache";	
		//abcmaths Server
		String path = "http://abcmaths.sbg.ac.at/images/cache/";	

		
		return path + width + "x" + height + "_"  + multimedia.getFileName();
		
		}
	

	public String getMiniPreviewMultimediaFilename(DkmsFileManager multimedia) {
		// TODO Werte von Property File auslesen
		Integer width = new Integer(280);
		Integer height = new Integer(150);
		return dkmsImageService.createImageURL(multimedia.getFileName(), width, height);
	}

	public List<DkmsContentItemFacade> getAllDkmsContentItems() {
		allDkmsContentItems = dkmsContentItemService.getAllDkmsContentItems();
		return allDkmsContentItems;
	}

	public List<DkmsContentItemFacade> getDkmsContentItems() {
		dkmsContentItems = dkmsContentItemService.getDkmsContentItems();
		return dkmsContentItems;
	}
	
	public List<DkmsContentItemFacade> getTrashedContentItems() {
		return dkmsContentItemService.getTrashedContentItems();
		
	}
	
	public List<DkmsContentItemFacade> getSimpleResources() {
		return dkmsContentItemService.getSimpleResources();
		
	}
	
	public List<DkmsContentItemFacade> getSegmentedResources() {
		return dkmsContentItemService.getSegmentedResources();
		
	}
	
	public List<DkmsContentItemFacade> getUploadedResources() {
		return dkmsContentItemService.getUploadedResources();
		
	}
	
	public List<DkmsContentItemFacade> getCombinedResources() {
		return dkmsContentItemService.getCombinedResources();
	
	}
	
	public List<DkmsContentItemFacade> getAllWikiResources() {
		return dkmsContentItemService.getAllWikiResources();
	
	}
	
	public List<DkmsContentItemFacade> getAllBlogResources() {
		return dkmsContentItemService.getAllBlogResources();
	
	}
	
	public List<DkmsContentItemFacade> getNewestDkmsContentItems() {
		return dkmsContentItemService.getNewestDkmsContentItems();
	
	}
	
	public List<DkmsContentItemFacade> getResultList() {
		return resultList;
	}

	public void setResultList(List<DkmsContentItemFacade> resultList) {
		this.resultList = resultList;
	}
	
	
}
