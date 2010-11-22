/**
 * 
 */


package kiwi.util.izpack;


/**
 * Factory for the register client..
 * 
 * @author Mihai
 */
public class KiWiRegisterServiceClientFactory {

    private static final KiWiRegisterServiceClientFactory THIS =
            new KiWiRegisterServiceClientFactory();

    /**
     * Don't let anyone to instantiate this class.
     */
    private KiWiRegisterServiceClientFactory() {
        // UNIMPLEMENTD
    }

    public static KiWiRegisterServiceClientFactory getInstance() {
        return THIS;
    }

    public KiWiRegisterServiceClient buildClient() {
        final KiWiRegisterServiceJerseyClient client =
                new KiWiRegisterServiceJerseyClient();
        return client;
    }
}
