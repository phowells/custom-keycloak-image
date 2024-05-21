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
  </Appenders>

  <Loggers>
    <Root level="INFO">
      <AppenderRef ref="STDOUT"/>
    </Root>

    {{- if .Values.infinispan.logging.categories}}
    {{- range .Values.infinispan.logging.categories }}
    <Logger name="{{ .category }}" level="{{ .level | upper }}" />
    {{- end }}
    {{- end }}

  </Loggers>
</Configuration>
{{- end }}