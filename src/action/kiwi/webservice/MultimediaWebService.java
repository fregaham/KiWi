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
package kiwi.webservice;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import kiwi.api.content.ContentItemService;
import kiwi.model.content.ContentItem;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;

/**
 * MultimediaWebService
 *
 * @author Sebastian Schaffert
 *
 */
/**
 * This is a RESTful webservice rendering the current content item as HTML,
 * including support for JSF components. The webservice is called by an
 * appropriate facelets ui:include in JSF files as
 * 
 * <ui:include src="/KiWi/seam/resources/services/render/html" />
 * 
 * @author Sebastian Schaffert
 * 
 */
@Name("multimediaWebService")
@Path("/multimedia")
@Scope(ScopeType.STATELESS)
public class MultimediaWebService {

	@Logger
	private Log log;
	
	@In
	ContentItemService contentItemService;

	@GET
	@Path("/get/{id}")
	public Response getMultimedia(@PathParam("id") Long id) {
		ContentItem ci = contentItemService.getContentItemById(id);
		
		log.info("retrieving multimedia content for item with id #0", id);
		
		if(ci != null && ci.getMediaContent() != null) {
			byte[] data = ci.getMediaContent().getData();
			String type = ci.getMediaContent().getMimeType();
			
			return Response.ok(data,type).build();
		} else {
			
			if(ci == null) {
				log.info("content item with database id #0 not found", id);
			} else {
				log.info("content item with database id #0 contains no multimedia content", id);
			}
			return Response.status(500).build();
		}
	}
	
}
