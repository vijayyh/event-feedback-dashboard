# Automated Event Feedback Dashboard

A web application where organisers register events, attendees submit feedback in under a
minute, and a dashboard instantly shows searchable records, summary indicators, status
drill-down and an automatically maintained alert/exception view.

Built as a **DevOps course project**: planned with Scrum, versioned with Git/GitHub, built
with Maven, tested with JUnit, integrated with Jenkins and deployed to Apache Tomcat 11.

## MVP modules

| Module | Description | Sprint |
|--------|-------------|:------:|
| Data / event entry | Register events, submit feedback with validation | 1 |
| Searchable dashboard | Keyword search and filters on events and feedback | 2 |
| Summary indicators | Total events, responses, average rating, satisfaction %, open alerts | 2 |
| Status drill-down | Status counts → event list → event detail | 3 |
| Alert / exception view | Automatic alerts for low ratings and negative comments | 3–4 |

## Tech stack

| Layer | Technology |
|-------|------------|
| Language / runtime | Java 21 |
| Framework | Spring Boot 3.5 (Web, Validation, Actuator) |
| Build | Apache Maven 3.9 → `event-feedback-dashboard.war` |
| Database | H2 (file mode) via Spring Data JPA |
| UI | HTML, CSS, vanilla JavaScript (served from the WAR) |
| Tests | JUnit 5, Spring MockMvc |
| CI/CD | Jenkins (port 8080) |
| Deployment target | Apache Tomcat 11 (port 8081) |

## Quick start

Prerequisites: JDK 21 and Maven 3.9 on the `PATH`.

```bash
git clone https://github.com/vijayyh/event-feedback-dashboard.git
cd event-feedback-dashboard
mvn clean verify        # compile, test and build target/event-feedback-dashboard.war
mvn spring-boot:run     # run on http://localhost:8090
```

| URL | What |
|-----|------|
| http://localhost:8090/ | Web UI – overview |
| http://localhost:8090/events.html | Register events, see responses and average rating |
| http://localhost:8090/feedback.html | Attendee feedback form (`?eventId=<id>` pre-selects an event) |
| http://localhost:8090/api/info | Application info (JSON) |
| http://localhost:8090/actuator/health | Health check |

On first start an empty database is filled with 5 sample events and 9 feedback entries
(`app.seed-demo-data=true`). Data is stored in `~/.event-feedback-dashboard/efd-db`.

### REST API (Feature 1)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/events` | List events (newest first) with `responseCount` and `averageRating` |
| POST | `/api/events` | Create an event – `title`, `category`, `eventDate` required → `201 Created` |
| GET | `/api/events/{id}` | One event with statistics, `404` if unknown |
| POST | `/api/events/{id}/feedback` | Submit feedback – three ratings 1–5 and `wouldRecommend` required; `409` for cancelled events |
| GET | `/api/events/{id}/feedback` | Feedback for an event, newest first (e-mail never returned) |

Validation errors return `400` with `fieldErrors`, e.g.
`{"status":400,"error":"Validation failed","fieldErrors":{"title":"must not be blank"}}`.

### Deploy to Tomcat 11

```bash
copy target\event-feedback-dashboard.war "C:\Program Files\Apache Software Foundation\Tomcat 11.0\webapps\"
```

Open http://localhost:8081/event-feedback-dashboard/

## Project structure

```text
event-feedback-dashboard/
├── .github/
│   ├── ISSUE_TEMPLATE/          # user story, bug report, DevOps task templates
│   └── pull_request_template.md # PR checklist = Definition of Done
├── docs/                        # project documentation (Tasks 1-5)
├── src/
│   ├── main/
│   │   ├── java/com/eventfeedback/
│   │   │   ├── EventFeedbackDashboardApplication.java
│   │   │   └── web/             # REST controllers
│   │   └── resources/
│   │       ├── application.properties
│   │       └── static/          # HTML, CSS, JS
│   └── test/java/com/eventfeedback/   # JUnit + MockMvc tests
├── CONTRIBUTING.md              # branch naming rules, commit and PR policy
└── pom.xml                      # Maven build (WAR packaging)
```

## Documentation

| Task | Document |
|------|----------|
| 1 | [Problem definition and scope](docs/01-problem-definition-and-scope.md) |
| 2 | [Agile planning and DevOps workflow](docs/02-agile-planning-and-devops-workflow.md) |
| 3 | [Requirements, architecture and technology setup](docs/03-requirements-and-architecture.md) |
| 4 | [Git and GitHub repository initialization](docs/04-git-and-github-setup.md) |
| 5 | [Feature development with branching](docs/05-feature-branching.md) |

## Branching and contributions

`main` holds released code, `develop` is the integration branch, and all work happens on
`feature/<issue>-<short-name>` branches merged through pull requests.
See [CONTRIBUTING.md](CONTRIBUTING.md) for the full branch policy and commit conventions.
