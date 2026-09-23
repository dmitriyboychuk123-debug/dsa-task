# DSM Producer-Consumer Task

## Solution description

The solution demonstrates producer-consumer setup where producer generates normally distributed double values with
randomly added anomalies and sends them to an SQS FIFO queue. The consumer constantly reads values from the queue and
performs anomaly detection in data distribution.

```
producer-service --(double as text)--> SQS FIFO queue (LocalStack) --> consumer-service --> console log
```

## Tech stack

- Java 21, Gradle (wrapper included)
- Spring Boot 4.1.1, Spring Cloud AWS 4.1.1 (SQS)
- LocalStack 4.14.0 for SQS FIFO queue
- Apache Commons Math 3 for rolling window statistics
- JUnit 5, Mockito, Testcontainers, Awaitility for tests

## Project structure

- `producer` - generates values and publishes them to the queue every 50 ms
- `consumer` - anomaly detection while listening to the queue
- `buildSrc` - shared Gradle conventions and dependencies
- `docker-compose.yaml` - orchestrates LocalStack, producer and consumer services

## Prerequisites

- Docker with Docker Compose
- JDK 21 only if building or testing outside of Docker
- On Windows, the `.sh` scripts require Git Bash or WSL

## How to run

1. Build and run `docker compose up -d` or `./run.sh`.
2. Observe anomaly detection in real time `docker compose logs -f consumer-service` or `./consumerMonitoring.sh`
3. Stop the execution `docker compose down -v` (`-v` also removes LocalStack's anonymous volume)

Sample output:

```
[2026-09-23T01:57:42.885139522Z] Data point: 49.04 | Status: OK | Z-score: 0.13
[2026-09-23T01:58:03.665768932Z] Data point: 97.32 | Status: ANOMALY DETECTED! | Z-score: 11.16 | ALERT: Significant deviation detected.
```

## How to test

Run `./gradlew test`. Docker must be running, as integration tests start LocalStack using Testcontainers.

## Configuration

The configuration lives in `application.yaml` files packaged into the images; changes are picked up by the next
`docker compose up`, which rebuilds them. Alternatively, any property can be overridden without rebuild using environment
variables in [docker-compose.yaml](docker-compose.yaml) (Spring relaxed binding), e.g.
`APP_ANOMALYDETECTION_ZSCORETHRESHOLD=4` for the consumer or `APP_GENERATOR_ANOMALYPROBABILITY=0.05` for the producer.

### Generation tuning

Real time generator can be tuned using `app.generator` properties
in [application.yaml](producer/src/main/resources/application.yaml) \
`app.generator.mean` - Mean of the normal distribution \
`app.generator.standard-deviation` - Standard deviation of the normal distribution \
`app.generator.anomaly-probability` - Probability of generating an anomaly instead \
`app.generator.anomaly-min-sigma` - Minimal distance of an anomaly from the mean, in σ \
`app.generator.anomaly-max-sigma` - Maximal distance of an anomaly from the mean, in σ \

### Using mock generator for demonstration

To make demo more deterministic, a MockDataGenerator can be used to read pregenerated values from txt files in a loop.
The mock generator can be enabled using `app.features.mock-data-generator.enabled` property in
[application.yaml](producer/src/main/resources/application.yaml). By default, it loops over 200 values with a single
anomaly among them. To switch to anomaly free data, the `app.features.mock-data-generator.add-anomaly-value` should be
set to `false`. The mock data follows a standard normal distribution (mean 0, σ 1), unlike the real time
generator.

## Anomaly detection

For anomaly detection, calculating and checking Z-score is used in the consumer. First, the rolling window must be
filled with enough data defined by `app.anomaly-detection.minimum-window-elements` property to perform calculations
(warm-up phase, logged with `WARMING-UP` status). Anomalies are kept out of rolling window to not 'poison' the data
unless the `app.anomaly-detection.maximum-consecutive-anomalies-allowed` limit is exceeded.

The Z-score calculation can be tuned in the [application.yaml](consumer/src/main/resources/application.yaml) using
`app.anomaly-detection` properties: \
`app.anomaly-detection.z-score-threshold` - Value is an anomaly if its Z-score is strictly greater \
`app.anomaly-detection.minimum-window-elements` - Values needed in the window before detection starts \
`app.anomaly-detection.maximum-window-elements` - Rolling window size \
`app.anomaly-detection.maximum-consecutive-anomalies-allowed` - Consecutive anomalies kept out of the window before the
next one is added \

Implementation details:

- Standard deviation is the sample standard deviation (n-1).
- The threshold is exclusive, so a Z-score of exactly `3.00` is reported as `OK`.
- If all values in the window are equal (σ = 0), any different value gets an infinite Z-score and is reported as an
  anomaly.
- The logged timestamp is the time of processing in the consumer, not the time of generation, since the message
  carries only the value.

## Up next

### Tool recommendations

It really depends on what tools are available and what are agreements in the team or organization, but generally:

1. Use checkstyle for keeping the codebase according to standards.
2. Add CI/CD configurations
3. Add semantic release for keeping the git commit history clean and maintainable changelog
4. Use Terraform or similar tool for AWS infrastructure management
5. Add different configs per environments.
6. Add gitleaks for checking if no secrets were committed to the git
7. Add end-to-end tests of the whole stack and load tests
8. Use SonarQube for static code analysis and test coverage
9. Add exception handling (like DLQ, handling of bad/empty messages)
10. Improve SQS message payload format (use JSON with value, event timestamp and id instead of plain numbers)

### Orchestration

As the containerization is already available, it depends on the target platform and if there is already a pipeline for
bringing brand-new services into k8s or similar platforms. From k8s perspective, the following would need to be
provided:

1. Deployment manifests or a Helm chart per service.
2. Liveness and readiness probes, e.g. using Spring Boot Actuator health endpoints.
3. Configuration via ConfigMaps or environment variables, and secret management (using AWS secret manager or Vault for
   example).
4. Workload identity instead of static AWS credentials (EKS Pod Identity), and a real SQS queue with all required AWS
   infra around it.
5. The consumer must run as a single replica, as the rolling window is kept in memory.

It would also require established CI/CD pipelines, image registry and a ready k8s cluster. Optionally ArgoCD can be
used for automatic version management.

### Missing Technical requirements

1. Target platform for deployment. Is it on prem deployment or cloud native?
2. Preferable libs and tools that are commonly used in the team
3. Is it going to be a distributed system where multiple consumers can read from the same queue? SQS uses 'at least
   once' delivery protocol due to distributed architecture in AWS, so duplications can appear if messages are read by
   multiple instances. Also, standard SQS queues don't guarantee the order of the messages, so FIFO queue is used. To
   scale out, the rolling window would have to be kept in a shared store like Redis, or data partitioned by message
   group, with one consumer per group.
4. The reasoning of using Z-score is missing. What is the nature of the data, how is it distributed in time, what are
   our baselines?
5. No requirements on fall-back scenarios. If service crashed, the in-memory data is lost and the window has to warm
   up again.
6. No requirements on alerting. Logging to console is not real alerting; metrics (e.g. Micrometer with Prometheus and
   Grafana) and notifications would be needed.
