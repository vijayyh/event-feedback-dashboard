package com.eventfeedback.ui;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

/**
 * Saves a PNG screenshot and the page HTML when a UI test fails.
 *
 * <p>Runs right after the test method and before {@code @AfterEach}, so the browser is still open.
 * Files go to {@code target/selenium-screenshots/<TestClass>_<testMethod>_<time>.png|.html}
 * (folder set by the failsafe property {@code selenium.screenshots}).
 */
public class ScreenshotOnFailure implements AfterTestExecutionCallback {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    @Override
    public void afterTestExecution(ExtensionContext context) throws IOException {
        if (context.getExecutionException().isEmpty()) {
            return;
        }
        Object test = context.getRequiredTestInstance();
        if (!(test instanceof UiTestBase uiTest) || uiTest.driver == null) {
            return;
        }
        WebDriver driver = uiTest.driver;
        Path folder = Path.of(System.getProperty("selenium.screenshots", "target/selenium-screenshots"));
        Files.createDirectories(folder);
        String name = context.getRequiredTestClass().getSimpleName() + "_"
                + context.getRequiredTestMethod().getName() + "_" + LocalDateTime.now().format(STAMP);

        Path png = folder.resolve(name + ".png");
        Files.write(png, ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
        Path html = folder.resolve(name + ".html");
        Files.writeString(html, driver.getPageSource(), StandardCharsets.UTF_8);

        System.out.println("[selenium] Test failed on " + driver.getCurrentUrl());
        System.out.println("[selenium] Screenshot: " + png.toAbsolutePath());
        System.out.println("[selenium] Page HTML:  " + html.toAbsolutePath());
    }
}
