package com.ibrasoft.jdriveclonr.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.common.util.concurrent.RateLimiter;
import lombok.Getter;

/**
 * Legacy ServiceRepository - now deprecated in favor of dependency injection.
 * Use GoogleServiceFactory.createServices() for new code.
 */
@Deprecated
public class ServiceRepository {

    private static final ThreadLocal<GoogleServiceFactory.GoogleServices> threadLocalServices = 
        ThreadLocal.withInitial(() -> null);

    @Getter
    private static final RateLimiter rateLimiter = RateLimiter.create(1);

    public static void init(Credential credential) {
        try {
            GoogleServiceFactory.GoogleServices services = GoogleServiceFactory.createServices(credential);
            threadLocalServices.set(services);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Google services", e);
        }
    }

    private static GoogleServiceFactory.GoogleServices getServices() {
        GoogleServiceFactory.GoogleServices services = threadLocalServices.get();
        if (services == null) {
            throw new IllegalStateException("ServiceRepository not initialized for current thread. Call init() first or use GoogleServiceFactory.createServices() instead.");
        }
        return services;
    }

    public static com.google.api.services.drive.Drive getDriveService() {
        return getServices().getDriveService();
    }

    public static com.google.api.services.slides.v1.Slides getSlidesService() {
        return getServices().getSlidesService();
    }

    public static com.google.api.services.sheets.v4.Sheets getSheetsService() {
        return getServices().getSheetsService();
    }

    public static Credential getCredential() {
        return getServices().getCredential();
    }

    public static boolean isInitialized() {
        return threadLocalServices.get() != null;
    }

    public static void clear() {
        threadLocalServices.remove();
    }
}
