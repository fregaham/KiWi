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
package kiwi.model.informationextraction;

import java.io.Serializable;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Version;

import kiwi.model.kbase.KiWiResource;

import org.hibernate.annotations.Index;
import org.hibernate.annotations.OptimisticLockType;


/**
 * Stores the trained classifier.  A ClassifierEntity is associated with a 
 * resource and an Extractlet.
 * 
 * @author Marek Schmidt
 *
 */
@Entity
@org.hibernate.annotations.Entity(optimisticLock=OptimisticLockType.VERSION)
@NamedQueries({	
	@NamedQuery(name  = "kiwi.informationextraction.informationExtractionService.listClassifiers",
				query = "select c " +
						"from kiwi.model.informationextraction.ClassifierEntity c "),
	@NamedQuery(name  = "kiwi.informationextraction.informationExtractionService.listClassifiersByExtractletName",
						query = "select c " +
						"from kiwi.model.informationextraction.ClassifierEntity c " +
						"where c.extractletName = :name"),
	@NamedQuery(name  = "kiwi.informationextraction.informationExtractionService.listClassifiersByResourceId",
				query = "select c " +
						"from kiwi.model.informationextraction.ClassifierEntity c " +
						"where c.resource.id = :resourceId")
})
public class ClassifierEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;
	
	/**
	 * Serialized trained Mallet classifier  
	 */
	private Serializable mallet;
	
	/**
	 * The component name of the associated extractlet.
	 */
	@Basic
	@Index(name="ie_classifierentity_extractletname")
	private String extractletName;
	
	public String getExtractletName() {
		return extractletName;
	}
	
	public void setExtractletName(String extractletName) {
		this.extractletName = extractletName;
	}

	@Version
	private Long version;
	
	/**
	 * The resource that is somehow related to this classifier (it can be a tag, type, etc.)
	 */
	@ManyToOne(cascade = {CascadeType.REFRESH}, fetch=FetchType.LAZY)
    @Index(name="ie_classifierentity_resource")
    private KiWiResource resource;
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setResource(KiWiResource resource) {
		this.resource = resource;
	}

	public KiWiResource getResource() {
		return resource;
	}

	/**
	 * @param version the version to set
	 */
	public void setVersion(Long version) {
		this.version = version;
	}

	/**
	 * @return the version
	 */
	public Long getVersion() {
		return version;
	}

	public void setMallet(Serializable mallet) {
		this.mallet = mallet;
	}

	public Serializable getMallet() {
		return mallet;
	}
}
