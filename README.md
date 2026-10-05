# JobHunter AI 🎯
> **Autonomous Career Intelligence & Dual-Engine Public/Private Job Discovery Platform**

JobHunter AI is an autonomous, high-conviction job discovery, verification, eligibility matching, and application platform. Built with a strict two-pillar architecture, it seamlessly separates commercial private employment matching from authenticated Indian public sector recruitment.

---

## ⚡ Core Pillars

### 1. 💼 Private Sector Jobs Engine
- **Profile-First Discovery**: Matches candidate master facts, skills, and commercial experiences against real market job descriptions.
- **Strict Grounding & Zero-Hallucination Gate**: Audits every bullet point against candidate ground-truth facts. Never fabricates claims or companies.
- **OfferPilot LaTeX Tailoring**: Re-aligns existing factual achievements to match job requirements and generates crisp, ATS-compliant single-page PDFs.
- **Visual Diff Inspection**: Side-by-side diff tracking highlighting exactly what was emphasized before downloading.

### 2. 🏛️ Government Recruitment Engine (India-Wide)
- **Multi-Tier Official Discovery**: Discovers recruitment notifications across Central Ministries, State PSCs, District Administrations (NIC portals), Municipal Corporations, Panchayati Raj, Health Missions (NHM), Women & Child Development (Anganwadi), Samvida, and Contractual postings.
- **Strictly NO Resume Tailoring**: Government recruitment strictly forbids modified resumes. Instead, JobHunter connects candidates to original notification PDFs and official application portals.
- **Deterministic Eligibility Evaluation**: Matches candidate state, district, age (with category relaxations), minimum educational qualifications (8th, 10th, 12th, ITI, Diploma, Graduation, Post-Graduation), and specialized certificates.
- **Authenticity & Source Transparency**: Clear verification badges indicating official government domains (`.gov.in`, `.nic.in`) and live source registry telemetry.

### 3. 📋 Applications Tracker & Profile Hub
- **Applications Tracking**: Minimal, authentic tracker of submitted applications with direct links and status toggles.
- **Unified Profile Hub**: Seamless two-tab management for both Private Career Facts & Master Resume upload and Government Eligibility Parameters.

---

## 🧭 Information Architecture

```text
JobHunter
│
├── Jobs                 (Private sector discovery, ATS matching & grounded tailoring)
├── Government Jobs      (Verified public recruitment feed, eligibility & official links)
├── Applications         (Minimal tracking of applied opportunities)
└── Profile              (Career facts, Master Resume PDF & Government eligibility)
```

---

## 🏗️ Technical Architecture

```
                       ┌─────────────────────────┐
                       │   React 18 + Vite UI    │
                       │ (TypeScript + Tailwind) │
                       └────────────┬────────────┘
                                    │ REST / JWT (Bearer)
                                    ▼
                       ┌─────────────────────────┐
                       │   Spring Boot 3 (Java)  │
                       │  - Private Matcher      │
                       │  - Gov Eligibility     │
                       │  - Source Registry      │
                       │  - Resume Auditor       │
                       └────────────┬────────────┘
                     ┌──────────────┴──────────────┐
                     ▼                             ▼
       ┌───────────────────────────┐ ┌───────────────────────────┐
       │   PostgreSQL 16 + JSONB   │ │   AI / Web Intelligence   │
       │ (Flyway V1-V8 Migrations) │ │  (Gemini API + Firecrawl) │
       └───────────────────────────┘ └───────────────────────────┘
```

- **Backend**: Java 17 LTS, Spring Boot 3.3.4, Spring Security 6 (Stateless JWT), Spring Data JPA, Hibernate, Flyway, Resilience4j, Apache PDFBox.
- **Frontend**: React 18.3, TypeScript, Vite, Tailwind CSS, Lucide Icons, Axios.
- **Database**: PostgreSQL 16 with native JSONB, GIN indexing, and Flyway schema versioning.
- **Security**: IDOR ownership checks, configurable CORS origins, HTTP security headers (`DENY` frames, `nosniff`), sanitized production error messages.

---

## 🚀 Local Development Setup

### Prerequisites
- **Java 17+** (JDK)
- **Node.js 18+** & `npm`
- **PostgreSQL 16+**
- **Gemini API Key** & **Firecrawl API Key** (optional for live deep crawling)

### 1. Database Setup
```sql
CREATE DATABASE jobhunter_db;
CREATE USER postgres WITH PASSWORD 'postgres';
GRANT ALL PRIVILEGES ON DATABASE jobhunter_db TO postgres;
```

### 2. Environment Configuration
Copy `.env.example` to `.env`:
```bash
cp .env.example .env
```
Fill in your credentials:
```env
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/jobhunter_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
APP_JWT_SECRET=your_secure_256_bit_secret_key_here
GEMINI_API_KEY=your_gemini_api_key
FIRECRAWL_API_KEY=your_firecrawl_api_key
```

### 3. Start Backend
```bash
cd backend
./mvnw.cmd spring-boot:run
```
Backend starts at `http://localhost:8085`.

### 4. Start Frontend
```bash
cd frontend
npm install
npm run dev
```
Frontend starts at `http://localhost:5173`.

Default seeded demo credentials:
- **Email**: `candidate@jobhunter.ai`
- **Password**: `password123`

---

## 📦 Production Build & Testing

### Run Backend Unit & Integration Tests (123 Tests)
```bash
cd backend
./mvnw.cmd test
```

### Build Executable Spring Boot JAR
```bash
cd backend
./mvnw.cmd clean package -DskipTests
```
Generates executable archive at `backend/target/jobhunter-ai-backend-0.0.1-SNAPSHOT.jar`. Run with:
```bash
java -jar backend/target/jobhunter-ai-backend-0.0.1-SNAPSHOT.jar
```

### Build Production Frontend Bundle
```bash
cd frontend
npm run build
```
Generates production static bundle in `frontend/dist/`.

---

## 📄 License
MIT License. See [LICENSE](LICENSE) for details.
