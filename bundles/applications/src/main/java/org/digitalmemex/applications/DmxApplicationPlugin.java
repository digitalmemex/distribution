package org.digitalmemex.applications;

import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.Inject;
import de.deepamehta.plugins.files.service.FilesService;
import org.digitalmemex.bootstrap.service.DmxRepositoryService;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.logging.Logger;

@Path("/app")
@Produces("application/json")
public class DmxApplicationPlugin extends PluginActivator {

    @Inject
    protected DmxRepositoryService repoService;

    @Inject
    protected FilesService filesService;

    protected Logger logger = Logger.getLogger(getClass().getName());

    @GET
    @Path("/{name}/")
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

    @GET
    @Path("/{name}/{path : .+}")
    @Produces("text/html")
    public InputStream getApplicationResource(@PathParam("name") String name, @PathParam("path") String path) {
        logger.info("request application " + name + " resource " + path);

        if (filesService == null) {
            String message = "file service plugin is not available";
            Response response = Response.status(500).entity(message).build();
            throw new WebApplicationException(response);
        }

        String resource = "/dmx/" + name + "/" + path;
        try {
            File file = filesService.getFile(resource);
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            String message = "resource file  " + resource + " not found";
            Response response = Response.status(404).entity(message).build();
            throw new WebApplicationException(response);
        }

    }
}
