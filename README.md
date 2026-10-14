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
| Framework | Spring Boot 3.5 (Web, Validation, Security, Actuator) |
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
copy application-local.properties.example application-local.properties   # then set your own organiser password
mvn clean verify        # compile, test and build target/event-feedback-dashboard.war
mvn spring-boot:run     # run on http://localhost:8090 (loads application-local.properties)
```

`application-local.properties` is git-ignored, so the password never reaches GitHub. On a server or in
Jenkins set the environment variable `SPRING_SECURITY_USER_PASSWORD` instead. If neither is set,
Spring Boot prints a random password in the log at startup.

## Who uses what

| Role | Sign-in | Can do |
|------|---------|--------|
| **Attendee** | none | Browse events, filter by status, read ratings and every comment, give feedback for **ongoing** or **completed** events |
| **Organiser** | user `organiser` + configured password | Create events, change status (Upcoming → Ongoing → Completed / Cancelled), read feedback. Cannot submit feedback. |

| Page | Who | What |
|------|-----|------|
| http://localhost:8090/ | everyone | Events with **All / Upcoming / Ongoing / Completed** tabs, average rating and response count |
| `event.html?id=<id>` | everyone | Event details, rating summary (average, % recommend, 5★–1★ distribution) and all feedback |
| `feedback.html?eventId=<id>` | attendees | Feedback form (1–5 ratings, recommend, comment, optional name/e-mail) |
| `organiser.html` | organiser | Create events and manage their status (redirects to `login.html` when signed out) |
| `dashboard.html` | organiser | KPI cards (auto-refresh), events/feedback by status with drill-down, feedback search and follow-up workflow |
| `about.html` | everyone | Roles, system health/version and MVP modules |
| `/api/info`, `/actuator/health` | everyone | Application info and health check (JSON) |

The UI opens in a **light theme**; the header button switches to dark (remembered in the browser).

On first start an empty database is filled with 5 sample events and 9 feedback entries
(`app.seed-demo-data=true`). Data is stored in `~/.event-feedback-dashboard/efd-db`.

### REST API

| Method | Endpoint | Who | Description |
|--------|----------|-----|-------------|
| GET | `/api/events?q=&status=&category=&from=&to=` | everyone | List/search events (newest first) with `responseCount` and `averageRating` |
| GET | `/api/events/{id}` | everyone | One event with statistics, `404` if unknown |
| POST | `/api/events` | organiser | Create an event – `title`, `category`, `eventDate` required → `201 Created` |
| PATCH | `/api/events/{id}/status` | organiser | Change status, body `{"status":"ONGOING"}` |
| GET | `/api/events/{id}/feedback` | everyone | Feedback for an event, newest first (e-mail never returned) |
| GET | `/api/events/{id}/summary` | everyone | Rating distribution, averages and % recommend for one event |
| GET | `/api/feedback?q=&eventId=&status=&minRating=&maxRating=` | organiser | Search feedback across events |
| PATCH | `/api/feedback/{id}/status` | organiser | Follow-up: `NEW` → `REVIEWED` / `ACTION_REQUIRED` → `RESOLVED` (final) |
| GET | `/api/dashboard/summary` | organiser | Events, responses, average rating, satisfaction %, recommend % |
| GET | `/api/dashboard/status-breakdown` | organiser | Counts per event status and per feedback status |
| POST | `/api/events/{id}/feedback` | attendees | Submit feedback; `409` for upcoming/cancelled events, `403` for organisers |
| POST | `/api/auth/login` | – | Organiser sign-in (form fields `username`, `password`) → `204` / `401` |
| POST | `/api/auth/logout` | organiser | Sign out → `204` |
| GET | `/api/auth/me` | everyone | `{"organiser": true/false, "username": …}` |

Write requests need the CSRF token: the `XSRF-TOKEN` cookie value sent back as the `X-XSRF-TOKEN` header
(the UI does this automatically). Without sign-in, organiser endpoints return `401`.

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
│   │   │   ├── config/          # security, demo data
│   │   │   ├── event/ feedback/ # entities, services, REST controllers
│   │   │   └── web/             # info and auth endpoints
│   │   └── resources/
│   │       ├── application.properties
│   │       └── static/          # index, event, feedback, organiser, login, about pages + css/js
│   └── test/java/com/eventfeedback/   # JUnit + MockMvc tests
├── application-local.properties.example   # template for the git-ignored organiser password
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
