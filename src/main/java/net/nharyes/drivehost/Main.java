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

package net.nharyes.drivehost;

import com.google.inject.Guice;
import com.google.inject.Injector;
import net.nharyes.drivehost.mod.MainModule;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Main {

    public static void main(String[] args) throws Exception {

        // setup logging
        LogManager.getLogManager().reset();
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        Logger.getLogger("global").setLevel(Level.FINEST);
        Logger.getLogger("net.nharyes").setLevel(Level.FINEST);

        // GUICE injector
        Injector injector = Guice.createInjector(new MainModule());

        // start Jetty on port 8080
        Server server = new Server(new QueuedThreadPool(128, 8));
        ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory());
        connector.setPort(8080);
        server.addConnector(connector);
        server.setHandler(injector.getInstance(Handler.class));
        server.start();
        server.join();
    }
}
