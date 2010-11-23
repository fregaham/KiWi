/*
 * File : SKOSToPrefixMapper.java
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


package kiwi.model.ontology;


import java.io.Serializable;

import javax.persistence.*;


/**
 * @author Mihai
 * @version 1.00
 * @since 1.00
 */
@NamedQueries({
        @NamedQuery(name = "select.skosMapForURI", query = "SELECT skos_map FROM SKOSToPrefixMapper AS skos_map  WHERE skos_map.uri=:uri"),
        @NamedQuery(name = "select.skosMapForPrefix", query = "SELECT skos_map from SKOSToPrefixMapper AS skos_map WHERE skos_map.prefix=:prefix"),
        @NamedQuery(name = "select.skosMapForPrefixAndLabel", query = "SELECT skos_map from SKOSToPrefixMapper AS skos_map WHERE skos_map.prefix=:prefix AND skos_map.label LIKE :pattern"),
        @NamedQuery(name = "select.allSkosMaps", query = "SELECT skos_map from SKOSToPrefixMapper AS skos_map"),
        @NamedQuery(name = "select.skosMapForLevel", query = "SELECT skos_map from SKOSToPrefixMapper AS skos_map WHERE skos_map.level=:level"),
        @NamedQuery(name = "select.skosMapForLevelAndParent", query = "SELECT skos_map from SKOSToPrefixMapper AS skos_map WHERE skos_map.level=:level AND skos_map.parentURI=:parentURI"),
        @NamedQuery(name = "select.skosMapForTopConceptPrefixAndLevel", 
                    query = "SELECT skos_map from SKOSToPrefixMapper AS skos_map " +
        		            " WHERE skos_map.parentURI=:parentURI " +
        		            " AND skos_map.level=:level " +
                            " AND skos_map.prefix=:prefix"),
        @NamedQuery(name = "select.skosTopConceptLevel", query = "SELECT MAX(skos_map.level) from SKOSToPrefixMapper AS skos_map WHERE skos_map.parentURI=:parentURI"),
        @NamedQuery(name = "update.skosConceptsForParentAndLevel", query = 
                  "UPDATE SKOSToPrefixMapper skos_map " +
         		  " SET skos_map.prefix=:prefix " +
                  " WHERE skos_map.parentURI=:parentURI AND skos_map.level=:level"),
        @NamedQuery(name = "delete.allSkosMaps", query = "DELETE FROM SKOSToPrefixMapper")})
@Entity
public class SKOSToPrefixMapper implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String uri;

    private String parentURI;

    private String label;

    private String prefix;

    private int level;

    public SKOSToPrefixMapper(String uri, String parentURI, String prefix,
            String label, int level) {
        this.uri = uri;
        this.parentURI = parentURI;
        this.prefix = prefix;
        this.label = label;
        this.level = level;
    }

    public SKOSToPrefixMapper() {
        // UNIMPLEMENTD
    }

    /**
     * @return the id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * @param uri the uri to set
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * @return the prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * @param prefix the prefix to set
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(String label) {
        this.label = label;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getParentURI() {
        return parentURI;
    }

    public void setParentURI(String parentURI) {
        this.parentURI = parentURI;
    }

    @Override
    public String toString() {
        return "SKOSToPrefixMapper [uri=" + uri + ", parentURI=" + parentURI
                + ", label=" + label + ", prefix=" + prefix + ", level="
                + level + "]";
    }
}
