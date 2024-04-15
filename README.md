This repository is now **archived**.

Drive Host
==========

Once upon a time there was a cool [service](https://support.google.com/drive/answer/2881970?hl=en) maintained by Google that allowed to host a static website with Google Drive.
Unfortunately, the service was [deprecated](http://googleappsupdates.blogspot.co.uk/2015/08/deprecating-web-hosting-support-in.html) in August 2015, so I decided to create a simplified version of it running on Docker Containers. 

[![](https://images.microbadger.com/badges/version/gherynos/drivehost.svg)](https://microbadger.com/images/gherynos/drivehost "Get your own version badge on microbadger.com") [![](https://images.microbadger.com/badges/image/gherynos/drivehost.svg)](https://microbadger.com/images/gherynos/drivehost "Get your own image badge on microbadger.com")

Getting started
---------------

Drive Host relies on [Drive Copy](https://github.com/Gherynos/DriveCopy) to download the files from Google Drive: before running the image, some access tokens need to be generated.  
In order to do that, download the [latest release](https://pkg.naes.co/drivecopy/drivecopy.jar) of Drive Copy and follow the [Setup](https://github.com/Gherynos/DriveCopy/wiki/Setup) steps.  
Once completed the process, a file called `drivecopy.properties` containing the required tokens will be generated.

How to use this image
---------------------

### Build the image

```bash
$ docker build -t drivehost .
```

### Environment variables

Drive Host requires the following variables defined:

| Variable                      | Details                                                            |
| ----------------------------- | ------------------------------------------------------------------ |
| DRIVEHOST_CACHE_ENABLED       | enables/disables the local cache (for speed and reduced API calls) |
| DRIVEHOST_CACHE_REFRESH_KEY   | the password used to invalidate the cache (see next chapter)       |
| DRIVEHOST_ROOT_FOLDER_ID      | the ID of the Google Drive folder to host                          |
| DRIVEHOST_CLIENT_ID           | the OAuth 2.0 Client ID (see previous step)                        |
| DRIVEHOST_CLIENT_SECRET       | the OAuth 2.0 Client Secret (see previous step)                    |
| DRIVEHOST_ACCESS_TOKEN        | the OAuth 2.0 Access Token (see previous step)                     |
| DRIVEHOST_REFRESH_TOKEN       | the OAuth 2.0 Refresh Token (see previous step)                    |

### Run the image:

```bash
$ docker run -d -p "8080:8080" -e "DRIVEHOST_CACHE_ENABLED=<true/false>" \
  -e "DRIVEHOST_CACHE_REFRESH_KEY=<value>" \
  -e "DRIVEHOST_ROOT_FOLDER_ID=<value>" \
  -e "DRIVEHOST_CLIENT_ID=<value>" \
  -e "DRIVEHOST_CLIENT_SECRET=<value>" \
  -e "DRIVEHOST_ACCESS_TOKEN=<value>" \
  -e "DRIVEHOST_REFRESH_TOKEN=<value>" \
  -t drivehost
```

and browse to _http://localhost:8080_ to view the website.

If no file is specified in the URL, Drive Host looks for `index.html` in the folder and, if not found, will return 403.

Cache invalidation
------------------

When the cache is enabled (`DRIVEHOST_CACHE_ENABLED=true`) Drive Host tries to serve the requested files from the local copies;
only if the local copy has expired or is not present it will download the file from Google Drive and update the cache.

In order to invalidate the cache for a single file, add the `refresh=<key>` query parameter to the request, i.e.:

    GET http://localhost:8080/myfile.png?refresh=mykey

where `<key>` is the same value specified in the `DRIVEHOST_CACHE_REFRESH_KEY` environment variable.
This will force Drive Host to download a new copy of the requested file.

It is also possible to invalidate the whole cache by sending a POST request to any URL together with the `refresh_all=<key>` parameter:

    POST http://localhost:8080/myfile.png?refresh_all=mykey

this will remove all the local copies of the files requested.

Copyright and license
---------------------

Copyright 2016 Luca Zanconato (<gherynos.com>)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this work except in compliance with the License.
You may obtain a copy of the License in the LICENSE file, or at:

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
