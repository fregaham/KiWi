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
package kiwi.action.ui;

import kiwi.api.content.ContentItemService;
import kiwi.api.perspectives.PerspectiveService;
import kiwi.api.triplestore.TripleStore;
import kiwi.context.CurrentContentItemFactory;
import kiwi.model.Constants;
import kiwi.model.content.ContentItem;
import kiwi.model.kbase.KiWiUriResource;
import kiwi.model.perspective.Perspective;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;

/**
 * PageActionsAction. Used as a backing bean for the page actions panel and allows to create new pages of predefined
 * types.
 *
 * @author Sebastian Schaffert
 *
 */
@Name("kiwi.ui.pageActionsAction")
@Scope(ScopeType.CONVERSATION)
@AutoCreate
public class PageActionsAction {

	@Logger
	private Log log;
	
	@In
	private CurrentContentItemFactory currentContentItemFactory;
	
	@In
	private ContentItemService contentItemService;
	
	@In
	private PerspectiveService perspectiveService;
	
	@In
	private TripleStore tripleStore;
	
	@In("kiwi.ui.perspectiveAction")
	private PerspectiveAction perspectiveAction;
	
	private String title, dlgHeader, mode;
	
	
	
	
	public void setMode(String mode) {
		this.mode = mode;
		
		if("wikipage".equals(mode)) {
			setDlgHeader("Create New Wiki Page");
		} else if("media".equals(mode)) {
			setDlgHeader("Create New Media Content");
		} else if("event".equals(mode)) {
			setDlgHeader("Create New Event");
		} else if("location".equals(mode)) {
			setDlgHeader("Create New Location");
		} else if("concept".equals(mode)) {
			setDlgHeader("Create New SKOS Concept");
		} else if("class".equals(mode)) {
			setDlgHeader("Create New OWL Class");
		} else if("objectproperty".equals(mode)) {
			setDlgHeader("Create New OWL Object Property");
		} else if("dataproperty".equals(mode)) {
			setDlgHeader("Create New OWL Datatype Property");
		} else if("person".equals(mode)) {
			setDlgHeader("Create New FOAF Person");
		}
	}
	
	
	public void save() {
		log.info("saving in mode #0, title #1", mode, title);
		
		if("wikipage".equals(mode)) {
			createWikiPage(title);
		} else if("media".equals(mode)) {
			createMediaPage(title);
		} else if("event".equals(mode)) {
			createEvent(title);
		} else if("location".equals(mode)) {
			createLocation(title);
		} else if("concept".equals(mode)) {
			createConcept(title);
		} else if("class".equals(mode)) {
			createClass(title);
		} else if("objectproperty".equals(mode)) {
			createObjectProperty(title);
		} else if("dataproperty".equals(mode)) {
			createDatatypeProperty(title);
		} else if("person".equals(mode)) {
			createPerson(title);
		} 
		
		title = "";
	}
	
	public void createWikiPage(String title) {
		ContentItem item = contentItemService.createContentItem();
		item.setTitle(title);
		contentItemService.saveContentItem(item);
		
		loadContentItem(item.getId());
		
		log.info("created wiki page #0", item.getKiwiIdentifier());
	}

	
	public void createMediaPage(String title) {
		ContentItem item = contentItemService.createContentItem();
		item.setTitle(title);
		contentItemService.saveContentItem(item);
		
		Perspective p_media = perspectiveService.getPerspective("Media");
		perspectiveService.attachPerspective(item, p_media);
		
		loadContentItem(item.getId());
		
		perspectiveAction.setCurrentPerspective(p_media);
		log.info("created media page #0", item.getKiwiIdentifier());
	}
	
	
	public void createEvent(String title) {
		KiWiUriResource kiwi_event = tripleStore.createUriResource(Constants.NS_KIWI_CAL+"Event");
		
		
		ContentItem item = contentItemService.createContentItem();
		item.setTitle(title);
		item.addType(kiwi_event);
		contentItemService.saveContentItem(item);
		
		loadContentItem(item.getId());
		
		Perspective p_event = perspectiveService.getPerspective("Event");
		perspectiveAction.setCurrentPerspective(p_event);
		log.info("created event page #0", item.getKiwiIdentifier());
	}

	public void createLocation(String title) {
		KiWiUriResource geo_point = tripleStore.createUriResource(Constants.NS_GEO+"Point");
		
		
		ContentItem item = contentItemService.createContentItem();
		item.setTitle(title);
		item.addType(geo_point);
		contentItemService.saveContentItem(item);
		
		loadContentItem(item.getId());
		
		Perspective p_loc = perspectiveService.getPerspective("Location");
		perspectiveAction.setCurrentPerspective(p_loc);
		log.info("created location page #0", item.getKiwiIdentifier());
	}
	
	public void createClass(String title) {
		KiWiUriResource owl_class = tripleStore.createUriResource(Constants.NS_OWL+"Class");
		KiWiUriResource rdfs_class = tripleStore.createUriResource(Constants.NS_RDFS+"Class");
		
		
		ContentItem item = contentItemService.createContentItem();
		item.setTitle(title);
		item.addType(owl_class);
		item.addType(rdfs_class);
		contentItemService.saveContentItem(item);
		
		loadContentItem(item.getId());
		
		Perspective p_class = perspectiveService.getPerspective("Class");
		perspectiveAction.setCurrentPerspective(p_class);
		log.info("created class page #0", item.getKiwiIdentifier());
	}

	public void createObjectProperty(String title) {
		KiWiUriResource owl_objectprop = tripleStore.createUriResource(Constants.NS_OWL+"ObjectProperty");
		KiWiUriResource rdf_prop = tripleStore.createUriResource(Constants.NS_RDF+"Property");
		
		
		ContentItem item = contentItemService.createContentItem();
		item.setTitle(title);
		item.addType(owl_objectprop);
		item.addType(rdf_prop);
		contentItemService.saveContentItem(item);
		
		loadContentItem(item.getId());
		
		Perspective p_property = perspectiveService.getPerspective("Property");
		perspectiveAction.setCurrentPerspective(p_property);
		log.info("created property page #0", item.getKiwiIdentifier());
	}
	
	public void createDatatypeProperty(String title) {
		KiWiUriResource owl_dataprop = tripleStore.createUriResource(Constants.NS_OWL+"DatatypeProperty");
		KiWiUriResource rdf_prop = tripleStore.createUriResource(Constants.NS_RDF+"Property");
		
		
		ContentItem item = contentItemService.createContentItem();
		item.setTitle(title);
		item.addType(owl_dataprop);
		item.addType(rdf_prop);
		contentItemService.saveContentItem(item);
		
		loadContentItem(item.getId());
		
		Perspective p_property = perspectiveService.getPerspective("Property");
		perspectiveAction.setCurrentPerspective(p_property);
		log.info("created property page #0", item.getKiwiIdentifier());
	}
	
	
	public void createConcept(String title) {
		KiWiUriResource skos_concept = tripleStore.createUriResource(Constants.NS_SKOS+"Concept");
		
		ContentItem item = contentItemService.createContentItem();
		item.setTitle(title);
		item.addType(skos_concept);
		contentItemService.saveContentItem(item);
		
		loadContentItem(item.getId());
		
		Perspective p_concept = perspectiveService.getPerspective("Concept");
		perspectiveAction.setCurrentPerspective(p_concept);
		log.info("created concept page #0", item.getKiwiIdentifier());
	}
	
	public void createPerson(String title) {
		KiWiUriResource foaf_person = tripleStore.createUriResource(Constants.NS_FOAF+"Person");
		
		ContentItem item = contentItemService.createContentItem();
		item.setTitle(title);
		item.addType(foaf_person);
		contentItemService.saveContentItem(item);
		
		loadContentItem(item.getId());
		
		Perspective p_person = perspectiveService.getPerspective("Person");
		perspectiveAction.setCurrentPerspective(p_person);
		log.info("created person page #0", item.getKiwiIdentifier());
	}
	
	
	private void loadContentItem(Long currentId) {
        currentContentItemFactory.setCurrentItemId(currentId);
        currentContentItemFactory.refresh();
	}


	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}


	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}


	/**
	 * @return the dlgHeader
	 */
	public String getDlgHeader() {
		return dlgHeader;
	}


	/**
	 * @param dlgHeader the dlgHeader to set
	 */
	public void setDlgHeader(String dlgHeader) {
		this.dlgHeader = dlgHeader;
	}
	
	
}
