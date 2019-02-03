# 使用Docker/Kubernetes部署一个MySQL+Redis+SpringBoot应用

demo代码仓库：[https://github.com/songxinjianqwe/capsule-demo-app](https://github.com/songxinjianqwe/capsule-demo-app)
# 项目
该项目跑了一个简单的用户添加、查询的示例。
* GET /users
  * 返回所有用户
* GET /users/{userId}
  * 返回该用户信息，优先从Redis缓存中获取，没有命中则从DB拿
* POST /users   
  * body
```json
{
  "id": "tom",
  "nickName": "tom"
}
```
  * 创建一个用户，然后放到Redis缓存中，并落库
# 
# 基于原生Docker+Link
## MySQL
1. docker pull mysql
1. docker run --name=mysql -e MYSQL_ROOT_PASSWORD=$ROOT_PASSWORD -p 3306:3306 -d mysql
1. docker exec -it $CONTAINER_ID bash
1. 创建database
1. exit
### 如何实现数据持久化(Volumn)
-v /data:/var/lib/mysql<br />/data是宿主机的文件目录。<br />-v的意思就是把容器中的目录和宿主机中的目录做映射，我们只要把容器中mysql的数据目录映射到本地，将来就算这个容器被删除了，那么数据也还是在本地。

注意：<br />-v /data<br />是将宿主机下的/var/lib/docker下的某个目录映射到了容器的/data下。<br />这种方式在容器销毁后还会保留，但是宿主机的映射的目录是随机的，不方便重新挂载。

docker run --name=mysql<br />-v /host/mysql/conf:/etc/mysql/conf.d<br />-v /host/mysql/logs:/logs<br />-v /host/mysql/data:/var/lib/mysql<br />-e MYSQL_ROOT_PASSWORD=$ROOT_PASSWORD -p 3306:3306 -d mysql

```powershell
docker run --name=mysql -v /Users/jasper/Dev/data/mysql/data:/var/lib/mysql -v /Users/jasper/Dev/data/mysql/logs:/logs -e MYSQL_ROOT_PASSWORD=123456 -p 3306:3306 -d mysql
```
挂载了数据卷后，即使销毁容器，数据库里的数据也不会丢失，只要指定一个固定的宿主机目录即可（如果-v /var/lib/mysql也是可以挂载的，但是宿主机目录不是固定的，而是随机的，下次再次启动后不方便找到该目录重新挂载）<br />注意，首次创建容器后，需要创建一个名为demo的databse（不需要手动建表，由JPA来自动建表）。之后重启容器后不需要重复此步骤，因为volume可以持久化数据。

## Redis
docker pull redis<br />docker run -d -p 6379:6379 redis

可选：<br />-v ./host/redis/data:/data<br />-v /host/redis/config/redis.conf:/usr/local/etc/redis/redis.conf<br />CMD为redis-server /usr/local/etc/redis/redis.conf<br />
```shell
docker run --name=redis -v /Users/jasper/Dev/data/redis/data:/data -d -p 6379:6379 redis
```

## Spring Boot

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

# 基于Kubernetes
不需要修改Spring Boot的Dockefile。<br />需要将之前docker run时指定的指令均落到Kubernetes的yaml配置文件中。
## MySQL

创建一个单节点的MySQL，它依赖于Persistent-Volume。<br />这里创建了一个PersistentVolume和PersistentVolumeClaim。注意hostPath最好不要设置为用户目录，否则会出现权限问题。
```yaml
kind: PersistentVolume
apiVersion: v1
metadata:
  name: mysql-pv-volume
  labels:
    type: local
spec:
  storageClassName: manual
  capacity:
    storage: 20Gi
  accessModes:
    - ReadWriteOnce
  hostPath:
    path: "/container"
---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: mysql-pv-claim
spec:
  storageClassName: manual
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 20Gi
```
然后创建MySQL的Deployment。
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mysql-deployment
spec:
  replicas: 1
  selector:
    matchLabels:
      app: mysql
  template:
    metadata:
      labels:
        app: mysql
    spec:
      containers:
        - name: mysql
          image: mysql:5.6
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 3306
          env:
            - name: MYSQL_ROOT_PASSWORD
              value: "123456"
          volumeMounts:
            - name: mysql-storage
              mountPath: /var/lib/mysql
      volumes:
        - name: mysql-storage
          persistentVolumeClaim:
            claimName: mysql-pv-claim

```

之后再创建Service，暴露为一个服务，注册DNS。

```yaml
kind: Service
apiVersion: v1
metadata:
  name: mysql-service
spec:
  selector:
    app: mysql
  ports:
    - protocol: TCP
      port: 3306
      targetPort: 3306

```

## Redis
创建一个Redis的Deployment。

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: redis-deployment
spec:
  replicas: 1
  selector:
    matchLabels:
      app: redis
  template:
    metadata:
      labels:
        app: redis
    spec:
      containers:
        - name: redis
          image: redis
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 6379

```
然后暴露为服务：

```yaml
kind: Service
apiVersion: v1
metadata:
  name: redis-service
spec:
  selector:
    app: redis
  ports:
    - protocol: TCP
      port: 6379
      targetPort: 6379

```

## Spring Boot
在运行App前，请先确保Redis和MySQL正确启动。<br />使用kubectl get pods可以查看Pod的状态，如果状态异常，可以使用kubectl describe pods $POD_ID来排查问题，也可以使用kubectl logs -f POD_ID来查看运行日志。

运行App需要注意几个问题：
* image这里使用了本地镜像，否则每次有代码变更就要重新传到公开或私有的镜像仓库（比如阿里云），效率很低，使用本地镜像（即让minikube共享docker daemon的local repository）请按照以下步骤进行：
>   * As the [README](https://github.com/kubernetes/minikube/blob/0c616a6b42b28a1aab8397f5a9061f8ebbd9f3d9/README.md#reusing-the-docker-daemon) describes, you can reuse the Docker daemon from Minikube with `eval $(minikube docker-env)`.
  * So to use an image without uploading it, you can follow these steps:

>   * set the environment variables with `eval $(minikube docker-env)`
  * build the image with the Docker daemon of Minukube (eg `docker build -t my-image .`)
  * set the image in the pod spec like the build tag (eg `my-image`)
  * set the [`imagePullPolicy`](https://kubernetes.io/docs/api-reference/v1/definitions/#_v1_container) to `Never`, otherwise Kubernetes will try to download the image.
  * **Important note:** You have to run `eval $(minikube docker-env)` on each terminal you want to use, since it only sets the environment variables for the current shell session.

  * 如果想让minikube使用本地镜像，需要做到：
    * 在当前shell中eval $(minikube docker-env)
    * docker build
    * 打标签，不能是latest
    * yaml里image pull policy不能用always
    * 在当前shell中kubectl create 
* 如何感知其他服务？
  * Docker是使用Link，而Link是通过修改当前容器的/etc/hosts实现的。
  * Kubernetes是使用环境变量+DNS的方式来实现的
    * 首先需要将Redis、MySQL等暴露为Service，之后会注册到DNS中
      * 可以使用nslookup命令来检查是否正确注册到DNS
      * 域名格式为service-name.namespace.svc.cluster.local
    * 将Spring Boot配置文件中原来的localhost改为${占位符}，如${MYSQL_HOST}，Spring会将该占位符作为key，从环境变量中取值，来拿到完整的域名
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: capsule-demo-app-deployment
spec:
  replicas: 3
  selector:
    matchLabels:
      app: capsule-demo-app
  template:
    metadata:
      labels:
        app: capsule-demo-app
    spec:
      containers:
        - name: capsule-demo-app
          image: capsule/capsule-demo-app:1.0
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 8080
          env:
            - name: MYSQL_HOST
              # 使用环境变量+DNS的方式来访问redis、mysql等其他service暴露的服务
              # 这里环境变量的格式就是service-name.namespace.svc.cluster.local
              # 当redis、mysql暴露为服务后，会注册到DNS中，之后可以使用DNS的方式来访问服务
              # nslookup domain可以测试是否正确注册DNS
              value: mysql-service.default.svc.cluster.local
            - name: REDIS_HOST
              value: redis-service.default.svc.cluster.local
            - name: SPRING_PROFILES_ACTIVE
              value: 'prodk8s'
```
注意下面的${MYSQL_HOST}和${REDIS_HOST}
```yaml
spring:
  datasource:
    url: jdbc:mysql://${MYSQL_HOST}:3306/demo?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT
    username: root
    password: 123456
    driver-class-name: com.mysql.jdbc.Driver
  redis:
    host: ${REDIS_HOST}
    database: 0
    port: 6379
    password:
```

当Deployment启动OK后，暴露为服务：

```yaml
kind: Service
apiVersion: v1
metadata:
  name: capsule-demo-app-service
spec:
  type: NodePort
  selector:
    app: capsule-demo-app
  ports:
    - protocol: TCP
      port: 8080
      targetPort: 8080
      nodePort: 32141
```

启动后，可以通过``minikube service capsule-demo-app --url``来拿到地址，也可以通过'kubectl cluster-info'拿到集群IP，然后端口号为NodePort（32141），这样就可以在宿主机上进行访问了服务。
