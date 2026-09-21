
FROM eclipse-temurin:26-jdk-noble

ENV CATALINA_HOME=/opt/tomcat
ENV PATH="${CATALINA_HOME}/bin:${PATH}"

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates tar \
    && rm -rf /var/lib/apt/lists/*

RUN curl -fSL \
    https://dlcdn.apache.org/tomcat/tomcat-10/v10.1.60/bin/apache-tomcat-10.1.60.tar.gz \
    -o /tmp/tomcat.tar.gz \
    && mkdir -p ${CATALINA_HOME} \
    && tar -xzf /tmp/tomcat.tar.gz \
       --strip-components=1 \
       -C ${CATALINA_HOME} \
    && rm /tmp/tomcat.tar.gz

RUN rm -rf ${CATALINA_HOME}/webapps/*

COPY dist/MPLADS.war ${CATALINA_HOME}/webapps/MPLADS.war

RUN sed -i 's/port="8080"/port="${PORT:8080}"/' \
    ${CATALINA_HOME}/conf/server.xml

EXPOSE 8080

CMD ["catalina.sh", "run"]