# How to deploy a redis container
```bash
docker run -d -p6379:6379 --name redis redis
```

# How to create local redis cluster
> `en0` is your network interface that you're using right now.

```bash
ip=$(ipconfig getifaddr en0) docker compose up -d
```

# How to remove local redis cluster
```bash
ip=$(ipconfig getifaddr en0) docker compose down
```