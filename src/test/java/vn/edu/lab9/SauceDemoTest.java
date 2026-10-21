package vn.edu.lab9;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.openqa.selenium.By;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SauceDemoTest {
    private static final String BASE_URL = "https://www.saucedemo.com/";
    private static final String USERNAME = "standard_user";
    private static final String PASSWORD = "secret_sauce";
    private static final AtomicBoolean ENVIRONMENT_PRINTED = new AtomicBoolean();
    private static final Map<String, String> EVIDENCE_FILES = Map.of(
            "tc01_loginSuccess", "01-login-success.png",
            "tc02_loginWrongPassword", "02-login-invalid.png",
            "tc03_addProductToCart", "03-add-to-cart.png",
            "tc04_removeProductFromCart", "04-remove-from-cart.png",
            "tc05_logout", "05-logout.png");

    private static final By USERNAME_INPUT = By.id("user-name");
    private static final By PASSWORD_INPUT = By.id("password");
    private static final By LOGIN_BUTTON = By.id("login-button");
    private static final By PAGE_TITLE = By.cssSelector("[data-test='title']");
    private static final By LOGIN_ERROR = By.cssSelector("[data-test='error']");
    private static final By ADD_BACKPACK = By.id("add-to-cart-sauce-labs-backpack");
    private static final By REMOVE_BACKPACK = By.id("remove-sauce-labs-backpack");
    private static final By CART_LINK = By.cssSelector("[data-test='shopping-cart-link']");
    private static final By CART_BADGE = By.cssSelector("[data-test='shopping-cart-badge']");
    private static final By CART_ITEMS = By.cssSelector("[data-test='inventory-item']");
    private static final By ITEM_NAME = By.cssSelector("[data-test='inventory-item-name']");
    private static final By ITEM_QUANTITY = By.cssSelector("[data-test='item-quantity']");

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeEach
    void setUp(TestInfo testInfo) throws IOException {
        String testMethod = testInfo.getTestMethod().orElseThrow().getName();
        Files.deleteIfExists(Path.of("evidence", EVIDENCE_FILES.get(testMethod)));

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--incognito", "--window-size=1366,900");
        if (Boolean.getBoolean("headless")) {
            options.addArguments("--headless=new");
        }

        Map<String, Object> preferences = new HashMap<>();
        preferences.put("credentials_enable_service", false);
        preferences.put("profile.password_manager_enabled", false);
        preferences.put("profile.password_manager_leak_detection", false);
        options.setExperimentalOption("prefs", preferences);

        driver = new ChromeDriver(options);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        printEnvironmentOnce();
        driver.get(BASE_URL);
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    @DisplayName("TC01 - Đăng nhập thành công với tài khoản hợp lệ")
    void tc01_loginSuccess() throws IOException {
        loginSuccessfully();

        assertEquals("Products", visible(PAGE_TITLE).getText());
        assertTrue(driver.getCurrentUrl().endsWith("/inventory.html"));
        saveEvidence("01-login-success.png");
    }

    @Test
    @DisplayName("TC02 - Đăng nhập thất bại khi sai mật khẩu")
    void tc02_loginWrongPassword() throws IOException {
        login("wrong_password");

        assertEquals(
                "Epic sadface: Username and password do not match any user in this service",
                visible(LOGIN_ERROR).getText());
        assertEquals(BASE_URL, driver.getCurrentUrl());
        assertTrue(visible(LOGIN_BUTTON).isDisplayed());
        saveEvidence("02-login-invalid.png");
    }

    @Test
    @DisplayName("TC03 - Thêm Sauce Labs Backpack vào giỏ hàng")
    void tc03_addProductToCart() throws IOException {
        loginSuccessfully();
        addBackpack();
        openCart();

        wait.until(ExpectedConditions.numberOfElementsToBe(CART_ITEMS, 1));
        List<WebElement> items = driver.findElements(CART_ITEMS);
        assertEquals(1, items.size());
        assertEquals("Sauce Labs Backpack", items.get(0).findElement(ITEM_NAME).getText());
        assertEquals("1", items.get(0).findElement(ITEM_QUANTITY).getText());
        assertEquals("1", visible(CART_BADGE).getText());
        saveEvidence("03-add-to-cart.png");
    }

    @Test
    @DisplayName("TC04 - Xóa sản phẩm khỏi giỏ hàng")
    void tc04_removeProductFromCart() throws IOException {
        loginSuccessfully();
        addBackpack();
        openCart();
        wait.until(ExpectedConditions.numberOfElementsToBe(CART_ITEMS, 1));
        click(REMOVE_BACKPACK);

        wait.until(ExpectedConditions.numberOfElementsToBe(CART_ITEMS, 0));
        wait.until(ExpectedConditions.invisibilityOfElementLocated(CART_BADGE));
        assertEquals("Your Cart", visible(PAGE_TITLE).getText());
        assertTrue(driver.findElements(CART_ITEMS).isEmpty());
        assertTrue(driver.findElements(CART_BADGE).isEmpty());
        saveEvidence("04-remove-from-cart.png");
    }

    @Test
    @DisplayName("TC05 - Đăng xuất và trở về trang đăng nhập")
    void tc05_logout() throws IOException {
        loginSuccessfully();
        click(By.id("react-burger-menu-btn"));
        click(By.id("logout_sidebar_link"));

        assertTrue(visible(LOGIN_BUTTON).isDisplayed());
        assertTrue(visible(USERNAME_INPUT).isDisplayed());
        assertEquals(BASE_URL, driver.getCurrentUrl());
        saveEvidence("05-logout.png");
    }

    private void login(String password) {
        visible(USERNAME_INPUT).sendKeys(USERNAME);
        visible(PASSWORD_INPUT).sendKeys(password);
        click(LOGIN_BUTTON);
    }

    private void loginSuccessfully() {
        login(PASSWORD);
        wait.until(ExpectedConditions.urlToBe(BASE_URL + "inventory.html"));
        assertEquals("Products", visible(PAGE_TITLE).getText());
    }

    private void addBackpack() {
        click(ADD_BACKPACK);
        assertEquals("1", visible(CART_BADGE).getText());
    }

    private void openCart() {
        click(CART_LINK);
        wait.until(ExpectedConditions.urlToBe(BASE_URL + "cart.html"));
        assertEquals("Your Cart", visible(PAGE_TITLE).getText());
    }

    private WebElement visible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    private void click(By locator) {
        wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
    }

    private void saveEvidence(String fileName) throws IOException {
        wait.until(browser -> Boolean.TRUE.equals(((JavascriptExecutor) browser).executeScript(
                "return document.fonts.status === 'loaded' && "
                        + "Array.from(document.images).every(image => image.complete);")));
        Path evidenceDirectory = Path.of("evidence");
        Files.createDirectories(evidenceDirectory);
        byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
        Files.write(evidenceDirectory.resolve(fileName), screenshot);
    }

    private void printEnvironmentOnce() {
        if (ENVIRONMENT_PRINTED.compareAndSet(false, true)) {
            Capabilities capabilities = ((ChromeDriver) driver).getCapabilities();
            System.out.println("Java version: " + System.getProperty("java.version"));
            System.out.println("Browser: " + capabilities.getBrowserName()
                    + " " + capabilities.getBrowserVersion());
            Object chromeCapability = capabilities.getCapability("chrome");
            if (chromeCapability instanceof Map<?, ?> chromeDetails) {
                System.out.println("ChromeDriver: " + chromeDetails.get("chromedriverVersion"));
            }
        }
    }
}
