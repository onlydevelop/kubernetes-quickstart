JAR_NAME=demo-0.1.0.jar

run:
	./gradlew bootRun

compile:
	./gradlew clean build

start:
	java -jar ./build/libs/$(JAR_NAME)

docker-build: compile
	docker build -f Dockerfile -t java-sample-demo:0.1 .

docker-build-small: compile
	docker build -f Dockerfile2 -t java-sample-demo:0.2 .

docker-run:
	docker run -it -p 8080:8080 java-sample-demo:0.1

docker-run-small:
	docker run -it -p 8080:8080 java-sample-demo:0.2
