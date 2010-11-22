/*
	Copyright © dkms, artaround
 */

package dkms.service;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import kiwi.api.content.ContentItemService;
import kiwi.api.entity.KiWiEntityManager;
import kiwi.api.search.KiWiSearchCriteria;
import kiwi.api.search.KiWiSearchResults;
import kiwi.api.search.SolrService;
import kiwi.api.search.KiWiSearchResults.SearchResult;
import kiwi.api.triplestore.TripleStore;
import kiwi.model.Constants;
import kiwi.model.content.ContentItem;
import kiwi.model.user.User;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.log.Log;

import dkms.action.dkmsContentItem.DkmsBlogManager;
import dkms.action.dkmsContentItem.DkmsCombinedComponentManager;
import dkms.action.dkmsContentItem.DkmsContentItemBean;
import dkms.action.dkmsContentItem.DkmsFileManager;
import dkms.action.dkmsContentItem.DkmsMovieManager;
import dkms.action.dkmsContentItem.DkmsSequenceComponentManager;
import dkms.action.dkmsContentItem.DkmsSlideManager;
import dkms.action.dkmsContentItem.DkmsWikiManager;
import dkms.action.utils.DkmsLastSubmittedComparator;
import dkms.datamodel.dkmsContentItem.DkmsBlogFacade;
import dkms.datamodel.dkmsContentItem.DkmsCombinedComponentFacade;
import dkms.datamodel.dkmsContentItem.DkmsContentItemFacade;
import dkms.datamodel.dkmsContentItem.DkmsMovieFacade;
import dkms.datamodel.dkmsContentItem.DkmsMultimediaFacade;
import dkms.datamodel.dkmsContentItem.DkmsSequenceComponentFacade;
import dkms.datamodel.dkmsContentItem.DkmsSlideFacade;
import dkms.datamodel.dkmsContentItem.DkmsWikiFacade;



@Scope(ScopeType.STATELESS)
@Name("dkmsContentItemService")
@Transactional
@AutoCreate
public class DkmsContentItemService {
	
	@In
	private TripleStore tripleStore;
	
	@In
    private KiWiEntityManager kiwiEntityManager;
	
	@In
	private SolrService solrService;
	
	@In(required = false)
	private Comparator<DkmsContentItemFacade> comp;
	
    @In(create = true)
    private User currentUser;
    
	@In
	private ContentItemService contentItemService;
		

    @Logger
    private Log log;
	
	public List<DkmsContentItemFacade> getAllDkmsContentItems(){
		KiWiSearchCriteria criteria = new KiWiSearchCriteria();
		criteria.getTypes().add(tripleStore.createUriResource(Constants.DKMS_CORE + "DkmsContentItem").getKiwiIdentifier());
		criteria.setLimit(Integer.MAX_VALUE);
		List <DkmsContentItemFacade> allDkmsContentItems = getDkmsContentItemsWithCriteria(criteria);		
		return allDkmsContentItems;
	}
	
	/**
	 * This class <b>returns all DkmsContentItems</b>
	 * @param no parameters
	 * @return returns all ArtWors
	 */
	public List<DkmsContentItemFacade> getDkmsContentItems(){
		KiWiSearchCriteria criteria = new KiWiSearchCriteria();
		criteria.getTypes().add(tripleStore.createUriResource(Constants.DKMS_CORE + "DkmsContentItem").getKiwiIdentifier());
		criteria.setPerson(currentUser.getLogin());
		criteria.setLimit(Integer.MAX_VALUE);
		List <DkmsContentItemFacade> dkmsContentItems = getDkmsContentItemsWithCriteria(criteria);	
		return dkmsContentItems;
	}
	
	private List<DkmsContentItemFacade> getDkmsContentItemsWithCriteria(KiWiSearchCriteria criteria){
		List <DkmsContentItemFacade> allDkmsContentItems = new LinkedList<DkmsContentItemFacade>();
		KiWiSearchResults results = solrService.search(criteria);
		//log.info("Number of DkmsContentItems: "+results.getResultCount());
		
		for(SearchResult r : results.getResults()) {
			if(r.getItem() != null) {
				DkmsContentItemFacade i = kiwiEntityManager.createFacade(r.getItem(),
						DkmsContentItemFacade.class);
				
				//log.info("TimeStamp: #0", i.getCreated()+" "+i.getModified());
				allDkmsContentItems.add(i);		
			}
		}
		
		//log.info(comp == null?"comp is null":"comp is not null");
		if(comp == null){
			comp = new DkmsLastSubmittedComparator();
		}
		Collections.sort(allDkmsContentItems,comp);
		return allDkmsContentItems;
	}
	
	
	//updates an existing DkmsContentItem by values hold in a given DkmsContentItemBean
	public void updateDkmsContentItem(DkmsContentItemFacade dkmsContentItemFacade, DkmsContentItemBean dkmsContentItemBean){
						
		Date d = new Date();
		dkmsContentItemFacade.setModified(d);
		dkmsContentItemBean.setLastUpdate(d.toString());

		createDkmsContentItem(dkmsContentItemBean, dkmsContentItemFacade);				
	}
	
	//creates a new DkmsContentItem by values hold in a given DkmsContentItemBean
	public DkmsContentItemFacade createDkmsContentItem(DkmsContentItemBean dkmsContentItemBean){		
		final ContentItem dkmsContentItemItem = contentItemService.createContentItem();
		DkmsContentItemFacade dkmsContentItem = kiwiEntityManager.createFacade(dkmsContentItemItem, DkmsContentItemFacade.class);		
		return createDkmsContentItem(dkmsContentItemBean, dkmsContentItem);		
	}

	/**
	 * @param dkmsContentItemBean
	 * @param dkmsContentItem
	 */
	private DkmsContentItemFacade createDkmsContentItem(DkmsContentItemBean dkmsContentItemBean, DkmsContentItemFacade dkmsContentItem) {
		contentItemService.updateTitle(dkmsContentItem, dkmsContentItemBean.getDkmsContentItemName());
		dkmsContentItem.setAuthor(currentUser);
		
		dkmsContentItem.setDescription(dkmsContentItemBean.getDescription());
		dkmsContentItem.setCommentText(dkmsContentItemBean.getCommentText());

		dkmsContentItem.setPublicAccess(dkmsContentItemBean.getPublicAccess());
		
		dkmsContentItem.setDkmsContentItemTextList(dkmsContentItemBean.getDkmsContentItemTextList());		
		dkmsContentItem.setTrashState(dkmsContentItemBean.getTrashState());
		dkmsContentItem.setAuthorName(dkmsContentItemBean.getAuthorName());
		
		dkmsContentItem.setExerciseSheetContent(dkmsContentItemBean.getExerciseSheetContent());	
		
		dkmsContentItem.setTaskContent(dkmsContentItemBean.getTaskContent());
		
		dkmsContentItem.setTaskTitle(dkmsContentItemBean.getTaskTitle());		
				
		dkmsContentItem.setActiveTask(dkmsContentItemBean.getActiveTask());	
		
		dkmsContentItem.setSeqTaskState(dkmsContentItemBean.getSeqTaskState());		
				
		dkmsContentItem.setContentItemState(dkmsContentItemBean.getContentItemState());
	
		
		dkmsContentItem.setLatitude(dkmsContentItemBean.getLatitude());
		dkmsContentItem.setLongitude(dkmsContentItemBean.getLongitude());
		
		dkmsContentItem.setDkmsContentItemType(dkmsContentItemBean.getDkmsContentItemType());
		
		dkmsContentItem.setTmpContentItem(dkmsContentItemBean.getTmpContentItem());
		
		dkmsContentItem.setContentItemIdentifier(dkmsContentItem.getKiwiIdentifier());
		
		dkmsContentItem.setLastUpdate(new Date().toString());
		
		
		
		LinkedList<DkmsFileManager> mTmp =  dkmsContentItemBean.getDkmsMediaList();
		LinkedList<DkmsMultimediaFacade> aml = new LinkedList<DkmsMultimediaFacade>();
		if(mTmp != null){
			for (DkmsFileManager mediaTmp : mTmp) {
				//create all contentItems according to the values of mTmp
				final ContentItem DkmsFileManager = contentItemService.createContentItem();
				DkmsMultimediaFacade dkmsMedia = kiwiEntityManager.createFacade(DkmsFileManager, DkmsMultimediaFacade.class);
				dkmsMedia.setFilename(mediaTmp.getFileName());
				dkmsMedia.setMimeType(mediaTmp.getMimeType());

				aml.add(dkmsMedia);

			}
			//Add MultimediaFacase to DkmsContentItemFacade
			dkmsContentItem.setDkmsMediaList(aml);
		}
		
		LinkedList<DkmsWikiManager> wTmp =  dkmsContentItemBean.getDkmsWikiList();
		LinkedList<DkmsWikiFacade> wml = new LinkedList<DkmsWikiFacade>();
		if(wTmp != null){
			for (DkmsWikiManager wikiTmp : wTmp) {
				//create all contentItems according to the values of wTmp
				final ContentItem DkmsWikiManager = contentItemService.createContentItem();
				DkmsWikiFacade dkmsWiki = kiwiEntityManager.createFacade(DkmsWikiManager, DkmsWikiFacade.class);
				dkmsWiki.setWikiContent(wikiTmp.getWikiContent());
				dkmsWiki.setVersion(wikiTmp.getVersion());
				dkmsWiki.setWikiAuthor(wikiTmp.getWikiAuthor());

				wml.add(dkmsWiki);

			}
			//Add WikiFacade to DkmsContentItemFacade
			dkmsContentItem.setDkmsWikiList(wml);
		}
		
		LinkedList<DkmsBlogManager> bTmp =  dkmsContentItemBean.getDkmsCommentList();
		LinkedList<DkmsBlogFacade> bml = new LinkedList<DkmsBlogFacade>();
		if(bTmp != null){
			for (DkmsBlogManager blogTmp : bTmp) {
				//create all contentItems according to the values of wTmp
				final ContentItem DkmsBlogManager = contentItemService.createContentItem();
				DkmsBlogFacade dkmsBlog = kiwiEntityManager.createFacade(DkmsBlogManager, DkmsBlogFacade.class);
				dkmsBlog.setCommentContent(blogTmp.getCommentContent());
				dkmsBlog.setVersion(blogTmp.getVersion());
				dkmsBlog.setCommentAuthor(blogTmp.getCommentAuthor());

				bml.add(dkmsBlog);

			}
			//Add BlogFacade to DkmsContentItemFacade
			dkmsContentItem.setDkmsCommentList(bml);
		}
		
		LinkedList<DkmsSlideManager> slTmp =  dkmsContentItemBean.getDkmsSlideList();
		LinkedList<DkmsSlideFacade> slml = new LinkedList<DkmsSlideFacade>();
		if(slTmp != null){
			for (DkmsSlideManager slideTmp : slTmp) {
				//create all contentItems according to the values of slTmp
				final ContentItem DkmsSlideManager = contentItemService.createContentItem();
				DkmsSlideFacade dkmsSlide = kiwiEntityManager.createFacade(DkmsSlideManager, DkmsSlideFacade.class);
				dkmsSlide.setFilename(slideTmp.getFileName());
				dkmsSlide.setMimeType(slideTmp.getMimeType());

				slml.add(dkmsSlide);

			}
			//Add SlideFacade to DkmsContentItemFacade
			dkmsContentItem.setDkmsSlideList(slml);
		}
		
		LinkedList<DkmsMovieManager> mvTmp =  dkmsContentItemBean.getDkmsMovieList();
		LinkedList<DkmsMovieFacade> mvml = new LinkedList<DkmsMovieFacade>();
		if(mvTmp != null){
			for (DkmsMovieManager movieTmp : mvTmp) {
				//create all contentItems according to the values of mTmp
				final ContentItem DkmsMovieManager = contentItemService.createContentItem();
				DkmsMovieFacade dkmsMovie = kiwiEntityManager.createFacade(DkmsMovieManager, DkmsMovieFacade.class);
				dkmsMovie.setFilename(movieTmp.getFileName());
				dkmsMovie.setMimeType(movieTmp.getMimeType());

				mvml.add(dkmsMovie);

			}
			//Add MovieFacade to DkmsContentItemFacade
			dkmsContentItem.setDkmsMovieList(mvml);
		}
		
		LinkedList<DkmsSequenceComponentManager> scTmp =  dkmsContentItemBean.getDkmsSequenceComponentList();
		LinkedList<DkmsSequenceComponentFacade> scml = new LinkedList<DkmsSequenceComponentFacade>();
		if(scTmp != null){
			for (DkmsSequenceComponentManager sequenceComponentTmp : scTmp) {
				//create all contentItems according to the values of scTmp
				final ContentItem DkmsSequenceComponentManager = contentItemService.createContentItem();
				DkmsSequenceComponentFacade dkmsSequenceComponent = kiwiEntityManager.createFacade(DkmsSequenceComponentManager, DkmsSequenceComponentFacade.class);
				dkmsSequenceComponent.setTaskId(sequenceComponentTmp.getTaskId());
				dkmsSequenceComponent.setTaskTitle(sequenceComponentTmp.getTaskTitle());
				dkmsSequenceComponent.setSequenceContent(sequenceComponentTmp.getSequenceContent());
				dkmsSequenceComponent.setVersion(sequenceComponentTmp.getVersion());				

				scml.add(dkmsSequenceComponent);

			}
			//Add SequenceComponentFacade to DkmsContentItemFacade
			dkmsContentItem.setDkmsSequenceComponentList(scml);
		}
		
		LinkedList<DkmsCombinedComponentManager> combTmp =  dkmsContentItemBean.getDkmsCombinedComponentList();
		LinkedList<DkmsCombinedComponentFacade> combml = new LinkedList<DkmsCombinedComponentFacade>();
		if(combTmp != null){
			for (DkmsCombinedComponentManager combinedComponentTmp : combTmp) {
				//create all contentItems according to the values of combTmp
				final ContentItem DkmsCombinedComponentManager = contentItemService.createContentItem();
				DkmsCombinedComponentFacade dkmsCombinedComponent = kiwiEntityManager.createFacade(DkmsCombinedComponentManager, DkmsCombinedComponentFacade.class);
				//dkmsCombinedComponent.setCombinedItem(combinedComponentTmp.getCombinedItem());
				dkmsCombinedComponent.setCombinedItemId(combinedComponentTmp.getCombinedItemId());	
				dkmsCombinedComponent.setCombinedItemTitle(combinedComponentTmp.getCombinedItemTitle());
				dkmsCombinedComponent.setCombinedItemType(combinedComponentTmp.getCombinedItemType());
			

				combml.add(dkmsCombinedComponent);

			}
			//Add SequenceComponentFacade to DkmsContentItemFacade
			dkmsContentItem.setDkmsCombinedComponentList(combml);
		}
		
		kiwiEntityManager.persist(dkmsContentItem);	
		return dkmsContentItem;
	}
	


	
	
	public void deleteMultimediaFromDkmsContentItem(DkmsContentItemFacade dkmsContentItem, DkmsFileManager pic){
		LinkedList<DkmsMultimediaFacade> mf = dkmsContentItem.getDkmsMediaList();
		for (DkmsMultimediaFacade dkmsMultimediaFacade : mf) {
			if(dkmsMultimediaFacade.getFilename().equals(pic.getFileName())){
				mf.remove(dkmsMultimediaFacade);
				dkmsContentItem.setDkmsMediaList(mf);	
				kiwiEntityManager.remove(dkmsMultimediaFacade.getDelegate());
				return;
			}
		}				
	}
	
	public void deleteWikiContentFromDkmsContentItem(DkmsContentItemFacade dkmsContentItem, DkmsWikiManager wiki){
		LinkedList<DkmsWikiFacade> wf = dkmsContentItem.getDkmsWikiList();
		for (DkmsWikiFacade dkmsWikiFacade : wf) {
			if(dkmsWikiFacade.getWikiContent().equals(wiki.getWikiContent())){
				wf.remove(dkmsWikiFacade);
				dkmsContentItem.setDkmsWikiList(wf);	
				kiwiEntityManager.remove(dkmsWikiFacade.getDelegate());
				return;
			}
		}				
	}
	
	public void deleteSlideFromDkmsContentItem(DkmsContentItemFacade dkmsContentItem, DkmsSlideManager slide){
		LinkedList<DkmsSlideFacade> sf = dkmsContentItem.getDkmsSlideList();
		for (DkmsSlideFacade dkmsSlideFacade : sf) {
			if(dkmsSlideFacade.getFilename().equals(slide.getFileName())){
				sf.remove(dkmsSlideFacade);
				dkmsContentItem.setDkmsSlideList(sf);	
				kiwiEntityManager.remove(dkmsSlideFacade.getDelegate());
				return;
			}
		}				
	}
	
	public void deleteItemFromDkmsSequenceContentItem(DkmsContentItemFacade dkmsContentItem, DkmsSequenceComponentManager sequenceItem){
		LinkedList<DkmsSequenceComponentFacade> seq = dkmsContentItem.getDkmsSequenceComponentList();
		for (DkmsSequenceComponentFacade dkmsSequenceComponentFacade : seq) {
			if(dkmsSequenceComponentFacade.getSequenceItem().equals(sequenceItem.getSequenceItem())){
				seq.remove(dkmsSequenceComponentFacade);
				dkmsContentItem.setDkmsSequenceComponentList(seq);	
				kiwiEntityManager.remove(dkmsSequenceComponentFacade.getDelegate());
				return;
			}
		}				
	}
	

	
	
}
