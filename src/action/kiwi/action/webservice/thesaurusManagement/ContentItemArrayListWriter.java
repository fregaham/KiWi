/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008 The KiWi Project. All rights reserved.
 * http://www.kiwi-project.eu
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  KiWi designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 *
 */
package kiwi.action.webservice.thesaurusManagement;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import kiwi.api.importexport.ExportService;
import kiwi.model.content.ContentItem;

import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;

@Name("contentItemArrayListWriter") 
@Scope(ScopeType.APPLICATION)
@Provider
@Produces("application/rdf+xml")
public class ContentItemArrayListWriter implements MessageBodyWriter<ArrayList<ContentItem>> {

	@Logger
	private Log log;
	
	@In(required = false)
	private Calendar since;
	
	@In(required = false)
	private WS ws;
	
	
	public void writeTo(ArrayList<ContentItem> contentItems, Class<?> type, Type genericType,			
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders,
			java.io.OutputStream entityStream) throws IOException,
			WebApplicationException {
			
			log.info("processingx ContentItemArrayListWriter");
		
			ExportService ies = (ExportService) Component.getInstance("kiwi.core.exportService"); 
			if(since != null){
				ies.exportUpdateOntology(contentItems, "application/rdf+xml",entityStream, since.getTime());}
			else{
				ies.exportItems(contentItems, "application/rdf+xml",entityStream);
			}
		
	}

	public long getSize(ArrayList<ContentItem> st, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediatype) {
		return -1;
	}

	public boolean isWriteable(Class<?> type, Type genericType,
		Annotation[] annotations, MediaType mediaType) {
		return (ArrayList.class == type) && (ws == WS.ArrayCIs);
	}
}
