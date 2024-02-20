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
package org.apache.celeborn.common.rpc

import org.apache.celeborn.common.network.sasl.{SaslCredentials, SecretRegistry}
import org.apache.celeborn.common.network.sasl.registration.RegistrationInfo

/**
 * Represents the rpc context, combining both client and server contexts.
 *
 * @param clientRpcContext Optional client rpc context.
 * @param serverRpcContext Optional server rpc context.
 */
private[celeborn] case class RpcContext(
    clientRpcContext: Option[ClientRpcContext] = None,
    serverRpcContext: Option[ServerSaslContext] = None)

/**
 * Represents the client RPC context.
 * @param appId     The application id.
 * @param saslCredentials sasl credentials.
 * @param addRegistrationBootstrap Whether to add registration bootstrap.
 */
private[celeborn] case class ClientRpcContext(
    appId: String,
    saslCredentials: Option[SaslCredentials] = None,
    addRegistrationBootstrap: Boolean = false,
    registrationInfo: RegistrationInfo = null)

/**
 * Represents the server RPC context.
 * @param secretRegistry  The secret registry.
 * @param addRegistrationBootstrap  Whether to add registration bootstrap.
 */
private[celeborn] case class ServerSaslContext(
    secretRegistry: Option[SecretRegistry] = None,
    addRegistrationBootstrap: Boolean = false)

/**
 * Builder for [[ClientRpcContext]].
 */
private[celeborn] class ClientRpcContextBuilder {
  private var saslUser: String = _
  private var saslPassword: String = _
  private var appId: String = _
  private var addRegistrationBootstrap: Boolean = false
  private var registrationInfo: RegistrationInfo = _

  def withSaslUser(user: String): ClientRpcContextBuilder = {
    this.saslUser = user
    this
  }

  def withSaslPassword(password: String): ClientRpcContextBuilder = {
    this.saslPassword = password
    this
  }

  def withAppId(appId: String): ClientRpcContextBuilder = {
    this.appId = appId
    this
  }

  def withAddRegistrationBootstrap(addRegistrationBootstrap: Boolean): ClientRpcContextBuilder = {
    this.addRegistrationBootstrap = addRegistrationBootstrap
    this
  }

  def withRegistrationInfo(registrationInfo: RegistrationInfo): ClientRpcContextBuilder = {
    this.registrationInfo = registrationInfo
    this
  }

  def build(): ClientRpcContext = {
    if (appId == null) {
      throw new IllegalArgumentException("App id is not set.")
    }
    if (addRegistrationBootstrap && registrationInfo == null) {
      throw new IllegalArgumentException("Registration info is not set.")
    }
    var saslCredentials: Option[SaslCredentials] = None
    if (saslUser != null && saslPassword != null) {
      saslCredentials = Some(new SaslCredentials(saslUser, saslPassword))
    }
    ClientRpcContext(
      appId,
      saslCredentials,
      addRegistrationBootstrap,
      registrationInfo)
  }
}

/**
 * Builder for [[ServerSaslContext]].
 */
private[celeborn] class ServerRpcContextBuilder {
  private var secretRegistry: SecretRegistry = _
  private var addRegistrationBootstrap: Boolean = false

  def withSecretRegistry(secretRegistry: SecretRegistry): ServerRpcContextBuilder = {
    this.secretRegistry = secretRegistry
    this
  }

  def withAddRegistrationBootstrap(addRegistrationBootstrap: Boolean): ServerRpcContextBuilder = {
    this.addRegistrationBootstrap = addRegistrationBootstrap
    this
  }

  def build(): ServerSaslContext = {
    ServerSaslContext(
      Option(secretRegistry),
      addRegistrationBootstrap)
  }
}

/**
 * Builder for [[RpcContext]].
 */
private[celeborn] class RpcSecurityContextBuilder {
  private var clientSaslContext: Option[ClientRpcContext] = None
  private var serverSaslContext: Option[ServerSaslContext] = None

  def withClientSaslContext(context: ClientRpcContext): RpcSecurityContextBuilder = {
    this.clientSaslContext = Some(context)
    this
  }

  def withServerSaslContext(context: ServerSaslContext): RpcSecurityContextBuilder = {
    this.serverSaslContext = Some(context)
    this
  }

  def build(): RpcContext = {
    if (clientSaslContext.nonEmpty && serverSaslContext.nonEmpty) {
      throw new IllegalArgumentException("Both client and server sasl context cannot be set.")
    }
    RpcContext(clientSaslContext, serverSaslContext)
  }
}
