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

package org.apache.celeborn.client.write;

import java.io.IOException;

import org.apache.celeborn.common.CelebornConf;

/** A Simple strategy that control the push speed by a solid configure, pushMaxReqsInFlight. */
public class SimplePushSpeedStrategy extends PushSpeedStrategy {

  private final int maxInFlight;

  public SimplePushSpeedStrategy(CelebornConf conf) {
    super(conf);
    this.maxInFlight = conf.pushMaxReqsInFlight();
  }

  @Override
  public void onSuccess() {
    // No op
  }

  @Override
  public void onCongestControl() {
    // No op
  }

  @Override
  public void limitPushSpeed(String mapKey, PushState pushState, String hostAndPushPort) throws IOException {
    awaitInFlightRequestsMatched(mapKey, pushState, this.maxInFlight);
  }
}
