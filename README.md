# Grape
Grape is a distributed based on redis delay task system.

## Job Lifecycle
A job in Grape during its life it can be in one of four states: "delayed", "ready", "reserved", or "failed".

Here is a picture of the typical job lifecycle:
![image](https://github.com/dinstone/grape/wiki/images/DelayJobStatemachine.png)

## Admin UI
Access grape admin endpoint: http://localhost:9595/

![image](https://github.com/dinstone/grape/wiki/images/admin-main.jpeg)

![image](https://github.com/dinstone/grape/wiki/images/admin-chart.png)

## Quick Start
### step 1: clone project from github
```
git clone https://github.com/dinstone/grape.git
```
### step 2: source building
```
maven install
```
### step 3: deployment package
```
unzip grape-server-1.2.0.zip
```
### step 4: start grape by script
```
cd grape-server-1.2.0/bin
./start.sh
```
### step 5: stop grape by script
```
cd grape-server-1.2.0/bin
./stop.sh
```