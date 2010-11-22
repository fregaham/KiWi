/*
 * File : JDBCConnectionDataValidator.java.java
 * Date : Aug 13, 2010
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


import java.util.Properties;

import com.izforge.izpack.installer.AutomatedInstallData;
import com.izforge.izpack.installer.DataValidator;


/**
 * This validator is used to register KiWi related instalation
 * information.
 * 
 * @author mihai
 * @version 1.0rc1
 * @since 1.0rc1
 */
public final class UserRegisterValidator implements DataValidator {

    private final String AGREE = "kiwi.register.agree";

    private final String NAME = "kiwi.register.name";

    private final String ORGANISATION = "kiwi.register.organization";

    private final String E_MAIL = "kiwi.register.email";

    private final String DOMAIN = "kiwi.register.domain";

    private final StringBuilder errorMessage;

    private static boolean wasRegister;

    /**
     * 
     */
    public UserRegisterValidator() {
        errorMessage = new StringBuilder();
    }

    @Override
    public boolean getDefaultAnswer() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getErrorMessageId() {
        return errorMessage.toString();
    }

    @Override
    public String getWarningMessageId() {
        return "";
    }

    @Override
    public Status validateData(AutomatedInstallData data) {

        if (wasRegister) {
            // in this way I avoid to register the same info over
            // and over again. This class instance will be used
            // only once during the installation.
            return Status.OK;
        }

        final Properties variables = data.getVariables();

        final String agree = variables.getProperty(AGREE);
        final String name = variables.getProperty(NAME);
        final String eMail = variables.getProperty(E_MAIL);
        final String domain = variables.getProperty(DOMAIN);
        final String organisation = variables.getProperty(ORGANISATION);

        boolean registrationFails;
        try {
            RegisterUserInformation.resiter(agree, name, organisation, eMail,
                    domain);
            registrationFails = false;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            buildErrorMessage(name, eMail, domain, organisation);
            registrationFails = true;
        }

        wasRegister = true;
        return registrationFails ? Status.ERROR : Status.OK;
    }

    private void buildErrorMessage(String name, String eMail, String domain,
            String org) {
        clearErrorMessage();

        errorMessage.append("Register of user : ");
        errorMessage.append(name);
        errorMessage.append(" , eMail : ");
        errorMessage.append(eMail);
        errorMessage.append(" , domain : ");
        errorMessage.append(domain);
        errorMessage.append(" , organisation : ");
        errorMessage.append(domain);

        errorMessage.append(" fails.");
    }

    private void clearErrorMessage() {
        if (!errorMessage.toString().isEmpty()) {
            errorMessage.delete(0, errorMessage.length());
        }
    }
}
