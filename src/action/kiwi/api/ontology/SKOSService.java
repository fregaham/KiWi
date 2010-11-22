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
 * 
 * 
 */
package kiwi.api.ontology;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import kiwi.model.kbase.KiWiResource;
import kiwi.model.ontology.SKOSConcept;

/**
 * SKOSService - A service for managing SKOS concepts in the KiWi triple store. Offers methods for querying,
 * listing and updating concepts.
 * 
 * @author Rolf Sint
 * @author Sebastian Schaffert
 *
 */
public interface SKOSService {


    /**
     * Returns the list of all top concepts in the thesaurus.
     * @return
     */
    public List<SKOSConcept> getTopConcepts();

    /**
     * Returns a concept named conceptName or null
     * @param conceptName
     * @return
     */
    public SKOSConcept getConcept(String conceptName);

    //	public SKOSConcept createSkosConcept(String name);

    /**
     * @param name
     * @param loc
     * @return
     */
    public SKOSConcept createSkosConcept(String name, Locale loc);

    /**
     * It returns all existing skos concepts given a provided label.
     * @param label
     * @return
     */
    public List<SKOSConcept> getConceptByLabel(String label);


    /**
     * Return all concepts with a reverse Algo.
     *
     * @return A List with all SKOSConcepts
     */
    public List<SKOSConcept> getAllConcepts();

    /**
     * Return given concept with subconcepts
     *
     * @return A List with all Sub-SKOSConcepts
     */
    public List<SKOSConcept> getAllConcepts(SKOSConcept concept);

    /**
     * Return a list of all concepts whose title starts with the given prefix. Makes use of
     * KiWi's search functionality for the autocompletion
     * 
     * @param prefix the title prefix to look for, at least two characters long
     * @return a list of KiWiResources representing the skos:Concepts whose title starts with prefix
     */
    public List<KiWiResource> autocompleteConcept(String prefix);

    /**
     * Get the preferred label of the concept given as parameter. Currently, multilingual
     * labels are not supported.
     * 
     * @param concept the concept for which to get the preferred label
     * @return the preferred label of the concept
     */
    public String getPreferredLabel(KiWiResource concept);

    /**
     * Set the preferred label of the concept given as parameter. Removes all existing
     * preferred labels. Currently, multilingual labels are not supported.
     * 
     * @param concept the concept for which to set the preferred label
     * @param label the new preferred label
     */
    public void setPreferredLabel(KiWiResource concept, String label);

    /**
     * Get a list of all alternative labels of the concept given as argument. Currently,
     * multilingual labels are not supported; in such cases, all alternative labels will
     * be listed without distinguishing languages.
     * 
     * @param concept the concept for which to list the alternative labels
     * @return a list of strings representing the alternative labels of the concept, ordered
     *         in alphabetical order
     */
    public List<String> listAlternativeLabels(KiWiResource concept);

    /**
     * Add the given alternative label to the given concept. If the label already exists,
     * nothing happens.
     * @param concept the concept for which to add an alternative label
     * @param label the alternative label to add
     */
    public void addAlternativeLabel(KiWiResource concept, String label);

    /**
     * Remove the given alternative label from the given concept. If the label is not an
     * alternative label of the concept, nothing happens.
     * @param concept
     * @param label
     */
    public void removeAlternativeLabel(KiWiResource concept, String label);

    /**
     * List the broader concepts of the concept given as parameter. Will query for both,
     * skos:broader and inverse skos:narrower to retrieve all relevant concepts. If
     * inferred is false, only base relations that are not also inferred are listed.
     * 
     * @param concept the concept for which to list the broader resources.
     * @return
     */
    public List<KiWiResource> listBroader(KiWiResource concept, boolean inferred);

    /**
     * Add a skos:broader relation from the KiWiResource concept to the KiWiResource
     * broader. Will also add the inverse relationship skos:narrower.
     * @param concept
     * @param broader
     */
    public void addBroader(KiWiResource concept, KiWiResource broader);

    /**
     * Remove the skos:broader relation between the two KiWiresources given as
     * parameter. Will also remove the inverse skos:narrower relation if it exists.
     * @param concept
     * @param broader
     */
    public void removeBroader(KiWiResource concept, KiWiResource broader);

    /**
     * List all narrower resources of the concept. Will query for both, skos:narrower
     * and inverse skos:broader to retrieve all relevant concepts. If inferred is false,
     * only base relations that are not also inferred are listed.
     * 
     * @param concept
     * @param inferred
     * @return
     */
    public List<KiWiResource> listNarrower(KiWiResource concept, boolean inferred);

    /**
     * Add a skos:narrower relation from the KiWiResource concept to the KiWiResource
     * narrower. Will also add the inverse relationship skos:broader.
     * @param concept
     * @param broader
     */
    public void addNarrower(KiWiResource concept, KiWiResource narrower);

    /**
     * Remove the skos:narrower relation between the two KiWiresources given as
     * parameter. Will also remove the inverse skos:broader relation if it exists.
     * @param concept
     * @param broader
     */
    public void removeNarrower(KiWiResource concept, KiWiResource narrower);

    /**
     * List all related resources of the concept. Will query for both, skos:related
     * and inverse skos:related to retrieve all relevant concepts. If inferred is false,
     * only base relations that are not also inferred are listed.
     * 
     * @param concept
     * @param inferred
     * @return
     */
    public List<KiWiResource> listRelated(KiWiResource concept, boolean inferred);

    /**
     * Add a skos:related relation from the KiWiResource concept to the KiWiResource
     * narrower. Will also add the inverse relationship skos:related from related to
     * concept.
     * 
     * @param concept
     * @param broader
     */
    public void addRelated(KiWiResource concept, KiWiResource related);

    /**
     * Remove the skos:related relation between the two KiWiresources given as
     * parameter. Will also remove the inverse skos:related relation if it exists.
     * @param concept
     * @param broader
     */
    public void removeRelated(KiWiResource concept, KiWiResource related);

    /**
     * Returns all the SKOS concepts for a given nesting level.
     * 
     * @param level the nesting level must be great or equals
     *            with 0.
     * @return all the SKOS concepts for a given nesting level.
     */
    Set<SKOSConcept> getAllSKOSConcepts(int level);

    /**
     * Returns all the SKOS concepts for a given top concept and
     * a nesting level.
     * 
     * @param concept the top level concept.
     * @param level the nesting level must be great or equals
     *            with 0.
     * @return all the SKOS concepts for a given nesting level.
     */
    Set<SKOSConcept> getAllSKOSConcepts(SKOSConcept concept, int level);
}
