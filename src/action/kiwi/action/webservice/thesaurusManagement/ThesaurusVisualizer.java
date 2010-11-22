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
package kiwi.action.webservice.thesaurusManagement;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import javax.servlet.http.HttpServletResponse;
import javax.faces.context.FacesContext;
import javax.faces.context.ExternalContext;
import javax.servlet.ServletOutputStream;
import kiwi.api.content.ContentItemService;
import kiwi.api.importexport.ExportService;
import org.apache.commons.io.FileUtils;
import org.jboss.seam.security.Identity;

import kiwi.api.ontology.SKOSService;
import kiwi.model.content.ContentItem;
import kiwi.model.ontology.SKOSConcept;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;

/**
 * @author Rolf Sint
 * 
 */
@Name("thesaurusVisualizer")
@Scope(ScopeType.CONVERSATION)
public class ThesaurusVisualizer implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private static String pathToExportFile = "/tmp/thesaurus.rdf";
    @In(create = true)
    private SKOSService skosService;
    private HashMap<String, List<String>> narrowerRelationship;
    @In
    private ContentItemService contentItemService;
    @Logger
    private Log log;
    @In("kiwi.core.exportService")
    private ExportService exportService;
    @In(value = "#{facesContext.externalContext}")
    private ExternalContext extCtx;
    @In(value = "#{facesContext}")
    private FacesContext facesContext;

    public void generateXml() {

        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        try {
            //ugly hack ;-/		(it is possible to avoid the hack by changing the flash file that it should look up to the tmp directory where the xml file is stored)
            XMLStreamWriter writer = factory.createXMLStreamWriter(new FileOutputStream(
                    "../server/default/deploy/KiWi.ear/kiwiext-w2k.jar/visualizeThesaurus/relationBrowser.xml"));

            writer.writeStartDocument();

            writer.writeStartElement("RelationViewerData");

            writer.writeStartElement("Settings");
            writer.writeAttribute("appTitle", "Browse Thesaurus");
            writer.writeAttribute("startID", "uri::http://www.eduhui.at/concepts#778"); //TODO
            writer.writeAttribute("defaultRadius", "150");
            writer.writeAttribute("maxRadius", "180");

            writer.writeStartElement("RelationTypes");
            writer.writeStartElement("DirectedRelation");
            writer.writeAttribute("color", "0xAAAAAA");
            writer.writeAttribute("lineSize", "4");
            writer.writeAttribute("letterSymbol", "N");
            writer.writeAttribute("labelText", "Narrower");
            writer.writeEndElement();

            writer.writeStartElement("MyCustomRelation");
            writer.writeAttribute("color", "0xB7C631");
            writer.writeAttribute("lineSize", "4");
            writer.writeAttribute("letterSymbol", "B");
            writer.writeAttribute("labelText", "Broader");
            writer.writeEndElement();

            writer.writeStartElement("UndirectedRelation");
            writer.writeAttribute("color", "0x85CDE4");
            writer.writeAttribute("lineSize", "4");
            writer.writeEndElement();
            writer.writeEndElement();

            writer.writeStartElement("NodeTypes");
            writer.writeEmptyElement("Node");
            writer.writeEmptyElement("Comment");
            writer.writeEmptyElement("Person");
            writer.writeEmptyElement("Document");
            writer.writeEndElement();

            writer.writeEndElement(); //End setting

            LinkedList<SKOSConcept> topConcepts = (LinkedList<SKOSConcept>) skosService.getTopConcepts();

            HashMap<String, String> topConceptRelationship = new HashMap<String, String>();
            narrowerRelationship = new HashMap<String, List<String>>();

            writer.writeStartElement("Nodes");

            SKOSConcept old = null;
            for (SKOSConcept skosConcept : topConcepts) {
                if (old != null) {
                    topConceptRelationship.put(skosConcept.getKiwiIdentifier(), old.getKiwiIdentifier());
                }
                writer.writeStartElement("Node");
                writer.writeAttribute("id", skosConcept.getKiwiIdentifier());
                String name = SKOSConceptUtils.getConceptLabelsInLanguages(skosConcept, new Locale("de"), new Locale("en"));
                writer.writeAttribute("name", name);

                writer.writeEndElement();
                old = skosConcept;
                generateNarrowers(skosConcept, writer);

            }
            writer.writeEndElement(); //End Nodes

            writer.writeStartElement("Relations");
            Collection<String> topConceptRel = topConceptRelationship.keySet();

            for (String s : topConceptRel) {
                writer.writeStartElement("UndirectedRelation");
                writer.writeAttribute("fromID", "" + s);
                writer.writeAttribute("toID", topConceptRelationship.get(s));
                writer.writeEndElement();
            }

            Collection<String> narrowerRel = narrowerRelationship.keySet();
            //iterate over all keys
            for (String s : narrowerRel) {
                List<String> na = narrowerRelationship.get(s);
                for (String s1 : na) {
                    //narrower
                    writer.writeStartElement("DirectedRelation");
                    writer.writeAttribute("fromID", "" + s);
                    writer.writeAttribute("toID", s1);
                    writer.writeEndElement();
                }
            }

            writer.writeEndElement(); //end relations
            writer.writeEndElement(); //End RelationViewerData
            writer.writeEndDocument();
            writer.close();

        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (XMLStreamException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void generateNarrowers(SKOSConcept skosConcept, XMLStreamWriter writer) {
        HashSet<SKOSConcept> narrower = skosConcept.getNarrower();
        List<String> temp = new LinkedList<String>();

        for (SKOSConcept skosConceptNar : narrower) {
            temp.add(skosConceptNar.getKiwiIdentifier());
            try {
                writer.writeStartElement("Node");
                writer.writeAttribute("id", skosConceptNar.getKiwiIdentifier());
                String label = SKOSConceptUtils.getConceptLabelsInLanguages(skosConceptNar, new Locale("de"), new Locale("en"));
                writer.writeAttribute("name", label);
                writer.writeEndElement();
                narrowerRelationship.put(skosConcept.getKiwiIdentifier(), temp);
            } catch (XMLStreamException e) {
                e.printStackTrace();
            }
            //recursive call to get the children from the children
            generateNarrowers(skosConceptNar, writer);
        }
    }

    public String exportData() {

        Identity.setSecurityEnabled(false);

        List<SKOSConcept> items = skosService.getAllConcepts();

        File f = new File(pathToExportFile);
        OutputStream out = null;
        try {
            out = FileUtils.openOutputStream(f);
            exportService.exportItems(transform(items), "application/rdf+xml", out);
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

            String contentType = "application/rdf+xml";
            String fileName = "thesaurus.rdf";

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
     * transforms a List of SKOSConcepts to a List of ContentItems
     * @param sc
     * @return
     */
    private LinkedList<ContentItem> transform(List<SKOSConcept> sc) {
        LinkedList<ContentItem> skContentItems = new LinkedList<ContentItem>();
        for (SKOSConcept s : sc) {
            skContentItems.add(s.getDelegate());
        }
        return skContentItems;
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
