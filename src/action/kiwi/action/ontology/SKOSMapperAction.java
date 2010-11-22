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


import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import kiwi.api.ontology.SKOSPrefixMapperService;
import kiwi.api.ontology.SKOSService;
import kiwi.model.ontology.SKOSConcept;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;


/**
 * @author Mihai
 * @version 1.01
 * @since 1.01
 */
@Scope(ScopeType.APPLICATION)
@Name("skosMapperAction")
public class SKOSMapperAction {

    @Logger
    private Log log;

    @In
    private SKOSService skosService;

    @In
    private SKOSPrefixMapperService skosPrefixMapperService;

    private SKOSConceptUIHelper[] conceptHelpers;

    private int conceptLevel;

    /**
     * Builds a <code>SkosMapperAction</code> instance.
     */
    public SKOSMapperAction() {
        // UNIMPLEMENTED
    }

    @Create
    public void init() {
        conceptLevel = 1;
        final List<SKOSConcept> topConcepts = skosService.getTopConcepts();
        conceptHelpers = new SKOSConceptUIHelper[topConcepts.size()];
        int coneptIdex = 0;
        for (SKOSConcept concept : topConcepts) {

            // mihai : I am not sure about the title, this can be
            // that two different SKOS concepts have the same
            // title.
            final String title = concept.getTitle();
            conceptHelpers[coneptIdex++] =
                    new SKOSConceptUIHelper(1, title, title);

        }
    }

    public void defaultMap() {
        skosPrefixMapperService.removeAllMapping();
        final List<SKOSConcept> topConcepts = skosService.getTopConcepts();

        Iterator<SKOSConcept> actual = topConcepts.iterator();
        int level = 0;
        // mihai : the Set guarantee no duuplicates
        Set<SKOSConcept> allNarrower = new HashSet<SKOSConcept>();
        while (actual.hasNext()) {
            // mihai : I know this loop in not efficent but I run
            // out of time.
            // I can imagine that are more elegant ways to get
            // the top concept fro a given concept but I don't
            // know so much about SKOS
            SKOSConcept nextConcept = actual.next();

            final String defaultPrefix = nextConcept.getTitle() + "_prefix";
            skosPrefixMapperService.assingSKOSToPrefix(nextConcept,
                    defaultPrefix, level);

            // mihai : very ugly way to the top concept.
            final boolean isTopLevel = level > 0;
            final SKOSConcept topConcept =
                    isTopLevel ? nextConcept : getTopConcept(nextConcept);

            final HashSet<SKOSConcept> nextNarrower = nextConcept.getNarrower();
            allNarrower.addAll(nextNarrower);
            final Iterator<SKOSConcept> nextNarrowerIterator =
                    nextNarrower.iterator();
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
    }

    private SKOSConcept getTopConcept(SKOSConcept nextConcept) {
        SKOSConcept top = nextConcept;
        while (top.getBroader() != null) {
            top = top.getBroader();
        }

        return top;
    }

    private void show(Set<SKOSConcept> narrower) {
        for (SKOSConcept concept : narrower) {
            System.out.println("--->" + concept.getTitle());
        }
    }

    private String buildPrefix(SKOSConcept concept) {
        // mihai : use this method to customize the way how the
        // prefix for each concept is builded.
        final String preferredLabel = concept.getPreferredLabel();
        final String title = concept.getTitle();

        if (preferredLabel != null) {
            return preferredLabel;
        }

        if (title != null) {
            return title;
        }

        return "default prefix";
    }

    /**
     * @return the conceptLevel
     */
    public int getConceptLevel() {
        return conceptLevel;
    }

    /**
     * @param conceptLevel the conceptLevel to set
     */
    public void setConceptLevel(int conceptLevel) {
        this.conceptLevel = conceptLevel;
    }

    /**
     * @return the conceptHelpers
     */
    public SKOSConceptUIHelper[] getConceptHelpers() {
        return conceptHelpers;
    }

    /**
     * @param conceptHelpers the conceptHelpers to set
     */
    public void setConceptHelpers(SKOSConceptUIHelper[] conceptHelpers) {
        this.conceptHelpers = conceptHelpers;
    }
}
