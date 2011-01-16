/*
Copyright © dkms, artaround
 */

package dkms.action.contentRetrieval;

import java.io.Serializable;
import java.text.Collator;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import kiwi.api.entity.KiWiEntityManager;
import kiwi.api.ontology.OntologyService;
import kiwi.api.search.KiWiSearchCriteria;
import kiwi.api.search.KiWiSearchResults;
import kiwi.api.search.SolrService;
import kiwi.api.search.KiWiSearchResults.KiWiFacet;
import kiwi.api.search.KiWiSearchResults.SearchResult;
import kiwi.api.triplestore.TripleStore;
import kiwi.model.content.ContentItem;
import kiwi.model.kbase.KiWiResource;
import kiwi.model.kbase.KiWiUriResource;
import kiwi.model.ontology.KiWiProperty;

import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.log.Log;

import dkms.action.dkmsContentItem.DkmsContentItemBean;
import dkms.datamodel.dkmsContentItem.DkmsContentItemFacade;



@Scope(ScopeType.SESSION)
@Name("dkmsContentRetrievalAction")
@Transactional
public class DkmsContentRetrievalAction implements Serializable {	
	
	private static final long serialVersionUID = 1L;
	
	@Logger
	private Log log;
	
	@Out(required = false)
	private DkmsContentItemFacade selectedDkmsContentItem;
	
	private String contentItemTypeName;
	
	private String fullquery;
	
	private int page;
	
	private String sort;
	
	private String order;
	
	private static final int PAGE_SIZE = 1000000;
	
	private String[] bigIdeas = {"functional dependence", "argumentation/proof", "infinity", "multiple representation", "insecurity/variation, random deviations", "doing/undoing,inverting", "specialisation/generalisation"};
	private String[] contentAreas = {"number", "algebra functions", "shape", "space measures", "handling data"};
	private String[] languages = {"english", "german"};
	private String[] stati = {"mathematics content knowledge", "pedagogical content knowledge", "classroom situations"};
	private String[] typeOfResources = {"introduction to Big Ideas", "problems and activities", "inteachers engaging with Big Ideas", "supporting materials", "research", "examples of students learning"};
	private String[] intentions = {"theoretical input", "exercise", "exemplar", "review", "constructional guidance"};
	private String[] ageGroups = {"5 to 11", "11 to 16", "16 to 19", "every age group", "independent", "material for teachers"};
	
	
	
	private KiWiUriResource bigIdeaResource;
//	private KiWiUriResource representationTypeResource;
	
	private List<KiWiFacet<KiWiResource>> bigIdeaFacets;
//	private List<KiWiFacet<KiWiResource>> representationTypeFacets;
	
	private List<KiWiFacet<KiWiResource>> relevantTypes;
		
	@In
	private TripleStore tripleStore;
	
	@In
    private KiWiEntityManager kiwiEntityManager;
	
	private KiWiSearchResults searchResults;
	
	@In(create = true)
	private DkmsContentItemBean dkmsContentItemBean;
	
	
	
	@In(create=true)
	private SolrService solrService;
	
	@In
	private OntologyService ontologyService;
	
	private Map<String, Set<String>> bigIdeaMap; 
//	private Map<String, Set<String>> representationTypeMap; 
		
	private Set<KiWiUriResource> selectedBigIdeas;
//	private Set<KiWiUriResource> selectedRepresentationTypes;
	
	private Set<KiWiUriResource> selectedTypes;
		
	@Create
	public String reset() {
		clear();
		fullquery = "";
		sort = "score";
		order = "desc";
		searchResults = new KiWiSearchResults();
		
		List<KiWiProperty> resBI = ontologyService.listDatatypePropertiesByName("Big Idea");
//		List<KiWiProperty> resRT = ontologyService.listDatatypePropertiesByName("Representation Type");
		
		log.info(resBI == null?"facet is null":"facet is not null");
//		log.info(resRT == null?"facet is null":"facet is not null");
		
		if( resBI.isEmpty() ) bigIdeaResource = null;
		else bigIdeaResource = ((KiWiUriResource)resBI.get(0).getResource());
		
//		if( resRT.isEmpty() ) representationTypeResource = null;
//		else representationTypeResource = ((KiWiUriResource)resRT.get(0).getResource());
//		
		log.info(bigIdeaResource.getLabel());
		log.info(bigIdeaResource == null?"bigIdeaResource is null":"bigIdeaResource is not null");
		
//		log.info(representationTypeResource.getLabel());
//		log.info(representationTypeResource == null?"representationTypeResource is null":"representationTypeResource is not null");
		
		log.info(resBI == null || resBI.isEmpty()?"res is null":"res is not null");
//		log.info(resRT == null || resRT.isEmpty()?"res is null":"res is not null");
		
		bigIdeaFacets = new LinkedList<KiWiFacet<KiWiResource>>();
//		representationTypeFacets = new LinkedList<KiWiFacet<KiWiResource>>();
		relevantTypes = new LinkedList<KiWiFacet<KiWiResource>>();
		
		bigIdeaMap = new HashMap<String, Set<String>>();
//		representationTypeMap = new HashMap<String, Set<String>>();
		
		selectedBigIdeas = new HashSet<KiWiUriResource>();
//		selectedRepresentationTypes = new HashSet<KiWiUriResource>();
		selectedTypes = new HashSet<KiWiUriResource>();
		
		return "search";
	}
	
	
	public String search() {
		log.info(fullquery);
		clear();
		runBigIdeas(null);
//		runRepresentationTypes(null);
		return "search";
	}
	
	public String searchAll() {
		fullquery = "*";
		log.info(fullquery);
		clear();
		runBigIdeas(null);
//		runRepresentationTypes(null);
		return "search";
	}
	
	
	
	//return type facets if there are some
	private List<KiWiFacet<KiWiResource>> buildRelevantTypes() {
		List<KiWiFacet<KiWiResource>> results = new LinkedList<KiWiFacet<KiWiResource>>();
		
		if(searchResults != null) {
			
			for (Iterator iterator = searchResults.getTypeFacets().iterator(); iterator.hasNext();) {
				KiWiFacet<KiWiResource> kiWiFacet = (KiWiFacet<KiWiResource>) iterator.next(); 
				log.info(kiWiFacet.getContent().getLabel());
				//if(kiWiFacet.getContent().getLabel().contains("ArtWork") || kiWiFacet.getContent().getLabel().contains("ArtAroundUser")){
				if(kiWiFacet.getContent().getLabel().contains("DkmsContentItem")){
					results.add(kiWiFacet); 
				}
			}
			
			Collections.sort(results, new Comparator<KiWiFacet<KiWiResource>>() {

				@Override
				public int compare(KiWiFacet<KiWiResource> o1, KiWiFacet<KiWiResource> o2) {				
					return Collator.getInstance().compare(o1.getContent().getLabel(), o2.getContent().getLabel());
				}
				
			});
			
		} else {
			log.warn("called getRelevantTypes without a search result");
		}
		log.debug("getRelevantTypes: return #0 elements", results.size());
		return results;
	}
	
		
	public List<KiWiFacet<KiWiResource>> getRelevantTypes() {
		return relevantTypes;
	}


	private String runBigIdeas(Map<String, Set<String>> bigIdeaMap) {
		try {			
			KiWiSearchCriteria ksc = getBigIdeasCriteria();
			
			//bigIdea
			if(bigIdeaMap != null)
				ksc.setRdfObjectProperties(bigIdeaMap);		
				
				Set<String> ts = ksc.getTypes();

				for (Iterator iterator = selectedTypes.iterator(); iterator.hasNext();) {
					KiWiUriResource type = (KiWiUriResource) iterator.next();
					ts.add(type.getKiwiIdentifier());
				}				
				ksc.setTypes(ts);			
			
			searchResults = solrService.search(ksc);
			
			log.info(searchResults.getResults().size());
			
			//set status facets
			if(searchResults != null) {
				bigIdeaFacets = buildBigIdeaFacet();
				
				relevantTypes = buildRelevantTypes();
			}
			else Collections.emptyList();
			
		} catch(DkmsContentRetrievalException e) {
			searchResults = new KiWiSearchResults();
		}
		return "search";
	}
	
	
//	private String runRepresentationTypes(Map<String, Set<String>> representationTypeMap) {
//		try {			
//			KiWiSearchCriteria ksc = getRepresentationTypesCriteria();
//			
//			
//			if(representationTypeMap != null)
//				ksc.setRdfObjectProperties(representationTypeMap);
//	
//				Set<String> ts = ksc.getTypes();
//
//				for (Iterator iterator = selectedTypes.iterator(); iterator.hasNext();) {
//					KiWiUriResource type = (KiWiUriResource) iterator.next();
//					ts.add(type.getKiwiIdentifier());
//				}				
//				ksc.setTypes(ts);				
//
//			
//			searchResults = solrService.search(ksc);
//			
//			log.info(searchResults.getResults().size());
//			
//			//set status facets
//			if(searchResults != null) {
//				
//				representationTypeFacets = buildRepresentationTypeFacet();
//				relevantTypes = buildRelevantTypes();
//			}
//			else Collections.emptyList();
//			
//		} catch(DkmsContentRetrievalException e) {
//			searchResults = new KiWiSearchResults();
//		}
//		return "search";
//	}

	private KiWiSearchCriteria getBigIdeasCriteria() throws DkmsContentRetrievalException {
		KiWiSearchCriteria criteria = new KiWiSearchCriteria();
		//set full search query string
		if(fullquery != null && !fullquery.equals("")) {
			if("*".equals(fullquery)){
				//criteria = solrService.parseSearchString("");
				//criteria.setSolrSearchString("type:\"uri::http://www.artaround.at/ArtWork\" OR type:\"uri::http://www.artaround.at/ArtAroundUser\"");
				criteria.setSolrSearchString("type:\"uri::http://www.dkms.at/DkmsContentItem\"");
				//criteria = solrService.parseSearchString("type:artaround:ArtWork");
				//criteria.getTypes().add(tripleStore.createUriResource(Constants.ART_AROUND_CORE + "ArtWork").getKiwiIdentifier());
				//criteria.getTypes().add(tripleStore.createUriResource(Constants.NS_FOAF+"Person").getKiwiIdentifier());
			}else{
				criteria = solrService.parseSearchString(fullquery);
				//criteria.setSolrSearchString("(type:\"uri::http://www.artaround.at/ArtWork\" OR type:\"uri::http://www.artaround.at/ArtAroundUser\")");
				criteria.setSolrSearchString("type:\"uri::http://www.dkms.at/DkmsContentItem\"");
				//criteria = solrService.parseSearchString(fullquery + " type:artaround:ArtWork");
				}
		} else {
			throw new DkmsContentRetrievalException("no query defined");
		}
		
		
		log.info("order #0",order);
		log.info("sort #0",sort);
		
		//set pages,limit etc.
		criteria.setOffset(page*PAGE_SIZE);
		criteria.setLimit(PAGE_SIZE);
		criteria.setSortField(sort);
		
		Set<String> s = new HashSet<String>();
		s.add("http://www.dkms.at/bigIdeas");
		
		criteria.setRdfObjectFacets(s);
			
		
		if( order.equals("asc") ) criteria.setSortOrder(ORDER.asc);
		else criteria.setSortOrder(ORDER.desc);
		
		
		return criteria;
	}
	
//	private KiWiSearchCriteria getRepresentationTypesCriteria() throws DkmsContentRetrievalException {
//		KiWiSearchCriteria criteria = new KiWiSearchCriteria();
//		//set full search query string
//		if(fullquery != null && !fullquery.equals("")) {
//			if("*".equals(fullquery)){
//				//criteria = solrService.parseSearchString("");
//				//criteria.setSolrSearchString("type:\"uri::http://www.artaround.at/ArtWork\" OR type:\"uri::http://www.artaround.at/ArtAroundUser\"");
//				criteria.setSolrSearchString("type:\"uri::http://www.dkms.at/DkmsContentItem\"");
//				//criteria = solrService.parseSearchString("type:artaround:ArtWork");
//				//criteria.getTypes().add(tripleStore.createUriResource(Constants.ART_AROUND_CORE + "ArtWork").getKiwiIdentifier());
//				//criteria.getTypes().add(tripleStore.createUriResource(Constants.NS_FOAF+"Person").getKiwiIdentifier());
//			}else{
//				criteria = solrService.parseSearchString(fullquery);
//				//criteria.setSolrSearchString("(type:\"uri::http://www.artaround.at/ArtWork\" OR type:\"uri::http://www.artaround.at/ArtAroundUser\")");
//				criteria.setSolrSearchString("type:\"uri::http://www.dkms.at/DkmsContentItem\"");
//				//criteria = solrService.parseSearchString(fullquery + " type:artaround:ArtWork");
//				}
//		} else {
//			throw new DkmsContentRetrievalException("no query defined");
//		}
//		
//		
//		log.info("order #0",order);
//		log.info("sort #0",sort);
//		
//		//set pages,limit etc.
//		criteria.setOffset(page*PAGE_SIZE);
//		criteria.setLimit(PAGE_SIZE);
//		criteria.setSortField(sort);
//		
//		Set<String> s = new HashSet<String>();
//		s.add("http://www.dkms.at/representationType");
//		criteria.setRdfObjectFacets(s);
//			
//		
//		if( order.equals("asc") ) criteria.setSortOrder(ORDER.asc);
//		else criteria.setSortOrder(ORDER.desc);
//		
//		
//		return criteria;
//	}
	
	private void clear() {
		//reset the search (pages etc.)
		bigIdeaMap = new HashMap<String, Set<String>>();
//		representationTypeMap = new HashMap<String, Set<String>>();
		
		selectedBigIdeas = new HashSet<KiWiUriResource>();
//		selectedRepresentationTypes = new HashSet<KiWiUriResource>();
		selectedTypes = new HashSet<KiWiUriResource>();
		page = 0;
	}
	
	public String clearFacetes() {
		clear();
		//selectedBigIdeas.removeAll(bigIdeaFacets);
		//selectedRepresentationTypes.removeAll(representationTypeFacets);
		return "/edukms/abcmaths/searchKMS.xhtml";
	}
	
	
    public String getSearchView() {
    	search();
    	return  "/edukms/abcmaths/searchKMS.xhtml";
	}

	public String getFullquery() {
		return fullquery;
	}

	public void setFullquery(String fullquery) {
		this.fullquery = fullquery;
	}
		
	//+++++++++++++++++ sort and ordering +++++++++++++
	public void setSort(String sort) {
		this.sort = sort;
	}

	public String getSort() {
		return sort;
	}
	
	public void setOrder(String order) {
		this.order = order;
	}

	public String getOrder() {
		return order;
	}
	
	// facets for facetted search
	
	public List<KiWiFacet<KiWiResource>> getBigIdeaStatusFacet() {
		if( searchResults.getResultCount() == 0 ) return Collections.emptyList();
		else return bigIdeaFacets;
	}
	
//	public List<KiWiFacet<KiWiResource>> getRepresentationTypeStatusFacet() {
//		if( searchResults.getResultCount() == 0 ) return Collections.emptyList();
//		else return representationTypeFacets;
//	}
	
	private List<KiWiFacet<KiWiResource>> buildBigIdeaFacet() {
						
		if( bigIdeaResource == null ) return Collections.emptyList(); 
		
		List<KiWiFacet<KiWiResource>> result = new LinkedList<KiWiFacet<KiWiResource>>();
		log.info(searchResults.getObjectPropertyFacets().size());
		
		
		Map<KiWiUriResource, Set<KiWiFacet<KiWiResource>>> m = searchResults.getObjectPropertyFacets(); 
		Collection<Set<KiWiFacet<KiWiResource>>> hh = m.values(); 
		
		for(Set<KiWiFacet<KiWiResource>> kur : hh){
			for(KiWiFacet<KiWiResource> f: kur){
				result.add(f); 
				log.info(f.getContent().getContentItem().getTitle()); f.getContent().getKiwiIdentifier();
				
				log.info(f.getQuery());
				log.info(hh);
				log.info(f.getContent().getTypes()); 
			}
	
		}
		
			
		log.info(searchResults.getObjectPropertyFacets().get(bigIdeaResource) == null?"opf are null":"opf are not null");
		
		
		return result;
	}
	
//	private List<KiWiFacet<KiWiResource>> buildRepresentationTypeFacet() {
//		
//		if( representationTypeResource == null ) return Collections.emptyList();
//		
//		List<KiWiFacet<KiWiResource>> result = new LinkedList<KiWiFacet<KiWiResource>>();
//		log.info(searchResults.getObjectPropertyFacets().size());
//		
//		
//		Map<KiWiUriResource, Set<KiWiFacet<KiWiResource>>> m = searchResults.getObjectPropertyFacets();
//		Collection<Set<KiWiFacet<KiWiResource>>> hh = m.values();
//		
//		for(Set<KiWiFacet<KiWiResource>> kur : hh){
//			for(KiWiFacet<KiWiResource> f: kur){
//				result.add(f);
//				log.info(f.getContent().getContentItem().getTitle());
//			}
//	
//		}
//		
//			
//		log.info(searchResults.getObjectPropertyFacets().get(representationTypeResource) == null?"opf are null":"opf are not null");
//		
//		
//		return result;
//	}
	
	
	
	public List<KiWiFacet<KiWiResource>> getBigIdeaFacets() {
		if( searchResults.getResultCount() == 0 ) return Collections.emptyList(); 
		else return bigIdeaFacets;
	}
	
	
	public List<KiWiFacet<KiWiResource>> getBigIdeaSet() {
		if( searchResults.getResultCount() == 0 ) return Collections.emptyList(); 
		else {
			List<KiWiFacet<KiWiResource>> bigIdeaSet = new LinkedList<KiWiFacet<KiWiResource>>();
		
			for(KiWiFacet<KiWiResource> l : bigIdeaFacets){
				for (String m : bigIdeas){
					if (l.getContent().getLabel().equals(m)){
						bigIdeaSet.add(l);	
					}
				}
			}
			return bigIdeaSet;
		}
	}
	
	
	public List<KiWiFacet<KiWiResource>> getContentAreaSet() {
		if( searchResults.getResultCount() == 0 ) return Collections.emptyList(); 
		else {
			List<KiWiFacet<KiWiResource>> contentAreaSet = new LinkedList<KiWiFacet<KiWiResource>>();
		
			for(KiWiFacet<KiWiResource> l : bigIdeaFacets){
				for (String m : contentAreas){
					if (l.getContent().getLabel().equals(m)){
						contentAreaSet.add(l);	
					}
				}
			}
			return contentAreaSet;
		}
	}
	
	public List<KiWiFacet<KiWiResource>> getLanguageSet() {
		if( searchResults.getResultCount() == 0 ) return Collections.emptyList(); 
		else {
			List<KiWiFacet<KiWiResource>> languageSet = new LinkedList<KiWiFacet<KiWiResource>>();
		
			for(KiWiFacet<KiWiResource> l : bigIdeaFacets){
				for (String m : languages){
					if (l.getContent().getLabel().equals(m)){
						languageSet.add(l);	
					}
				}
			}
			return languageSet;
		}
	}
	
	
	public List<KiWiFacet<KiWiResource>> getStatusSet() {
		if( searchResults.getResultCount() == 0 ) return Collections.emptyList(); 
		else {
			List<KiWiFacet<KiWiResource>> statusSet = new LinkedList<KiWiFacet<KiWiResource>>();
		
			for(KiWiFacet<KiWiResource> l : bigIdeaFacets){
				for (String m : stati){
					if (l.getContent().getLabel().equals(m)){
						statusSet.add(l);	
					}
				}
			}
			return statusSet;
		}
	}
	
	public List<KiWiFacet<KiWiResource>> getTypeOfResourceSet() {
		if( searchResults.getResultCount() == 0 ) return Collections.emptyList(); 
		else {
			List<KiWiFacet<KiWiResource>> typeOfResourceSet = new LinkedList<KiWiFacet<KiWiResource>>();
		
			for(KiWiFacet<KiWiResource> l : bigIdeaFacets){
				for (String m : typeOfResources){
					if (l.getContent().getLabel().equals(m)){
						typeOfResourceSet.add(l);	
					}
				}
			}
			return typeOfResourceSet;
		}
	}
	
	public List<KiWiFacet<KiWiResource>> getIntentionSet() {
		if( searchResults.getResultCount() == 0 ) return Collections.emptyList(); 
		else {
			List<KiWiFacet<KiWiResource>> intentionSet = new LinkedList<KiWiFacet<KiWiResource>>();
		
			for(KiWiFacet<KiWiResource> l : bigIdeaFacets){
				for (String m : intentions){
					if (l.getContent().getLabel().equals(m)){
						intentionSet.add(l);	
					}
				}
			}
			return intentionSet;
		}
	}
	
	public List<KiWiFacet<KiWiResource>> getAgeGroupSet() {
		if( searchResults.getResultCount() == 0 ) return Collections.emptyList(); 
		else {
			List<KiWiFacet<KiWiResource>> ageGroupSet = new LinkedList<KiWiFacet<KiWiResource>>();
		
			for(KiWiFacet<KiWiResource> l : bigIdeaFacets){
				for (String m : ageGroups){
					if (l.getContent().getLabel().equals(m)){
						ageGroupSet.add(l);	
					}
				}
			}
			return ageGroupSet;
		}
	}
	
	
	
	
//	public List<KiWiFacet<KiWiResource>> getRepresentationTypeFacets() {
//		if( searchResults.getResultCount() == 0 ) return Collections.emptyList();
//		else return representationTypeFacets;
//	}
	
	// ------ Paging
	
	public int getCurrentPage() {
		return page+1;
	}
	
	public boolean[] getPageArray() {
		boolean[] a = new boolean[(int)Math.ceil((double)searchResults.getResultCount()/PAGE_SIZE)];
		if( a.length > 0 ) a[page] = true;
		return a;
	}
	
	public int getFirstContentNumber() {
		return (page*PAGE_SIZE)+1;
	}
	
	public long getLastContentNumber() {
		return Math.min((long)((page*PAGE_SIZE)+PAGE_SIZE), searchResults.getResultCount());
	}
	
	public long getResultCount() {
		return searchResults.getResultCount();
	}
	
	public String setPage(int i) {
		this.page = i;
		runBigIdeas(null);
//		runRepresentationTypes(null);
		return "search";
	}
	
	public String nextPage() {
		this.page++;
		runBigIdeas(null);
//		runRepresentationTypes(null);
		return "search";
	}
	
	public String prevPage() {
		this.page--;
		runBigIdeas(null);
//		runRepresentationTypes(null);
		return "search";
	}
	
	public boolean hasNextPage() {
		return getLastContentNumber() < searchResults.getResultCount();
	}
	
	public boolean hasPrevPage() {
		return page > 0;
	}
	
	//+++++++++++++++++++++ Search Results ++++++++++++++++++++++
	/**
	 * @return a list of searchResults depending on query String
	 */
	public List<SearchResult> getSearchResults() {
		return searchResults.getResults();
	}
	
	// bigIdea
	public KiWiUriResource getBigIdeaResource() {
		return bigIdeaResource;
	}
	
	// representationType
//	public KiWiUriResource getRepresentationTypeResource() {
//		return representationTypeResource;
//	}

	//convert set to list
	public List<KiWiUriResource> getSelectedBigIdeas() {
		LinkedList<KiWiUriResource> l = new LinkedList<KiWiUriResource>();
		for(KiWiUriResource s: selectedBigIdeas){
			l.add(s);
		}		
		return l;
	}
	
//	public List<KiWiUriResource> getSelectedRepresentationTypes() {
//		LinkedList<KiWiUriResource> l = new LinkedList<KiWiUriResource>();
//		for(KiWiUriResource s: selectedRepresentationTypes){
//			l.add(s);
//		}		
//		return l;
//	}
	
	public String setType(KiWiUriResource type){
		selectedTypes.add(type);
		runBigIdeas(null);
//		runRepresentationTypes(null);
		return "search";
	}
	
	public List<KiWiUriResource> getSelectedTypes(){
		LinkedList<KiWiUriResource> l = new LinkedList<KiWiUriResource>();
		for(KiWiUriResource s: selectedTypes){
			l.add(s);
		}		
		return l;
	}
	

	public String setBigIdeaResource(KiWiUriResource bigIdeaResource) {						
		selectedBigIdeas.add(bigIdeaResource);
		log.info(bigIdeaResource.getUri());
		
		if(bigIdeaMap == null){ 
			 bigIdeaMap = new HashMap<String, Set<String>>();
		}
		
		
		bigIdeaMap.put("http://www.dkms.at/bigIdeas", kiwiResource2Uri(selectedBigIdeas));

		
//		runRepresentationTypes(representationTypeMap);
		runBigIdeas(bigIdeaMap);
		return "search";
				
	}
	
//	public String setRepresentationTypeResource(KiWiUriResource representationTypeResource) {						
//		selectedRepresentationTypes.add(representationTypeResource);
//		log.info(representationTypeResource.getUri());
//		
//		if(representationTypeMap == null){ 
//			 representationTypeMap = new HashMap<String, Set<String>>();
//		}
//		
//		
//		
//		representationTypeMap.put("http://www.dkms.at/representationType", kiwiResource2Uri(selectedRepresentationTypes));
//
//		runBigIdeas(bigIdeaMap);
//		runRepresentationTypes(representationTypeMap);
//		return "search";
//	}
	
	public String removeBigIdea(KiWiUriResource bigIdeaResource){
		selectedBigIdeas.remove(bigIdeaResource);
		if(selectedBigIdeas.size() >0){
			bigIdeaMap.put("http://www.dkms.at/bigIdeas", kiwiResource2Uri(selectedBigIdeas));
			
		}
		else
		{
			bigIdeaMap = null;
		}
		runBigIdeas(bigIdeaMap);
//		runRepresentationTypes(representationTypeMap);
		return "search";
	}
	
//	public String removeRepresentationType(KiWiUriResource representationTypeResource){
//		selectedRepresentationTypes.remove(representationTypeResource);
//		if(selectedRepresentationTypes.size() >0){
//			
//			representationTypeMap.put("http://www.dkms.at/representationType", kiwiResource2Uri(selectedRepresentationTypes));
//		}
//		else
//		{
//			representationTypeMap = null;
//		}
//		runRepresentationTypes(representationTypeMap);
//		runBigIdeas(bigIdeaMap);
//		
//		return "search";
//	}
	
	public String removeType(KiWiUriResource typeResource){
		selectedTypes.remove(typeResource);
		runBigIdeas(bigIdeaMap);
//		runRepresentationTypes(representationTypeMap);
		return "search";
	}
	
	
	private Set<String> kiwiResource2Uri(Set<KiWiUriResource> ks){
		Set<String> ls = new HashSet<String>();
		for(KiWiUriResource s: ks){
			ls.add(s.getUri());
		}
		return ls;
	}
	
	public String getContentItemTypeName() {
		return contentItemTypeName;
	}


	public void setContentItemTypeName(String contentItemTypeName) {
		this.contentItemTypeName = contentItemTypeName;
	}
	
	public String showContentItemPresentation(ContentItem ci){		
		
		 DkmsContentItemFacade contentItemFacade = kiwiEntityManager.createFacade(ci, DkmsContentItemFacade.class);		
		 dkmsContentItemBean.init(contentItemFacade);
		 selectedDkmsContentItem = contentItemFacade;
		 
		 if (contentItemFacade.getDkmsContentItemType().equals("1")){
			 setContentItemTypeName("Simple Resource");
			 return "/edukms/abcmaths/exerciseSheetPresentation.xhtml";
			}
		 else if (contentItemFacade.getDkmsContentItemType().equals("2")){
			 setContentItemTypeName("Segmented Resource");
			 return "/edukms/abcmaths/sequencePresentation.xhtml";
			}
		 else if (contentItemFacade.getDkmsContentItemType().equals("3")){
			 setContentItemTypeName("Uploaded Resource");
			 return "/edukms/abcmaths/documentPresentation.xhtml";
			}
		 else if (contentItemFacade.getDkmsContentItemType().equals("4")){
			 setContentItemTypeName("Wiki Resource");
			 return "/edukms/abcmaths/wikiPresentation.xhtml";
			}
		 else if (contentItemFacade.getDkmsContentItemType().equals("5")){
			 setContentItemTypeName("Blog");
			 return "/edukms/abcmaths/blogPresentation.xhtml";
			}
		 else if (selectedDkmsContentItem.getDkmsContentItemType().equals("6")){
			 setContentItemTypeName("Combined Resource");
			 return "/edukms/abcmaths/combinedResourcesPresentation.xhtml";
			}
		 else return "";
		 
	}
				
}	
	
	
	
	
