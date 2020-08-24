
# 1st stage, build the app
FROM maven:3.6-jdk-11 as build

WORKDIR /helidon

ADD pom.xml .
RUN mvn package -DskipTests

# Do the Maven build!
# Incremental docker builds will resume here when you change sources
ADD src src
RUN mvn package -DskipTests
RUN echo "done!"

# 2nd stage, build the runtime image

FROM oracle/graalvm-ce:20.2.0-java11

WORKDIR /helidon

#Install
RUN $JAVA_HOME/bin/gu install python R 


ADD scripts/*  /helidon/scripts/
ADD  datagouvfr.pem /helidon/certs/datagouvfr.pem 

# Add datagouvfr cert
RUN  keytool  -importcert  -file /helidon/certs/datagouvfr.pem  -alias datagouvfr -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit -trustcacerts -noprompt    




# Copy the binary built in the 1st stage
COPY --from=build /helidon/target/helidon-polyglot-demo.jar ./
COPY --from=build /helidon/target/libs ./libs

CMD ["java", "-jar", "-Dapp.covid.rscript=/helidon/scripts/covidgraph.R", "-Dapp.covid.cscript=/helidon/scripts/downloadstatus" , "-Dapp.covid.pyscript=/helidon/scripts/department.py" , "helidon-polyglot-demo.jar"]

EXPOSE 8080
