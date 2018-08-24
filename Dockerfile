FROM java:8
WORKDIR /
ADD ./build/libs/demo-0.1.0.jar demo-0.1.0.jar
EXPOSE 8080
CMD java -jar demo-0.1.0.jar