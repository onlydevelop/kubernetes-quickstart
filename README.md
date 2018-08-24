# Simple Kubernetes demo

This is a Simple java application which is run in a local minikube Kubernetes setup.

# How to

## Step 1: Create the application

Just create an application using SpringBoot which will return the hostname and timestamp. The controller looks like:

```java
@RestController
public class InfoController {

	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");

	@RequestMapping("/")
	public Info getInfo() throws UnknownHostException {
		return new Info(
				InetAddress.getLocalHost().getHostName(),
				sdf.format(new Timestamp(System.currentTimeMillis()))
				);
	}
}
```

So, if I do a `./gradlew bootRun`, from a different terminal, I can curl the URL:

```
$ curl localhost:8080
{"hostname":"dream.local","timestamp":"2018.08.24.11.38.13"}
```
## Step 2: Dockerize the application

Dockerfile
```
FROM java:8
WORKDIR /
ADD ./build/libs/demo-0.1.0.jar demo-0.1.0.jar
EXPOSE 8080
CMD java -jar demo-0.1.0.jar
```

The above Dockerfile can be built using:

```
$ docker build -f Dockerfile -t java-sample-demo:0.1 .
```

and it can be run as:

```
$ docker run -it -p 8080:8080 java-sample-demo:0.1
```

It can be tested using the same curl command:

```
$ curl localhost:8080
{"hostname":"a9970441b6cc","timestamp":"2018.08.24.12.40.12"}
```

## Step 3: Create the Kubernetes helm chart

1. Create a directory kubernetes
2. Create the following files:

`kubernetes/Chart.yaml`
```yaml
name: Sample App
version: 0.1.0
description: Helm Demo
maintainers:
    - name: Dipanjan Bhowmik
```

`kubernetes/templates/deployment.yaml`
```
apiVersion: extensions/v1beta1
kind: Deployment
metadata:
    name: sample-app
spec:
    replicas: 2
    template:
        metadata:
            labels:
                app: sample-app
        spec:
            containers:
            - name: sample-app
              image: java-sample-demo:0.1
              image-pull-policy: Never
              ports:
              - containerPort: 8080
```

`kubernetes/templates/service.yaml'
```yaml
apiVersion: v1
kind: Service
metadata:
    name: sample-app
spec:
    selector:
        app: sample-app
    ports:
        - name: sample-app
          protocol: TCP
          port: 8080
```
## Step 4: Start minikube

You must have minikube and virtualbox driver (I am using it, you can use any other driver) installed as a pre-requisite. So, if you have that following are the commands to execute:

```
$ minikube start --vm-driver=virtualbox
$ kubectl config use-context minikube
```

Just to check the cluster status:

```
$ kubectl cluster-info

```

We need to switch the docker context as well for the docker images to be available locally for the minikube.

```
$ eval $(minikube docker-env)
```

## Step 5: Run the image in the Kubernetes

As a next step we need to build the image again in the minikube docker context.

```
$ docker build -f Dockerfile -t java-sample-demo:0.1 .
```

We are almost there. So, now we need to use helm to deploy the service to the Kubernetes cluster. Run the following command from the root of the project.

```
$ helm install kubernetes
```

You will see that the containers are getting created using the following command:

```
$ kubectl get pods
```

To see if the exposed service has the endpoints, issue the command:

```
$ kubectl get svc
```

When the containers are ready, run the following command to open a browser which will point to your service:

```
$ minikube service sample-app
```
It might take a while to start the service properly. So, just give some time.

I find it easier to check that from the `curl` command. Curl the url which was opened in the browser multiple times and you will see the hostnames will change. These hostnames will actually show which pods is currently serving your request. So, the service actually load balances your request to the internal containers running in the pods.

## Step 6: Scale the Service

Now, it is time to scale the service. It is very easy. Just change the line  `replicas: 2` to `replicas: 3` in the `kubernetes/templates/deployment.yaml` and then run the command:

```
$ helm upgrade <release_name> kubernetes
```
<release_name> is the name of the release you got at the first line of output of `helm install` command. You can see that also from the `helm ls` command as well.

Now, if you do a `kubectl get pods` you will see that the new container is getting created in a new pod. Mind it, the service is still live and serving requests which you can see by the `curl` command as issued earlier.

Now, when the new container is running fully, you will be able to see that the service is load balancing your requests to now new set of containers.

## Step 7: [Optional] Delete a pod

Now, lets simulate a situation when a pod is deleted. Issue the following command:

```
$ kubectl delete pod <pod_name>
```

Replace <pod_name> with any of the pod_name from the `kubectl get pods` command.

Now, continuously hit the service endpoint with `curl` command. You will notice it will be fine.

You can issue a `kubectl get pods` command as well to see auto-healing of Kubernetes in action, where a new container is getting created.

## Step 8: Rollback a deployment

Now, lets try to rollback the deployment which have just done by upgrade.

First, lets see the deployment history:

```
$ helm history <release_name>
```

You will see all the deployments done here.

Now, to rollback you need to run the simple command:

```
$ helm rollback <release_name> <revision>
```

And, that's it! It will do the rollback for you.

After the rollback is complete. Please do a `helm history` to see that the rollback state gets a new revision in helm.

## Step 9: Delete the deployment

So, we are done with the demo, so we will tear down the deployment just by this command:

```
$ helm delete <release_name>
```

And, if you also want to stop the minikube after that:

```
$ minukube stop
```

## Bonus: Creating a smaller docker image

The docker image we have created was large in size. So, we can create a smaller image using docker builder pattern.

We have created a new dockerfile.

Dockerfile2
```
FROM java:8 AS BUILD_IMAGE
ENV APP_HOME=/root/demo/
RUN mkdir -p $APP_HOME/src/main/java
WORKDIR $APP_HOME
COPY build.gradle gradlew gradlew.bat $APP_HOME
COPY gradle $APP_HOME/gradle
RUN ./gradlew init
COPY . .
RUN ./gradlew build

FROM anapsix/alpine-java
WORKDIR /root/
COPY --from=BUILD_IMAGE /root/demo/build/libs/demo-0.1.0.jar .
EXPOSE 8080
CMD java -jar demo-0.1.0.jar
```

Please note that this has two FROM line. The first one is the container where the jar is built and the second is the container which is backed with jar copied from the first container using the `COPY --from` command.

This a standard pattern and best practice for creating a smaller docker image. One of the primary reason is when a new pods is created (in the begining or scaling or auto-heal time), the container pulls the image. So, smaller the image, quicker is the boot time for the container in the pods, which all of us want.

## Bonus: Makefile

I am still a fan of Makefile. Because, I find it as a cleanest way of documenting your command and running it. So, I have a Makefile in the root of this project which can be used very easily (in case you never used it earlier):

```
$ make docker-build-small # To build the small container
$ make docker-run-small # To run the small container
```

## Disclaimer

This is just a very basic demo to get you started with Kubernetes. I do not recommend it to be done in the production.
