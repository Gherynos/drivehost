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

package net.nharyes.drivehost.biz;

import com.google.api.client.util.IOUtils;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.nharyes.drivecopy.biz.bo.EntryBO;
import net.nharyes.drivecopy.biz.bo.TokenBO;
import net.nharyes.drivecopy.biz.exc.WorkflowManagerException;
import net.nharyes.drivecopy.biz.wfm.TokenWorkflowManager;
import net.nharyes.drivecopy.srvc.exc.FolderNotFoundException;
import net.nharyes.drivecopy.srvc.exc.ItemNotFoundException;
import net.nharyes.drivecopy.srvc.exc.SdoException;
import net.nharyes.drivehost.EnvVariables;
import net.nharyes.drivehost.biz.exc.ForbiddenException;
import net.nharyes.drivehost.srvc.StreamDriveSdo;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.jcs.JCS;
import org.apache.commons.jcs.access.CacheAccess;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
public class RequestHandler extends AbstractHandler {

    /*
     * Logger
	 */
    protected final Logger logger = Logger.getLogger(getClass().getName());

    private static final String INDEX_FILE_NAME = "index.html";

    private StreamDriveSdo driveSdo;

    private TokenWorkflowManager tokenWorkflowManager;

    private boolean cacheEnabled;

    private String cacheKey;

    private String rootFolder;

    private CacheAccess<String, String[]> targetsCache = null;

    private CacheAccess<String, byte[]> filesCache = null;

    @Inject
    public RequestHandler(StreamDriveSdo driveSdo, TokenWorkflowManager tokenWorkflowManager, Configuration configuration) {

        super();

        this.driveSdo = driveSdo;
        this.tokenWorkflowManager = tokenWorkflowManager;

        cacheEnabled = configuration.getBoolean(EnvVariables.CACHE_ENABLED, true);
        cacheKey = configuration.getString(EnvVariables.CACHE_REFRESH_KEY, "");
        rootFolder = configuration.getString(EnvVariables.ROOT_FOLDER_ID);

        // check root folder
        if (rootFolder == null)
            throw new RuntimeException("Please provide the root folder environment variable.");

        if (cacheEnabled) {

            targetsCache = JCS.getInstance("targetsCache");
            filesCache = JCS.getInstance("filesCache");
        }
    }

    private String[] extractFolders(String filePath) {

        // extract folders
        String[] folders = filePath.split("/");
        if (folders.length > 1) {

            // remove file name from folders
            String[] nf = new String[folders.length - 1];
            System.arraycopy(folders, 0, nf, 0, nf.length);
            folders = nf;

        } else
            folders = null;

        return folders;
    }

    private String extractFileName(String filePath) {

        // return only file name
        if (filePath.contains("/"))
            return filePath.substring(filePath.lastIndexOf("/") + 1);

        return filePath;
    }

    private EntryBO loadIndexPage(TokenBO token, String lastFolder) throws ForbiddenException, SdoException {

        try {

            // try index.html page
            return driveSdo.searchEntry(token, INDEX_FILE_NAME, lastFolder);

        } catch (ItemNotFoundException ex) {

            // directory listing disabled
            throw new ForbiddenException("directory listing disabled");
        }
    }

    private void processGetRequest(String target, Request request, HttpServletRequest req, HttpServletResponse res) throws IOException, WorkflowManagerException {

        try {

            // just in case...
            if (target.startsWith("./") || target.startsWith("../"))
                throw new ItemNotFoundException("");

            // refresh parameter
            boolean refresh = false;
            if (cacheEnabled && !cacheKey.isEmpty()) {

                String ref = req.getParameter("refresh");
                refresh = ref != null && ref.equals(cacheKey);
            }

            // check existing entry in cache
            EntryBO entry = null;
            if (cacheEnabled && !refresh) {

                String[] elems = targetsCache.get(target);
                if (elems != null) {

                    logger.fine("entry found in cache");

                    entry = new EntryBO();
                    entry.setId(elems[0]);
                    entry.setMimeType(elems[1]);
                }
            }

            TokenBO token = tokenWorkflowManager.handleWorkflow(null, TokenWorkflowManager.ACTION_GET);
            if (entry == null) {

                // extract folders and filename from target
                String lastFolder = driveSdo.getLastFolderId(token, extractFolders(target.substring(1)), rootFolder, false);
                String fileName = extractFileName(target);

                if (!target.equals("/")) {

                    if (target.endsWith("/")) {

                        // the target is a folder
                        lastFolder = driveSdo.getLastFolderId(token, extractFolders(target.substring(1) + INDEX_FILE_NAME), rootFolder, false);
                        entry = loadIndexPage(token, lastFolder);
                        fileName = INDEX_FILE_NAME;

                    } else {

                        try {

                            // try to load entry by name
                            entry = driveSdo.searchEntry(token, fileName, lastFolder);

                        } catch (ItemNotFoundException ex) {

                            // maybe entry is a folder
                            lastFolder = driveSdo.getLastFolderId(token, extractFolders(target.substring(1) + "/" + INDEX_FILE_NAME), rootFolder, false);
                            entry = loadIndexPage(token, lastFolder);
                            fileName = INDEX_FILE_NAME;
                        }
                    }

                } else {

                    // root folder
                    entry = loadIndexPage(token, lastFolder);
                    fileName = INDEX_FILE_NAME;
                }

                // set mime type
                if (entry.getMimeType() == null || entry.getMimeType().equalsIgnoreCase("application/octet-stream"))
                    entry.setMimeType(MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(fileName));
            }

            if (!cacheEnabled) {

                // stream entry
                entry = driveSdo.downloadEntry(token, entry, res.getOutputStream());

            } else {

                // check file existence
                logger.fine(String.format("file '%s' requested", entry.getId()));
                synchronized (entry.getId().intern()) {

                    byte[] data = filesCache.get(entry.getId());
                    if (refresh || data == null) {

                        // (re)create cache file
                        ByteArrayOutputStream bout = new ByteArrayOutputStream();
                        entry = driveSdo.downloadEntry(token, entry, bout);
                        data = bout.toByteArray();
                        filesCache.put(entry.getId(), data);
                        logger.fine(String.format("cache file '%s' created", entry.getId()));
                    }

                    IOUtils.copy(new ByteArrayInputStream(data), res.getOutputStream(), true);
                }

                // update targets cache
                if (refresh || targetsCache.get(target) == null) {

                    logger.fine("store entry in cache");
                    targetsCache.put(target, new String[]{entry.getId(), entry.getMimeType()});
                }
            }

            // populate response
            res.setContentType(entry.getMimeType());
            res.setStatus(HttpServletResponse.SC_OK);

        } catch (FolderNotFoundException | ItemNotFoundException ex) {

            res.sendError(HttpServletResponse.SC_NOT_FOUND);

        } catch (ForbiddenException ex) {

            res.sendError(HttpServletResponse.SC_FORBIDDEN);

        } catch (SdoException ex) {

            logger.log(Level.SEVERE, ex.getMessage(), ex);
            res.sendError(ex.getCause() != null ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR : HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    public void handle(String target, Request request, HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

        try {

            // check HTTP method
            if (!request.getMethod().equals(HttpMethod.GET.asString())) {

                // check invalidate cache command
                String ref = req.getParameter("refresh_all");
                if (cacheEnabled && request.getMethod().equals(HttpMethod.POST.asString()) && !cacheKey.isEmpty() && ref != null && ref.equals(cacheKey)) {

                    // flush the cache
                    targetsCache.clear();
                    filesCache.clear();

                    res.setStatus(HttpServletResponse.SC_OK);

                } else
                    res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);

            } else
                processGetRequest(target, request, req, res);

        } catch (WorkflowManagerException ex) {

            logger.log(Level.SEVERE, ex.getMessage(), ex);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

        } finally {

            request.setHandled(true);
            logger.info(String.format("GET %s %d", target, res.getStatus()));
        }
    }
}
