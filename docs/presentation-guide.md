# Presentation Guide – how to run and demo the project

**Project:** Automated Event Feedback Dashboard. Use this page as a checklist before and during the presentation.

---

## 1. What is running where

| What | URL / location | Starts how |
|------|----------------|------------|
| GitHub repository | https://github.com/vijayyh/event-feedback-dashboard | – |
| Jenkins | http://localhost:8080 (sign in with your Jenkins account in Chrome) | Windows service **Jenkins**, starts automatically with Windows |
| CI job (Task 7) | http://localhost:8080/job/EFD-CI-Build/ | Polls GitHub `develop` every 2 min |
| CD pipeline (Tasks 8 + 10) | http://localhost:8080/job/EFD-Pipeline/ | Polls GitHub `develop` every 2 min, or **Build with Parameters** |
| Deployed app – staging | http://localhost:8081/event-feedback-dashboard-staging/ | Tomcat 11 instance started by the pipeline |
| Deployed app – production | http://localhost:8081/event-feedback-dashboard/ | Same Tomcat, pipeline run with `DEPLOY_ENV = production` |
| Developer copy | http://localhost:8090/ | `mvn spring-boot:run` in the project folder |

Ports: Jenkins **8080** · Tomcat **8081** · developer server **8090**.

Organiser sign-in: user **`organiser`**. The password is the one in your git-ignored
`application-local.properties` (developer copy), or the Jenkins secret `efd-organiser-password` (deployed copies).
Attendees do not sign in.

## 2. Ten minutes before you present

1. Open **http://localhost:8080** in Chrome and check you are signed in.
2. **After a PC restart the Tomcat instance is not running**, because the pipeline started it. Open
   *EFD-Pipeline → Build with Parameters → staging → Build*. Within about 3 minutes it tests, deploys and starts
   Tomcat again. Do this before the audience arrives, or live as part of the demo (section 4, step 4).
3. Open the staging URL and check the page loads, with `staging` shown in the footer.
4. Open these tabs in order: GitHub repo · Jenkins dashboard · staging app · the `docs/` folder on GitHub.
5. Optional: open a terminal in the project folder for the local Selenium demo:

   ```bash
   cd C:\Users\vijay\OneDrive\Desktop\DevOps-Project\event-feedback-dashboard
   ```

## 3. Running it yourself (developer view)

```bash
git pull                                   # latest develop
mvn clean verify                           # compile + 51 unit/API tests + 9 Selenium journeys + WAR + coverage gate
mvn spring-boot:run                        # run on http://localhost:8090 (Ctrl+C to stop)
```

Show the Selenium tests driving a **visible** Chrome window, the most impressive part to watch:

```bash
mvn verify -DskipUTs=true -Dselenium.headless=false
```

Only one journey (faster, about 40 s):

```bash
mvn verify -DskipUTs=true -Dselenium.headless=false -Dit.test=SubmitFeedbackUiIT
```

Reports after a run: `target/reports/failsafe.html`, `target/site/jacoco/index.html`, and
`target/selenium-screenshots/` (only when a test failed).

## 4. Demo script (about 12–15 minutes)

| # | Time | Show | Say | Task |
|---|------|------|-----|------|
| 1 | 2 min | Staging app as an **attendee**: status tabs *All / Upcoming / Ongoing / Completed*, open an event (ratings, % recommend, comments), give feedback on an ongoing event | "Attendees rate events without an account; everyone can read the feedback." | 1, 3 |
| 2 | 2 min | **Sign in as organiser** (*Organiser* menu): create an event and set it to *Ongoing*. Then **Dashboard** (KPI cards, drill-down) and **Alerts** (a 1-star review raises a `LOW_RATING` alert; acknowledge / resolve) | "Organisers manage events and get alerted to problems automatically." | 1, 6 |
| 3 | 2 min | GitHub: **Issues** (backlog T01–T15, bugs), **Pull requests** (#22/#23 with the merge conflict, bug fixes #25, #33), **network graph**, tag **v0.9.0** / Releases | "Work is planned as issues, built on feature branches, reviewed in pull requests, and released with tags (GitFlow)." | 2, 4, 5, 6 |
| 4 | 3 min | Jenkins **EFD-Pipeline → Build with Parameters** (show `DEPLOY_ENV`), start it, open **Pipeline Overview**. While it runs, show the **Jenkinsfile** on GitHub | "The whole delivery process is code: build, test, package, deploy, smoke test. The environment is a parameter, so the same WAR goes to staging or production." | 8 |
| 5 | 1 min | **EFD-CI-Build**: build history (*Started by an SCM change*), *Changes*, *Last Successful Artifacts* (WAR), test trend | "Every push to develop is built automatically; Jenkins polls GitHub because it runs on localhost." | 7 |
| 6 | 2 min | Pipeline result: **Tests** tab (60 tests: 51 unit/API + 9 Selenium), **Artifacts** (WAR, coverage), the deployed URL. Footer shows `staging`; `/api/info` shows the environment | "Only a fully green build is deployed, and the smoke test checks the deployed app really works." | 8, 10 |
| 7 | 2 min | **EFD-Pipeline #7** (red): *UI tests* failed, Package / Deploy / Smoke test **skipped**. Open *Tests* (1 failure: "0% would recommend it") and *Artifacts* (failure screenshot). Then GitHub PR #36 (deliberate defect) → issue #37 → PR #38 (fix) → **EFD-Pipeline #8** (green, deployed) | "A deliberately introduced defect was caught by the Selenium journey, so the release was stopped and users never saw it. After the fix, the pipeline deployed again by itself." | 10 |
| 8 | 1 min | Optional: the local visible Selenium run (section 3) | "This is what Jenkins does headless on every build." | 9 |

Tip: start step 4 first and talk through steps 5 and 3 while it runs, since the pipeline takes about 3 minutes.

## 5. Questions you may get – short answers

| Question | Answer |
|----------|--------|
| Why polling instead of a webhook? | Jenkins runs on `localhost`, so GitHub cannot reach it. Polling (`H/2 * * * *`) asks GitHub for new commits instead. With a public Jenkins URL a webhook would be used. |
| What stops a broken build reaching users? | The *Deploy* stage only runs when `currentBuild` is still SUCCESS. Any failing unit, API or Selenium test, or the coverage gate (70 %), stops the pipeline before deployment. The smoke test then checks the deployed app. |
| How are secrets handled? | The organiser password is a Jenkins *Secret text* credential, bound only in the deploy stage and masked in logs. Locally it lives in a git-ignored file. It is never in Git. |
| How does `DEPLOY_ENV` change the app without rebuilding? | The pipeline writes a Tomcat context file with `<Parameter>` entries (environment name, data folder, password). Spring Boot reads them like `application.properties`. |
| Why its own Tomcat instance? | The installed Tomcat 11 service is set to Java 8, but Tomcat 11 needs Java 17+. Changing the Windows service is an admin change, so the pipeline runs Tomcat 11 with its own configuration folder on JDK 21. |
| What did the tests actually catch? | Real defects: the event page crash (#24), the alert badge showing "0", garbled characters on Tomcat (#32, found by deploying, now guarded by the smoke test), and the deliberate Task 10 defect (#37). |
| Was anything else found by running the pipeline? | Yes. Each deployment restarted the app twice, so the URL gave 404 for about 15 s after the smoke test (#34). Fixed by only rewriting the Tomcat context file when a setting changes. |
| What was the hardest problem? | Pipeline runs that were "green" in 4 seconds without running any stage: two Jenkins plugin files were corrupt. Lesson: check what ran, not only the colour (docs/08 §7.1). |

## 6. If something goes wrong during the demo

| Symptom | Fix |
|---------|-----|
| Staging URL does not load | Tomcat is not running (PC restarted). Run *EFD-Pipeline → Build with Parameters → staging*. |
| Pipeline finishes in seconds with no stages | Check *Manage Jenkins → Plugins* for errors (docs/08 §7.1). |
| Jenkins asks to sign in | Use your Jenkins account in Chrome. |
| `mvn` not found in the terminal | Open a new terminal, or use the full path to Maven. JDK 21 must be `JAVA_HOME`. |
| Port 8090 already in use | Another `spring-boot:run` is still open. Stop it with Ctrl+C in that terminal. |

## 7. Where the written evidence is

| Task | Document |
|------|----------|
| 1 Problem and scope | [01-problem-definition-and-scope.md](01-problem-definition-and-scope.md) |
| 2 Agile plan and DevOps workflow | [02-agile-planning-and-devops-workflow.md](02-agile-planning-and-devops-workflow.md) |
| 3 Requirements and architecture | [03-requirements-and-architecture.md](03-requirements-and-architecture.md) |
| 4 Git and GitHub | [04-git-and-github-setup.md](04-git-and-github-setup.md) |
| 5 Feature branching | [05-feature-branching.md](05-feature-branching.md) |
| 6 MVP and collaboration | [06-mvp-completion-and-collaboration.md](06-mvp-completion-and-collaboration.md) |
| 7 Jenkins CI job | [07-jenkins-ci-job.md](07-jenkins-ci-job.md) |
| 8 Pipeline and Tomcat deployment | [08-pipeline-and-tomcat-deployment.md](08-pipeline-and-tomcat-deployment.md) |
| 9 Selenium test plan | [09-selenium-test-plan.md](09-selenium-test-plan.md) |
| 10 Continuous testing in Jenkins | [10-continuous-testing-in-jenkins.md](10-continuous-testing-in-jenkins.md) |
