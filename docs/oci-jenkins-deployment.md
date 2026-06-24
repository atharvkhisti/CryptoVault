# Guide: Deploying CryptoVault on Oracle Cloud (OCI) with Jenkins

This guide details how to set up a 100% free cloud instance on Oracle Cloud Infrastructure (OCI) and host a serverless Jenkins environment to build, test, and deploy the CryptoVault microservices stack using Docker Compose.

Using OCI Always Free + Jenkins is the perfect setup to gain hands-on experience with industrial CI/CD systems at zero cost.

---

## Architecture Overview

```mermaid
flowchart TD
    GitHub[GitHub Repository] -->|Webhook Trigger| Jenkins[Jenkins on OCI VM]
    
    subgraph OCI Always Free VM (4 CPUs / 24 GB RAM / Ubuntu)
        Jenkins -->|1. Pull Code| Code[Source Code Workspace]
        Jenkins -->|2. Run Maven Test & Build| Code
        Jenkins -->|3. Build Docker Images| DockerEngine[Docker Daemon Host]
        DockerEngine -->|4. Restart Containers| Containers[(Running Microservices Stack)]
        Database[(PostgreSQL Container)] <--> Containers
    end
    
    Client[Browser / User] -->|HTTP Port 80| Containers
```

---

## Step 1: Provision the OCI Always Free ARM Instance

Oracle Cloud Infrastructure (OCI) offers a generous free tier that includes up to 4 CPUs and 24 GB RAM.

1. **Sign Up**: Register at [oracle.com/cloud/free](https://www.oracle.com/cloud/free/).
2. **Create VM Instance**:
   - Go to **Compute -> Instances -> Create Instance**.
   - **Placement**: Keep default Availability Domain.
   - **Image and Shape**:
     - Click **Edit**.
     - Under **Image**, select **Ubuntu** (latest LTS version).
     - Under **Shape**, click **Change Shape**. Select **Ampere (ARM)** -> **VM.Standard.A1.Flex**.
     - Set OCPUs to **4** and Memory to **24 GB** (this fits the entire CryptoVault stack comfortably).
   - **Networking**:
     - Create a new Virtual Cloud Network (VCN) and subnet.
     - Select **Assign a public IPv4 address**.
   - **SSH Keys**:
     - Select **Generate a key pair for me** and download both the **Private Key** and **Public Key**. Keep these safe!
   - **Boot Volume**: Keep default settings (up to 200 GB is free).
   - Click **Create**.

---

## Step 2: Open OCI Network Firewalls (Ingress Rules)

By default, OCI blocks all incoming traffic. You must explicitly allow the ports for your frontend and dashboards:

1. In the OCI Console, navigate to your instance details and click on your **Virtual Cloud Network (VCN)**.
2. Click on **Security Lists** -> **Default Security List**.
3. Click **Add Ingress Rules** and add rules for the following ports:
   * **Port `80`** (Source: `0.0.0.0/0`, TCP) - React Web Frontend.
   * **Port `8080`** (Source: `0.0.0.0/0`, TCP) - API Gateway entrypoint.
   * **Port `8088`** (Source: `0.0.0.0/0`, TCP) - Jenkins Dashboard.
   * **Port `9000`** (Source: `0.0.0.0/0`, TCP) - SonarQube Dashboard.

---

## Step 3: Setup VM and Install Docker & Docker Compose

Once the instance shows a status of **Running**, SSH into it using your private key:

```bash
# On your local machine (replace VM_PUBLIC_IP with your actual OCI VM IP)
ssh -i /path/to/your/ssh-key.key ubuntu@VM_PUBLIC_IP
```

Inside the VM terminal, run the following commands to install Docker and Docker Compose:

```bash
# Update OS packages
sudo apt-get update && sudo apt-get upgrade -y

# Install Docker
sudo apt-get install -y docker.io docker-compose

# Start and enable Docker daemon
sudo systemctl start docker
sudo systemctl enable docker

# Allow non-root execution of Docker commands
sudo usermod -aG docker ubuntu

# Log out and log back in to apply group changes
exit
```

Log back in:
```bash
ssh -i /path/to/your/ssh-key.key ubuntu@VM_PUBLIC_IP
```

---

## Step 4: Run the CryptoVault Stack (Postgres, Jenkins, SonarQube)

Your project contains a pre-configured `docker-compose.yml` file that orchestrates the entire system.

1. **Clone the repository**:
   ```bash
   git clone <your-github-repo-url> CryptoVault
   cd CryptoVault
   ```
2. **Create the environment file**:
   Configure a `.env` file in the root directory:
   ```bash
   nano .env
   ```
   Add your variables (e.g. database credentials, JWT secret keys, and local service hostnames):
   ```env
   POSTGRES_DB=cryptovault
   POSTGRES_USER=postgres
   POSTGRES_PASSWORD=your_db_password
   JWT_SECRET_KEY=Y29uZmlndXJhYmxlc2VjcmV0a2V5Zm9yY3J5cHRvdmF1bHRhdXRoc2VydmljZWJhc2U2NA==
   ```
3. **Start the environment**:
   ```bash
   docker-compose up -d
   ```
   This will spin up:
   * `postgres-db` (on port `5433` for database persistence)
   * `jenkins` (on port `8088`, mounted with `/var/run/docker.sock` to trigger container builds)
   * `sonarqube` & `sonarqube-db` (on port `9000` for code quality metrics)
   * 8 backend microservices & the React `web-frontend`

---

## Step 5: Configure Jenkins and the CI/CD Pipeline

With Jenkins running, you can access the dashboard at `http://VM_PUBLIC_IP:8088`.

### 1. Unlock Jenkins
* Retrieve the initial administrator password:
  ```bash
  docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
  ```
* Copy the password, paste it into the Jenkins portal, and choose **Install Suggested Plugins**.
* Create your Admin User.

### 2. Configure GitHub Webhooks for Automated Builds
* In GitHub, go to your Repository -> **Settings** -> **Webhooks**.
* Click **Add Webhook**.
* Set **Payload URL** to: `http://VM_PUBLIC_IP:8088/github-webhook/`
* Set **Content type** to: `application/json`
* Under "Which events would you like to trigger this webhook?", select **Just the push event**.
* Click **Add webhook**.

### 3. Create the Jenkins Pipeline Job
* In the Jenkins Dashboard, click **New Item**.
* Name it `CryptoVault-Pipeline` and select **Pipeline**. Click **OK**.
* Under **Build Triggers**, check **GitHub hook trigger for GITScm polling**.
* Under **Pipeline**, set:
  - **Definition**: `Pipeline script from SCM`
  - **SCM**: `Git`
  - **Repository URL**: `<your-github-repo-url>`
  - **Credentials**: (Add your GitHub Username and Password/Token if private, or leave blank if public).
  - **Branch Specifier**: `*/main` or `*/master`
  - **Script Path**: `Jenkinsfile`
* Click **Save**.

---

## Understanding Your Jenkinsfile Pipeline

The pre-existing `Jenkinsfile` executes 10 crucial steps to build, analyze, and deploy your code:

1. **Checkout**: Clones the latest commit from your GitHub repository onto the workspace inside the Jenkins container.
2. **Build Common Library**: Compiles and installs the shared JAR dependency `common-lib` into the local Maven repository (`mvn clean install -DskipTests`). Downstream microservices require this library to compile.
3. **Build Backend Services**: Compiles all 8 Spring Boot projects (`mvn clean package -DskipTests`) in parallel or sequentially.
4. **Run Unit Tests**: Triggers unit and integration tests inside the services to ensure new code doesn't break functionality.
5. **Generate JaCoCo Reports**: The build generates XML and HTML test coverage files using the configured `jacoco-maven-plugin`.
6. **SonarQube Analysis**: Runs code scan analysis via `mvn sonar:sonar` and pushes reports to your SonarQube dashboard at `http://VM_PUBLIC_IP:9000` to analyze code smells and bugs.
7. **Build Frontend**: Setups Node.js, installs dependencies, and runs `npm run build` to package the React frontend.
8. **Build & Tag Docker Images**: Invokes Docker from inside Jenkins (via the mounted host socket `/var/run/docker.sock`) to build new Docker images from the updated code:
   `docker build -t cryptovault-auth-service:latest -f backend/auth-service/Dockerfile backend`
9. **Deploy / Restart Stack**: Stops the old containers and spins up the newly built images dynamically on the VM:
   `docker-compose up -d --no-deps <service-name>`
10. **Publish Reports**: Archives test reports and makes build outcomes available in the Jenkins console log.
