FROM openjdk:11
VOLUME /tmp
COPY /target/OrderMS-0.0.1-SNAPSHOT.jar /usr/app/
WORKDIR /usr/app
EXPOSE 9100
ENV JAVA_OPTS=""
RUN sh -c "touch OrderMS-0.0.1-SNAPSHOT.jar"
ENTRYPOINT ["java", "-jar", "OrderMS-0.0.1-SNAPSHOT.jar"]