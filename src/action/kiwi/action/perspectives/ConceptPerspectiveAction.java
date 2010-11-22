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

import kiwi.api.ontology.SKOSService;
import kiwi.model.content.ContentItem;
import kiwi.model.kbase.KiWiResource;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;

/**
 * ConceptPerspectiveAction
 *
 * @author Sebastian Schaffert
 *
 */
@Name("kiwi.core.ui.skosPerspectiveAction")
@Scope(ScopeType.PAGE)
public class ConceptPerspectiveAction {
	
	@In(create=true)
	private SKOSService skosService;
	
	@In
	private ContentItem currentContentItem;
	
	@In
	private FacesMessages facesMessages;
	
	private KiWiResource selectedResource;
	
	private String prefLabel;
	
	private String altLabel = "";
	
	public void addBroader() {
		if(selectedResource != null) {
			skosService.addBroader(currentContentItem.getResource(), selectedResource);
			
			selectedResource = null;
		} else {
			facesMessages.add("no resource has been selected");
		}
	}
	
	public void addNarrower() {
		if(selectedResource != null) {
			skosService.addNarrower(currentContentItem.getResource(), selectedResource);
			
			selectedResource = null;
		} else {
			facesMessages.add("no resource has been selected");
		}
	}

	public void addRelated() {
		if(selectedResource != null) {
			skosService.addRelated(currentContentItem.getResource(), selectedResource);
			
			selectedResource = null;
		} else {
			facesMessages.add("no resource has been selected");
		}
	}

	public void addAltLabel() {
		if(altLabel != null && !"".equals(altLabel)) {
			skosService.addAlternativeLabel(currentContentItem.getResource(), altLabel);
			
			altLabel = "";
		} else {
			facesMessages.add("no label has been selected");
		}
	}
	
	public void changePrefLabel() {
		if(prefLabel != null && !"".equals(prefLabel)) {
			skosService.setPreferredLabel(currentContentItem.getResource(), prefLabel);
			
			prefLabel = "";
		} else {
			facesMessages.add("no label has been selected");
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

	/**
	 * @return the prefLabel
	 */
	public String getPrefLabel() {
		return prefLabel;
	}

	/**
	 * @param prefLabel the prefLabel to set
	 */
	public void setPrefLabel(String prefLabel) {
		this.prefLabel = prefLabel;
	}

	/**
	 * @return the altLabel
	 */
	public String getAltLabel() {
		return altLabel;
	}

	/**
	 * @param altLabel the altLabel to set
	 */
	public void setAltLabel(String altLabel) {
		this.altLabel = altLabel;
	}
	
	

}
