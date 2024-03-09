/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.celeborn.server.common.http

import org.apache.commons.lang3.SystemUtils
import org.eclipse.jetty.server.{Handler, HttpConfiguration, HttpConnectionFactory, Server, ServerConnector}
import org.eclipse.jetty.server.handler.{ContextHandlerCollection, ErrorHandler}
import org.eclipse.jetty.util.component.LifeCycle
import org.eclipse.jetty.util.thread.{QueuedThreadPool, ScheduledExecutorScheduler}

import org.apache.celeborn.common.internal.Logging
import org.apache.celeborn.common.util.CelebornExitKind

private[celeborn] case class JettyServer(
    role: String,
    server: Server,
    connector: ServerConnector,
    rootHandler: ContextHandlerCollection) extends Logging {

  @volatile private var isStarted = false

  @throws[Exception]
  def start(): Unit = synchronized {
    server.start()
    connector.start()
    server.addConnector(connector)
    server.setStopAtShutdown(true)
    logInfo(s"$role: HttpServer started on ${connector.getHost}:${connector.getPort}.")
    isStarted = true
  }

  def stop(exitCode: Int): Unit = synchronized {
    if (isStarted) {
      if (exitCode == CelebornExitKind.EXIT_IMMEDIATELY) {
        // default graceful timeout is 30000L milliseconds
        server.setStopTimeout(0)
      }
      logInfo(s"$role: Stopping HttpServer")
      server.stop()
      connector.stop()
      server.getThreadPool match {
        case lifeCycle: LifeCycle => lifeCycle.stop()
        case _ =>
      }
      logInfo(s"$role: HttpServer stopped.")
      isStarted = false
    }
  }
  def getServerUri: String = connector.getHost + ":" + connector.getLocalPort

  def addHandler(handler: Handler): Unit = synchronized {
    rootHandler.addHandler(handler)
    if (!handler.isStarted) handler.start()
  }

  def addStaticHandler(
      resourceBase: String,
      contextPath: String): Unit = {
    addHandler(JettyUtils.createStaticHandler(resourceBase, contextPath))
  }

  def addRedirectHandler(
      src: String,
      dest: String): Unit = {
    addHandler(JettyUtils.createRedirectHandler(src, dest))
  }

  def getState: String = server.getState
}

object JettyServer {

  def apply(role: String, host: String, port: Int, poolSize: Int): JettyServer = {
    val pool = new QueuedThreadPool(poolSize)
    pool.setName(s"$role-JettyThreadPool")
    pool.setDaemon(true)
    val server = new Server(pool)

    val errorHandler = new ErrorHandler()
    errorHandler.setShowStacks(true)
    errorHandler.setServer(server)
    server.addBean(errorHandler)

    val collection = new ContextHandlerCollection
    server.setHandler(collection)

    val serverExecutor = new ScheduledExecutorScheduler(s"$role-JettyScheduler", true)
    val httpConf = new HttpConfiguration()
    val connector = new ServerConnector(
      server,
      null,
      serverExecutor,
      null,
      -1,
      -1,
      new HttpConnectionFactory(httpConf))
    connector.setHost(host)
    connector.setPort(port)
    connector.setReuseAddress(!SystemUtils.IS_OS_WINDOWS)
    connector.setAcceptQueueSize(math.min(connector.getAcceptors, 8))

    new JettyServer(role, server, connector, collection)
  }
}