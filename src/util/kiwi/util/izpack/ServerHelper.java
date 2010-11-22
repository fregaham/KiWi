/*
 * File : ServerHelper.java
 * Date : Oct 16, 2010
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


package kiwi.util.izpack;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;


/**
 * Registers and persists the web server related information
 * (like the default and ssl ports).
 * 
 * @author Mihai
 * @version 1.00
 * @since 1.00
 */
final class ServerHelper {

    /**
     * Web container configureation file.
     */
    private static final String SERVER_XML_FILE = "server.xml"; //$NON-NLS-1$

    /**
     * All the logging messages for this class go over this
     * field.
     */
    private static final Logger LOG = Logger.getLogger(ServerHelper.class
            .getName());

    /**
     * Don't let anybody to instantiate this class.
     */
    private ServerHelper() {
        // UNIMPLEMENTED
    }

    /**
     * Consumes the provided information and persists it in a
     * proper form.
     * 
     * @param serverCfgFile the configuration file for the web
     *            container (a.k.a. server.xml).
     * @param jBossHome the application server home directory.
     *            This must be the root directory for your jBoss
     *            installation.
     * @param port the port where the web container will accept
     *            HTTP requests.
     * @param sslPort the port where the web container will
     *            accept SSL requests.
     * @throws ParserConfigurationException indicates a serious
     *             configuration error for the XML parser.
     * @throws SAXException encapsulates a general SAX error or
     *             warning.
     * @throws IOException by any IO problem.
     * @throws XPathExpressionException signals an error in an
     *             XPath expression.
     */
    static void processServer(final File serverCfgFile, final String jBossHome,
            String port, String sslPort) throws ParserConfigurationException,
            SAXException, IOException, XPathExpressionException {

        final InputStream dsAsStream = new FileInputStream(serverCfgFile);
        final Document serverDocument = XMLUtil.getDocument(dsAsStream);
        final ServerParser serverParser = new ServerParser(serverDocument);

        serverParser.setDefaultConnectionPort(port);
        serverParser.setSSLConnectionPort(sslPort);

        final String serverFilePath = getDataServerFilePath(jBossHome);
        final String fileMsg =
                String.format(
                        "Default port : %s, ssl port %s are stored in the file %s.",
                        port, sslPort, serverFilePath);
        LOG.finest(fileMsg);

        final String truststoreFile = "${java.home}/lib/security/cacerts";
        serverParser.setTruststoreFile(truststoreFile);

        serverParser.persist(serverFilePath);
    }

    /**
     * Returns the web container configuration file (server.xml)
     * file depending on a given JBoss home directory.
     * 
     * @param jBossHome the application server home directory.
     *            This must be the root directory for your jBoss
     *            installation.
     * @return the web container configuration file (server.xml)
     *         file depending on a given JBoss home directory.
     * @see #getServerDefaultDeployDir(String)
     */
    private static String getDataServerFilePath(String jBossHome) {
        final String deployDir = getServerDefaultDeployDir(jBossHome);
        final StringBuffer result = new StringBuffer(deployDir);
        result.append(ServerHelper.SERVER_XML_FILE);
        return result.toString();
    }

    /**
     * Returns the parent directory for the web container
     * configuration file (server.xml) file depending on a given
     * JBoss home directory.
     * 
     * @param jBossHome the application server home directory.
     *            This must be the root directory for your jBoss
     *            installation.
     * @return the parent directory for the web container
     *         configuration file (server.xml) file depending on
     *         the given JBoss home directory.
     */
    private static String getServerDefaultDeployDir(String jBossHome) {
        final StringBuffer result = new StringBuffer();
        result.append(jBossHome);
        result.append(File.separator);
        result.append("server");
        result.append(File.separator);
        result.append("default");
        result.append(File.separator);
        result.append("deploy");
        result.append(File.separator);
        result.append("jboss-web.deployer");
        result.append(File.separator);

        return result.toString();
    }

}
