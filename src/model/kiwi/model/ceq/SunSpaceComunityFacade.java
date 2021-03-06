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
package kiwi.model.ceq;


import java.util.List;
import java.util.Set;

import kiwi.model.Constants;
import kiwi.model.annotations.KiWiFacade;
import kiwi.model.annotations.RDF;
import kiwi.model.annotations.RDFType;
import kiwi.model.content.ContentItem;
import kiwi.model.content.ContentItemI;


/**
 * A sun space community is the sun space equivalent in kiwi.
 *
 * @author Sebastian Schaffert
 */
@KiWiFacade
@RDFType({Constants.NS_SUN_SPACE + "SunSpaceComunity"})
public interface SunSpaceComunityFacade extends ContentItemI {

    /**
     * The URL for this sun space community.
     *
     * @return URL for this sun space community.
     */
    @RDF(Constants.NS_SUN_SPACE + "url")
    String getURL();

    void setURL(String url);

    /**
     * An HRI description for this sun space community.
     *
     * @return HRI description for this sun space community.
     */
    @RDF(Constants.NS_SUN_SPACE + "description")
    String getDescription();

    void setDescription(String description);

    @RDF(Constants.NS_SUN_SPACE + "participants")
    List<ContentItem> getParticipants();

    void setParticipants(List<ContentItem> participants);


    @RDF(Constants.NS_SUN_SPACE + "administrators")
    List<ContentItem> getAdministrators();

    void setAdministrators(List<ContentItem> admins);

    /**
     * Sets a new list of content items for this space. 
     * The old one will be overwited.
     * 
     * @param contents all the items for this space.
     */
    void setContents(Set<ContentItem> contents);

    /**
     * 
     * @return
     */
    Set<ContentItem> getContents();
}
