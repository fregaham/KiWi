/*
 * File : SkosConceptUIHelper.java
 * Date : Nov 7, 2010
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


import java.io.Serializable;

import kiwi.model.content.ContentItem;
import kiwi.model.kbase.KiWiUriResource;
import kiwi.model.ontology.SKOSConcept;


/**
 * This Helper is only used to encapsulate model specific
 * information in java bean manner.
 * 
 * @author Mihai
 * @version 1.01
 * @since 1.01
 */
public class SKOSConceptUIHelper implements Serializable {

    private static final long serialVersionUID = -808958766714416201L;

    private String patrentConceptURI;
    
    private boolean required;
    
    private String title;

    private String [] prefixes;

    public SKOSConceptUIHelper(SKOSConcept topConcept, String title, int prefixexCount, String prefix) {
        this.title = title;
        final ContentItem delegate = topConcept.getDelegate();
        final String uri = ((KiWiUriResource) delegate.getResource()).getUri();
        this.patrentConceptURI = uri;
        this.prefixes = new String[prefixexCount];
        this.required = true;
        for (int i = 0; i < prefixexCount; i++) {
            prefixes[i] = prefix;
        }
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
     * @return the prefixes
     */
    public String[] getPrefixes() {
        return prefixes;
    }

    /**
     * @param prefixes the prefixes to set
     */
    public void setPrefixes(String[] prefixes) {
        this.prefixes = prefixes;
    }

    /**
     * @return the patrentConceptURI
     */
    public String getPatrentConceptURI() {
        return patrentConceptURI;
    }

    /**
     * @param patrentConceptURI the patrentConceptURI to set
     */
    public void setPatrentConceptURI(String patrentConceptURI) {
        this.patrentConceptURI = patrentConceptURI;
    }
    
    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        final StringBuilder p = new StringBuilder();
        for (String prefix : prefixes) {
            p.append(prefix);
            p.append(",");
        }
        return "SKOSConceptUIHelper [title=" + title + " prefix=" + p + "]";
    }

}
