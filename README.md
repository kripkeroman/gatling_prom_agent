# Gatling Prometheus Agent

## Overview

**Gatling Prometheus Agent** is a Java agent for [Gatling](https://gatling.io/), that intercepts load testing events and sends metrics to [Prometheus Pushgateway](https://github.com/prometheus/pushgateway).

The agent leverages [ByteBuddy](https://bytebuddy.net/) to instrument Gatling internals dynamically via `-javaagent`. It automatically registers counters, histograms, and gauges, providing comprehensive statistics about executed load tests.

## Key Features

* **Prometheus Pushgateway support** for pushing metrics.
* Tracking of:

    * started and finished users per scenario;
    * number of active users;
    * requests count, errors count, response duration distribution;
    * aggregated statistics (min / max / mean / stddev of response time).
* Supports both **.yml** and **.properties** configuration files.
* Flexible configuration using:

    * JVM system properties (`-Dprom.*`);
    * environment variables;
    * external configuration file (`application.yml` or `config.properties`).

## Usage

### 1. Build the project

```bash
mvn clean package
```

The resulting agent jar will be available at `target/gatling-prom-agent.jar`.

### 2. Run Gatling with the agent

Example command for Windows (PowerShell):

```powershell
mvn gatling:test `
  "-Dgatling.jvmArgs=-javaagent:$($PWD.Path)\target\gatling-prom-agent.jar -Dprom.config.file=$($PWD.Path)\src\test\resources\application.yml" `
  "-Dlogback.configurationFile=logback.xml" `
  "-Dlogfile.name=gatling-metrics-$(Get-Date -Format 'yyyy-MM-dd_HH-mm').log"
```

Example command for Linux/macOS (bash):

```bash
mvn gatling:test \
  "-Dgatling.jvmArgs=-javaagent:$(pwd)/target/gatling-prom-agent.jar -Dprom.config.file=$(pwd)/src/test/resources/application.yml" \
  "-Dlogback.configurationFile=logback.xml" \
  "-Dlogfile.name=gatling-metrics-$(date +'%Y-%m-%d_%H-%M').log"
```

### 3. Configuration file

The agent requires a **configuration file** to know where to send metrics. Supported formats are `.yml` and `.properties`.

Example `application.yml`:

```yaml
prom:
  pushgateway:
    url: http://localhost:9091
  job: gatling
  instance: default
  push:
    period:
      seconds: 5
  delete:
    on:
      stop: true
  histogram:
    buckets:
      ms: 50,100,200,500,1000
```

Example `config.properties`:

```properties
prom.pushgateway.url=http://localhost:9091
prom.job=gatling
prom.instance=default
prom.push.period.seconds=5
prom.delete.on.stop=true
prom.histogram.buckets.ms=50,100,200,500,1000
```

### 4. Environment variables and system properties

Configuration values can be overridden:

* via JVM system properties (e.g. `-Dprom.job=myJob`)
* via environment variables (e.g. `PROM_JOB=myJob`)

Precedence order:

```
System properties > Environment variables > Config file > Defaults
```

## Example Metrics

Once the test starts, the following metrics will be available in Prometheus:

* `gatling_requests_total{name,group,status}`
* `gatling_response_time_seconds{name,group,status}`
* `gatling_users_started_total{scenario}`
* `gatling_users_finished_total{scenario}`
* `gatling_active_users{scenario}`
* `gatling_errors_total{name,group}`
* `gatling_response_time_min_ms`
* `gatling_response_time_max_ms`
* `gatling_response_time_mean_ms`
* `gatling_response_time_stddev_ms`
* `gatling_writer_heartbeat`

## License

This project is licensed under the [Apache License 2.0](LICENSE).
