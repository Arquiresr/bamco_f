/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.arqui.webserver;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;

import java.io.FileInputStream;
import java.io.IOException;

public class FirebaseInitializer {
  private static Firestore db;

    public static void init() throws IOException {
        
           FileInputStream serviceAccount = new FileInputStream("src/main/resources/firebase-config.json");

 GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
        FirestoreOptions options = FirestoreOptions.newBuilder()
            .setCredentials(credentials)
            .build();
        db = options.getService();
        System.out.println("✅ Firebase inicializado com sucesso.");
    }
    
     public static Firestore getDb() {
        return db;
    }
}
