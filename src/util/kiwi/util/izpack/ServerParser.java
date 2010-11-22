

package kiwi.util.izpack;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


/**
 * Used to manage and manipulate the web container related
 * information. Under the JBoss application server this file is
 * named server.xml and it is placed in the web deployer
 * directory.
 * 
 * @author Mihai
 * @version 1.00
 * @since 1.00
 */
final class ServerParser {

    /**
     * The truststore attribute name.
     */
    private static final String TRUSTSTORE_FILE_ATTRIBUTE = "truststoreFile"; //$NON-NLS-1$

    /**
     * The redirect attribute name.
     */
    private static final String REDIRECT_PORT_ATTRIBUTE = "redirectPort"; //$NON-NLS-1$

    /**
     * The port attribute name.
     */
    private static final String PORT_ATTRIBUTE = "port"; //$NON-NLS-1$

    /**
     * The XPath for the the default connector.
     */
    private static final String DEFALT_CONNECTOR_XPATH =
            "//Connector[@protocol=\"HTTP/1.1\"]"; //$NON-NLS-1$

    /**
     * The XPath for the the AJP connector.
     */
    private static final String AJP_CONNECTOR_XPATH =
            "//Connector[@protocol=\"AJP/1.3\"]"; //$NON-NLS-1$

    /**
     * The XPath for the the SSL connector.
     */
    private static final String SSL_CONNECTOR_XPATH =
            "//Connector[@SSLEnabled=\"true\"]"; //$NON-NLS-1$

    /**
     * The DOM which contains the configuration file for the web
     * container file.
     */
    private final Document document;

    /**
     * The node that contains the default connector information
     * (like attributes).
     */
    private final Node defaultConnector;

    /**
     * The node that contains the SSL connector information (like
     * attributes).
     */
    private final Node sslConnector;

    /**
     * The node that contains the AJP connector information (like
     * attributes).
     */
    private final Node ajpConnector;

    /**
     * Here are the connector information stored.
     */
    private File destinationFile;

    /**
     * Builds a <code>ServerParser</code> instance for a given
     * XML DOM. The given XML DOM must follow WEB container
     * standards server.xml.
     * 
     * @param document the DOM correspond to the application.xml
     *            file.
     * @throws XPathExpressionException by any XML related
     *             errors.
     */
    ServerParser(Document document) throws XPathExpressionException {
        this.document = document;
        final XPath xPath = XPathFactory.newInstance().newXPath();
        sslConnector =
                (Node) xPath.evaluate(SSL_CONNECTOR_XPATH,
                        document.getDocumentElement(), XPathConstants.NODE);
        ajpConnector =
                (Node) xPath.evaluate(AJP_CONNECTOR_XPATH,
                        document.getDocumentElement(), XPathConstants.NODE);

        defaultConnector =
                (Node) xPath.evaluate(DEFALT_CONNECTOR_XPATH,
                        document.getDocumentElement(), XPathConstants.NODE);
    }

    /**
     * Persist the actual state on a given path.
     * 
     * @param path the
     * @throws NullPointerException if the <code>path</code>
     *             argument is null.
     * @throws IOException by any IO related errors.
     */
    void persist(String path) throws IOException {

        if (path == null) {
            throw new NullPointerException("The path can not be null.");
        }

        final File dest = new File(path);
        final String parentStr = dest.getParent();
        final File parent = new File(parentStr);
        if (!parent.exists()) {
            parent.mkdirs();
        }

        final FileOutputStream outputStream = new FileOutputStream(path);
        XMLUtil.persist(outputStream, document);
    }

    /**
     * Persist the actual state for the (web container)
     * configuration in to the actual configuration file. The
     * configuration file can be specified with the
     * <code>setDestinationFile</code> method.
     * 
     * @throws IOException by any IO related porblems.
     * @throws IllegalStateException if the actual configuration
     *             file is null. The configuration file can be
     *             specified with the
     *             <code>setDestinationFile</code> method.
     * @see #setDestinationFile(File)
     */
    void persist() throws IOException {

        if (destinationFile == null) {
            final String msg =
                    "The destiantion file is null, use the setDestinationFile method to specify it.";
            throw new IllegalStateException(msg);
        }

        persist(destinationFile.getAbsolutePath());
    }

    /**
     * The actual web container configuration file.
     * 
     * @return the actual web container configuration file.
     */
    File getDestinationFile() {
        return destinationFile;
    }

    /**
     * Registers a new value for the actual web container
     * configuration file.
     * 
     * @param destinationFile a new value for the actual web
     *            container configuration file, it can not be
     *            null.
     * @throws NullPointerException if the
     *             <code>destinationFile</code> argument is null.
     */
    void setDestinationFile(File destinationFile) {
        if (destinationFile == null) {
            throw new NullPointerException(
                    "The destinatin file can nit be null.");
        }
        this.destinationFile = destinationFile;
    }

    /**
     * Returns the port where the web container accepts HTTP
     * requests.
     * 
     * @return the port where the web container accepts HTTP
     *         requests.
     */
    String getDefaultConnectionPort() {
        final String result =
                ((Element) defaultConnector)
                        .getAttribute(ServerParser.PORT_ATTRIBUTE);
        return result;
    }

    /**
     * Returns the port where the web container accepts SSL
     * requests.
     * 
     * @return the port where the web container accepts SSL
     *         requests.
     */
    String getSSLConnectionPort() {
        final String result =
                ((Element) sslConnector)
                        .getAttribute(ServerParser.PORT_ATTRIBUTE);
        return result;
    }

    /**
     * Registers a new value for the default port, the default
     * port is the port where the web container accepts HTTP
     * requests. The <code>port</code> argument must be integer
     * parsed to a String otherwise an exception raises.
     * 
     * @param port the new new value for the default port.
     * @throws NullPointerException if the <code>port</code>
     *             argument is null.
     * @throws NumberFormatException if the <code>port</code>
     *             argument is something other than an integer
     *             parsed to a String
     */
    void setDefaultConnectionPort(String port) {

        if (port == null) {
            throw new NullPointerException("The port can not be null");
        }

        // here a NumberFormatException raises if the port has
        // other format
        Integer.parseInt(port);

        ((Element) defaultConnector).setAttribute(ServerParser.PORT_ATTRIBUTE,
                port);
    }

    /**
     * Registers a new value for the secure/ssl port, the
     * secure/ssl port is the port where the web container
     * accepts SSL requests. The <code>port</code> argument must
     * be integer parsed to a String otherwise an exception
     * raises. <br>
     * 
     * @param port the new new value for the default port.
     * @throws NullPointerException if the <code>port</code>
     *             argument is null.
     * @throws NumberFormatException if the <code>port</code>
     *             argument is something other than an integer
     *             parsed to a String
     */
    void setSSLConnectionPort(String port) {

        if (port == null) {
            throw new NullPointerException("The port can not be null");
        }

        // here a NumberFormatException raises if the port has
        // other format
        Integer.parseInt(port);

        ((Element) sslConnector)
                .setAttribute(ServerParser.PORT_ATTRIBUTE, port);
        // mihai :
        // the default and the ajp connector uses are using the
        // same redirect port - the secure one.
        // if I change the ssl port I must change the also change
        // the redirect for (port) the default and the ajp
        // connector.
        ((Element) defaultConnector).setAttribute(
                ServerParser.REDIRECT_PORT_ATTRIBUTE, port);
        ((Element) ajpConnector).setAttribute(
                ServerParser.REDIRECT_PORT_ATTRIBUTE, port);
    }

    /**
     * Register a new value for the truststore file.
     * 
     * @param truststoreFile the new value for the truststore
     *            file.
     * @throws NullPointerException if the
     *             <code>truststoreFile</code> argument is null.
     */
    void setTruststoreFile(String truststoreFile) {

        if (truststoreFile == null) {
            throw new NullPointerException("The truststoreFile can not be ull");
        }

        ((Element) sslConnector).setAttribute(
                ServerParser.TRUSTSTORE_FILE_ATTRIBUTE, truststoreFile);
    }

    /**
     * Returns the actual value for the truststore attribute.
     * 
     * @return the actual value for the truststore attribute.
     */
    String getTruststoreFile() {
        return ((Element) sslConnector).getAttribute(TRUSTSTORE_FILE_ATTRIBUTE);
    }
}
