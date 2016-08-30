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

package net.nharyes.drivehost.mod;

import com.google.api.client.googleapis.media.MediaHttpDownloaderProgressListener;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import net.nharyes.drivecopy.FileDownloadProgressListener;
import net.nharyes.drivecopy.FileUploadProgressListener;
import net.nharyes.drivecopy.biz.wfm.TokenWorkflowManager;
import net.nharyes.drivehost.biz.RequestHandler;
import net.nharyes.drivehost.biz.wfm.EnvTokenWorkflowManagerImpl;
import net.nharyes.drivehost.srvc.StreamDriveSdo;
import net.nharyes.drivehost.srvc.StreamDriveSdoImpl;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.EnvironmentConfiguration;
import org.eclipse.jetty.server.Handler;

public class MainModule extends AbstractModule {

    @Provides
    @Singleton
    private Configuration provideConfiguration() {

        return new EnvironmentConfiguration();
    }

    @Override
    protected void configure() {

        // HTTP transport
        bind(HttpTransport.class).to(NetHttpTransport.class).in(Singleton.class);

        // JSON factory
        bind(JsonFactory.class).to(JacksonFactory.class).in(Singleton.class);

        // Stream Drive SDO
        bind(StreamDriveSdo.class).to(StreamDriveSdoImpl.class).in(Singleton.class);

        // File upload Progress Listener
        bind(MediaHttpUploaderProgressListener.class).to(FileUploadProgressListener.class);

        // File download Progress Listener
        bind(MediaHttpDownloaderProgressListener.class).to(FileDownloadProgressListener.class);

        // Request Handler
        bind(Handler.class).to(RequestHandler.class);

        // Token Workflow Manager
        bind(TokenWorkflowManager.class).to(EnvTokenWorkflowManagerImpl.class);
    }
}
