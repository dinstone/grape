# Grape
Grape is a distributed based on redis delay task system.

## Job Lifecycle
A job in Grape during its life it can be in one of four states: "delayed", "ready", "reserved", or "failed".

Here is a picture of the typical job lifecycle:
![image](https://github.com/dinstone/grape/wiki/images/DelayJobStatemachine.png)