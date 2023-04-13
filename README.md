# Grape

Grape is a distributed delay job system based on Redis.

## Job Lifecycle

A job in Grape during its life it can be in one of four states: "delay", "ready", "reserved", or "failed".

Here is a picture of the typical job lifecycle:
![image](https://github.com/dinstone/grape/wiki/images/DelayJobStatemachine.png)

## Admin UI
Access grape admin endpoint: http://localhost:9595/

![image](https://github.com/dinstone/grape/wiki/images/admin-main.jpeg)

![image](https://github.com/dinstone/grape/wiki/images/admin-chart.png)

# Quick Start

## step 1: clone project from github

```
git clone https://github.com/dinstone/grape.git
```

## step 2: source building

```
maven install
```

## step 3: deployment package

```
unzip grape-server-1.2.0.zip
```

## step 4: start grape by script

```
cd grape-server-1.2.0/bin
./start.sh
```

## step 5: stop grape by script

```
cd grape-server-1.2.0/bin
./stop.sh
```

# API and Examples

This here can find [API Definition](https://documenter.getpostman.com/view/8030511/SVYoufE8)

### Produce API
```
Example Request
curl --location --request POST "http://localhost:9521/api/job/produce?tube=test&id=j001&dtr=2000&ttr=10000" \
  --header "Content-Type: application/json" \
  --data "{
    \"command\": \"create order\",
    \"order\": \"order body\"
}"

Example Response 200 － OK
{
  "code": "1",
  "message": "success"
}
```
### Delete API
```
Example Request
curl --location --request DELETE "http://localhost:9521/api/job/delete?tube=test&id=j001" \
  --header "Content-Type: application/json"
Example Response200 OK
{
  "code": "1",
  "message": "success",
  "result": false
}
```
### Consume API
```
Example Request
curl --location --request GET "http://localhost:9521/api/job/consume?tube=test&max=10" \
  --header "Content-Type: application/json"
Example Response200 OK
{
  "code": "1",
  "message": "success",
  "result": [
    {
      "id": "j001",
      "dtr": 2000,
      "ttr": 10000,
      "noe": 1,
      "data": "ewogICAgImNvbW1hbmQiOiAiY3JlYXRlIG9yZGVyIiwKICAgICJvcmRlciI6ICJvcmRlciBib2R5Igp9"
    }
  ]
}
```
### Finish API
```
Example Request
curl --location --request DELETE "http://localhost:9521/api/job/finish?tube=test&id=j001" \
  --header "Content-Type: application/json"
Example Response200 OK
{
  "code": "1",
  "message": "success",
  "result": false
}
```
### Release API
```
Example Request
curl --location --request PUT "http://localhost:9521/api/job/release?tube=test&id=j001&dtr=2000" \
  --header "Content-Type: application/json"
Example Response200 OK
{
  "code": "1",
  "message": "success",
  "result": false
}
```
### Tube Set API
```
Example Request
curl --location --request GET "http://localhost:9521/api/tube/set" \
  --header "Content-Type: application/json"
Example Response200 OK
{
  "code": "1",
  "message": "success",
  "result": [
    "test"
  ]
}
```
### Tube Stats API
```
Example Request
curl --location --request GET "http://localhost:9521/api/tube/stats/test" \
  --header "Content-Type: application/json"
Example Response200 OK
{
  "code": "1",
  "message": "success",
  "result": {
    "dateTime": 1564712682507,
    "tubeName": "test",
    "totalJobSize": 3,
    "finishJobSize": 0,
    "delayQueueSize": 0,
    "readyQueueSize": 1,
    "retainQueueSize": 0,
    "failedQueueSize": 0
  }
}
```
