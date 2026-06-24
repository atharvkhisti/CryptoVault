# CryptoVault Free Tier Deployment Guide

This document outlines how to deploy the entire CryptoVault application stack (React frontend, 8 Spring Boot microservices, PostgreSQL, Jenkins, and SonarQube) completely within free tier limits, avoiding any cloud hosting costs.

---

## The Challenge of Multi-Service Stacks on Free Cloud Tiers

Standard cloud free tiers (such as Render or Railway) are designed for single monolithic applications and have strict limitations:
* **Render Free Tier**:
  - Limits Web Services to **512 MB RAM** each.
  - Automatically puts services to sleep after 15 minutes of inactivity (causing long cold-start delays).
  - Free PostgreSQL databases are deleted after **90 days**.
  - Limits the number of concurrent active free services per account.
* **Railway Free Tier**:
  - Provides a trial limit of 500 hours or $5 credit per month, which runs out in less than 2 days when running 9+ containers 24/7.

> [!WARNING]
> Running 8 separate Spring Boot microservices + a database on Render's free tier simultaneously is **not feasible** due to service count limits and memory constraints. 
> Below are the two most practical and sustainable ways to host this architecture for **100% free**.

---

## Option 1: Oracle Cloud Infrastructure (OCI) Always Free ARM (Recommended)

Oracle Cloud offers an exceptionally generous "Always Free" tier that can host your entire production-ready Docker Compose environment forever.

### What OCI Always Free Provides:
* **ARM Ampere A1 Compute Instances**: Up to **4 CPUs** and **24 GB of RAM** (allocatable as a single VM or split across up to 4 VMs).
* **Block Storage**: **200 GB** of free NVMe storage.
* **Network Egress**: 10 TB of free outbound bandwidth per month.

### Deployment Steps on OCI:

1. **Sign Up for Oracle Cloud**:
   - Create an account at [oracle.com/cloud/free](https://www.oracle.com/cloud/free/).
   - Select a home region that supports Ampere shapes (e.g., `ap-south-1` or `us-east-1`).

2. **Provision the Compute Instance**:
   - Launch a Compute Instance.
   - Change the Shape to **Ampere (ARM)** and configure it with **4 OCPUs** and **24 GB RAM**.
   - Select **Ubuntu** or **Oracle Linux** as the operating system.
   - Assign a public IP address and download your SSH private key.

3. **Install Docker & Docker Compose**:
   SSH into your new instance and install the runtime:
   ```bash
   sudo apt-get update
   sudo apt-get install -y docker.io docker-compose
   sudo systemctl start docker
   sudo systemctl enable docker
   sudo usermod -aG docker $USER
   ```

4. **Open Security Lists (Ports)**:
   In the OCI Console, navigate to your Virtual Cloud Network (VCN) -> Security Lists, and add Ingress Rules for:
   - Port `80` (React Frontend)
   - Port `8080` (API Gateway / Swagger Documentation)
   - Port `8088` (Jenkins Dashboard)
   - Port `9000` (SonarQube)

5. **Clone and Run via Docker Compose**:
   Clone your repository onto the VM and spin up the complete local environment:
   ```bash
   git clone <your-github-repo-url> CryptoVault
   cd CryptoVault
   docker-compose up -d
   ```
   All 8 microservices, the database, Jenkins, and SonarQube will boot up successfully and remain active 24/7 for free!

---

## Option 2: Local Docker Compose + Public Tunneling (Zero Setup Cloud)

If you want to demo or test the application over the internet without setting up a new cloud provider, you can host the stack on your local machine and use a secure tunnel.

### Deployment Steps:

1. **Run the Stack locally**:
   Make sure Docker Desktop is running, then run:
   ```powershell
   docker compose up -d
   ```

2. **Expose the Endpoints using Ngrok or LocalTunnel**:
   - **LocalTunnel** (Completely free, no account required):
     ```bash
     npm install -g localtunnel
     
     # Tunnel frontend (Port 80)
     lt --port 80 --subdomain cryptovault-app
     
     # Tunnel API Gateway (Port 8080)
     lt --port 8080 --subdomain cryptovault-api
     ```
   - **Ngrok** (Free tier, requires account signup):
     ```bash
     ngrok http 80
     ngrok http 8080
     ```

---

## Option 3: Hybrid Deployment (Vercel + Supabase + Render Core)

If you prefer using dedicated hosting platforms for different tiers:

1. **Frontend: Deploy to Vercel (100% Free & Active 24/7)**
   - Vercel is free for frontend static sites. It automatically connects to your GitHub repository and redeploys on every commit.
   - Connect your GitHub repo, select the `frontend/web-app` subdirectory, set the build command to `npm run build` and output directory to `dist`.
   - Add environment variable: `VITE_API_BASE_URL` pointing to your backend gateway URL.

2. **Database: Deploy to Supabase (100% Free Postgres)**
   - Supabase provides a free, cloud-hosted PostgreSQL database that doesn't expire.
   - Create a project at [supabase.com](https://supabase.com) and retrieve the PostgreSQL Connection String.
   - Point your backend services to this DB endpoint instead of a local container.

3. **Backend: Deploy Core Services to Render**
   - Since Render has limits, deploy only the critical services required for demoing (e.g., `api-gateway` and `auth-service`).
   - Create a Web Service on Render from your GitHub repository for each service.
   - Inject the Supabase connection parameters as environment variables.

---

## Jenkins vs. GitHub Actions (Choosing Your CI/CD Tool)

You can use **either** Jenkins or GitHub Actions to automate your pipeline. You do **not** need Jenkins if you choose to use GitHub Actions.

| Feature | Jenkins | GitHub Actions |
| --- | --- | --- |
| **Hosting Cost** | Requires running a server 24/7 (Local PC or VM), which can incur maintenance or hosting costs. | 100% Free on GitHub's cloud runners (2,000 mins/month for private repos, unlimited for public). |
| **Setup & Maintenance** | Requires manual server setup, OS security patches, Docker socket mounting, and plugin updates. | Serverless and zero-maintenance. Managed entirely by GitHub. |
| **Integration** | Requires setting up webhooks and exposing ports to receive GitHub trigger events. | Built natively into the GitHub ecosystem. Triggers automatically on pushes/PRs. |
| **Configuration** | Configured via `Jenkinsfile` in Groovy syntax. | Configured via `.github/workflows/ci-cd.yml` in standard YAML. |

> [!NOTE]
> For a 100% free and serverless pipeline, we have configured a GitHub Actions workflow in your repository at [.github/workflows/ci-cd.yml](file:///d:/CryptoVault/.github/workflows/ci-cd.yml).

---

## Setting Up GitHub Actions CI/CD

The configured workflow [.github/workflows/ci-cd.yml](file:///d:/CryptoVault/.github/workflows/ci-cd.yml) automates the entire compile, test, and deployment process:

1. **Backend Build & Verification**:
   - Spins up a clean Ubuntu container.
   - Installs JDK 17.
   - Compiles and runs unit tests for all 8 microservices and the `common-lib`.
2. **Frontend Build**:
   - Sets up Node.js 20.
   - Installs packages and runs `npm run build` using the environment secrets.
3. **Frontend Free Hosting (GitHub Pages)**:
   - Deploying static pages is completely free on GitHub.
   - The workflow automatically deploys the compiled React application directly to **GitHub Pages** (using the `peaceiris/actions-gh-pages` action).
   - In your GitHub Repository, go to **Settings -> Pages** and make sure the source is set to deploy from the `gh-pages` branch.

### How to Configure Secrets:
In your GitHub Repository, go to **Settings -> Secrets and variables -> Actions**, and add the following repository secret:
* `VITE_API_BASE_URL`: The URL of your API Gateway (e.g. your Render Gateway endpoint, LocalTunnel URL, or OCI VM public IP).

---

## Integrating Jenkins (Alternative Option)

If you still want to use Jenkins (e.g., to run it locally on your PC or OCI instance):

1. **Expose Jenkins to GitHub Webhooks**:
   If your Jenkins server is running locally or behind firewalls, ensure port `8088` is accessible to GitHub IPs.
2. **Configure GitHub Webhook**:
   In GitHub, go to Repository Settings -> Webhooks, and add:
   `http://<your-server-ip>:8088/github-webhook/`
3. **Use the Jenkinsfile**:
   The existing [Jenkinsfile](file:///d:/CryptoVault/Jenkinsfile) inside the root directory is pre-configured to execute Maven builds, unit testing, SonarQube quality gate analysis, and local Docker image builds.

