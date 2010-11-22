
package rrs.cfg;

import java.util.Set;

import kiwi.api.event.KiWiEvents;
import kiwi.api.extension.ExtensionService;
import kiwi.api.extension.KiWiApplication;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;

/**
 * @author Marek Schmidt
 *
 */
@Name("rrs.cfg")
@Scope(ScopeType.STATELESS)
public class ExtensionConfiguration {
    @Logger
    private Log log;

    @In
    private ExtensionService extensionService;

    @Observer(KiWiEvents.EXTENSIONSERVICE_INIT)
    public void initializeExtension() {

        log.info("KiWi ReReSearch Extension initialising ...");

        extensionService.registerApplication(new KiWiApplication() {

            @Override
            public String getIdentifier() {
                return "rrs";
            }

            @Override
            public String getName() {
                return "ReReSearch";
            }

            @Override
            public String getDescription() {
                return "KiWi ReReSearch Extension";
            }

			@Override
			public Set<String> getPermissibleRoles() {
				return null;
			}
        });

    }
}
