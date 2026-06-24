# CryptoVault — Secure Multi-Chain Vault Portfolio

CryptoVault is a production-grade, secure, multi-chain cryptocurrency vault and portfolio management application built using a modern Java microservices backend and a React TypeScript static frontend.

---

## 🏗️ System Architecture

CryptoVault consists of 8 downstream Spring Boot microservices protected by a centralized API Gateway perimeter:

* **`api-gateway` (Port `8080`)**: Entrypoint for all external API requests. Performs JWT verification, rate limiting, and injects secure user metadata headers (`X-USER-ID`, etc.) down to target services.
* **`auth-service` (Port `8083`)**: Handles secure user registration, password hashing, and stateless JWT token issuance.
* **`wallet-service` (Port `8081`)**: Manages multi-chain addresses, balances, and deposits/withdrawals.
* **`transaction-service` (Port `8082`)**: Orchestrates fund transfers, tracks transaction ledgers, and queries the wallet service.
* **`notification-service` (Port `8084`)**: Listens to system events and alerts users of deposits and security changes.
* **`risk-service` (Port `8085`)**: Executes security heuristics to detect suspicious transaction sizes or patterns.
* **`audit-service` (Port `8086`)**: Records an immutable trail of user activities and API requests for compliance.
* **`kyc-service` (Port `8087`)**: Manages customer identity submissions and secure image uploads.
* **`web-frontend` (Port `80`)**: Responsive React SPA built on TypeScript, TailwindCSS, and Vite.

---

## 🛠️ DevOps & Pipeline Automation

The platform features two fully functional CI/CD pipeline automation tracks:

### 1. GitHub Actions (Serverless CI/CD)
* Located in [`.github/workflows/ci-cd.yml`](.github/workflows/ci-cd.yml).
* Automatically triggers on every push to `main` or `master`.
* Sets up JDK 17, builds the Maven common dependency library, runs unit tests, compiles the frontend React code, and deploys the static assets directly to **GitHub Pages** for $0 hosting.

### 2. Jenkins & SonarQube (Self-Hosted CI/CD)
* Managed via the root [`Jenkinsfile`](Jenkinsfile) and orchestrates:
  - Maven compilation & unit testing.
  - **JaCoCo** code coverage reporting.
  - **SonarQube** code quality gate scan (analysis of code smells, vulnerabilities, and coverage).
  - Automates Docker image compilation and local deployment updating via `/var/run/docker.sock`.

---

## 🚀 Quick Start Guide (Running Locally)

To spin up the entire application stack—including the database, microservices, Jenkins, and SonarQube—ensure you have **Docker Desktop** running, and follow these steps:

1. **Configure Environment**:
   Create a `.env` file in the root folder (using `.env` template values).
2. **Launch Container Orchestration**:
   ```bash
   docker compose up -d
   ```
3. **Access Dashboards**:
   - **React Application**: `http://localhost`
   - **API Gateway (Swagger Spec)**: `http://localhost:8080/swagger-ui.html`
   - **Jenkins Dashboard**: `http://localhost:8088` (Initial Admin Password located inside container at `/var/jenkins_home/secrets/initialAdminPassword`)
   - **SonarQube Console**: `http://localhost:9000` (Default: `admin` / `admin`)

---

## 📖 Operational Documentation

* [Free Tier Deployment Guide](docs/free-tier-deployment.md) - Details hosting via Oracle Cloud (OCI) Always Free or local tunneling.
* [Local Jenkins Webhook Guide](docs/oci-jenkins-deployment.md) - Explains how to set up local webhook tunnels for GitHub-to-Jenkins integration.
* [AWS Deployment Architecture](docs/aws-architecture.md) - Overview of the AWS compute (ECS Fargate), routing (ALB/CloudFront), and database (RDS PostgreSQL) configuration.
