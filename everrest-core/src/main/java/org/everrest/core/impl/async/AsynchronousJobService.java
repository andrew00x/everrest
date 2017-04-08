/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.everrest.core.impl.async;

import org.everrest.core.GenericContainerRequest;
import org.everrest.core.ProviderBinder;
import org.everrest.core.async.AsynchronousJob;
import org.everrest.core.async.AsynchronousJobPool;
import org.everrest.core.impl.ApplicationContext;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Providers;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.LOCATION;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

/**
 * Service to get results of invocation asynchronous job from {@link AsynchronousJobPool}. Instance of AsynchronousJobPool obtained in
 * in this class via mechanism of injections. This resource must always be deployed as per-request resource.
 *
 * @author andrew00x
 */
@Path("async")
public class AsynchronousJobService {
    @Context
    private Providers providers;

    @GET
    @Path("{job}")
    public Object get(@PathParam("job") Long jobId, @Context UriInfo uriInfo, @Context SecurityContext securityContext) {
        final AsynchronousJobPool pool = getJobPool();
        final AsynchronousJob job = pool.getJob(jobId);
        if (job == null) {
            throw new WebApplicationException(Response.status(NOT_FOUND)
                                                      .entity(String.format("Job %d not found. ", jobId))
                                                      .type(TEXT_PLAIN).build());
        }

        // Get original request which initialize asynchronous job.
        final GenericContainerRequest request = (GenericContainerRequest)job.getContext().get("org.everrest.async.request");
        if (securityContext.isUserInRole("administrators")
            || principalMatched(request.getUserPrincipal(), securityContext.getUserPrincipal())) {
            if (job.isDone()) {
                Object result;
                try {
                    result = job.getResult();
                } finally {
                    pool.removeJob(jobId);
                    // Restore resource specific set of providers.
                    ApplicationContext.getCurrent().setProviders((ProviderBinder)job.getContext().get("org.everrest.async.providers"));
                }

                // This response will be sent to client side.
                Response response;
                if (result == null || result.getClass() == void.class || result.getClass() == Void.class) {
                    response = Response.noContent().build();
                } else if (Response.class.isAssignableFrom(result.getClass())) {
                    response = (Response)result;

                    if (response.getEntity() != null && response.getMetadata().getFirst(CONTENT_TYPE) == null) {
                        MediaType contentType = request.getAcceptableMediaType(job.getResourceMethod().produces());
                        response.getMetadata().putSingle(CONTENT_TYPE, contentType);
                    }
                } else {
                    MediaType contentType = request.getAcceptableMediaType(job.getResourceMethod().produces());
                    response = Response.ok(result, contentType).build();
                }

                // Result of job. Client get this response.
                ApplicationContext.getCurrent().getContainerResponse().setResponse(response);

                // This response (204) never sent to client side.
                return null;
            } else {
                final String jobUri = uriInfo.getRequestUri().toString();
                return Response.status(ACCEPTED)
                               .header(LOCATION, jobUri)
                               .entity(jobUri)
                               .type(TEXT_PLAIN).build();
            }
        } else {
            throw new WebApplicationException(Response.status(FORBIDDEN)
                                                      .entity(String.format("GET: (%d) - Operation not permitted. ", jobId))
                                                      .type(TEXT_PLAIN).build());
        }
    }

    @GET
    @Produces({APPLICATION_JSON, TEXT_PLAIN})
    public GenericEntity<List<AsynchronousProcess>> list() {
        AsynchronousJobPool pool = getJobPool();
        List<AsynchronousJob> jobs = pool.getAll();
        List<AsynchronousProcess> processes = new ArrayList<>(jobs.size());
        for (AsynchronousJob job : jobs) {
            GenericContainerRequest request = (GenericContainerRequest)job.getContext().get("org.everrest.async.request");
            Principal principal = request.getUserPrincipal();
            processes.add(new AsynchronousProcess(
                    principal != null ? principal.getName() : null,
                    job.getJobId(),
                    request.getRequestUri().getPath(),
                    job.isDone() ? "done" : "running"));
        }
        return new GenericEntity<List<AsynchronousProcess>>(processes) {
        };
    }

    @DELETE
    @Path("{job}")
    public void remove(@PathParam("job") Long jobId, @Context SecurityContext securityContext) {
        AsynchronousJobPool pool = getJobPool();
        AsynchronousJob job = pool.getJob(jobId);
        if (job == null) {
            throw new WebApplicationException(Response.status(NOT_FOUND)
                                                      .entity(String.format("Job %d not found. ", jobId))
                                                      .type(TEXT_PLAIN).build());
        }

        if (securityContext.isUserInRole("administrators")
            || principalMatched(((GenericContainerRequest)job.getContext().get("org.everrest.async.request")).getUserPrincipal(),
                                securityContext.getUserPrincipal())) {
            pool.removeJob(jobId);
        } else {
            throw new WebApplicationException(Response.status(FORBIDDEN)
                                                      .entity(String.format("DELETE: (%d) - Operation not permitted. ", jobId))
                                                      .type(TEXT_PLAIN).build());
        }
    }

    private boolean principalMatched(Principal jobCreatorPrincipal, Principal currentPrincipal) {
        if (jobCreatorPrincipal == null) {
            return true;
        }
        if (currentPrincipal != null) {
            String creatorName = jobCreatorPrincipal.getName();
            String currentUserName = currentPrincipal.getName();
            if (creatorName == null && currentUserName == null
                    || creatorName != null && creatorName.equals(currentUserName)) {
                return true;
            }
        }
        return false;
    }

    private AsynchronousJobPool getJobPool() {
        if (providers != null) {
            ContextResolver<AsynchronousJobPool> asyncJobsResolver = providers.getContextResolver(AsynchronousJobPool.class, null);
            if (asyncJobsResolver != null) {
                return asyncJobsResolver.getContext(null);
            }
        }
        throw new IllegalStateException("Asynchronous jobs feature is not configured properly. ");
    }
}
