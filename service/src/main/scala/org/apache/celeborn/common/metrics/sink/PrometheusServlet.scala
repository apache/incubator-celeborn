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

package org.apache.celeborn.common.metrics.sink

import java.util.Properties

import com.codahale.metrics.MetricRegistry
import org.eclipse.jetty.servlet.ServletContextHandler

import org.apache.celeborn.common.metrics.source.Source
import org.apache.celeborn.server.common.http.HttpUtils
import org.apache.celeborn.server.common.http.HttpUtils.ServletParams

class PrometheusServlet(
    val property: Properties,
    val registry: MetricRegistry,
    val sources: Seq[Source],
    val servletPath: String) extends AbstractServlet(sources) {

  override def createServletHandler(): ServletContextHandler = {
    HttpUtils.createServletHandler(
      servletPath,
      new ServletParams(_ => getMetricsSnapshot, "text/plain"))
  }
}
