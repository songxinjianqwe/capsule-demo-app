# Capsule-Demo-App
使用Docker+Link以及Kubernetes部署一个Spring Boot + MySQL + Redis的应用集群
后续会考虑自己实现一个容器运行时capsule；
并且会考虑实现Kubernetes的CRI(Container Runtime Interface)接口；
此后会基于capsule来部署我们这个demo应用。

## 以Docker+Link的方式部署
### MySQL
1. docker pull mysql
1. docker run --name=mysql -e MYSQL_ROOT_PASSWORD=$ROOT_PASSWORD -p 3306:3306 -d mysql
1. docker exec -it $CONTAINER_ID bash
1. 创建database
1. exit
#### 如何实现数据持久化(Volumn)
-v /data:/var/lib/mysql<br />/data是宿主机的文件目录。<br />-v的意思就是把容器中的目录和宿主机中的目录做映射，我们只要把容器中mysql的数据目录映射到本地，将来就算这个容器被删除了，那么数据也还是在本地。

注意：<br />-v /data<br />是将宿主机下的/var/lib/docker下的某个目录映射到了容器的/data下。<br />这种方式在容器销毁后还会保留，但是宿主机的映射的目录是随机的，不方便重新挂载。

docker run --name=mysql<br />-v /host/mysql/conf:/etc/mysql/conf.d<br />-v /host/mysql/logs:/logs<br />-v /host/mysql/data:/var/lib/mysql<br />-e MYSQL_ROOT_PASSWORD=$ROOT_PASSWORD -p 3306:3306 -d mysql

```powershell
docker run --name=mysql -v /Users/jasper/Dev/data/mysql/data:/var/lib/mysql -v /Users/jasper/Dev/data/mysql/logs:/logs -e MYSQL_ROOT_PASSWORD=123456 -p 3306:3306 -d mysql
```
挂载了数据卷后，即使销毁容器，数据库里的数据也不会丢失，只要指定一个固定的宿主机目录即可（如果-v /var/lib/mysql也是可以挂载的，但是宿主机目录不是固定的，而是随机的，下次再次启动后不方便找到该目录重新挂载）

### Redis
docker pull redis<br />docker run -d -p 6379:6379 redis

可选：<br />-v ./host/redis/data:/data<br />-v /host/redis/config/redis.conf:/usr/local/etc/redis/redis.conf<br />CMD为redis-server /usr/local/etc/redis/redis.conf<br />
```shell
docker run --name=redis -v /Users/jasper/Dev/data/redis/data:/data -d -p 6379:6379 redis
```

### Spring Boot

```dockerfile
FROM java:8
VOLUME /tmp
ADD capsule-demo-app.jar app.jar
ENTRYPOINT [ "sh", "-c", "java -jar /app.jar"]
```

```shell
docker run -d -p 8080:8080 -e "SPRING_PROFILES_ACTIVE=prod" --link mysql:mysql-container --link redis:redis-container --name capsule-demo-app capsule/capsule-demo-app
```
这里使用CMD来传入profile，指定为prod。<br />根据SpringBoot规范，我们会在src/main/resources下创建一个application-prod.yml/properties配置文件来指定profile为prod的特定配置，在这个配置文件中，我们会连接redis、mysql的url从localhost改为-link时候alias（冒号后的）。<br />--link默认是基于DNS实现的，在启动Spring Boot容器时，会将容器IP与容器别名放到/etc/hosts，这样我们只需要把连接的IP改为别名即可。

## 以Kubernetes的方式部署

