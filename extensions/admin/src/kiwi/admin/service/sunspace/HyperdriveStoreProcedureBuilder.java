/*
 * File : HyperdriveStoreProcedureBuilder.java.java
 * Date : Sep 29, 2010
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


package kiwi.admin.service.sunspace;


import java.util.HashMap;
import java.util.Map;

import kiwi.model.ceq.StoredProcedure;


/**
 * @author Mihai
 * @version 0.9
 * @since 0.9
 */
final class HyperdriveStoreProcedureBuilder implements StoredProcedureBuilder {

    private final static String HEADER_PREFIX = "CREATE OR REPLACE FUNCTION ";

    private final static String HEADER_SUBFIX =
            "(create_time timestamp, action_time timestamp, unit_value double precision) RETURNS double precision AS $$";

    private final static String FOOLTER = "$$ LANGUAGE plpgsql;";

    private final static Map<String, String> methodMapping;
    static {
        methodMapping = new HashMap<String, String>();
        methodMapping.put("AccessDeniedActivity", "accessDeniedAging");
        methodMapping.put("AddTypeActivity", "addtypeAging");
        methodMapping.put("AnnotateActivity", "annotateAging");
        methodMapping.put("AddFriendActivity", "addFriendAging");
        methodMapping.put("ChangeAuthorActivity", "changeAuthorAging");
        methodMapping.put("CommentActivity", "commentAging");
        methodMapping.put("CreateActivity", "createAging");
        methodMapping.put("DeleteActivity", "deleteAging");
        methodMapping.put("EditActivity", "editAging");
        methodMapping.put("GroupCreatedActivity", "groupCreatedAging");
        methodMapping.put("LoginActivity", "loginAging");
        methodMapping.put("LogoutActivity", "logoutAging");
        methodMapping.put("RateActivity", "rateAging");
        methodMapping.put("RegisterActivity", "registerAging");
        methodMapping.put("RemoveFriendActivity", "removeFriendAging");
        methodMapping.put("ShareActivity", "shareAging");
        methodMapping.put("SearchActivity", "searchAging");
        methodMapping.put("UserCreatedActivity", "userCreatedAging");
        methodMapping.put("UserRemvedActivity", "userRemovedAging");
        methodMapping.put("TweetActivity", "tweetAging");
        methodMapping.put("VisitActivity", "visitAging");
    }

    private static final StoredProcedureBuilder THIS =
            new HyperdriveStoreProcedureBuilder();

    /**
     * Don't let anybody to instatiate this class.
     */
    private HyperdriveStoreProcedureBuilder() {
        // UNIMPLEMENTED
    }

    static StoredProcedureBuilder getInstance() {
        return THIS;
    }

    @Override
    public String getSQLStatement(StoredProcedure procedure) {
        final StringBuilder result = new StringBuilder();
        result.append(HEADER_PREFIX);
        final String action = procedure.getAction();
        final String agingFunctionName =
                methodMapping.containsKey(action)
                        ? methodMapping.get(action)
                        : "generalAging";

        result.append(agingFunctionName);
        result.append(HEADER_SUBFIX);
        result.append("\n");
        result.append(procedure.getContent());
        result.append("\n");
        result.append(FOOLTER);

        return result.toString();
    }
}
