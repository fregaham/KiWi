/**
 * 
 */


package kiwi.util.izpack;


import java.util.Map;


/**
 * Define the functionality for the KiWi installation
 * registration service client. The KiWi registration service is
 * used to register track/log the KiWi installation related
 * informations (for all the KiWi users).
 * 
 * @author Mihai
 * @version 0.9
 * @since 0.9
 */
public interface KiWiRegisterServiceClient {

    /**
     * Submits a <code>java.util.Map</code> of given key-value
     * pairs to the KiWi registration service.
     * 
     * @param uri the uri for the web service.
     * @param params the key-value pairs to submit.
     * @return the submit acknowledge - this depends on the
     *         implementation.
     */
    String submit(String uri, Map<String, String> params);
}
