package org.digitalmemex.applications;

import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.Inject;
import org.digitalmemex.bootstrap.service.DmxRepositoryService;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.logging.Logger;

@Path("/app")
@Produces("application/json")
public class DmxApplicationPlugin extends PluginActivator {

    @Inject
    protected DmxRepositoryService repoService;

    protected Logger logger = Logger.getLogger(getClass().getName());

    @GET
    @Path("/{name}")
    @Produces("text/html")
    public InputStream getApplicationIndex(@PathParam("name") String name) {
        logger.info("request application " + name + " index");

        if (repoService == null) {
            String message = "repository service plugin is not available";
            Response response = Response.status(500).entity(message).build();
            throw new WebApplicationException(response);
        }

        return repoService.getApplicationIndex(name);
    }

}
