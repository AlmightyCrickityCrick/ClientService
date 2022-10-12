# Client Service
The following project is part of the laboratory number 2 for Network Programming Course. The objective of the laboratory work is to simulate a food ordering environment within Docker Containers using Threads.

To run this project it is recommended to rebuild the FatJar using the command.

```
./gradlew :buildFatJar  
```    

And then use the commands below to create the image and run the docker container. Beware that before running the client, all other containers and the, the docker network must be created first, as well as the system variable JAVA_HOME must be configured.

```
docker network create lina-restaurant network
docker build -t client .     
docker run --name client-container --network lina-restaurant-network  -p 8089:8089 client
```