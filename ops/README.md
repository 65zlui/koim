# koim monitoring stack

Local Prometheus + Grafana + Alertmanager that scrapes the koim Spring Boot app at
`/actuator/prometheus`.

## Bring up

```bash
cd ops
docker compose -f docker-compose.monitoring.yml up -d
```

Then run koim normally (`./gradlew bootRun`).

## URLs

| Service       | URL                          | Credentials       |
|---------------|------------------------------|-------------------|
| Prometheus    | http://localhost:9090        | —                 |
| Grafana       | http://localhost:3000        | admin / admin     |
| Alertmanager  | http://localhost:9093        | —                 |

In Grafana the **koim → IM Overview** dashboard is auto-provisioned. To verify the
scrape is healthy, check Prometheus → Status → Targets — the `koim` target should be `UP`.

## Files

- `docker-compose.monitoring.yml` — Prometheus + Grafana + Alertmanager
- `prometheus.yml` — scrape config (targets `host.docker.internal:8080` so the container
  can reach the koim app running on your host machine)
- `alerts.yml` — platform + business alert rules
- `alertmanager.yml` — routing & inhibit rules. Replace the placeholder webhook with
  your real Slack/Feishu/email integration.
- `grafana/provisioning/` — Grafana datasource + dashboard auto-provisioning

## Tear down

```bash
docker compose -f docker-compose.monitoring.yml down            # keep data
docker compose -f docker-compose.monitoring.yml down -v         # wipe data
```

## Production notes

- Replace `host.docker.internal:8080` with real targets (DNS, k8s service discovery, etc.).
- Bind actuator to a separate management port and firewall it. Right now `/actuator/**`
  is `permitAll` — fine for dev, dangerous in production.
- Add `redis_exporter`, `postgres_exporter`, and `node_exporter` for broader coverage.
