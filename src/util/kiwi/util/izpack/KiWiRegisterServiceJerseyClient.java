

package kiwi.util.izpack;


import java.net.URI;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.core.util.MultivaluedMapImpl;


/**
 * A (sun) Jersey web service based KiWi installation
 * registration service plain java client. This class provides
 * plain java access to the KiWi installation registration web
 * service.
 * 
 * @author Mihai
 * @version 0.9
 * @since 0.9
 */
final class KiWiRegisterServiceJerseyClient implements
        KiWiRegisterServiceClient {

    /**
     * The (Jersey client) configuration holder.
     */
    private final ClientConfig config;

    /**
     * Builds an <code>KiWiRegisterServiceJerseyClient</code>
     * instance.
     */
    KiWiRegisterServiceJerseyClient() {
        config = new DefaultClientConfig();
    }

    /**
     * Sends a set of parameters to a given URI (the KiWi install
     * registration end point) and returns the the submit
     * acknowledge result. This method uses the
     * <code>MediaType.TEXT_HTML</code> mime type - so the
     * acknowledge will be an html formated.
     * 
     * @param uriStr the URI for the KiWi install registration
     *            end point, it can not be null or empty String.
     * @param params the map of parameter to submit, it can not
     *            be null or an empty map.
     * @return the acknowledge (html formated).
     */
    @Override
    public String submit(String uriStr, Map<String, String> params) {

        if (uriStr == null || uriStr.isEmpty()) {
            throw new NullPointerException(
                    "The URI can not be null or empty String.");
        }

        if (params == null || params.isEmpty()) {
            throw new NullPointerException(
                    "The paramtter map can not be null or empty.");
        }

        final Client client = Client.create(config);
        final URI uri = UriBuilder.fromUri(uriStr).build();
        final WebResource webResource = client.resource(uri);
        final MultivaluedMap queryParams = mapToMultivaluedMap(params);

        // note the result is HTML, you can also have xml, or
        // plain text (MediaType.TEXT_XML, MediaType.TEXT_PLAIN)
        final Builder accept =
                webResource.path("rest").path("register")
                        .queryParams(queryParams).accept(MediaType.TEXT_HTML);

        final String result = accept.get(String.class);
        return result;
    }

    /**
     * Converts a <code>java.util.Map</code> in to a
     * <code>javax.ws.rs.core.MultivaluedMap</code>.
     * 
     * @param map the <code>java.util.Map</code> to convert.
     * @return the <code>javax.ws.rs.core.MultivaluedMap</code>
     *         for the given <code>java.util.Map</code>.
     */
    private MultivaluedMap mapToMultivaluedMap(Map<String, String> map) {
        final MultivaluedMap<String, String> result = new MultivaluedMapImpl();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            final String key = entry.getKey();
            final String value = entry.getValue();
            result.add(key, value);
        }

        return result;
    }
}
