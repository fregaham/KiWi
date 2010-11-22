/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2008-2009, The KiWi Project (http://www.kiwi-project.eu)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * - Redistributions of source code must retain the above copyright notice, 
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, 
 *   this list of conditions and the following disclaimer in the documentation 
 *   and/or other materials provided with the distribution.
 * - Neither the name of the KiWi Project nor the names of its contributors 
 *   may be used to endorse or promote products derived from this software 
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * Contributor(s):
 * sschaffe
 * 
 */
package kiwi.action.perspectives;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.EntityManager;

import kiwi.api.search.KiWiSearchCriteria;
import kiwi.api.search.SolrService;
import kiwi.api.triplestore.TripleStore;
import kiwi.api.user.UserService;
import kiwi.exception.NamespaceResolvingException;
import kiwi.model.Constants;
import kiwi.model.content.ContentItem;
import kiwi.model.kbase.KiWiNode;
import kiwi.model.kbase.KiWiResource;
import kiwi.model.kbase.KiWiTriple;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.log.Log;

/**
 * PersonPerspectiveAction
 *
 * @author Sebastian Schaffert
 *
 */
@Name("kiwi.core.ui.personPerspectiveAction")
@Scope(ScopeType.CONVERSATION)
public class PersonPerspectiveAction {

	@Logger
	private Log log;
	
	@In
	private UserService userService;
	
	@In
	private ContentItem currentContentItem;

	/** for class autocompletion */
	@In
	private SolrService solrService;

	/** For direct HQL queries */
	@In
	private EntityManager entityManager;

	private KiWiResource selectedFriend;

	private KiWiResource selectedInterest;
	
	private KiWiResource selectedGroup;
	
	private KiWiResource selectedCurrentProject;
	
	private KiWiResource selectedPastProject;
	
	private KiWiResource selectedPhoto;
	
	@In
	private FacesMessages facesMessages;

	
	private List<ContentItem> photos;
	
	public boolean hasProfilePhoto() {
		initPhotos();
		
		return photos.size() > 0 && photos.get(0).getMediaContent() != null;
	}

	
	
	public ContentItem getProfilePhoto() {
		initPhotos();
		
		if(photos.size() > 0) {
			return photos.get(0);
		} else {
			return null;
		}
	}

	
	private void initPhotos() {
		if(photos == null) {
			photos = new LinkedList<ContentItem>();
			
			try {
				for(KiWiNode n : currentContentItem.getResource().listOutgoingNodes(Constants.NS_FOAF+"img")) {
					if(n.isUriResource() || n.isAnonymousResource()) {
						photos.add(((KiWiResource)n).getContentItem());
					}
				}
				for(KiWiNode n : currentContentItem.getResource().listOutgoingNodes(Constants.NS_FOAF+"depiction")) {
					if(n.isUriResource() || n.isAnonymousResource()) {
						photos.add(((KiWiResource)n).getContentItem());
					}
				}
			} catch(NamespaceResolvingException ex) {}
		}
	}
	
	
	/**
	 * Provide autocompletion of content items that are of type "foaf:Person" or "kiwi:User" and 
	 * whose title starts with "prefix". Autocompletion is delegated to the SOLR search service.
	 * @param prefix
	 * @return
	 */
	public List<KiWiResource> autocompletePersons(String prefix) {
		if(prefix.length() >= 2) {
			StringBuilder qString = new StringBuilder();
			
			// add prefix to query string
			qString.append("title:"+prefix.toLowerCase()+"*");
			qString.append(" ");
			
			// add (type:kiwi:Tag OR type:skos:Concept)
			qString.append("(");
			qString.append("type:\"uri::"+Constants.NS_FOAF+"Person\"");
			qString.append(" OR ");
			qString.append("type:\"uri::"+Constants.NS_KIWI_CORE+"User\"");
			qString.append(")");
	
			return autocompleteInternal(qString.toString());
		} else {
			return Collections.emptyList();
		}
	}

	/**
	 * Provide autocompletion of content items that are of type "foaf:Person" or "kiwi:User" and 
	 * whose title starts with "prefix". Autocompletion is delegated to the SOLR search service.
	 * @param prefix
	 * @return
	 */
	public List<KiWiResource> autocompleteGroups(String prefix) {
		if(prefix.length() >= 2) {
			StringBuilder qString = new StringBuilder();
			
			// add prefix to query string
			qString.append("title:"+prefix.toLowerCase()+"*");
			qString.append(" ");
			
			// add (type:kiwi:Tag OR type:skos:Concept)
			qString.append("(");
			qString.append("type:\"uri::"+Constants.NS_FOAF+"Group\"");
			qString.append(" OR ");
			qString.append("type:\"uri::"+Constants.NS_KIWI_CORE+"Group\"");
			qString.append(" OR ");
			qString.append("type:\"uri::"+Constants.NS_SIOC+"Usergroup\"");
			qString.append(")");
	
			return autocompleteInternal(qString.toString());
		} else {
			return Collections.emptyList();
		}
	}

	
	/**
	 * Provide autocompletion of content items that are of type "skos:Concept"  and 
	 * whose title starts with "prefix". Autocompletion is delegated to the SOLR search service.
	 * @param prefix
	 * @return
	 */
	public List<KiWiResource> autocompleteInterests(String prefix) {
		if(prefix.length() >= 2) {
			StringBuilder qString = new StringBuilder();
			
			// add prefix to query string
			qString.append("title:"+prefix.toLowerCase()+"*");
			qString.append(" ");
			
			// add (type:kiwi:Tag OR type:skos:Concept)
			qString.append("(");
			qString.append("type:\"uri::"+Constants.NS_SKOS+"Concept\"");
			qString.append(")");
	
			return autocompleteInternal(qString.toString());
		} else {
			return Collections.emptyList();
		}
	}

	/**
	 * Provide autocompletion of content items that are of type "skos:Concept"  and 
	 * whose title starts with "prefix". Autocompletion is delegated to the SOLR search service.
	 * @param prefix
	 * @return
	 */
	public List<KiWiResource> autocompleteProjects(String prefix) {
		if(prefix.length() >= 2) {
			StringBuilder qString = new StringBuilder();
			
			// add prefix to query string
			qString.append("title:"+prefix.toLowerCase()+"*");
			qString.append(" ");
			
			// add (type:kiwi:Tag OR type:skos:Concept)
			qString.append("(");
			qString.append("type:\"uri::"+Constants.NS_FOAF+"Project\"");
			qString.append(")");
	
			return autocompleteInternal(qString.toString());
		} else {
			return Collections.emptyList();
		}
	}

	/**
	 * Provide autocompletion of content items that are of type "skos:Concept"  and 
	 * whose title starts with "prefix". Autocompletion is delegated to the SOLR search service.
	 * @param prefix
	 * @return
	 */
	public List<KiWiResource> autocompletePhotos(String prefix) {
		if(prefix.length() >= 0) {
			StringBuilder qString = new StringBuilder();
			
			// add prefix to query string
			qString.append("title:"+prefix.toLowerCase()+"*");
			qString.append(" ");
			
			// add (type:kiwi:Tag OR type:skos:Concept)
			qString.append("(");
			qString.append("type:\"uri::"+Constants.NS_KIWI_CORE+"Image\"");
			qString.append(")");
	
			return autocompleteInternal(qString.toString());
		} else {
			return Collections.emptyList();
		}
	}
	
	
	private List<KiWiResource> autocompleteInternal(String qString) {
		
		KiWiSearchCriteria crit = new KiWiSearchCriteria();
		
		crit.setSolrSearchString(qString.toString());
		
		SolrQuery query = solrService.buildSolrQuery(crit);
		query.setStart(0);
		query.setRows(Integer.MAX_VALUE);
		query.setSortField("title_id", ORDER.asc);
		
		query.setFields("title","id");
		
		QueryResponse rsp = solrService.search(query);
		
		if(rsp == null){
			log.error("autocomplete: solr delivered null for the query #0", 
					qString.toString());
			return Collections.emptyList();
		}
		SolrDocumentList docs = rsp.getResults();
		
		// a resourceMapping for KiWiResourceConverter
		HashMap<String,KiWiResource> resourceMappings = new HashMap<String, KiWiResource>();
		List<KiWiResource> result = new LinkedList<KiWiResource>();
		for(SolrDocument doc : docs) {
			ContentItem item = (ContentItem)entityManager.find(ContentItem.class, Long.parseLong((String)doc.getFieldValue("id")));
			if(item != null) {
				result.add(item.getResource());
				
				String label = item.getResource().getLabel() + " (" + item.getResource().getNamespacePrefix() + ")";
				resourceMappings.put(label, item.getResource());
			}
		}
		
		Contexts.getPageContext().set("resourceMappings", resourceMappings);
		
		Collections.sort(result, KiWiResource.LabelComparator.getInstance());
		return result;
	}
	

	public void addPhoto() {
		if(selectedPhoto != null) {
			try {
				// delete previous photos
				TripleStore ts = (TripleStore)Component.getInstance("tripleStore");
				try {
					for(KiWiTriple t : currentContentItem.getResource().listOutgoing(Constants.NS_FOAF+"img")) {
						ts.removeTriple(t);
					}
					for(KiWiTriple t : currentContentItem.getResource().listOutgoing(Constants.NS_FOAF+"depiction")) {
						ts.removeTriple(t);
					}
				} catch(NamespaceResolvingException ex) {}
				
				currentContentItem.getResource().addOutgoingNode(Constants.NS_FOAF+"depiction", selectedPhoto);
				currentContentItem.getResource().addOutgoingNode(Constants.NS_FOAF+"img", selectedPhoto);
				selectedPhoto = null;
				
				initPhotos();
			} catch (NamespaceResolvingException e) {
				facesMessages.add("an unexpected error occurred");
			}
			
		} else {
			facesMessages.add("no resource has been selected");
		}
	}
	
	public void addFriend() {
		if(selectedFriend != null) {
			try {
				currentContentItem.getResource().addOutgoingNode(Constants.NS_FOAF+"knows", selectedFriend);
				selectedFriend = null;
			} catch (NamespaceResolvingException e) {
				facesMessages.add("an unexpected error occurred");
			}
			
		} else {
			facesMessages.add("no resource has been selected");
		}
		
	}

	public void addInterest() {
		log.info("adding interest #0", selectedInterest);
		if(selectedInterest != null) {
			try {
				currentContentItem.getResource().addOutgoingNode(Constants.NS_FOAF+"interest", selectedInterest);
				selectedInterest = null;
			} catch (NamespaceResolvingException e) {
				facesMessages.add("an unexpected error occurred");
			}
			
		} else {
			facesMessages.add("no resource has been selected");
		}
		
	}

	public void addCurrentProject() {
		if(selectedCurrentProject != null) {
			try {
				currentContentItem.getResource().addOutgoingNode(Constants.NS_FOAF+"currentProject", selectedCurrentProject);
				selectedCurrentProject = null;
			} catch (NamespaceResolvingException e) {
				facesMessages.add("an unexpected error occurred");
			}
			
		} else {
			facesMessages.add("no resource has been selected");
		}
		
	}


	public void addPastProject() {
		if(selectedPastProject != null) {
			try {
				currentContentItem.getResource().addOutgoingNode(Constants.NS_FOAF+"pastProject", selectedPastProject);
				selectedPastProject = null;
			} catch (NamespaceResolvingException e) {
				facesMessages.add("an unexpected error occurred");
			}
			
		} else {
			facesMessages.add("no resource has been selected");
		}
		
	}


	public void save() {
		// just used for form submission at the moment
	}

	public void addGroup() {
		if(selectedGroup != null) {
			try {
				currentContentItem.getResource().addOutgoingNode(Constants.NS_FOAF+"member", selectedGroup);
				selectedGroup = null;
			} catch (NamespaceResolvingException e) {
				facesMessages.add("an unexpected error occurred");
			}
			
		} else {
			facesMessages.add("no resource has been selected");
		}
		
	}
	
	
	/**
	 * @return the selectedFriend
	 */
	public KiWiResource getSelectedFriend() {
		return selectedFriend;
	}



	/**
	 * @param selectedFriend the selectedFriend to set
	 */
	public void setSelectedFriend(KiWiResource selectedFriend) {
		this.selectedFriend = selectedFriend;
	}



	/**
	 * @return the selectedResource
	 */
	public KiWiResource getSelectedInterest() {
		return selectedInterest;
	}



	/**
	 * @param selectedResource the selectedResource to set
	 */
	public void setSelectedInterest(KiWiResource selectedResource) {
		this.selectedInterest = selectedResource;
	}



	/**
	 * @return the selectedGroup
	 */
	public KiWiResource getSelectedGroup() {
		return selectedGroup;
	}



	/**
	 * @param selectedGroup the selectedGroup to set
	 */
	public void setSelectedGroup(KiWiResource selectedGroup) {
		this.selectedGroup = selectedGroup;
	}



	/**
	 * @return the selectedCurrentProject
	 */
	public KiWiResource getSelectedCurrentProject() {
		return selectedCurrentProject;
	}



	/**
	 * @param selectedCurrentProject the selectedCurrentProject to set
	 */
	public void setSelectedCurrentProject(KiWiResource selectedCurrentProject) {
		this.selectedCurrentProject = selectedCurrentProject;
	}



	/**
	 * @return the selectedPastProject
	 */
	public KiWiResource getSelectedPastProject() {
		return selectedPastProject;
	}



	/**
	 * @param selectedPastProject the selectedPastProject to set
	 */
	public void setSelectedPastProject(KiWiResource selectedPastProject) {
		this.selectedPastProject = selectedPastProject;
	}



	/**
	 * @return the selectedPhoto
	 */
	public KiWiResource getSelectedPhoto() {
		return selectedPhoto;
	}



	/**
	 * @param selectedPhoto the selectedPhoto to set
	 */
	public void setSelectedPhoto(KiWiResource selectedPhoto) {
		this.selectedPhoto = selectedPhoto;
	}
	
	
	
}
