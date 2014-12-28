package org.digitalmemex.distribution;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;

public class AssemblyGitRepositoryFetcher {

    private final String uri;
    private final File directory;

    public AssemblyGitRepositoryFetcher(String uri, String directory) {
        this.uri = uri;
        this.directory = new File(directory);
    }

    public Git cloneRepository() throws IOException, GitAPIException {
        return Git.cloneRepository().setURI(uri).setDirectory(directory)
                .setBranch("master").setRemote("origin")
                .setBare(false).setNoCheckout(false).call();
    }

    public static void main(String[] args) throws IOException, GitAPIException {
        String uri = args[0];
        String directory = args[1];
        AssemblyGitRepositoryFetcher fetcher = new AssemblyGitRepositoryFetcher(uri, directory);
        Git git = fetcher.cloneRepository();
        System.out.println(git.toString());
    }

}
