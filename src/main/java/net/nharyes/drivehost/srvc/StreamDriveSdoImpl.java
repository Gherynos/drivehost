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

package net.nharyes.drivehost.srvc;

import com.google.api.client.googleapis.media.MediaHttpDownloader;
import com.google.api.client.googleapis.media.MediaHttpDownloaderProgressListener;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files.Get;
import com.google.api.services.drive.model.File;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.nharyes.drivecopy.biz.bo.EntryBO;
import net.nharyes.drivecopy.biz.bo.TokenBO;
import net.nharyes.drivecopy.srvc.DriveSdoImpl;
import net.nharyes.drivecopy.srvc.exc.ItemNotFoundException;
import net.nharyes.drivecopy.srvc.exc.SdoException;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.OutputStream;

@Singleton
public class StreamDriveSdoImpl extends DriveSdoImpl implements StreamDriveSdo {

    @Inject
    public StreamDriveSdoImpl(HttpTransport httpTransport, JsonFactory jsonFactory, MediaHttpUploaderProgressListener fileUploadProgressListener, MediaHttpDownloaderProgressListener fileDownloadProgressListener) {

        super(httpTransport, jsonFactory, fileUploadProgressListener, fileDownloadProgressListener);
    }

    public EntryBO downloadEntry(@Nonnull TokenBO token, @Nonnull EntryBO entry, @Nonnull OutputStream outputStream) throws SdoException {

        try {

            // get file
            Drive service = getService(token);
            Get get = service.files().get(entry.getId());
            MediaHttpDownloader downloader = new MediaHttpDownloader(httpTransport, service.getRequestFactory().getInitializer());
            downloader.setDirectDownloadEnabled(false);
            downloader.setProgressListener(fileDownloadProgressListener);
            File file = executeWithExponentialBackoff(get);

            // check download URL and size
            if (file.getDownloadUrl() != null && file.getDownloadUrl().length() > 0) {

                // download file
                downloader.download(new GenericUrl(file.getDownloadUrl()), outputStream);
                outputStream.flush();

                // return entry
                return entry;

            } else {

                // the file doesn't have any content stored on Drive
                throw new ItemNotFoundException(String.format("Remote file with id '%s' doesn't have any content stored on Drive", entry.getId()));
            }

        } catch (IOException | InterruptedException ex) {

            // re-throw exception
            throw new SdoException(ex.getMessage(), ex);
        }
    }
}
