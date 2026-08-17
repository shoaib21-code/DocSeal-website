# DocSeal — Master’s Major Project (Document Signing & Verification)

DocSeal is a web application I built as part of my **Master’s major project** to demonstrate a practical, end-to-end **document signing and verification** workflow using **public-key cryptography**—presented through a clean, easy-to-follow user experience.

**Author:** Shoaibuddin Ahmed Mohammed

---

## Why DocSeal?

Many document signing solutions feel either:
- too complex for everyday users, or
- too “black box” to understand how trust and verification actually work.

DocSeal focuses on a simple workflow while still implementing real cryptographic concepts like key generation, certificate-based identity, signing, and verification.

---

## Key Features

- **Key Pair Generation (server-side)**
  - Generate RSA key pairs (e.g., 3072/4096) securely on the server.

- **Certificate Workflow**
  - CSR → certificate → chain-ready artifacts for establishing signer identity.

- **Document Signing**
  - Sign documents and generate a **portable signature bundle** that can travel with the document.

- **Verification**
  - Verify a document’s **integrity** (unchanged content) and **origin** (who signed it) using the signer’s certificate details.

---

## How It Works (High-Level Flow)

1. **Generate Keys**
   - The system creates a key pair and stores it securely on the server.

2. **Create / Issue Certificate**
   - A CSR is generated and used to obtain/issue a certificate and chain-ready output.

3. **Sign Document**
   - The document is signed using the private key.
   - A signature bundle is produced (contains signature + related verification material).

4. **Verify Document**
   - Verification checks:
     - the document hash / integrity
     - the signature validity using the public key
     - certificate details (and chain rules where applicable)

---

## Tech Stack

- **Backend:** Spring Boot 3 (Java), Spring Data JPA  
- **Database:** PostgreSQL  
- **Frontend:** React + Vite  
- **Styling:** Tailwind CSS  

---

## Security Notes (Important)

- **Private keys are stored only on the server** and are **never returned to the client**.
- **Do not commit** secrets or sensitive material to GitHub (examples: `.env`, private keys, keystores, certificates meant to be private).
- For public repositories, always use environment variables and ignore sensitive files via `.gitignore`.

---

## Run Locally

> This project contains a backend (Spring Boot), a frontend (React/Vite), and PostgreSQL.  
> Update the environment variables based on your machine.

### Prerequisites
- Java (compatible with your Spring Boot setup)
- Node.js + npm
- PostgreSQL

### Environment Setup
Create environment variable files (recommended):
- Backend: `.env` (or application properties via environment variables)
- Frontend: `.env`

Example variables (adjust to your setup):

**Backend**
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- (Optional) storage path for keys/certs/signatures

**Frontend**
- `VITE_API_BASE_URL`

### Start Backend
- Open the backend project directory
- Run Spring Boot (via Maven/Gradle or your IDE)

### Start Frontend
- Open the frontend project directory
- Install and run:
  ```bash
  npm install
  npm run dev
