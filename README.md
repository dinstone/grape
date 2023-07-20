# Grape Introduce

Grape is a distributed delay job system based on Redis.

[真正的千万级分布式延迟任务系统 Grape](https://dinstone.github.io/2023/07/07/delay-task-with-redis/)

## Releases

* [1.3.1] improve API and bugfix.

* [1.3.0](https://github.com/dinstone/grape/releases/tag/1.3.0) support redis cluster model.

* [1.2.2](https://github.com/dinstone/grape/releases/tag/1.2.2)  is a stable version of 1.2 and has fixed several bugs.

* 1.3.1 optimized the scheduling model and supports zero delay scheduling algorithms

* 1.1.0 add admin web UI project.

* 1.0.0 have released a Redis based delayed job system and opened the rest interface.

## Job Lifecycle

A job in Grape during its life it can be in one of four states: "delay", "ready", "remain", or "failed".

Here is a picture of the typical job lifecycle:

![image](https://dinstone.github.io/img/arch/grape-status.png)

## Admin UI

Access grape admin endpoint: http://localhost:9595/

![image](https://github.com/dinstone/grape/wiki/images/admin-main.jpeg)

![image](https://github.com/dinstone/grape/wiki/images/admin-chart.png)

# Quick Start [快速开始](https://github.com/dinstone/grape/wiki/Quick.md)

## step 1: clone project from github

```shell
git clone https://github.com/dinstone/grape.git
```

## step 2: source building

```shell
cd grape
maven package
```

## step 3: deployment package

```shell
cd grape/grape-server/target
unzip grape-server-1.3.0.zip
cd grape-server-1.3.0/config/
```

edit redis config from config.json file.

* jedis pooled client config

```json
"redis": {
  "nodes": [
    {
      "host": "127.0.0.1",
      "port": 6379
    }
  ],
  "model": "pooled",
  "maxTotal": 8,
  "minIdle": 1,
  "timeout": 2000,
  "maxWaitMillis": 3000,
  "numTestsPerEvictionRun": -1,
  "minEvictableIdleTimeMillis": 60000,
  "timeBetweenEvictionRunsMillis": 30000
}
```

* jedis cluster client config

```json
"redis": {
  "nodes": [
    {
      "host": "192.168.1.120",
      "port": 7001
    },
    {
      "host": "192.168.1.120",
      "port": 7002
    },
    {
      "host": "192.168.1.120",
      "port": 7003
    }
  ],
  "model": "cluster",
  "maxTotal": 4,
  "minIdle": 1,
  "timeout": 2000,
  "maxWaitMillis": 3000,
  "numTestsPerEvictionRun": -1,
  "minEvictableIdleTimeMillis": 60000,
  "timeBetweenEvictionRunsMillis": 30000
}
```

edit users info from user.json file.

```json
{
	"admin": {
		"password": "grape",
		"roles": [
			"writer",
			"reader"
		]
	},
	"guid": {
		"password": "grape",
		"roles": [
			"reader"
		]
	}
}
```

## step 4: start grape by script

```shell
cd grape-server-1.3.1/bin
./start.sh
```

## step 5: stop grape by script

```shell
cd grape-server-1.3.1/bin
./stop.sh
```

# API and Examples

You can find the complete [API Definition](https://documenter.getpostman.com/view/8030511/SVYoufE8) here.

### Produce API

```shell
Example Request
curl --location 'http://localhost:9521/api/job/produce?tube=test&jid=j004&dtr=1000&ttr=10000' \
--header 'Content-Type: application/json' \
--data '{
    "orderId": "j001",
    "sumMoney": 2000,
    "count": 10000
}'

Example Response 200 － OK
true
```

### Delete API

```shell
Example Request
curl --location --request DELETE 'http://localhost:9521/api/job/delete?tube=test&jid=j001'

Example Response200 OK
true
```

### Consume API

```shell
Example Request
curl --location 'http://localhost:9521/api/job/consume?tube=test&max=10'

Example Response200 OK
[
    {
        "id": "j004",
        "dtr": 1000,
        "ttr": 10000,
        "noe": 0,
        "data": "ewogICAgIm9yZGVySWQiOiAiajAwMSIsCiAgICAic3VtTW9uZXkiOiAyMDAwLAogICAgImNvdW50IjogMTAwMDAKfQ"
    }
]
```

### Finish API

```shell
Example Request
curl --location --request DELETE 'http://localhost:9521/api/job/finish?tube=test&jid=j003'

Example Response200 OK
true
```

### Release API

```shell
Example Request
curl --location --request PUT 'http://localhost:9521/api/job/release?tube=test&jid=j001&dtr=20000'

Example Response200 OK
true
```

### Tube list API

```shell
Example Request
curl --location 'http://localhost:9521/api/tube/list'

Example Response200 OK
[
    "test"
]
```

### Tube Stats API

```shell
Example Request
curl --location 'http://localhost:9521/api/tube/stats'

Example Response200 OK
[
    {
        "dateTime": 1689390610122,
        "tubeName": "test",
        "totalJobSize": 0,
        "finishJobSize": 0,
        "delayQueueSize": 0,
        "retainQueueSize": 0,
        "failedQueueSize": 1
    }
]
```
# Documents

[架构设计](https://github.com/dinstone/grape/wiki)

[快速开始](https://github.com/dinstone/grape/wiki/Quick.md)


# License

Grape is released under the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0).