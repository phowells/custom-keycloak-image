{{- define "infinispan-log4j2.xml" -}}
<Configuration name="InfinispanServerConfig" monitorInterval="60" shutdownHook="disable">
  <Properties>
    <Property name="path">${sys:infinispan.server.log.path}</Property>
    <Property name="accessLogPattern">%X{address} %X{user} [%d{dd/MMM/yyyy:HH:mm:ss Z}] &quot;%X{method} %m %X{protocol}&quot; %X{status} %X{requestSize} %X{responseSize} %X{duration}%n</Property>
  </Properties>
  <Appenders>
    <!-- Colored output on the console -->
    <Console name="STDOUT">
      <PatternLayout pattern="%highlight{%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p (%t) [%c] %m%throwable}{INFO=normal, DEBUG=normal, TRACE=normal}%n"/>
    </Console>

    <!-- Rolling file -->
    <RollingFile name="FILE" createOnDemand="true"
                 fileName="${path}/server.log"
                 filePattern="${path}/server.log.%d{yyyy-MM-dd}-%i">
      <Policies>
        <OnStartupTriggeringPolicy />
        <SizeBasedTriggeringPolicy size="100 MB" />
        <TimeBasedTriggeringPolicy />
      </Policies>
      <PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p (%t) [%c] %m%throwable%n"/>
    </RollingFile>

    <!-- Rolling file -->
    <RollingFile name="AUDIT-FILE" createOnDemand="true"
                 fileName="${path}/audit.log"
                 filePattern="${path}/audit.log.%d{yyyy-MM-dd}-%i">
      <Policies>
        <OnStartupTriggeringPolicy />
        <SizeBasedTriggeringPolicy size="100 MB" />
        <TimeBasedTriggeringPolicy />
      </Policies>
      <PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss,SSS} %m%n"/>
    </RollingFile>

    <!-- Rolling JSON file, disabled by default -->
    <RollingFile name="JSON-FILE" createOnDemand="true"
                 fileName="${path}/server.log.json"
                 filePattern="${path}/server.log.json.%d{yyyy-MM-dd}-%i">
      <Policies>
        <OnStartupTriggeringPolicy />
        <SizeBasedTriggeringPolicy size="100 MB" />
        <TimeBasedTriggeringPolicy />
      </Policies>
      <JsonLayout compact="true" eventEol="true" stacktraceAsString="true">
        <KeyValuePair key="time" value="$${date:yyyy-MM-dd'T'HH:mm:ss.SSSZ}" />
      </JsonLayout>
    </RollingFile>

    <!-- Rolling HotRod access log, disabled by default -->
    <RollingFile name="HR-ACCESS-FILE" createOnDemand="true"
                 fileName="${path}/hotrod-access.log"
                 filePattern="${path}/hotrod-access.log.%i">
      <Policies>
        <SizeBasedTriggeringPolicy size="100 MB" />
      </Policies>
      <PatternLayout pattern="${accessLogPattern}"/>
    </RollingFile>
    <!-- Rolling REST access log, disabled by default -->
    <RollingFile name="REST-ACCESS-FILE" createOnDemand="true"
                 fileName="${path}/rest-access.log"
                 filePattern="${path}/rest-access.log.%i">
      <Policies>
        <SizeBasedTriggeringPolicy size="100 MB" />
      </Policies>
      <PatternLayout pattern="${accessLogPattern}"/>
    </RollingFile>
  </Appenders>

  <Loggers>
    <Root level="INFO">
      <AppenderRef ref="STDOUT"/>

      <!-- Uncomment just one of the two lines bellow to use alternatively JSON logging or plain-text logging to file-->
      <AppenderRef ref="FILE"/>
<!--      <AppenderRef ref="JSON-FILE"/>-->
    </Root>

    <!-- Set to INFO to enable audit logging -->
    <Logger name="org.infinispan.AUDIT" additivity="false" level="ERROR">
      <AppenderRef ref="AUDIT-FILE"/>
    </Logger>

    <!-- Set to TRACE to enable access logging for Hot Rod requests -->
    <Logger name="org.infinispan.HOTROD_ACCESS_LOG" additivity="false" level="INFO">
      <AppenderRef ref="HR-ACCESS-FILE"/>
    </Logger>

    <!-- Set to TRACE to enable access logging for REST requests -->
    <Logger name="org.infinispan.REST_ACCESS_LOG" additivity="false" level="INFO">
      <AppenderRef ref="REST-ACCESS-FILE"/>
    </Logger>

    {{- if .Values.infinispan.logging.categories}}
    {{- range .Values.infinispan.logging.categories }}
    <Logger name="{{ .category }}" level="{{ .level | upper }}" />
    {{- end }}
    {{- end }}

  </Loggers>
</Configuration>
{{- end }}