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



@Scope(ScopeType.SESSION)
@Name("dkmsContentRetrievalAction")
@Transactional
public class DkmsContentRetrievalAction implements Serializable {	
	
	private static final long serialVersionUID = 1L;
	
	@Logger
	private Log log;
	
	private String fullquery;
	
	private int page;
	
	private String sort;
	
	private String order;
	
	private static final int PAGE_SIZE = 10;

	private KiWiUriResource bigIdeaResource;
	
	private List<KiWiFacet<KiWiResource>> bigIdeaFacets;
	
	private List<KiWiFacet<KiWiResource>> relevantTypes;
	
	@In
	private TripleStore tripleStore;
	
	@In
    private KiWiEntityManager kiwiEntityManager;
	
	private KiWiSearchResults searchResults;
	
	
	@In(create=true)
	private SolrService solrService;
	
	@In
	private OntologyService ontologyService;
	
	private Map<String, Set<String>> bigIdeaMap; 
		
	private Set<KiWiUriResource> selectedBigIdeas;
	
	private Set<KiWiUriResource> selectedTypes;
		
	@Create
	public String reset() {
		clear();
		fullquery = "";
		sort = "score";
		order = "desc";
		searchResults = new KiWiSearchResults();
		
		List<KiWiProperty> res = ontologyService.listDatatypePropertiesByName("Big Idea");
		//List<KiWiProperty> res = ontologyService.listDatatypePropertiesByName("Representation Type");
		
		log.info(res == null?"facet is null":"facet is not null");
		
		if( res.isEmpty() ) bigIdeaResource = null;
		else bigIdeaResource = ((KiWiUriResource)res.get(0).getResource());
		
		log.info(bigIdeaResource.getLabel());
		log.info(bigIdeaResource == null?"bigIdeaResource is null":"bigIdeaResource is not null");
		log.info(res == null || res.isEmpty()?"res is null":"res is not null");
		
		bigIdeaFacets = new LinkedList<KiWiFacet<KiWiResource>>();
		relevantTypes = new LinkedList<KiWiFacet<KiWiResource>>();
		
		bigIdeaMap = new HashMap<String, Set<String>>();
		
		selectedBigIdeas = new HashSet<KiWiUriResource>();
		selectedTypes = new HashSet<KiWiUriResource>();
		
		return "search";
	}
	
	
	public String search() {
		log.info(fullquery);
		clear();
		return run(null);
	}
	
	public String searchAll() {
		fullquery = "*";
		log.info(fullquery);
		clear();
		return run(null);
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


	private String run(Map<String, Set<String>> bigIdeaMap) {
		try {			
			KiWiSearchCriteria ksc = getCriteria();
			
			//bigIdea
			if(bigIdeaMap != null)
				ksc.setRdfObjectProperties(bigIdeaMap);
			//type
		//	if(type != null){
				
				Set<String> ts = ksc.getTypes();

				for (Iterator iterator = selectedTypes.iterator(); iterator.hasNext();) {
					KiWiUriResource type = (KiWiUriResource) iterator.next();
					ts.add(type.getKiwiIdentifier());
				}				
				ksc.setTypes(ts);
				
				//String kiwiIdentifier = type.getKiwiIdentifier();

				
				//String crit = ksc.getSolrSearchString();
				
//				log.info(crit);
//				crit = crit+" and type:\""+kiwiIdentifier+"\"";
//				log.info(crit);
//				ksc.setSolrSearchString(crit);
		//	}
			
			
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
	


	private KiWiSearchCriteria getCriteria() throws DkmsContentRetrievalException {
		KiWiSearchCriteria criteria = new KiWiSearchCriteria();
		//set full search query string
		if(fullquery != null && !fullquery.equals("")) {
			if("*".equals(fullquery)){
				//criteria = solrService.parseSearchString("");
				//criteria.setSolrSearchString("type:\"uri::http://www.artaround.at/ArtWork\" OR type:\"uri::http://www.artaround.at/ArtAroundUser\"");
				
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
		criteria.setSolrSearchString("type:\"uri::http://www.dkms.at/DkmsContentItem\"");
		
		log.info("order #0",order);
		log.info("sort #0",sort);
		
		//set pages,limit etc.
		criteria.setOffset(page*PAGE_SIZE);
		criteria.setLimit(PAGE_SIZE);
		criteria.setSortField(sort);
		
		Set<String> s = new HashSet<String>();
		s.add("http://www.dkms.at/bigIdeas");
		//s.add("http://www.dkms.at/representationType");
		criteria.setRdfObjectFacets(s);
			
		
		if( order.equals("asc") ) criteria.setSortOrder(ORDER.asc);
		else criteria.setSortOrder(ORDER.desc);
		
		
		return criteria;
	}
	
	private void clear() {
		//reset the search (pages etc.)
		bigIdeaMap = new HashMap<String, Set<String>>();
		
		selectedBigIdeas = new HashSet<KiWiUriResource>();
		selectedTypes = new HashSet<KiWiUriResource>();
		page = 0;
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
	
	public List<KiWiFacet<KiWiResource>> getStatusFacet() {
		if( searchResults.getResultCount() == 0 ) return Collections.emptyList();
		else return bigIdeaFacets;
	}
	
	private List<KiWiFacet<KiWiResource>> buildBigIdeaFacet() {
						
		if( bigIdeaResource == null ) return Collections.emptyList();
		
		List<KiWiFacet<KiWiResource>> result = new LinkedList<KiWiFacet<KiWiResource>>();
		log.info(searchResults.getObjectPropertyFacets().size());
		
		
		Map<KiWiUriResource, Set<KiWiFacet<KiWiResource>>> m = searchResults.getObjectPropertyFacets();
		Collection<Set<KiWiFacet<KiWiResource>>> hh = m.values();
		
		for(Set<KiWiFacet<KiWiResource>> kur : hh){
			for(KiWiFacet<KiWiResource> f: kur){
				result.add(f);
				log.info(f.getContent().getContentItem().getTitle());
			}
	
		}
		
			
		log.info(searchResults.getObjectPropertyFacets().get(bigIdeaResource) == null?"opf are null":"opf are not null");
		
		
		return result;
	}
	
	
	
	public List<KiWiFacet<KiWiResource>> getBigIdeaFacets() {
		if( searchResults.getResultCount() == 0 ) return Collections.emptyList();
		else return bigIdeaFacets;
	}
	
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
		return run(null);
	}
	
	public String nextPage() {
		this.page++;
		return run(null);
	}
	
	public String prevPage() {
		this.page--;
		return run(null);
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

	//convert set to list
	public List<KiWiUriResource> getSelectedBigIdeas() {
		LinkedList<KiWiUriResource> l = new LinkedList<KiWiUriResource>();
		for(KiWiUriResource s: selectedBigIdeas){
			l.add(s);
		}		
		return l;
	}
	
	public String setType(KiWiUriResource type){
		selectedTypes.add(type);
		return run(null);
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
		//representationTypeMap.put("http://www.dkms.at/representationType", kiwiResource2Uri(selectedRepresentationTypes));

		return run(bigIdeaMap);
	}
	
	public String removeBigIdea(KiWiUriResource bigIdeaResource){
		selectedBigIdeas.remove(bigIdeaResource);
		if(selectedBigIdeas.size() >0){
			bigIdeaMap.put("http://www.dkms.at/bigIdeas", kiwiResource2Uri(selectedBigIdeas));
			//representationTypeMap.put("http://www.dkms.at/representationType", kiwiResource2Uri(selectedRepresentationTypes));
		}
		else
		{
			bigIdeaMap = null;
		}
		return run(bigIdeaMap);
	}
	
	public String removeType(KiWiUriResource typeResource){
		selectedTypes.remove(typeResource);
		return run(bigIdeaMap);
	}
	
	
	private Set<String> kiwiResource2Uri(Set<KiWiUriResource> ks){
		Set<String> ls = new HashSet<String>();
		for(KiWiUriResource s: ks){
			ls.add(s.getUri());
		}
		return ls;
	}
	
	
}
