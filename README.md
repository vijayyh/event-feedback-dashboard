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
| Tests | JUnit 5, Spring MockMvc, Selenium WebDriver (Chrome), JaCoCo |
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
| `alerts.html` | organiser | Alert/exception view: open, acknowledged and resolved alerts with severity filter |
| `about.html` | everyone | Roles, system health/version and MVP modules |
| `/api/info`, `/actuator/health` | everyone | Application info and health check (JSON) |

Alerts are raised automatically for every new feedback: rating ≤ 2 (`LOW_RATING`, high), a negative keyword in the
comment (`NEGATIVE_COMMENT`, medium) and an event average below 3.0 after 5+ responses (`LOW_AVERAGE`, critical).
Thresholds and keywords are set with `app.alerts.*` in `application.properties`.

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
| GET | `/api/alerts?status=&severity=` | organiser | Alerts, newest first (`LOW_RATING`, `NEGATIVE_COMMENT`, `LOW_AVERAGE`) |
| GET | `/api/alerts/count` | organiser | `{"unresolved": n}` for the navigation badge |
| PATCH | `/api/alerts/{id}` | organiser | `{"status":"ACKNOWLEDGED"}` or `RESOLVED` (never reopened) |
| POST | `/api/events/{id}/feedback` | attendees | Submit feedback; `409` for upcoming/cancelled events, `403` for organisers |
| POST | `/api/auth/login` | – | Organiser sign-in (form fields `username`, `password`) → `204` / `401` |
| POST | `/api/auth/logout` | organiser | Sign out → `204` |
| GET | `/api/auth/me` | everyone | `{"organiser": true/false, "username": …}` |

Write requests need the CSRF token: the `XSRF-TOKEN` cookie value sent back as the `X-XSRF-TOKEN` header
(the UI does this automatically). Without sign-in, organiser endpoints return `401`.

Validation errors return `400` with `fieldErrors`, e.g.
`{"status":400,"error":"Validation failed","fieldErrors":{"title":"must not be blank"}}`.

### Tests

| Command | Runs |
|---------|------|
| `mvn test` | 51 unit and API tests (JUnit 5, MockMvc) + JaCoCo report `target/site/jacoco/index.html` |
| `mvn verify` | the above + 9 **Selenium** browser journeys in headless Chrome (`*UiIT.java`) + coverage check (service layer ≥ 70 %) |
| `mvn verify -DskipUTs=true` | only the Selenium journeys |
| `mvn verify -Dselenium.headless=false` | watch the journeys in a visible Chrome window |

Failed UI tests leave a screenshot and the page HTML in `target/selenium-screenshots/`.
Test plan: [docs/09-selenium-test-plan.md](docs/09-selenium-test-plan.md).

### Run with Docker

```bash
mvn -B clean verify
docker build -t event-feedback-dashboard:1.0.0-SNAPSHOT .
docker run -d --name efd -p 8088:8080 -e SPRING_SECURITY_USER_PASSWORD=<organiser password> -v efd-data:/data event-feedback-dashboard:1.0.0-SNAPSHOT
```

Open http://localhost:8088/. The image packages the tested WAR on a Java 21 runtime, runs as a non-root user and
keeps the database in the `/data` volume. Details: [docs/11](docs/11-docker-image-and-container-lifecycle.md).

### Configure a server with Ansible

The [`ansible/`](ansible) folder configures an Ubuntu 24.04 server for the application:
- Java 21;
- the `efd` service user, folders and settings;
- the `efd` systemd service on port 8095;
- release deployment with a health check and automatic rollback.

From Windows (Ansible runs in Ubuntu on WSL):

```powershell
.\ansible\run.ps1 provision.yml -Options '-e','fresh=true'   # create a clean node
.\ansible\run.ps1 site.yml                                    # configure it and deploy the release
.\ansible\run.ps1 health.yml                                  # health check
.\ansible\run.ps1 rollback.yml                                # back to the previous release
```

Details: [docs/13](docs/13-configuration-management-ansible.md) and [docs/14](docs/14-provisioning-and-reliability.md).

### Deploy to Tomcat 11

Deployment is done by the Jenkins pipeline ([Jenkinsfile](Jenkinsfile)), not by hand. Every commit on `develop` is
built, tested and, only when all tests pass, deployed to a Tomcat 11 instance on JDK 21 (port 8081) and smoke-tested.
To deploy on demand, open *EFD-Pipeline → Build with Parameters* and choose `DEPLOY_ENV`:

| DEPLOY_ENV | URL |
|------------|-----|
| `staging` (default) | http://localhost:8081/event-feedback-dashboard-staging/ |
| `production` | http://localhost:8081/event-feedback-dashboard/ |

Details: [docs/08](docs/08-pipeline-and-tomcat-deployment.md).

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
| 6 | [MVP completion and Git collaboration](docs/06-mvp-completion-and-collaboration.md) · [CHANGELOG](CHANGELOG.md) |
| 7 | [Continuous integration with Jenkins](docs/07-jenkins-ci-job.md) |
| 8 | [Pipeline as code and deployment to Tomcat](docs/08-pipeline-and-tomcat-deployment.md) |
| 9 | [Selenium test design and local execution](docs/09-selenium-test-plan.md) |
| 10 | [Continuous testing in Jenkins](docs/10-continuous-testing-in-jenkins.md) |
| 11 | [Docker image and container lifecycle](docs/11-docker-image-and-container-lifecycle.md) |
| 12 | [Jenkins–Docker continuous deployment](docs/12-jenkins-docker-continuous-deployment.md) |
| 13 | [Configuration management with Ansible](docs/13-configuration-management-ansible.md) |
| 14 | [Automated provisioning and reliability validation](docs/14-provisioning-and-reliability.md) |
| – | [Presentation guide – how to run and demo](docs/presentation-guide.md) |

## Branching and contributions

`main` holds released code, `develop` is the integration branch, and all work happens on
`feature/<issue>-<short-name>` branches merged through pull requests.
See [CONTRIBUTING.md](CONTRIBUTING.md) for the full branch policy and commit conventions.
