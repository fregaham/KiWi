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
package kiwi.action.setup;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import kiwi.api.config.ConfigurationService;
import kiwi.api.informationextraction.InformationExtractionService;
import kiwi.api.perspectives.PerspectiveService;
import kiwi.api.reasoning.ReasoningService;
import kiwi.api.search.SemanticIndexingService;
import kiwi.api.triplestore.TripleStore;
import kiwi.model.Constants;
import kiwi.model.content.ContentItem;
import kiwi.model.perspective.Perspective;
import kiwi.service.config.ConfigurationServiceImpl;

import org.apache.commons.io.FileUtils;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.log.Log;


/**
 * SetupDataAction
 *
 * @author Sebastian Schaffert
 *
 */
@Name("kiwi.setup.setupDataAction")
@Scope(ScopeType.PAGE)
//@Transactional
public class SetupDataAction implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Logger
    private Log log;

    @In
    FacesMessages facesMessages;

	@In
	private PerspectiveService perspectiveService;

    
    @In("kiwi.core.semanticIndexingService")
    private SemanticIndexingService semanticIndexingService;
    
    @In
    private ConfigurationService configurationService;
    
    @In
    private TripleStore tripleStore;

    private boolean runSemVector = true;

    private boolean loadHelp = false;

    private boolean loadDemo = false;

    private boolean changePersistence = true;
    
    private boolean informationExtraction = true;
    
    private boolean reasoning = true;

    public String loadData() {
        doSemanticIndexing();
        doChangePersistence();
        doPerspectives();
        doInformationExtraction();
        doReasoning();
        
        
        ConfigurationServiceImpl.setupInProgress = false;
        ConfigurationServiceImpl.configurationInProgress = false;
        return "success";
    }
    
    
    

    public void doSemanticIndexing() {
        if(runSemVector) {
            semanticIndexingService.reIndex();
        }
    }

    public void doChangePersistence() {
        if(changePersistence) {
            // TODO: it's a hack!
            String earPath = System.getProperty("ear.path");

            File kiwiP = new File(earPath + File.separator + "KiWi.jar" +
                                           File.separator + "META-INF" +
                                           File.separator + "persistence.xml");
            try {
                String s = FileUtils.readFileToString(kiwiP);
                s = s.replace("\"create\"", "\"validate\"");
                FileUtils.writeStringToFile(kiwiP,s);
            } catch (IOException e) {
                facesMessages.add("I/O error while reading KiWi persistence.xml");
                log.error("I/O error while reading KiWi persistence.xml",e);
            }
        }
    }
    
    
    public void doPerspectives() {
    	ContentItem ci_resource = tripleStore.createUriResource(Constants.NS_KIWI_CORE+"ContentItem").getContentItem();
    	
    	Perspective p_metadata = new Perspective("Metadata", "metadata");
    	p_metadata.setDescription("Perspective for viewing and editing the RDF metadata of a content item. Displays RDF datatype properties as a tabular list");
    	p_metadata.getTypes().add(ci_resource);
    	perspectiveService.addPerspective(p_metadata);
    	
    	//SunSpace Perspective - added by Josef
    	ContentItem ci_sunspace = tripleStore.createUriResource(Constants.NS_KIWI_CORE+"SunSpaceItem").getContentItem();
    	Perspective p_sunspace = new Perspective("SunSpace", "sunspace");
    	p_sunspace.setDescription("SunSpace Use Case perspective");
    	p_sunspace.getTypes().add(ci_sunspace);
    	perspectiveService.addPerspective(p_sunspace);
   	
    	ContentItem ci_photo = tripleStore.createUriResource(Constants.NS_KIWI_CORE+"Image").getContentItem();
    	ContentItem ci_media = tripleStore.createUriResource(Constants.NS_KIWI_CORE+"MediaContent").getContentItem();
    	Perspective p_media = new Perspective("Media", "media");
    	p_media.setDescription("Perspective for viewing and editing media resources. Displays additional information, e.g. EXIF.");
    	p_media.getTypes().add(ci_photo);
    	p_media.getTypes().add(ci_media);
    	perspectiveService.addPerspective(p_media);
    	
    	ContentItem ci_tag = tripleStore.createUriResource(Constants.NS_KIWI_CORE+"Tag").getContentItem();
    	Perspective p_tag = new Perspective("Tag", "tag");
    	p_tag.setDescription("Perspective for viewing and editing content items used as tag. Displays additional information, e.g. tag usage");
    	p_tag.getTypes().add(ci_tag);
    	perspectiveService.addPerspective(p_tag);

    	ContentItem ci_location = tripleStore.createUriResource(Constants.NS_GEO+"Point").getContentItem();
    	Perspective p_location = new Perspective("Location", "location");
    	p_location.setDescription("Perspective for viewing and editing location. Displays location on a Google Map.");
    	p_location.getTypes().add(ci_location);
    	perspectiveService.addPerspective(p_location);

    	ContentItem ci_event = tripleStore.createUriResource(Constants.NS_KIWI_CAL+"Event").getContentItem();
    	Perspective p_event = new Perspective("Event", "event");
    	p_event.setDescription("Perspective for viewing and editing events. Displays event start and end in a calendar.");
    	p_event.getTypes().add(ci_event);
    	perspectiveService.addPerspective(p_event);

    	ContentItem ci_rdfs_class = tripleStore.createUriResource(Constants.NS_RDFS+"Class").getContentItem();
    	ContentItem ci_owl_class = tripleStore.createUriResource(Constants.NS_OWL+"Class").getContentItem();
    	Perspective p_class = new Perspective("Class", "class");
    	p_class.setDescription("Perspective for viewing and editing classes. Displays additional class-related information for the content item.");
    	p_class.getTypes().add(ci_rdfs_class);
    	p_class.getTypes().add(ci_owl_class);
    	perspectiveService.addPerspective(p_class);

    	ContentItem ci_rdfs_property = tripleStore.createUriResource(Constants.NS_RDF+"Property").getContentItem();
    	ContentItem ci_owl_op = tripleStore.createUriResource(Constants.NS_OWL+"ObjectProperty").getContentItem();
    	ContentItem ci_owl_dp = tripleStore.createUriResource(Constants.NS_OWL+"DatatypeProperty").getContentItem();
    	Perspective p_property = new Perspective("Property", "property");
    	p_property.setDescription("Perspective for viewing and editing properties. Displays additional property-related information for the content item.");
    	p_property.getTypes().add(ci_rdfs_property);
    	p_property.getTypes().add(ci_owl_op);
    	p_property.getTypes().add(ci_owl_dp);
    	perspectiveService.addPerspective(p_property);

    	ContentItem ci_skos_concept = tripleStore.createUriResource(Constants.NS_SKOS+"Concept").getContentItem();
    	Perspective p_concept = new Perspective("Concept", "concept");
    	p_concept.setDescription("Perspective for viewing and editing SKOS concepts. Displays additional concept-related information for the content item.");
    	p_concept.getTypes().add(ci_skos_concept);
    	perspectiveService.addPerspective(p_concept);

    	ContentItem ci_person = tripleStore.createUriResource(Constants.NS_FOAF+"Person").getContentItem();
    	ContentItem ci_user = tripleStore.createUriResource(Constants.NS_KIWI_CORE+"User").getContentItem();
    	Perspective p_person = new Perspective("Person", "person");
    	p_person.setDescription("Perspective for viewing and editing person profiles. Displays additional person-related information for the content item.");
    	p_person.getTypes().add(ci_person);
    	p_person.getTypes().add(ci_user);
    	perspectiveService.addPerspective(p_person);
    	
    	Perspective p_extraction = new Perspective("Extraction", "extraction");
    	p_extraction.setDescription("Perspective for discovering other instances using machine learning.");
    	perspectiveService.addPerspective(p_extraction);
    	
    	Perspective p_equity = new Perspective("Equity", "equity");
    	p_equity.setEdit(false);
    	p_equity.setWidget(false);
    	p_equity.setSearch(false);
    	p_extraction.setDescription("Perspective for viewing the community equity analysis of a content item.");
    	perspectiveService.addPerspective(p_equity);
    }

    public void doInformationExtraction() {
		String gateHome = configurationService.getConfiguration("kiwi.work.dir") + File.separator + "gate";
		configurationService.setConfiguration("kiwi.gate.home", gateHome);

    	
    	configurationService.setBooleanConfiguration("kiwi.informationextraction.informationExtractionService.enabled", informationExtraction);
		configurationService.setBooleanConfiguration("kiwi.informationextraction.informationExtractionService.online", informationExtraction);

		InformationExtractionService ies = (InformationExtractionService) Component.getInstance("kiwi.informationextraction.informationExtractionService");
		ies.setEnabled(informationExtraction);
		ies.setOnline(informationExtraction);

    }
    
    public void doReasoning() {
    	ReasoningService rs = (ReasoningService) Component.getInstance("kiwi.core.reasoningService");
    	rs.setReasoningEnabled(reasoning);
    }
    
    /**
     * @return the runSemVector
     */
    public boolean isRunSemVector() {
        return runSemVector;
    }



    /**
     * @param runSemVector the runSemVector to set
     */
    public void setRunSemVector(boolean runSemVector) {
        this.runSemVector = runSemVector;
    }



    /**
     * @return the loadHelp
     */
    public boolean isLoadHelp() {
        return loadHelp;
    }



    /**
     * @param loadHelp the loadHelp to set
     */
    public void setLoadHelp(boolean loadHelp) {
        this.loadHelp = loadHelp;
    }



    /**
     * @return the loadDemo
     */
    public boolean isLoadDemo() {
        return loadDemo;
    }



    /**
     * @param loadDemo the loadDemo to set
     */
    public void setLoadDemo(boolean loadDemo) {
        this.loadDemo = loadDemo;
    }



    /**
     * @return the changePersistence
     */
    public boolean isChangePersistence() {
        return changePersistence;
    }



    /**
     * @param changePersistence the changePersistence to set
     */
    public void setChangePersistence(boolean changePersistence) {
        this.changePersistence = changePersistence;
    }




	/**
	 * @return the informationExtraction
	 */
	public boolean isInformationExtraction() {
		return informationExtraction;
	}




	/**
	 * @param informationExtraction the informationExtraction to set
	 */
	public void setInformationExtraction(boolean informationExtraction) {
		this.informationExtraction = informationExtraction;
	}




	/**
	 * @return the reasoning
	 */
	public boolean isReasoning() {
		return reasoning;
	}




	/**
	 * @param reasoning the reasoning to set
	 */
	public void setReasoning(boolean reasoning) {
		this.reasoning = reasoning;
	}


}
