/**
 * Copyright 2016 Pinterest, Inc.
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
package com.pinterest.teletraan.resource;

import com.pinterest.deployservice.bean.Resource;
import com.pinterest.deployservice.bean.Role;
import com.pinterest.deployservice.bean.TokenRolesBean;
import com.pinterest.deployservice.common.CommonUtils;
import com.pinterest.deployservice.dao.TokenRolesDAO;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.security.Authorizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.*;
import java.net.URI;
import java.util.List;

public abstract class TokenRoles {
    private static final Logger LOG = LoggerFactory.getLogger(TokenRoles.class);
    final static public long VALIDATE_TIME = 10 * 365 * 24 * 60 * 60 * 1000L;
    private final TokenRolesDAO tokenRolesDAO;
    private final Authorizer authorizer;

    @Context
    UriInfo uriInfo;

    public TokenRoles(TeletraanServiceContext context) {
        tokenRolesDAO = context.getTokenRolesDAO();
        authorizer = context.getAuthorizer();
    }

    public List<TokenRolesBean> getByResource(SecurityContext sc, String resourceId,
        Resource.Type resourceType) throws Exception {
        authorizer.authorize(sc, new Resource(resourceId, resourceType), Role.ADMIN);
        return tokenRolesDAO.getByResource(resourceId, resourceType);
    }

    public TokenRolesBean getByNameAndResource(SecurityContext sc, String scriptName,
        String resourceId, Resource.Type resourceType) throws Exception {
        authorizer.authorize(sc, new Resource(resourceId, resourceType), Role.ADMIN);
        return tokenRolesDAO.getByNameAndResource(scriptName, resourceId, resourceType);
    }

    public void update(SecurityContext sc, TokenRolesBean bean, String scriptName,
        String resourceId, Resource.Type resourceType) throws Exception {
        authorizer.authorize(sc, new Resource(resourceId, resourceType), Role.ADMIN);
        tokenRolesDAO.update(bean, scriptName, resourceId, resourceType);
        LOG.info("Successfully updated script {} permission for resource {} with {}",
            scriptName, resourceId, bean);
    }

    public Response create(SecurityContext sc, TokenRolesBean bean, String resourceId,
        Resource.Type resourceType) throws Exception {
        authorizer.authorize(sc, new Resource(resourceId, resourceType), Role.ADMIN);
        String token = CommonUtils.getBase64UUID();
        bean.setToken(token);
        bean.setResource_id(resourceId);
        bean.setResource_type(resourceType);
        bean.setExpire_date(System.currentTimeMillis() + VALIDATE_TIME);
        tokenRolesDAO.insert(bean);
        bean.setToken("xxxxxxxx");
        LOG.info("Successfully created new script permission for resource {} with {}",
            resourceId, bean);
        TokenRolesBean newBean = tokenRolesDAO.getByToken(token);
        UriBuilder ub = uriInfo.getAbsolutePathBuilder();
        URI roleUri = ub.path(newBean.getScript_name()).build();
        return Response.created(roleUri).entity(newBean).build();
    }

    public void delete(SecurityContext sc, String scriptName, String resourceId,
        Resource.Type resourceType) throws Exception {
        authorizer.authorize(sc, new Resource(resourceId, resourceType), Role.ADMIN);
        tokenRolesDAO.delete(scriptName, resourceId, resourceType);
        LOG.info("Successfully deleted script {} permission for resource {}",
            scriptName, resourceId);
    }
}
