package org.digitalmemex.bootstrap.service;

import de.deepamehta.core.service.PluginService;
import org.digitalmemex.bootstrap.DmxRepository;

import java.io.InputStream;

public interface DmxRepositoryService extends PluginService {

    DmxRepository getRepository(String name);

    InputStream getApplicationIndex(String name);

}
