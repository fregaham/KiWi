/*
 * File : SkosMapperAction.java
 * Date : Nov 3, 2010
 * 
 *
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

package kiwi.action.ontology;




import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import kiwi.api.ontology.SKOSService;
import kiwi.api.search.SolrService;
import kiwi.api.sun.SunTagMapperService;
import kiwi.exception.NamespaceResolvingException;
import kiwi.model.content.ContentItem;
import kiwi.model.kbase.KiWiUriResource;
import kiwi.model.ontology.SKOSConcept;
import kiwi.util.MD5;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.log.Log;

/**
 * @author Mihai
 * @version 1.01
 * @since 1.01
 */
@Name("skosMapperAction")
@Scope(ScopeType.APPLICATION)
public class SKOSMapperAction {

    @Logger
    private Log log;

    /**
     * Sends messages to the uert interface.
     */
    @In
    private FacesMessages facesMessages;

    @In(create = true)
    private SunTagMapperService sunTagMapperService;

    @In(create = true)
    private SKOSService skosService;

    /**
     * A SKOSConceptUIHelper can represent a SKOS Concept tree in a very simplistic way.
     */
    private SKOSConceptUIHelper[] conceptHelpers;

    /**
     * Builds a <code>SkosMapperAction</code> instance.
     */
    public SKOSMapperAction() {
        // UNIMPLEMENTED
    }

    /**
     * Acts like a constructor in the Seam specific environment.
     */
    @Create
    public void init() {
        updateMappings();
    }

    // FIXME : make the event key a constant!
    @Observer("sunTagMapperService.updateMappings")
    public void updateMappings() {
        final List<SKOSConcept> topConcepts = skosService.getTopConcepts();
        conceptHelpers = new SKOSConceptUIHelper[topConcepts.size()];
        int index = 0;
        for (SKOSConcept concept : topConcepts) {
            conceptHelpers[index++] = getConceptUIHelper(concept);
        }
    }

    /**
     * Builds a <code>SKOSConceptUIHelper</code> for a given
     * <code>SKOSConcept</code> instance.
     *
     * @param concept the SKOSConcept for the helper, it can not be null.
     * @return a <code>SKOSConceptUIHelper</code> for a given
     * <code>SKOSConcept</code> instance.
     * @see SKOSConceptUIHelper
     */
    private SKOSConceptUIHelper getConceptUIHelper(SKOSConcept concept) {

        if (concept == null) {
            throw new NullPointerException("The SKOS concept can not be null.");
        }

        final SKOSConceptUIHelper helper = new SKOSConceptUIHelper();
        final String title = concept.getTitle();
        helper.setTitle(title);
        helper.setConcept(concept);

        Iterator<SKOSConcept> actual = concept.getNarrower().iterator();
        // I choose 1 because the level 1 is the top level concepts
        int level = 1;

        // FIXME : this loop is resource expensive,
        // find an other way to get all the narrower.
        Set<SKOSConcept> allNarrower = new HashSet<SKOSConcept>();
        while (actual.hasNext()) {
            SKOSConcept nextConcept = actual.next();
            final Set<SKOSConcept> nextNarrower = nextConcept.getNarrower();

            final Iterator<SKOSConcept> nextNarrowerIterator = nextNarrower
                    .iterator();
            allNarrower.addAll(nextNarrower);
            while (nextNarrowerIterator.hasNext()) {
                final SKOSConcept nextC = nextNarrowerIterator.next();
                final Set<SKOSConcept> narrower = nextC.getNarrower();
                allNarrower.addAll(narrower);
            }

            if (!actual.hasNext()) {
                actual = allNarrower.iterator();
                allNarrower = new HashSet<SKOSConcept>();
                level++;
            }
        }

        final String[] prefixes = new String[level];
        final String realTitle = title + "_prefix";
        Arrays.fill(prefixes, realTitle);
        helper.setPrefixes(prefixes);
        helper.setRequired(true);

        return helper;
    }

    /**
     * Generated the default mapping for all the SKOS Concepts. This method may
     * require a lot of time/resources.
     */
    public void generateSKOSMap() {
        sunTagMapperService.defaultMapping();
    }

    /**
     * @return the conceptHelpers
     */
    public SKOSConceptUIHelper[] getConceptHelpers() {
        return conceptHelpers;
    }

    /**
     * @param conceptHelpers
     *            the conceptHelpers to set
     */
    public void setConceptHelpers(SKOSConceptUIHelper[] conceptHelpers) {
        this.conceptHelpers = conceptHelpers;
    }

    /**
     * Commits all the changes done in the user interface. <br>
     * This method may requires time and/or resources use it properly.
     */
    public void commitAll() {
        for (SKOSConceptUIHelper helper : conceptHelpers) {
            final SKOSConcept topConcept = helper.getConcept();
            final String[] prefixes = helper.getPrefixes();
            final boolean required = helper.isRequired();

            for (int prefIndex = 0; prefIndex < prefixes.length; prefIndex++) {
                final Set<SKOSConcept> search = sunTagMapperService.search(
                        topConcept, prefIndex);
                for (SKOSConcept toAssing : search) {
                    final String prefix = prefixes[prefIndex];
                    logAssigng(topConcept, prefIndex, toAssing, prefix);
                    try {
                        sunTagMapperService.assignResource(toAssing, prefix,
                                -1, required);
                    } catch (NamespaceResolvingException e) {
                        log.error(e.getMessage(), e);
                    }
                }
            }
        }
    }

    /**
     * @param topConcept
     * @param nestLevel
     * @param toAssing
     * @param prefix
     */
    private void logAssigng(final SKOSConcept topConcept, int nestLevel,
            SKOSConcept toAssing, final String prefix) {

        final String topConceptTitle = topConcept.getTitle();
        final String topUri = ((KiWiUriResource)topConcept.getResource()).getUri();
        final String md5 = MD5.md5sum(topUri);
        final String title = toAssing.getTitle();
        log.debug("Assignment pramters[Top concent : #0 Top concent URI :#1, Top concent URI-md5:#2, Concept title:#3, Prefix for concept #4, Nesting level :#5",topConceptTitle, topUri, md5, title, prefix, nestLevel);

//        System.out.println("======================");
//        System.out.println("Top concent : " + topConceptTitle);
//        System.out.println("Top concent URI : " + topUri);
//
//        System.out.println("Top concent URI-md5 : " + md5);
//
//        System.out.println("Concept title : " + title);
//        System.out.println("Prefix : " + prefix);
//        System.out.println("Prefix index " + nestLevel);
//        System.out.println("======================");
    }
}
