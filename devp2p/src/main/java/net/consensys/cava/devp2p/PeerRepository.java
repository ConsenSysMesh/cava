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
package net.consensys.cava.devp2p;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import net.consensys.cava.bytes.Bytes;

import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A repository of all Peers in an Ethereum network.
 */
public final class PeerRepository {

  private static final Pattern DISCPORT_QUERY_STRING_REGEX = Pattern.compile(".*discport=([^&]+).*");

  private final Supplier<Instant> currentTimeSupplier;
  private final ConcurrentHashMap<Bytes, RepositoryPeer> peers = new ConcurrentHashMap<>();
  private final Set<Consumer<Peer>> peerActiveObservers = ConcurrentHashMap.newKeySet();
  private final Set<Consumer<Peer>> peerInactiveObservers = ConcurrentHashMap.newKeySet();
  private final Set<BiConsumer<Set<String>, Peer>> peerCapabilityObservers = ConcurrentHashMap.newKeySet();
  private final Set<Consumer<Peer>> peerAdditionObservers = ConcurrentHashMap.newKeySet();

  public PeerRepository() {
    this(Instant::now);
  }

  PeerRepository(Supplier<Instant> currentTimeSupplier) {
    this.currentTimeSupplier = currentTimeSupplier;
  }

  /**
   * Get a peer from the repository.
   *
   * @param nodeId The node id for the Peer.
   * @return The peer.
   */
  public Peer get(Bytes nodeId) {
    return peers.computeIfAbsent(nodeId, RepositoryPeer::new);
  }

  /**
   * Get a peer from the repository, potentially updating its endpoint.
   *
   * The returned peer will use the endpoint supplied, unless the peer is already active, in which case its endpoint
   * will be unchanged.
   *
   * @param nodeId The node Id.
   * @param endpoint An endpoint.
   * @return The peer.
   */
  public Peer get(Bytes nodeId, Endpoint endpoint) {
    return peers.compute(nodeId, (id, peer) -> {
      if (peer == null) {
        return new RepositoryPeer(id, endpoint);
      }
      peer.updateEndpointIfInactive(endpoint);
      return peer;
    });
  }

  /**
   * Get a Peer based on a URI.
   *
   * The returned peer will use the endpoint from the URI, unless the peer is already active, in which case its endpoint
   * will be unchanged.
   *
   * @param uri The enode URI.
   * @return The peer.
   */
  public Peer get(String uri) {
    return get(URI.create(uri));
  }

  /**
   * Get a Peer based on a URI.
   *
   * The returned peer will use the endpoint from the URI, unless the peer is already active, in which case its endpoint
   * will be unchanged.
   *
   * @param uri The enode URI.
   * @return The peer.
   * @throws IllegalArgumentException If the URI does is not a valid enode URI.
   */
  public Peer get(URI uri) {
    checkNotNull(uri);
    checkArgument("enode".equals(uri.getScheme()));
    checkArgument(uri.getUserInfo() != null, "URI does not have a nodeId");

    Bytes nodeId = Bytes.fromHexString(uri.getUserInfo());

    int tcpPort = Endpoint.DEFAULT_PORT;
    if (uri.getPort() >= 0) {
      tcpPort = uri.getPort();
    }

    // If TCP and UDP ports differ, expect a query param 'discport' with the UDP port.
    // See https://github.com/ethereum/wiki/wiki/enode-url-format
    int udpPort = tcpPort;
    String query = uri.getQuery();
    if (query != null) {
      Matcher matcher = DISCPORT_QUERY_STRING_REGEX.matcher(query);
      if (matcher.matches()) {
        try {
          udpPort = Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException("Invalid discport query parameter");
        }
      }
    }

    Endpoint endpoint = new Endpoint(uri.getHost(), udpPort, tcpPort);
    return get(nodeId, endpoint);
  }

  /**
   * Add an observer that will be notified when a {@link Peer} is added.
   *
   * Note that the observer will be called while a lock is held on the Peer, so it should return promptly.
   *
   * @param fn The callback that will be supplied with the {@link Peer} is added.
   * @return <tt>true</tt> if the callback function was not already registered for observing.
   */
  public boolean observePeerAddition(Consumer<Peer> fn) {
    return peerAdditionObservers.add(fn);
  }

  /**
   * Remove an observer.
   *
   * @param fn The callback function that was registered using {@link PeerRepository#observePeerAddition(Consumer)}.
   * @return <tt>true</tt> if the callback function was registered for observing.
   */
  public boolean unObservePeerAddition(Consumer<Peer> fn) {
    return peerAdditionObservers.remove(fn);
  }

  /**
   * Add an observer that will be notified when a {@link Peer} becomes active.
   *
   * Note that the observer will be called while a lock is held on the Peer, so it should return promptly.
   *
   * @param fn The callback that will be supplied with the {@link Peer} that has become active.
   * @return <tt>true</tt> if the callback function was not already registered for observing.
   */
  public boolean observePeerActive(Consumer<Peer> fn) {
    return peerActiveObservers.add(fn);
  }

  /**
   * Remove an observer.
   *
   * @param fn The callback function that was registered using {@link PeerRepository#observePeerActive(Consumer)}.
   * @return <tt>true</tt> if the callback function was registered for observing.
   */
  public boolean unObservePeerActive(Consumer<Peer> fn) {
    return peerActiveObservers.remove(fn);
  }

  /**
   * Add an observer that will be notified when a {@link Peer} becomes inactive.
   *
   * Note that the observer will be called while a lock is held on the Peer, so it should return promptly.
   *
   * @param fn The callback that will be supplied with the {@link Peer} that has become inactive.
   * @return <tt>true</tt> if the callback function was not already registered for observing.
   */
  public boolean observePeerInactive(Consumer<Peer> fn) {
    return peerInactiveObservers.add(fn);
  }

  /**
   * Remove an observer.
   *
   * @param fn The callback function that was registered using {@link PeerRepository#observePeerInactive(Consumer)}.
   * @return <tt>true</tt> if the callback function was registered for observing.
   */
  public boolean unObservePeerInactive(Consumer<Peer> fn) {
    return peerInactiveObservers.remove(fn);
  }

  /**
   * Add an observer that will be notified when a {@link Peer} changes capabilities.
   *
   * Note that the observer will be called while a lock is held on the Peer, so it should return promptly.
   *
   * @param fn The callback that will be supplied with the old capability set and the {@link Peer}.
   * @return <tt>true</tt> if the callback function was not already registered for observing.
   */
  public boolean observePeerCapabilities(BiConsumer<Set<String>, Peer> fn) {
    return peerCapabilityObservers.add(fn);
  }

  /**
   * Remove an observer.
   *
   * @param fn The callback function that was registered using
   *        {@link PeerRepository#observePeerCapabilities(BiConsumer)}.
   * @return <tt>true</tt> if the callback function was registered for observing.
   */
  public boolean unObservePeerCapabilities(BiConsumer<Set<String>, Peer> fn) {
    return peerCapabilityObservers.remove(fn);
  }

  // An implementation of Peer connected to the PeerRepository. The Peer repository ensures there is
  // only one instance per nodeId, which allows object equality to be used.
  private final class RepositoryPeer implements Peer {

    private final Bytes nodeId;
    private boolean active;
    private Optional<Endpoint> endpoint;
    private Set<String> capabilities = Collections.emptySet();
    private Optional<Instant> lastSeen = Optional.empty();

    RepositoryPeer(Bytes nodeId) {
      this(nodeId, Optional.empty());
    }

    RepositoryPeer(Bytes nodeId, Endpoint endpoint) {
      this(nodeId, Optional.of(endpoint));
    }

    RepositoryPeer(Bytes nodeId, Optional<Endpoint> optionalEndpoint) {
      this.nodeId = nodeId;
      this.endpoint = optionalEndpoint;
      peerAdditionObservers.forEach(o -> o.accept(this));
    }

    @Override
    public Bytes nodeId() {
      return nodeId;
    }

    @Override
    public Optional<Endpoint> endpoint() {
      return endpoint;
    }

    @Override
    public boolean isActive() {
      return active;
    }

    @Override
    public boolean hasCapability(String capability) {
      return capabilities.contains(capability);
    }

    @Override
    public Set<String> capabilities() {
      return capabilities;
    }

    @Override
    public Optional<Instant> lastSeen() {
      return lastSeen;
    }

    @Override
    public synchronized void setActive(Endpoint endpoint) {
      if (this.active) {
        return;
      }
      this.active = true;
      this.endpoint = Optional.of(endpoint);
      peerActiveObservers.forEach(o -> o.accept(this));
    }

    @Override
    public synchronized void setInactive() {
      this.active = false;
      peerInactiveObservers.forEach(o -> o.accept(this));
      if (!capabilities.isEmpty()) {
        Set<String> oldCapabilities = this.capabilities;
        this.capabilities = Collections.emptySet();
        peerCapabilityObservers.forEach(o -> o.accept(oldCapabilities, this));
      }
    }

    @Override
    public synchronized void updateEndpointIfInactive(Endpoint endpoint) {
      if (this.active) {
        assert (this.endpoint.isPresent());
        return;
      }
      this.endpoint = Optional.of(endpoint);
    }

    @Override
    public void setCapabilities(String... capabilities) {
      setCapabilities(Arrays.asList(capabilities));
    }

    @Override
    public synchronized void setCapabilities(Collection<String> capabilities) {
      if (!this.active) {
        assert (this.capabilities.isEmpty());
        return;
      }
      Set<String> oldCapabilities = this.capabilities;
      this.capabilities = new HashSet<>(capabilities);
      peerCapabilityObservers.forEach(o -> o.accept(oldCapabilities, this));
    }

    @Override
    public void updateLastSeen() {
      lastSeen = Optional.of(currentTimeSupplier.get());
    }
  }
}
