FROM docker.elastic.co/logstash/logstash:8.18.0-a7f60a3d-SNAPSHOT

ENV AZURE_CLIENT_ID=
ENV AZURE_TENANT_ID=
ENV AZURE_CLIENT_SECRET=

COPY target/azure-kafka-oauthbearer-0.0.1-SNAPSHOT.jar /usr/share/logstash/logstash-core/lib/jars/azure-kafka-oauthbearer-0.1.0.jar
COPY target/dependencies/* /usr/share/logstash/logstash-core/lib/jars/

COPY logstash.conf /usr/share/logstash/pipeline/logstash.conf
