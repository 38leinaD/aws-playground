FROM jboss/wildfly
COPY ./target/aws-playground.war ${JBOSS_HOME}/standalone/deployments/
#CMD ["/bin/sh"]
