/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.cava.devp2p

import kotlinx.coroutines.GlobalScope
import net.consensys.cava.concurrent.AsyncResult
import net.consensys.cava.concurrent.coroutines.asyncResult
import net.consensys.cava.crypto.SECP256K1
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

/**
 * A repository of peers in an Ethereum network.
 *
 * Conceptually, this repository stores information about <i>all</i> peers in an Ethereum network, hence the
 * retrieval methods always return a valid [Peer]. However, the [Peer] objects are only generated on demand and
 * may be purged from underlying storage if they can be recreated easily.
 */
interface PeerRepository {

  /**
   * Get a peer.
   *
   * @param nodeId the node id
   * @return the peer
   */
  suspend fun get(nodeId: SECP256K1.PublicKey): Peer

  /**
   * Get a peer.
   *
   * @param nodeId the node id
   * @return the peer
   */
  fun getAsync(nodeId: SECP256K1.PublicKey): AsyncResult<Peer>

  /**
   * Get a Peer based on a URI.
   *
   * The returned peer will use the endpoint from the URI, unless the peer is already active, in
   * which case its endpoint will be unchanged.
   *
   * @param uri the enode URI
   * @return the peer
   * @throws IllegalArgumentException if the URI is not a valid enode URI
   */
  suspend fun get(uri: URI): Peer

  /**
   * Get a Peer based on a URI.
   *
   * The returned peer will use the endpoint from the URI, unless the peer is already active, in
   * which case its endpoint will be unchanged.
   *
   * @param uri the enode URI
   * @return the peer
   * @throws IllegalArgumentException if the URI is not a valid enode URI
   */
  fun getAsync(uri: URI): AsyncResult<Peer>

  /**
   * Get a Peer based on a URI string.
   *
   * The returned peer will use the endpoint from the URI, unless the peer is already active, in
   * which case its endpoint will be unchanged.
   *
   * @param uri the enode URI
   * @return the peer
   * @throws IllegalArgumentException if the URI is not a valid enode URI
   */
  suspend fun get(uri: String) = get(URI.create(uri))

  /**
   * Get a Peer based on a URI string.
   *
   * The returned peer will use the endpoint from the URI, unless the peer is already active, in
   * which case its endpoint will be unchanged.
   *
   * @param uri the enode URI
   * @return the peer
   * @throws IllegalArgumentException if the URI is not a valid enode URI
   */
  fun getAsync(uri: String): AsyncResult<Peer>
}

/**
 * An in-memory peer repository.
 *
 * Note: as the storage is in-memory, no retrieval methods in this implementation will suspend.
 */
class EphemeralPeerRepository : PeerRepository {

  private val peers = ConcurrentHashMap<SECP256K1.PublicKey, EphemeralPeer>()

  override suspend fun get(nodeId: SECP256K1.PublicKey) =
    peers.compute(nodeId) { _, peer -> peer ?: EphemeralPeer(nodeId) } as Peer

  override fun getAsync(nodeId: SECP256K1.PublicKey): AsyncResult<Peer> = GlobalScope.asyncResult { get(nodeId) }

  override suspend fun get(uri: URI): Peer {
    val (nodeId, endpoint) = parseEnodeUri(uri)
    val peer = get(nodeId) as EphemeralPeer
    if (peer.endpoint == null) {
      synchronized(peer) {
        if (peer.endpoint == null) {
          peer.endpoint = endpoint
        }
      }
    }
    return peer
  }

  override fun getAsync(uri: URI): AsyncResult<Peer> = GlobalScope.asyncResult { get(uri) }

  override fun getAsync(uri: String): AsyncResult<Peer> = GlobalScope.asyncResult { get(uri) }

  private inner class EphemeralPeer(
    override val nodeId: SECP256K1.PublicKey,
    knownEndpoint: Endpoint? = null
  ) : Peer {

    @Volatile
    override var endpoint: Endpoint? = knownEndpoint

    @Synchronized
    override fun getEndpoint(ifVerifiedOnOrAfter: Long): Endpoint? {
      if ((lastVerified ?: 0) >= ifVerifiedOnOrAfter) {
        return this.endpoint
      }
      return null
    }

    @Volatile
    override var lastVerified: Long? = null

    @Volatile
    override var lastSeen: Long? = null

    @Synchronized
    override fun updateEndpoint(endpoint: Endpoint, time: Long, ifVerifiedBefore: Long?): Endpoint {
      val currentEndpoint = this.endpoint
      if (currentEndpoint == endpoint) {
        this.seenAt(time)
        return currentEndpoint
      }

      if (currentEndpoint == null || ifVerifiedBefore == null || (lastVerified ?: 0) < ifVerifiedBefore) {
        if (currentEndpoint?.address != endpoint.address || currentEndpoint.udpPort != endpoint.udpPort) {
          lastVerified = null
        }
        this.endpoint = endpoint
        this.seenAt(time)
        return endpoint
      }

      return currentEndpoint
    }

    @Synchronized
    override fun verifyEndpoint(endpoint: Endpoint, time: Long): Boolean {
      if (endpoint != this.endpoint) {
        return false
      }
      seenAt(time)
      if ((lastVerified ?: 0) < time) {
        lastVerified = time
      }
      return true
    }

    @Synchronized
    override fun seenAt(time: Long) {
      if (this.endpoint == null) {
        throw IllegalStateException("Peer has no endpoint")
      }
      if ((lastSeen ?: 0) < time) {
        lastSeen = time
      }
    }
  }
}
