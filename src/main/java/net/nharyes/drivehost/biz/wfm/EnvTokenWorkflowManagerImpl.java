/*
 * Copyright 2016 Luca Zanconato
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.nharyes.drivehost.biz.wfm;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.nharyes.drivecopy.biz.bo.TokenBO;
import net.nharyes.drivecopy.biz.exc.WorkflowManagerException;
import net.nharyes.drivecopy.biz.wfm.BaseWorkflowManager;
import net.nharyes.drivecopy.biz.wfm.TokenWorkflowManager;
import net.nharyes.drivehost.EnvVariables;
import org.apache.commons.configuration.Configuration;

@Singleton
public class EnvTokenWorkflowManagerImpl extends BaseWorkflowManager<TokenBO> implements TokenWorkflowManager {

    private Configuration configuration;

    @Inject
    public EnvTokenWorkflowManagerImpl(Configuration configuration) {

        this.configuration = configuration;
    }

    @Override
    public TokenBO handleWorkflow(TokenBO tokenBO, int action) throws WorkflowManagerException {

        switch (action) {

            case ACTION_GET:
                return get(tokenBO);
            default:
                throw new WorkflowManagerException("Action not found");
        }
    }

    private TokenBO get(TokenBO token) throws WorkflowManagerException {

        TokenBO out = new TokenBO(configuration.getString(EnvVariables.CLIENT_ID), configuration.getString(EnvVariables.CLIENT_SECRET),
                configuration.getString(EnvVariables.ACCESS_TOKEN), configuration.getString(EnvVariables.REFRESH_TOKEN));

        if (out.getAccessToken() == null || out.getClientId() == null || out.getClientSecret() == null || out.getRefreshToken() == null)
            throw new WorkflowManagerException("Please provide all the environment variables required.");

        return out;
    }
}
