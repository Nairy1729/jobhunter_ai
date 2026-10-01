# JobHunter AI 🎯
> **Autonomous, Intelligent Career Copilot & Real-Time Job Application Optimization Platform**

JobHunter AI is an autonomous, high-conviction job discovery, semantic matching, and application preparation platform built for software engineers. Unlike high-volume generic scrapers, JobHunter AI emphasizes deep semantic understanding, verifiable grounding against candidate experience, and automated application collateral generation without hallucination.

---

## ⚡ Core Highlights

- 🔎 **Autonomous Multi-Source Discovery**: Integrates Firecrawl to search, crawl, and scrape ATS career portals (Greenhouse, Lever, Ashby, Workday, etc.) with SHA-256 deduplication.
- 🧠 **Deep Semantic Understanding**: Extracts hard requirements, nice-to-haves, seniority, domain, work mode, and implicit requirements with token-boundary regex and LLM reasoning.
- 🛡️ **Zero-Tolerance Hallucination Gate**: Programmatic Java validation cross-references all claims against actual candidate experience, skills, and projects before any resume modification.
- 📊 **Application Advantage System ("The Edge")**: Computes multi-dimensional fit scoring, honest gap identification, talking points, and tailored application strategies.
- 📝 **OfferPilot Tailoring Engine**: Produces role-specific, grounded resume revisions with visual side-by-side diff tracking.
- 🔒 **Mandatory Human-in-the-Loop Gate**: Pure transparency and control—no automated blind submissions.

---

## 🏗️ Architecture

```
                       ┌─────────────────────────┐
                       │   React 18 + Vite UI    │
                       │ (TypeScript + Tailwind) │
                       └────────────┬────────────┘
                                    │ REST / JWT
                                    ▼
                       ┌─────────────────────────┐
                       │   Spring Boot 3 (Java)  │
                       │  - Semantic Matcher     │
                       │  - Grounding Gate       │
                       │  - Discovery Engine     │
                       └────────────┬────────────┘
                     ┌──────────────┴──────────────┐
                     ▼                             ▼
       ┌───────────────────────────┐ ┌───────────────────────────┐
       │   PostgreSQL 16 + JSONB   │ │   AI / Web Intelligence   │
       │ (Flyway Migrations + GIN) │ │  (Gemini API + Firecrawl) │
       └───────────────────────────┘ └───────────────────────────┘
```

- **Backend**: Java 17 LTS, Spring Boot 3.3.4, Spring Security 6 (Stateless JWT), Spring Data JPA, Hibernate, Flyway, Resilience4j, Apache PDFBox.
- **Frontend**: React 18.3, TypeScript, Vite, Tailwind CSS, Lucide Icons, Axios.
- **Data Store**: PostgreSQL 16 with native JSONB vector/embedding structures and GIN indexing.
- **AI & Web Intelligence**: Google Gemini API, Firecrawl Web Crawl/Scrape API.

---

## 🚀 Getting Started

### Prerequisites

- **Java 17+**
- **Node.js 18+** & `npm`
- **PostgreSQL 16+**
- **Gemini API Key** & **Firecrawl API Key** (optional for live scraping)

### 1. Database Setup

Ensure PostgreSQL is running and create the database:

```sql
CREATE DATABASE jobhunter_db;
CREATE USER jobhunter WITH PASSWORD 'jobhunter';
GRANT ALL PRIVILEGES ON DATABASE jobhunter_db TO jobhunter;
```

### 2. Environment Configuration

Copy the example environment file and set your keys:

```bash
cp .env.example .env
```

Set your API keys inside `.env`:
```env
GEMINI_API_KEY=your_gemini_api_key_here
FIRECRAWL_API_KEY=your_firecrawl_api_key_here
```

### 3. Backend Setup

```bash
cd backend
./mvnw clean spring-boot:run
```
The backend starts at `http://localhost:8085`.

Default seeded test candidate credentials:
- **Email**: `candidate@jobhunter.ai`
- **Password**: `password123`

### 4. Frontend Setup

```bash
cd frontend
npm install
npm run dev
```
The frontend starts at `http://localhost:5173`.

---

## 📂 Documentation

Comprehensive engineering specs and milestone reports are available in the [`docs/`](docs/) directory:

- [Product Specification](docs/PRODUCT_SPEC.md)
- [System Architecture](docs/ARCHITECTURE.md)
- [Database Design & Schema](docs/DATABASE_DESIGN.md)
- [Agent & Multi-Agent Architecture](docs/AGENT_ARCHITECTURE.md)
- [Security Model & Threat Mitigation](docs/SECURITY_MODEL.md)
- [Firecrawl Integration Blueprint](docs/FIRECRAWL_INTEGRATION.md)
- [OfferPilot Tailoring Engine](docs/OFFERPILOT_TAILORING_BEHAVIOR.md)
- [Milestone Implementation Reports](docs/)

---

## 📄 License

MIT License. See [LICENSE](LICENSE) for details.
