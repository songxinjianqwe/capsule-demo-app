FROM java:8
VOLUME /tmp
ADD capsule-demo-app.jar app.jar
EXPOSE 8080
ENTRYPOINT [ "sh", "-c", "java -jar /app.jar"]