FROM java:8
VOLUME /tmp
ADD capsule-demo-app.jar app.jar
RUN bash -c 'touch /app.jar'
ENTRYPOINT [ "sh", "-c", "java -jar /app.jar"]