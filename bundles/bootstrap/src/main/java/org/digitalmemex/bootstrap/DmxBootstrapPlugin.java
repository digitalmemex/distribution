package org.digitalmemex.bootstrap;

import de.deepamehta.core.Topic;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.Inject;
import de.deepamehta.core.service.Transactional;
import de.deepamehta.core.service.event.PreDeleteTopicListener;
import de.deepamehta.plugins.files.service.FilesService;
import org.digitalmemex.bootstrap.service.DmxRepositoryService;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.util.FileUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/dmx")
@Produces("application/json")
public class DmxBootstrapPlugin extends PluginActivator implements DmxRepositoryService, PreDeleteTopicListener {

    @Inject
    protected FilesService filesService;

    protected Logger logger = Logger.getLogger(getClass().getName());

    @GET
    @Path("/repository/{name}")
    public DmxRepository getRepository(@PathParam("name") String name) {
        logger.info("repository request " + name + " topic");

        Topic repoName = dms.getTopic("dmx.repository.name", new SimpleValue(name));
        if (repoName == null) {
            Response response = Response.status(404).entity("repository name " + name + " not found").build();
            throw new WebApplicationException(response);
        }

        Topic repo = repoName.getRelatedTopic("dm4.core.composition", "dm4.core.child", "dm4.core.parent", "dmx.repository");
        if (repo == null) {
            logger.warning("repository parent topic of name " + name + " not found");
            Response response = Response.status(404).entity("repository topic " + name + " not found").build();
            throw new WebApplicationException(response);
        }

        return new DmxRepository(repo);
    }

    public InputStream getApplicationIndex(String name) {
        logger.info("request application " + name + " index");

        DmxRepository repo = getRepository(name);
        if (filesService == null) {
            String message = "file service plugin is not available";
            Response response = Response.status(500).entity(message).build();
            throw new WebApplicationException(response);
        } else if (repo.isInstalled() == false) {
            String message = "repository " + name + " not installed";
            Response response = Response.status(404).entity(message).build();
            throw new WebApplicationException(response);
        } else {
            String index = "/dmx/" + repo.getName() + "/index.html";
            try {
                File file = filesService.getFile(index);
                return new FileInputStream(file);
            } catch (FileNotFoundException e) {
                String message = "index file  " + index + " not found";
                Response response = Response.status(404).entity(message).build();
                throw new WebApplicationException(response);
            }
        }
    }

    @GET
    @Path("/version")
    public String version() {
        return "0.0.1-17";
    }

    @GET
    @Produces("text/html")
    public InputStream index() {
        // TODO use plugin configuration instead
        return getApplicationIndex("repository-manager");
    }

    private File getRepositoryDirectory(DmxRepository repo) {
        if (filesService == null) {
            String message = "file service plugin is not available";
            Response response = Response.status(500).entity(message).build();
            throw new WebApplicationException(response);
        }

        File root = filesService.getFile("/dmx");
        File repoDir = new File(root, repo.getName());
        if (repoDir.exists() == false) { // create it
            filesService.createFolder("dmx/" + repo.getName(), "/");
        }
        return repoDir;
    }

    @GET
    @Path("/repository/{name}/pull")
    @Transactional
    public DmxRepository pullRepository(@PathParam("name") String name) throws IOException {
        logger.info("pull request " + name);
        DmxRepository repo = getRepository(name);
        File repoDir = getRepositoryDirectory(repo);
        File gitDir = new File(repoDir, ".git");
        Repository gitRepo = new FileRepositoryBuilder().readEnvironment().findGitDir()
                .setGitDir(gitDir).build();
        try {
            // TODO set remote and branch references
            PullCommand pull = new Git(gitRepo).pull();
            PullResult result = pull.call();
            logger.info("pull result " + result);
            repo.setBranch(gitRepo.getFullBranch());
            repo.setHead(gitRepo.resolve("HEAD").getName());
            return repo;
        } catch (GitAPIException e) {
            logger.log(Level.WARNING, "pull request for " + name + " has failed", e);
            String message = "pull request for repository " + name + " has failed: " + e.getMessage();
            throw new WebApplicationException(Response.status(500).entity(message).build());
        }
    }

    @GET
    @Path("/repository/{name}/clone")
    @Transactional
    public DmxRepository cloneRepository(@PathParam("name") String name) throws IOException {
        logger.info("clone request " + name);
        DmxRepository repo = getRepository(name);
        File path = getRepositoryDirectory(repo);
        try {
            // TODO use remote and branch configuration
            Git git = Git.cloneRepository().setURI(repo.getUri()).setDirectory(path)
                    .setBranch("master").setRemote("origin")
                    .setBare(false).setNoCheckout(false).call();
            Repository gitRepo = git.getRepository();
            repo.setBranch(gitRepo.getBranch());
            repo.setHead(gitRepo.resolve("HEAD").getName());
            repo.setStatusInstalled();
            return repo;
        } catch (GitAPIException e) {
            logger.log(Level.WARNING, "the cloning of " + name + " has failed", e);
            String message = "repository " + name + " not installed: " + e.getMessage();
            throw new WebApplicationException(Response.status(500).entity(message).build());
        } catch (JGitInternalException e) {
            logger.log(Level.WARNING, "the cloning of " + name + " has failed", e);
            String message = "repository " + name + " not installed: " + e.getMessage();
            throw new WebApplicationException(Response.status(500).entity(message).build());
        }
    }

    /**
     * Delete local files of the Git repository clone first.
     *
     * @param topic
     */
    @Override
    public void preDeleteTopic(Topic topic) {
        if (topic.getTypeUri().equals("dmx.repository")) {
            try {
                DmxRepository repo = new DmxRepository(topic);
                File path = getRepositoryDirectory(repo);
                logger.info("delete repo clone " + path.getAbsolutePath());
                FileUtils.delete(path, FileUtils.RECURSIVE);
            } catch (IOException e) {
                throw new RuntimeException("repository clone deletion failed", e);
            }
        }
    }
}
