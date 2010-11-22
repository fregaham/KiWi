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
package kiwi.admin.action;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;


import kiwi.api.content.ContentItemService;
import kiwi.model.content.ContentItem;
import javax.servlet.http.HttpServletResponse;
import javax.faces.context.FacesContext;
import javax.faces.context.ExternalContext;
import javax.servlet.ServletOutputStream;
import kiwi.api.importexport.exporter.Exporter;

import org.apache.commons.io.FileUtils;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;
import org.jboss.seam.security.Identity;

/**
 * @author Stefan Robert
 * 
 */
@Name("kiwi.admin.exportAction")
public class ExportAction {

    private static String pathToExportFile = "/tmp/export.kiwi";
    @Logger
    private Log log;
    @In
    private ContentItemService contentItemService;
    @In("kiwi.service.exporter.kiwi")
    private Exporter exporter;
    @In(value = "#{facesContext.externalContext}")
    private ExternalContext extCtx;
    @In(value = "#{facesContext}")
    private FacesContext facesContext;

    public String exportAllData() {

        Identity.setSecurityEnabled(false);

        Collection<ContentItem> items = getContentItemService().getContentItems();

        File f = new File(pathToExportFile);
        OutputStream out = null;
        try {
            out = FileUtils.openOutputStream(f);
            exporter.exportItems(items, "application/x-kiwi", out);
        } catch (IOException ex) {
            log.error("Error by creating zip file #0", ex.getMessage());
        } finally {
            try {
                out.close();
            } catch (IOException ex) {
                log.error("Error by close outputstream #0", ex.getMessage());
            }
        }

        log.info("Create export file #0", f.getAbsolutePath());

        try {
            InputStream newIs = new FileInputStream(f);
            byte[] baBinary = new byte[newIs.available()];
            newIs.read(baBinary);

            HttpServletResponse response = (HttpServletResponse) extCtx.getResponse();

            String contentType = "application/zip";
            String fileName = "export.kiwi";

            response.setContentType(contentType);
            response.addHeader("Content-disposition", "attachment; filename=\"" + fileName + "\"");

            ServletOutputStream os = response.getOutputStream();
            os.write(baBinary);
            os.flush();
            os.close();
            facesContext.responseComplete();
        } catch (Exception ex) {
            log.error("Error by downloading file #0", ex.getMessage());
        } finally {
            try {
                out.close();
            } catch (IOException ex) {
                log.error("Error by close outputstream #0", ex.getMessage());
            }
        }

        return null;

    }

    /**
     * @return the contentItemService
     */
    public ContentItemService getContentItemService() {
        return contentItemService;
    }

    /**
     * @param contentItemService the contentItemService to set
     */
    public void setContentItemService(ContentItemService contentItemService) {
        this.contentItemService = contentItemService;
    }
}
