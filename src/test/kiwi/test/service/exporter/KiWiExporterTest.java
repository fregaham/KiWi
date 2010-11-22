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
package kiwi.test.service.exporter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import kiwi.api.content.ContentItemService;
import kiwi.api.importexport.exporter.Exporter;
import kiwi.api.multimedia.MultimediaService;
import kiwi.model.content.ContentItem;
import kiwi.test.base.KiWiTest;

import org.apache.commons.io.FileUtils;
import org.jboss.seam.Component;
import org.jboss.seam.security.Identity;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * MultimediaServiceTest - Test the functionality of the MultimediaService
 *
 * @author Sebastian Schaffert
 *
 */
@Test
public class KiWiExporterTest extends KiWiTest {

    private static String pathToExportFile = "/tmp/export.kiwi";
//    private static String pathToExportFile = "C:\\Users\\Stefan\\Desktop\\export.kiwi";

    private static final String KIWI1PNG = "kiwi1.png";
    private static final String KIWIS_IN_PRAGUE4JPG = "kiwis_in_prague-4.jpg";
    private static final String KIWI_ESWC08DOC = "KiWi_ESWC08.doc";
    private static final String KIWI_ESWC08PDF = "KiWi_ESWC08.pdf";

    private static byte[] kiwi1;
    private static byte[] kiwis_in_prague;
    private static byte[] kiWi_ESWC08DOC;
    private static byte[] kiWi_ESWC08PDF;

    /**
     * Test whether extracting metadata works; for this purpose, we call
     * ContentItemService.createMultimediaItem, which in turn uses extractMetadata from
     * MultimediaService
     *
     * @throws Exception
     */
    @Test
    public void testExportKiWi() throws Exception {


        final URL datapath = this.getClass().getResource("data");

        String[] ontologies = {
            "ontology_kiwi.owl", //    			"imports/foaf.owl",
        //    			"imports/sioc.owl",
        //    			"imports/hgtags.owl",
        //    			"imports/skos-core.rdf",
        //    			"imports/exif.rdf"
        };
        setupDatabase(ontologies);

        /*
         * Test a JPEG file
         */
        new FacesRequest() {

            @Override
            protected void invokeApplication() throws Exception {

                Identity.setSecurityEnabled(false);

                MultimediaService ms = (MultimediaService) Component.getInstance("multimediaService");
                ContentItemService cs = (ContentItemService) Component.getInstance("contentItemService");

                File f1 = new File(datapath.getPath() + File.separator + KIWIS_IN_PRAGUE4JPG);

                kiwi1 = FileUtils.readFileToByteArray(f1);
                String mimeType = ms.getMimeType(f1);
                String fileName = f1.getName();

                ContentItem c1 = cs.createMediaContentItem(kiwi1, mimeType, fileName);

                File f2 = new File(datapath.getPath() + File.separator + KIWI1PNG);

                kiwis_in_prague = FileUtils.readFileToByteArray(f2);
                String mimeType2 = ms.getMimeType(f2);
                String fileName2 = f2.getName();

                ContentItem c2 = cs.createMediaContentItem(kiwis_in_prague, mimeType2, fileName2);

                File f3 = new File(datapath.getPath() + File.separator + KIWI_ESWC08PDF);

                kiWi_ESWC08DOC = FileUtils.readFileToByteArray(f3);
                String mimeType3 = ms.getMimeType(f3);
                String fileName3 = f3.getName();

                ContentItem c3 = cs.createMediaContentItem(kiWi_ESWC08DOC, mimeType3, fileName3);

                File f4 = new File(datapath.getPath() + File.separator + KIWI_ESWC08DOC);

                kiWi_ESWC08PDF = FileUtils.readFileToByteArray(f4);
                String mimeType4 = ms.getMimeType(f4);
                String fileName4 = f4.getName();

                ContentItem c4 = cs.createMediaContentItem(kiWi_ESWC08PDF, mimeType4, fileName4);

            }
        }.run();

        /*
         * Test Export of this components
         */
        new FacesRequest() {

            @Override
            protected void invokeApplication() throws Exception {

                Identity.setSecurityEnabled(false);

                MultimediaService ms = (MultimediaService) Component.getInstance("multimediaService");
                ContentItemService cs = (ContentItemService) Component.getInstance("contentItemService");

                Exporter exporter = (Exporter) Component.getInstance("kiwi.service.exporter.kiwi");

                Collection<ContentItem> items = cs.getContentItems();

//                File f = new File("/tmp/export.kiwi");
                File f = new File(pathToExportFile);
                OutputStream out = FileUtils.openOutputStream(f);
                exporter.exportItems(items, "application/x-kiwi", out);
                out.close();
            }
        }.run();

        /*
         * Includs new ContentItems?
         */
        new FacesRequest() {

            @Override
            protected void invokeApplication() throws Exception {

                String source = pathToExportFile;

                String[] titlesItems = {
                    KIWIS_IN_PRAGUE4JPG, KIWI1PNG, KIWI_ESWC08PDF, KIWI_ESWC08DOC,};

                String[] titlesMedia = {
                    KIWIS_IN_PRAGUE4JPG, KIWI1PNG, KIWI_ESWC08PDF, KIWI_ESWC08DOC,};

                List listItems = Arrays.asList(titlesItems);
                Set<String> setItems = new HashSet(listItems);

                List listMedia = Arrays.asList(titlesMedia);
                Set<String> setMedia = new HashSet(listMedia);

                try {
                    ZipFile zipFile = new ZipFile(source);
                    Enumeration<? extends ZipEntry> zipEntryEnum = zipFile.entries();

                    while (zipEntryEnum.hasMoreElements()) {
                        ZipEntry zipEntry = zipEntryEnum.nextElement();
                        extractEntry(zipFile, zipEntry, setItems, setMedia);
                    }
                    new File(pathToExportFile).delete();

                    Assert.assertEquals(0, setItems.size());
                } catch (FileNotFoundException e) {
                    Assert.fail(e.getMessage());
                } catch (IOException e) {
                    Assert.fail(e.getMessage());
                }
            }
        }.run();

        clearDatabase();
    }

    /**
     * try to remove entry from Set, if it is in Set
     *
     * @param zf zipfile of entry
     * @param entry entry in the given zipfile
     * @param allNewContentItems
     * @throws IOException
     * @throws JDOMException
     */
    private static void extractEntry(ZipFile zf, ZipEntry entry,
            Set<String> titlesItems, Set<String> titlesMedia)
            throws IOException, JDOMException {

        if (!entry.isDirectory()) {

            InputStream is = null;

            try {
                is = zf.getInputStream(entry);

                isNewContentItem(is, titlesItems,titlesMedia, zf, entry);

            } finally {
                is.close();
            }
        }
    }

    /**
     * True: if InputStream is a RDF-File and includs title-node with a entry
     * of titles
     *
     * @param inputStream Stream of a ZipEntry
     * @param titles Set of all search ContentItems
     * @return
     */
    private static boolean isNewContentItem(InputStream inputStream,
            Set<String> titlesItems, Set<String> titlesMedia, ZipFile zf, ZipEntry entry)
            throws JDOMException, IOException {

        try {
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(inputStream);

            Element root = doc.getRootElement();

            List<Element> children = root.getChildren();

            // In directory media tagname is file-name in items title
            for (int i = 0; i < children.size(); i++) {
                if ((children.get(i).getName().equals("title")
                        && titlesItems.contains(children.get(i).getText())) ||
                        (children.get(i).getName().equals("file-name")
                        && titlesMedia.contains(children.get(i).getText()))) {

                    String entryString = null;

                    // Associated binary equals input binary?
                    if(entry.getName().endsWith("meta")) {
                        InputStream meta = null;

                        ByteArrayOutputStream bosMeta = null;
                        InputStream binary = null;
                        ByteArrayOutputStream bosBinary = null;

                        try {
                            // Find *.binary of *.meta
                            entryString =
                                    entry.getName().replaceFirst("meta", "bin");
                            ZipEntry binaryEntry = zf.getEntry(entryString);

                            // Transfer ZipEntry to byte Array
                            binary = zf.getInputStream(binaryEntry);
                            byte[] baBinary = new byte[binary.available()];
                            binary.read(baBinary);


                            byte[] binaryComparison =
                                    findBinaryFile(children.get(i).getText());

                            Assert.assertEquals(baBinary, binaryComparison);

                        } finally {
                            meta.close();
                            bosMeta.close();
                            binary.close();
                            bosBinary.close();
                        }
                    }

                    if(children.get(i).getName().equals("title")) {
                        titlesItems.remove(children.get(i).getText());
                    } else if(children.get(i).getName().equals("file-name")) {
                        titlesMedia.remove(children.get(i).getText());
                    }
                    return true;
                }
            }
        } catch (Exception e) {
        }

        return false;
    }

    /**
     * Search the Binaryfile of the RDF-File with the given title.
     *
     *
     * @param title title of Binaryfile e.g. kiwi1.png
     * @return originaly byte array of the binary. Null if it not found.
     */
    private static byte[] findBinaryFile(String title) {

        if(title.equals(KIWI1PNG)) {

            return kiwi1;
        } else if(title.equals(KIWIS_IN_PRAGUE4JPG)) {

            return kiwis_in_prague;
        } else if(title.equals(KIWI_ESWC08DOC)) {

            return kiWi_ESWC08DOC;
        } else if(title.equals(KIWI_ESWC08PDF)) {

            return kiWi_ESWC08PDF;
        }

        return null;

    }
}
