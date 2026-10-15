# Changelog

All notable changes to the Event Feedback Dashboard. Versions follow [Semantic Versioning](https://semver.org/).

## [0.9.0] – 2026-10-14 · MVP feature-complete baseline

All five MVP modules are implemented and tested. Release-ready source baseline for the CI/CD work
(Jenkins pipeline, Tomcat deployment, Selenium tests) that follows.

### Added
- **Data / event entry** – organisers create events and move them Upcoming → Ongoing → Completed / Cancelled;
  attendees submit feedback (three 1–5 ratings, recommend, comment, optional name/e-mail) for ongoing or completed
  events (#2, #3, #20).
- **Searchable dashboard** – keyword, status, category and date search for events; keyword, event, status and rating
  search for feedback; organiser dashboard page (#4, #6).
- **Summary indicators** – events, responses, average rating, satisfaction %, recommend %, open alerts with
  30-second auto-refresh (#5, #6).
- **Status drill-down** – counts per event and feedback status → event list → event page with rating distribution
  and averages (#7); feedback follow-up workflow New → Reviewed / Action required → Resolved (#8).
- **Alert / exception view** – automatic `LOW_RATING`, `NEGATIVE_COMMENT` and `LOW_AVERAGE` alerts with configurable
  thresholds and keywords; alert page with acknowledge / resolve and a navigation badge (#9, #10).
- Organiser sign-in (Spring Security) with the password kept out of Git; anonymous attendees (#20).
- Light theme by default with a dark-mode toggle (#20).
- 45 automated tests (JUnit 5 + MockMvc).

### Fixed
- Events table cramped on desktop and pages wider than a phone screen (#17).
- Event page failed with `renderEventHeader is not defined` after the dashboard merge (#24).

## [0.1.0] – 2026-10-14 · Skeleton
- Spring Boot WAR skeleton with `/api/info` and `/actuator/health`, project documentation for Tasks 1–3 (#1).
