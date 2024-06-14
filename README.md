# NBA Player Statistics System

# Overview

This project is a scalable system designed to log and calculate NBA player statistics. The system can be deployed both
on-premises and on AWS (dockerfile example provided), and it provides the following capabilities:

- Log NBA player statistics.
- Calculate aggregate statistics per player and per team for a season.
- **Average Response Time < 50 ms with Throughput ~1k TPS** (3 small replicas on dev PC, will be even better on real 
  prod env)

# Features

- **Reactive Programming**: Leveraging Micronaut and R2DBC for non-blocking I/O operations.
- **Scalability**: Designed to handle large volumes of requests and data efficiently.
- **High Availability**: Multiple replicas and distributed caching with Hazelcast ensure minimal downtime.
- **Consistency**: Ensures data consistency across different components using Hazelcast map-store, which provides
  eventual consistency.
- **Maintainability**: Modular architecture with clear boundaries and manageable complexity.

## Technologies Used

Java, Micronaut, PostgreSQL, Hazelcast, Docker, NGINX, Gradle, Liquibase, JUnit, Reactive Programming


# Nonfunctional Requirements

- **Scalability**: The system must handle increasing amounts of load, including the ability to scale horizontally.
- **Availability**: The system should ensure high availability, minimizing downtime and ensuring continuous operation.
- **Performance**: The system must have high throughput for both read and write operations.
- **Maintainability**: The system should be easy to maintain and extend, with clear modular boundaries and manageable
  complexity.
- **Consistency**: The system should ensure data consistency across different components and services using Hazelcast
  map-store, which provides eventual consistency.

# PACELC Theorem Choices

- **Partition (P) occurs**:
    - **Availability (A) over Consistency (C)**: In case of network partition, the system favors availability using
      Hazelcast to ensure the application remains responsive, potentially sacrificing some consistency temporarily.
- **Else (E), no partition**:
    - **Consistency (C) over Latency (L)**: Under normal conditions, the system prioritizes consistency, ensuring that
      all nodes have the same view of the data, leveraging PostgreSQL's strong consistency guarantees.

# Technology/Architecture Decisions

| Comparison                                   | Pros                                                                                                 | Cons                                                                                                           | Decision                                                                                       |
|----------------------------------------------|------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------|
| **Java vs. Go**                              | **Java:** Rich ecosystem, extensive library support, JVM optimizations.                              | **Java:** Higher memory footprint, longer startup time, slower compared to some alternatives.                | **Java** was chosen for its robust ecosystem, mature tools, and strong community support.       |
|                                              | **Go:** Excellent concurrency support, lower memory usage, fast startup.                             | **Go:** Smaller ecosystem, less mature libraries, less suited for complex enterprise applications.            |                                                                                                |
| **Reactive Style vs. Synchronous, Asynchronous, and Actor Model** | **Reactive:** Non-blocking I/O, better resource utilization, ideal for many concurrent requests.     | **Reactive:** Steeper learning curve, complex error handling, requires reactive ecosystem.                   | **Reactive Style** was chosen for its non-blocking I/O, better resource utilization, and scalability. |
|                                              | **Synchronous:** Easier to understand, simpler error handling, straightforward coding.               | **Synchronous:** Blocking I/O, resource inefficiencies, not suitable for high concurrency.                   |                                                                                                |
|                                              | **Asynchronous:** Non-blocking, better resource utilization, easier transition from synchronous.     | **Asynchronous:** Callback hell, complex error handling, requires async libraries.                           |                                                                                                |
|                                              | **Actor Model:** Natural concurrency, simplifies state management, scalable.                        | **Actor Model:** New paradigm, limited support, message-passing overhead.                                    |                                                                                                |
| **Eventual Consistency vs. Strong Consistency vs. Causal Consistency** | **Eventual:** High availability, suitable for distributed systems, better performance.               | **Eventual:** Temporary data staleness, complex to ensure data correctness, increased application logic complexity. | **Eventual Consistency** was chosen for high availability and better performance, despite complexity. |
|                                              | **Strong:** All nodes see the same data, simplifies application logic, immediate feedback on writes. | **Strong:** Reduced availability, higher latency, complex and costly to maintain.                            |                                                                                                |
|                                              | **Causal:** Causally related operations seen in order, balances between strong and eventual consistency, good for collaborative applications. | **Causal:** More complex than eventual consistency, higher latencies, less intuitive for some use cases.      |                                                                                                |
| **Micronaut vs. Spring vs. Quarkus**         | **Micronaut:** Fast startup, low memory footprint, native reactive programming support.             | **Micronaut:** Smaller community, fewer out-of-the-box features compared to Spring.                          | **Micronaut** was chosen for its lightweight nature, fast startup times, and low memory consumption. |
|                                              | **Spring Boot:** Extensive feature set, large community, rich ecosystem.                            | **Spring Boot:** Higher memory consumption, slower startup time.                                             |                                                                                                |
|                                              | **Quarkus:** Optimized for containers, fast startup, low memory footprint.                          | **Quarkus:** Less mature, smaller ecosystem, fewer features compared to Spring.                              |                                                                                                |
| **PostgreSQL vs. MySQL**                     | **PostgreSQL:** Advanced features, strong consistency, robust for complex queries.                  | **PostgreSQL:** Higher setup complexity, resource-intensive.                                                 | **PostgreSQL** is chosen over MySQL due to its advanced features, extensibility, standards compliance, and strong community support. |
|                                              | **MySQL:** High performance for read-heavy workloads, large community.                              | **MySQL:** Less suited for complex queries, fewer advanced features.                                         |                                                                                                |
| **Hazelcast vs. Redis vs. Ehcache**          | **Hazelcast:** Distributed caching, in-memory data grid, ease of integration.                       | **Hazelcast:** Higher resource usage, more complex configuration.                                            | **Hazelcast** is chosen over Redis and Ehcache due to its good consistency guarantees, robust distributed caching capabilities, and seamless integration with Java applications. |
|                                              | **Redis:** High performance, rich data structures, simplicity.                                      | **Redis:** Limited clustering in open-source version, data persistence can be complex.                       |                                                                                                |
|                                              | **Ehcache:** Strong integration with Java, local caching, simple setup.                             | **Ehcache:** Not suited for distributed scenarios, limited scalability.                                      |                                                                                                |
| **ZGC vs. G1 GC vs. Shenandoah GC vs. CMS GC** | **ZGC:** Low-latency with pause times under 10ms, efficient for large heaps, predictable performance. | **ZGC:** Relatively new, fewer optimizations, higher memory overhead.                                         | **ZGC** was chosen for its low-latency garbage collection and efficiency with large heaps, aligning with the project's needs for high throughput and active memory management. |
|                                              | **G1 GC:** Good performance, low pause times, tunable for optimization.                             | **G1 GC:** Higher pause times than ZGC, complex tuning, higher fragmentation over time.                      |                                                                                                |
|                                              | **Shenandoah GC:** Low pause times, concurrent compacting, suitable for large heaps.                | **Shenandoah GC:** Newer and less mature, limited documentation, potentially higher CPU overhead.            |                                                                                                |
|                                              | **CMS GC:** Long history, lower pause times, good for smaller heaps.                                | **CMS GC:** Deprecated post JDK 9, fragmentation issues, less efficient for large heaps.                     |                                                                                                |


# API Documentation

Below is a brief description of the API endpoints:

- **POST /save**: Save player game data.
    - **Request Body**:
      ```json
      {
        "playerName": "LeBron James",
        "teamName": "Team1",
        "seasonName": "2023-2024",
        "gameId": "2983ec0f-00de-4f64-bdd0-f0b224576da0",
        "points": 25,
        "rebounds": 10,
        "assists": 5,
        "steals": 3,
        "blocks": 2,
        "fouls": 1,
        "turnovers": 4,
        "minutesPlayed": 30.5
      }
      ```
    - **Response**: 
      - `200 OK` on success (no body)
      - `400 Bad Request` on validation error
        ```json
        { "error": "Fouls must be between 0 and 6" }
        ```

- **GET /stats/players**: Get player statistics for a given season (JSON Lines).
    - **Query Parameters**: `season`
    - **Response**: Stream of player statistics in JSON format.
```http request
GET /stats/players?season=2023-2024 HTTP/1.1
 
HTTP/1.1 200 OK
Server: nginx
Content-Type: application/x-json-stream
Transfer-Encoding: chunked
 
{"playerName":"LeBron James","avgPoints":7.0,"avgRebounds":20.0,"avgAssists":9.0,"avgSteals":5.0,"avgBlocks":9.0,"avgFouls":2.0,"avgTurnovers":4.0,"avgMinutesPlayed":0.0}
{"playerName":"Giannis Antetokounmpo","avgPoints":22.0,"avgRebounds":17.0,"avgAssists":1.0,"avgSteals":3.0,"avgBlocks":5.0,"avgFouls":1.0,"avgTurnovers":3.0,"avgMinutesPlayed":0.0}
{"playerName":"Stephen Curry","avgPoints":49.0,"avgRebounds":12.0,"avgAssists":4.0,"avgSteals":0.0,"avgBlocks":6.0,"avgFouls":5.0,"avgTurnovers":2.0,"avgMinutesPlayed":0.0}
{"playerName":"Nikola Jokić","avgPoints":10.0,"avgRebounds":15.0,"avgAssists":7.0,"avgSteals":5.0,"avgBlocks":6.0,"avgFouls":2.0,"avgTurnovers":0.0,"avgMinutesPlayed":0.0}
{"playerName":"Kevin Durant","avgPoints":29.0,"avgRebounds":3.0,"avgAssists":5.0,"avgSteals":3.0,"avgBlocks":9.0,"avgFouls":1.0,"avgTurnovers":8.0,"avgMinutesPlayed":0.0}
```

- **GET /stats/teams**: Get team statistics for a given season (JSON Lines).
    - **Query Parameters**: `season`
    - **Response**: Stream of team statistics in JSON format.
```http request
GET /stats/teams?season=2023-2024

HTTP/1.1 200 OK
Server: nginx
Content-Type: application/x-json-stream
Transfer-Encoding: chunked

{"teamName":"Los Angeles Lakers","avgPoints":43.0,"avgRebounds":7.0,"avgAssists":2.0,"avgSteals":7.0,"avgBlocks":0.0,"avgFouls":0.0,"avgTurnovers":2.0,"avgMinutesPlayed":0.0}
{"teamName":"Golden State Warriors","avgPoints":2.0,"avgRebounds":2.0,"avgAssists":13.0,"avgSteals":5.0,"avgBlocks":6.0,"avgFouls":5.0,"avgTurnovers":2.0,"avgMinutesPlayed":0.0}
{"teamName":"Milwaukee Bucks","avgPoints":31.0,"avgRebounds":5.0,"avgAssists":10.0,"avgSteals":10.0,"avgBlocks":2.0,"avgFouls":5.0,"avgTurnovers":5.0,"avgMinutesPlayed":0.0}
{"teamName":"Denver Nuggets","avgPoints":16.0,"avgRebounds":11.5,"avgAssists":6.5,"avgSteals":4.5,"avgBlocks":8.0,"avgFouls":1.5,"avgTurnovers":7.0,"avgMinutesPlayed":0.0}
{"teamName":"Brooklyn Nets","avgPoints":21.5,"avgRebounds":7.5,"avgAssists":6.5,"avgSteals":6.0,"avgBlocks":6.5,"avgFouls":2.5,"avgTurnovers":6.5,"avgMinutesPlayed":0.0}
```

---

# Performance testing

### GET Requests:
- **Average Response Time**: 4.02 ms
- **Success Rate**: 100%
- **Throughput**: 1100.31 requests/second
- **50th Percentile (Median) Response Time**: 4.0 ms
- **99th Percentile Response Time**: 9.0 ms

### POST Requests:
- **Average Response Time**: 26.42 ms
- **Success Rate**: 100%
- **Throughput**: 986.75 requests/second
- **50th Percentile (Median) Response Time**: 21.0 ms
- **99th Percentile Response Time**: 58.01 ms

### Combined GET and POST Requests:
- **Average Response Time**: 42.31 ms
- **Success Rate**: 100%
- **Throughput**: 668.28 requests/second
- **50th Percentile (Median) Response Time**: 43.0 ms
- **99th Percentile Response Time**: 108.0 ms

### Setup Information:

Used setup from Docker Compose with 3 instances of `nba-svc`:
- **Resources per service**: 2 CPU cores (AMD Ryzen 7 5800X), 700MB RAM
- **Load profiles**: Only GET, Only POST, GET/POST (20/80)

Docker Compose configuration:
```yaml
name: nba-compose
services:
  nba-svc:
    build: .
    ports: [8080, 5005]
    deploy: { replicas: 3, resources: { limits: { memory: 700M, cpus: '2.0' } }, restart_policy: { condition: on-failure, max_attempts: 1 } }
  nginx:
    image: nginx:stable-alpine
    ports: ["8080:80"]
  postgres:
    image: postgres:latest
    environment: { POSTGRES_USER: postgres, POSTGRES_PASSWORD: postgres }
    ports: ["5432:5432"]
    deploy: { resources: { limits: { cpus: '2.0', memory: 500M } } }
  hazelcast-management-center:
    image: hazelcast/management-center:5.4.1
    ports: ["8081:8080"]
networks:
  nba-network: { driver: bridge }
```

#### Only GET requests

<details open>

![](images/get_load_profile/ResponseTimesOverTimeGet.png)
![](images/get_load_profile/TransactionsPerSecondGet.png)

</details>

#### Only POST requests

<details open>

![](images/post_load_profile/ResponseTimesOverTimePOST.png)
![](images/post_load_profile/TransactionsPerSecondPOST.png)

</details>

#### GET/POST with 20/80 proportion

<details open>

![](images/get_post_load_profile/ResponseTimesOverTimeGetPost.png)
![](images/get_post_load_profile/TransactionsPerSecondGetPost.png)

</details>


# AWS Cost Estimations

To map the calculations to AWS services and estimate costs, we'll consider the following AWS services:

- **Amazon EC2**: For compute (CPU and memory).
- **Amazon RDS**: For database storage.
- **Amazon S3**: For object storage.

We'll use the following assumptions:

- **EC2 Instance Type**: m5.large (2 vCPUs, 8 GB RAM) for simplicity.
- **Amazon RDS**: Using RDS for PostgreSQL with db.m5.large instance.
- **Amazon S3**: For object storage costs.

### EC2 Cost Estimation
- m5.large: $0.096 per hour, $69.12 per month, $829.44 per year.

### RDS Cost Estimation
- db.m5.large: $0.096 per hour, $69.12 per month, $829.44 per year.
- Storage: $0.115 per GB-month for General Purpose SSD (gp2).

### S3 Cost Estimation
- S3 Standard Storage: $0.023 per GB.

### Revised Table with AWS Cost Estimations

| Load (RPS) | Period  | Replicas | Memory (GB) | CPU (Cores) | Storage (TB) | EC2 Cost (USD/year) | RDS Cost (USD/year) | S3 Cost (USD/year) | Total Cost (USD/year)     |
|------------|---------|----------|-------------|-------------|--------------|----------------------|----------------------|---------------------|--------------------------|
| 100        | 1 year  | 1        | 0.8         | 2           | 3.15         | 829.44               | 829.44               | 72.45               | 1731.33                  |
| 100        | 3 years | 1        | 0.8         | 2           | 9.45         | 2488.32              | 2488.32              | 217.35              | 5193.99                  |
| 100        | 5 years | 1        | 0.8         | 2           | 15.75        | 4147.20              | 4147.20              | 362.25              | 8656.65                  |
| 1,000      | 1 year  | 1-2      | 0.8-1.6     | 2-4         | 31.5         | 829.44-1658.88       | 829.44-1658.88       | 724.50              | 2383.38-4042.26          |
| 1,000      | 3 years | 1-2      | 0.8-1.6     | 2-4         | 94.5         | 2488.32-4976.64      | 2488.32-4976.64      | 2173.50             | 7150.14-12440.78         |
| 1,000      | 5 years | 1-2      | 0.8-1.6     | 2-4         | 157.5        | 4147.20-8294.40      | 4147.20-8294.40      | 3622.50             | 11916.90-20031.30        |
| 100,000    | 1 year  | 100-200  | 80-160      | 200-400     | 3150         | 82944-165888         | 82944-165888         | 72450               | 238338-404226            |
| 100,000    | 3 years | 100-200  | 80-160      | 200-400     | 9450         | 248832-497664        | 248832-497664        | 217350              | 715014-1244078           |
| 100,000    | 5 years | 100-200  | 80-160      | 200-400     | 15750        | 414720-829440        | 414720-829440        | 362250              | 1191690-2003130          |

### Summary
- **100 RPS:**
    - 1 year: $1,731.33.
    - 3 years: $5,193.99.
    - 5 years: $8,656.65.
- **1,000 RPS:**
    - 1 year: $2,383.38 - $4,042.26.
    - 3 years: $7,150.14 - $12,440.78.
    - 5 years: $11,916.90 - $20,031.30.
- **100,000 RPS:**
    - 1 year: $238,338 - $404,226.
    - 3 years: $715,014 - $1,244,078.
    - 5 years: $1,191,690 - $2,003,130.

These cost estimates provide a high-level view of the resources and associated costs required to handle different load profiles over 1, 3, and 5 years on AWS. Adjustments may be necessary based on real-world testing, specific application requirements, and optimizations.




# Getting Started

### Prerequisites

- Docker
- Docker Compose
- Java 22+
- Gradle 8.8+

### Installation

1. **Clone the repository**:
   ```sh
   git clone https://github.com/Vlad-Lapysh/nba-stats.git
   cd nba-stats
   ```
2. **Build the project:**
    ```shell
    ./gradlew clean build
    ``` 
3. **Run the application using Docker Compose:**
    ```shell
    docker-compose up --build
    ```

# Improvements
- Add queue for buffering incoming /save requests when svc can't process more
- Add Micrometer with Prometheus
- Implement rate limiting to prevent abuse.
- Use HTTPS for external communications.
- Implement JWT-based authentication for securing APIs.
- Use environment-specific configurations to manage different deployment stages.
- Expand automated testing coverage.
- Implement more detailed performance monitoring and alerting.
- Use API versioning to ensure backward compatibility.
- etc