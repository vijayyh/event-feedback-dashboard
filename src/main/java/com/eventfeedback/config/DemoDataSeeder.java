package com.eventfeedback.config;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.eventfeedback.event.Event;
import com.eventfeedback.event.EventCategory;
import com.eventfeedback.event.EventRepository;
import com.eventfeedback.event.EventStatus;
import com.eventfeedback.feedback.Feedback;
import com.eventfeedback.feedback.FeedbackRepository;

/**
 * Fills an empty database with a few realistic events and feedback so the UI is not blank
 * on first start. Disabled with {@code app.seed-demo-data=false} (tests do this).
 */
@Component
@Order(1)
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final EventRepository events;
    private final FeedbackRepository feedback;

    public DemoDataSeeder(EventRepository events, FeedbackRepository feedback) {
        this.events = events;
        this.feedback = feedback;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (events.count() > 0) {
            return;
        }
        LocalDate today = LocalDate.now();

        Event workshop = event("Hands-on Workshop: Jenkins CI/CD Pipelines", EventCategory.WORKSHOP,
                today.minusDays(10), EventStatus.COMPLETED, "Seminar Hall A", "CSE Department", 120,
                "Build, test and deploy a Java web app with Maven, Jenkins and Tomcat.");
        Event lecture = event("Guest Lecture: Careers in Cloud Computing", EventCategory.SEMINAR,
                today.minusDays(5), EventStatus.COMPLETED, "Main Auditorium", "Training & Placement Cell", 300,
                "Industry talk on cloud roles, certifications and interview preparation.");
        Event hackathon = event("24-Hour Hackathon: Smart Campus", EventCategory.HACKATHON,
                today, EventStatus.ONGOING, "Innovation Lab", "IEEE Student Branch", 80,
                "Teams build solutions for campus problems: parking, energy, attendance.");
        event("Annual Cultural Fest - Rhythm 2026", EventCategory.CULTURAL,
                today.plusDays(12), EventStatus.UPCOMING, "Open Air Theatre", "Student Council", 1500,
                "Music, dance and drama competitions across departments.");
        event("Webinar: Introduction to Kubernetes", EventCategory.WEBINAR,
                today.minusDays(2), EventStatus.CANCELLED, "Online (Google Meet)", "CSE Department", 200,
                "Postponed – speaker unavailable.");

        feedback(workshop, 5, 5, 4, true, "Very practical session, the live Jenkins demo was the best part.", "Ananya");
        feedback(workshop, 4, 4, 4, true, "Good pace. Would like a follow-up on Docker.", null);
        feedback(workshop, 5, 5, 5, true, null, "Rahul");
        feedback(workshop, 3, 4, 2, true, "Content was great but the lab PCs were slow to set up.", null);
        feedback(workshop, 2, 3, 1, false, "Sound was not audible in the back rows.", null);
        feedback(lecture, 4, 5, 4, true, "Very useful career guidance.", "Priya");
        feedback(lecture, 5, 5, 4, true, null, null);
        feedback(lecture, 3, 3, 3, false, "Too long, could have been shorter with more Q&A.", null);
        feedback(hackathon, 4, 4, 3, true, "Great problem statements. Wi-Fi keeps dropping.", "Team Byte");

        log.info("Seeded demo data: {} events, {} feedback entries", events.count(), feedback.count());
    }

    private Event event(String title, EventCategory category, LocalDate date, EventStatus status,
                        String venue, String organizer, int expected, String description) {
        Event e = new Event(title, category, date);
        e.setStatus(status);
        e.setVenue(venue);
        e.setOrganizer(organizer);
        e.setExpectedAttendees(expected);
        e.setDescription(description);
        return events.save(e);
    }

    private void feedback(Event event, int overall, int content, int organization, boolean recommend,
                          String comments, String name) {
        Feedback f = new Feedback(event, overall, content, organization, recommend);
        f.setComments(comments);
        f.setAttendeeName(name);
        feedback.save(f);
    }
}
