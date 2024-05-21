{{- define "infinispan.yml" -}}
infinispan:
  cacheContainer:
    # Cache Container name is used for defining health status probe URL paths
    name: default
    statistics: true
    # Requires user permission to access caches and perform operations
    security:
      authorization: {}
    transport:
      # Configure Infinispan to use DNS PING for discovery
      stack: kubernetes
      cluster: \${infinispan.cluster.name}
      site: \${gke.cluster.name}
      # We will map the GKE Zone to the rack
      rack: \${gke.zone.name}
      node-name: \${gke.pod.name}
    serialization:
      # Configure the Keycloak classes that can be deserialized in the console
      allow-list:
        class: java.util.UUID
        regex: org.keycloak.*
    caches:
      # Template for Replicated Caches
      replicated-cache-cfg:
        replicated-cache-configuration:
          mode: SYNC
          statistics: "true"
          locking:
            isolation: READ_COMMITTED
            # Disable striping to create a new lock per entry (greater concurrent throughput, avoid potential deadlocks)
            # Increases memory usage and garbage collection
            striping: false
            # Amount of time, in milliseconds, to wait for a contented lock
            acquire-timeout: 20000
          transaction:
            mode: NON_XA
            locking: PESSIMISTIC
          encoding:
            mediaType: application/x-jboss-marshalling
      # Template for Distributed Caches
      distributed-cache-cfg:
        distributed-cache-configuration:
          mode: SYNC
          statistics: "true"
          locking:
            isolation: READ_COMMITTED
            # Disable striping to create a new lock per entry (greater concurrent throughput, avoid potential deadlocks)
            # Increases memory usage and garbage collection
            striping: false
            # Amount of time, in milliseconds, to wait for a contented lock
            acquire-timeout: 20000
          transaction:
            mode: NON_XA
            locking: PESSIMISTIC
          encoding:
            mediaType: application/x-jboss-marshalling
      # Keycloak work cache
      work:
        replicated-cache:
          configuration: replicated-cache-cfg
      # Keycloak sessions cache
      sessions:
        distributed-cache:
          configuration: distributed-cache-cfg
          owners: {{ .Values.infinispan.config.distributedCacheOwners }}
      # Keycloak authenticated sessions cache
      authenticationSessions:
        distributed-cache:
          configuration: distributed-cache-cfg
          owners: {{ .Values.infinispan.config.distributedCacheOwners }}
      # Keycloak offline sessions cache
      offlineSessions:
        distributed-cache:
          configuration: distributed-cache-cfg
          owners: {{ .Values.infinispan.config.distributedCacheOwners }}
      # Keycloak client sessions cache
      clientSessions:
        distributed-cache:
          configuration: distributed-cache-cfg
          owners: {{ .Values.infinispan.config.distributedCacheOwners }}
      # Keycloak offline client sessions cache
      offlineClientSessions:
        distributed-cache:
          configuration: distributed-cache-cfg
          owners: {{ .Values.infinispan.config.distributedCacheOwners }}
      # Keycloak login failures cache
      loginFailures:
        distributed-cache:
          configuration: distributed-cache-cfg
          owners: {{ .Values.infinispan.config.distributedCacheOwners }}
      # Keycloak action tokens cache
      actionTokens:
        distributed-cache:
          configuration: distributed-cache-cfg
          owners: {{ .Values.infinispan.config.distributedCacheOwners }}
  server:
    endpoints:
      # [USER] Hot Rod and REST endpoints.
      - securityRealm: default
        socketBinding: default
        connectors:
          rest:
            restConnector:
          hotrod:
            hotrodConnector:
      # [METRICS] Metrics endpoint for cluster monitoring capabilities.
      - connectors:
          rest:
            restConnector:
              authentication:
                mechanisms: BASIC
        securityRealm: metrics
        socketBinding: metrics
    interfaces:
      - name: public
        anyAddress: ~
    socketBindings:
      defaultInterface: public
      portOffset: 0
      socketBinding:
        # [USER] Socket binding for the Hot Rod and REST endpoints.
        - name: default
          port: 11222
          # [METRICS] Socket binding for the metrics endpoint.
        - name: metrics
          port: 11223
    security:
      credentialStores:
        - clearTextCredential:
            clearText: secret
          name: credentials
          path: credentials.pfx
      securityRealms:
        # [USER] Security realm for the Hot Rod and REST endpoints.
        - name: default
          # [USER] Comment or remove this properties realm to disable authentication.
          propertiesRealm:
            groupProperties:
              path: groups.properties
            groupsAttribute: Roles
            userProperties:
              path: users.properties
          # [METRICS] Security realm for the metrics endpoint.
        - name: metrics
          propertiesRealm:
            groupProperties:
              path: metrics-groups.properties
              relativeTo: infinispan.server.config.path
            groupsAttribute: Roles
            userProperties:
              path: metrics-users.properties
              relativeTo: infinispan.server.config.path
  {{- end }}
