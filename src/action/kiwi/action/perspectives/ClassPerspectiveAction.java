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

import kiwi.api.ontology.OntologyService;
import kiwi.api.search.KiWiSearchCriteria;
import kiwi.api.search.SolrService;
import kiwi.model.Constants;
import kiwi.model.content.ContentItem;
import kiwi.model.kbase.KiWiResource;

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
 * ClassPerspectiveAction
 *
 * @author Sebastian Schaffert
 *
 */
@Name("kiwi.core.ui.classPerspectiveAction")
@Scope(ScopeType.PAGE)
public class ClassPerspectiveAction {

	@Logger
	private Log log;
	
	@In
	private OntologyService ontologyService;
	
	@In
	private ContentItem currentContentItem;
	
	@In
	private FacesMessages facesMessages;
	
	private KiWiResource selectedResource;
	
	
	public void addSuperclass() {
		if(selectedResource != null) {
			ontologyService.addSuperClass(currentContentItem.getResource(), selectedResource);
			
			selectedResource = null;
		} else {
			facesMessages.add("no resource has been selected");
		}
	}
	
	public void addSubclass() {
		if(selectedResource != null) {
			ontologyService.addSuperClass(selectedResource,currentContentItem.getResource());
			
			selectedResource = null;
		} else {
			facesMessages.add("no resource has been selected");
		}
	}
	
	public void addInstance() {
		if(selectedResource != null) {
			ontologyService.addInstance(currentContentItem.getResource(),selectedResource);
			
			selectedResource = null;
		} else {
			facesMessages.add("no resource has been selected");
		}
	}

	/**
	 * @return the selectedResource
	 */
	public KiWiResource getSelectedResource() {
		return selectedResource;
	}

	/**
	 * @param selectedResource the selectedResource to set
	 */
	public void setSelectedResource(KiWiResource selectedResource) {
		this.selectedResource = selectedResource;
	}

	
	// quick hack until we have autocompletion service:
	public List<KiWiResource> autocompleteResource(String prefix) {
		if(prefix.length() >= 2) {
			StringBuilder qString = new StringBuilder();
			
			// add prefix to query string
			qString.append("title:"+prefix.toLowerCase()+"*");
	
		
			SolrService solrService = (SolrService)Component.getInstance("solrService");
			EntityManager entityManager = (EntityManager)Component.getInstance("entityManager");


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
		} else {
			return Collections.emptyList();
		}

	}

}
