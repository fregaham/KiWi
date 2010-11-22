/*
 * File : RegisterUserInformarin.java.java
 * Date : Oct 10, 2010
 * 
 *
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
 */


package kiwi.util.izpack;


import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;


/**
 * Used to send installation information the the KiWi
 * registration service.
 * 
 * @author Mihai
 * @version 0.9
 * @since 0.9
 */
final class RegisterUserInformation {

    private final static Logger LOG = Logger
            .getLogger(RegisterUserInformation.class.getName());

    // FIXME : use the real IP and port
    private static final String KIWI_INSTALLERLOG_URI =
            "http://localhost:8080/kiwi.installerlog";

// private static final String KIWI_INSTALLERLOG_URI =
// "http://showcase.kiwi-project.eu:8090/kiwi.installerlog";

    /**
     * Sends the installation information to the registration
     * (web) service.
     * 
     * @param agree
     * @param name
     * @param organization
     * @param email
     * @param domain
     */
    static void resiter(String agree, String name, String organization,
            String email, String domain) {

        if (!"on".equalsIgnoreCase(agree)) {
            return;
        }

        final KiWiRegisterServiceClientFactory factory =
                KiWiRegisterServiceClientFactory.getInstance();
        final KiWiRegisterServiceClient client = factory.buildClient();

        final Map<String, String> params = new HashMap<String, String>();
        params.put("name", name);
        params.put("organisation", organization);
        params.put("domainOfActivity", domain);
        params.put("email", email);

        try {
            final String submit = client.submit(KIWI_INSTALLERLOG_URI, params);
        } catch (Exception exception) {
            // IOExcpetions are allowed here, can be that the
            // installer does not have an internet connection.
            LOG.throwing("RegisterUserInformation", "resiter", exception);
        }
    }
}
