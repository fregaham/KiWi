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
package kiwi.admin.action;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import kiwi.api.config.ConfigurationService;
import kiwi.api.content.ContentItemService;
import kiwi.api.ontology.OntologyService;
import kiwi.api.perspectives.PerspectiveService;
import kiwi.model.content.ContentItem;
import kiwi.model.ontology.KiWiClass;
import kiwi.model.perspective.Perspective;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.log.Log;

/**
 * PerspectiveConfigurationAction - used to add, remove and edit perspective descriptions.
 *
 * @author Sebastian Schaffert
 *
 */
@Name("kiwi.admin.perspectiveConfigurationAction")
@Scope(ScopeType.PAGE)
public class PerspectiveConfigurationAction {
	
	@Logger
	private Log log;
	
	@In
	private ConfigurationService configurationService;
	
	@In
	private ContentItemService contentItemService;
	
	@In
	private PerspectiveService perspectiveService;

	@In
	private FacesMessages facesMessages;

	
	private List<Perspective> perspectives;

	private Perspective newPerspective;
	
	private List<String> selectedPerspectiveTypes, availablePerspectiveTypes;
	
	private Perspective selectedPerspective;
	
	@In
	private OntologyService ontologyService;
	
	@In
	private EntityManager entityManager;

	/**
	 * @return the perspectives
	 */
	public List<Perspective> getPerspectives() {
		if(perspectives == null) {
			perspectives = new LinkedList<Perspective>();
			perspectives.addAll(perspectiveService.listPerspectives());
		}
		
		return perspectives;
	}
	
	public void removePerspective(Perspective p) {
		log.info("removePerspective called for perspective with id #0, name #1", p.getId(), p.getName());
		
		perspectiveService.removePerspective(p);
//		entityManager.flush();

		perspectives = null;
	}
	
	
	public Perspective getNewPerspective() {
		if(newPerspective == null) {
			newPerspective = new Perspective();
		}
		return newPerspective;
	}
	
	public void addPerspective() {
		log.info("adding new perspective: #0", newPerspective.getName());
		
		if(newPerspective.getName() != null && !newPerspective.getName().equals("") &&
				newPerspective.getPath() != null && !newPerspective.getPath().equals("")) {
			perspectiveService.addPerspective(newPerspective);
//			entityManager.flush();
			
			perspectives = null;
			
			newPerspective = new Perspective();
		} else {
			facesMessages.add("Please fill in all required fields before adding a new perspective");
		}
	}
	
	public void commit() {
		log.info("updating perspectives");
		
		for(Perspective p : perspectives) {
			perspectiveService.updatePerspective(p);
		}
	}

	/**
	 * @return the selectedPerspectiveTypes
	 */
	public List<String> getSelectedPerspectiveTypes() {
		return selectedPerspectiveTypes;
	}

	/**
	 * @param selectedPerspectiveTypes the selectedPerspectiveTypes to set
	 */
	public void setSelectedPerspectiveTypes(List<String> selectedPerspectiveTypes) {
		this.selectedPerspectiveTypes = selectedPerspectiveTypes;
	}

	/**
	 * @return the availablePerspectiveTypes
	 */
	public List<String> getAvailablePerspectiveTypes() {
		return availablePerspectiveTypes;
	}

	/**
	 * @param availablePerspectiveTypes the availablePerspectiveTypes to set
	 */
	public void setAvailablePerspectiveTypes(List<String> availablePerspectiveTypes) {
		this.availablePerspectiveTypes = availablePerspectiveTypes;
	}

	/**
	 * @return the selectedPerspective
	 */
	public Perspective getSelectedPerspective() {
		return selectedPerspective;
	}

	/**
	 * @param selectedPerspective the selectedPerspective to set
	 */
	public void setSelectedPerspective(Perspective selectedPerspective) {
		this.selectedPerspective = selectedPerspective;
	}
	
	
	public void selectPerspective(Perspective p) {
		setSelectedPerspective(p);
		
		selectedPerspectiveTypes = new LinkedList<String>();
		for(ContentItem ci : p.getTypes()) {
			selectedPerspectiveTypes.add(ci.getResource().getKiwiIdentifier());
		}
		
		availablePerspectiveTypes = new LinkedList<String>();
		for(KiWiClass cls : ontologyService.listClasses()) {
			if(!selectedPerspectiveTypes.contains(cls.getKiwiIdentifier()) ) {
				availablePerspectiveTypes.add(cls.getKiwiIdentifier());
			}
		}
		Collections.sort(selectedPerspectiveTypes);
		Collections.sort(availablePerspectiveTypes);
	}
	
	public void saveSelectedTypes() {
		facesMessages.add("saving selected types...");
		
		selectedPerspective.getTypes().clear();
		for(String type : selectedPerspectiveTypes) {
			selectedPerspective.getTypes().add(contentItemService.getContentItemByKiwiId(type));
		}
		
	}
	
	public <T> List<T> setToList(Set<T> set) {
		LinkedList<T> result = new LinkedList<T>();
		for(T elem : set) {
			result.add(elem);
		}
		return result;
	}
}
