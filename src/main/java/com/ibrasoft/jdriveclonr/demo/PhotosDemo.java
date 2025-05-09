//package com.ibrasoft.jdriveclonr.demo;
//
//import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
//import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
//import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
//import com.google.api.client.json.jackson2.JacksonFactory;
//import com.google.api.client.util.store.FileDataStoreFactory;
//import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
//import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
//
//import com.google.auth.oauth2.AccessToken;
//import com.google.auth.oauth2.UserCredentials;
//import com.google.photos.types.proto.Album;
//import com.google.photos.types.proto.MediaItem;
//
//import com.ibrasoft.jdriveclonr.auth.GoogleOAuthService;
//import com.ibrasoft.jdriveclonr.service.PhotosAPIService;
//
//import java.io.InputStreamReader;
//import java.nio.file.Paths;
//import java.util.Date;
//import java.util.List;
//import java.util.Map;
//
//public class PhotosDemo {
//
//    /* ────────────────────────── CONFIG ────────────────────────── */
//
//    private static final String APP_NAME     = "DriveClonr-PhotosDemo";
//    private static final String TOKENS_DIR   = ".tokens/photos";        // token cache
//    private static final List<String> SCOPES =
//            List.of("https://www.googleapis.com/auth/photoslibrary.readonly");
//
//    /* ────────────────────────── MAIN ──────────────────────────── */
//
//    public static void main(String[] args) throws Exception {
//
//        // 2. Your wrapper service (mirrors DriveAPIService style)
//        try (PhotosAPIService photosSvc = new PhotosAPIService(GoogleOAuthService.authorize())) {
//
//            // 3. Get albums + uncategorised list
//            Map.Entry<Map<Album, List<MediaItem>>, List<MediaItem>> data =
//                    photosSvc.getAlbumsAndUncategorized();
//
//            Map<Album, List<MediaItem>> albums   = data.getKey();
//            List<MediaItem> uncategorised        = data.getValue();
//
//            // 4. Simple console summary (swap for UI tree population)
//            System.out.println("\n=== Albums ===");
//            albums.forEach((alb, items) ->
//                    System.out.printf(" - %-40s : %3d items%n",
//                            alb.getTitle(), items.size()));
//
//            System.out.printf("%n=== Uncategorized ===%n");
//            System.out.printf("Total uncategorised items: %d%n", uncategorised.size());
//
//            // 5. Download the first uncategorised photo (just to prove it works)
//            if (!uncategorised.isEmpty()) {
//                MediaItem first = uncategorised.get(0);
//                var saved = photosSvc.downloadToDirectory(
//                        first,
//                        Paths.get("downloads"));
//
//                System.out.printf("Downloaded %s → %s%n",
//                        first.getFilename(), saved.toAbsolutePath());
//            }
//        }
//    }
//}
