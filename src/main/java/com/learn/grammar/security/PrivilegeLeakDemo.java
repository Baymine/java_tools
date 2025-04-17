package com.learn.grammar.security;

import javax.security.auth.Subject;
import java.security.PrivilegedAction;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PrivilegeLeakDemo {
    public static void main(String[] args) {
        System.setSecurityManager(new SecurityManager());

        ExecutorService executor = Executors.newFixedThreadPool(1);

        Subject adminSubject = createSubject("admin");
        Subject guestSubject = createSubject("guest");

        executor.submit(() -> {
            Subject.doAs(adminSubject, (PrivilegedAction<Void>) () -> {
                try {
                    System.out.println("Admin is running");
                    java.nio.file.Files.readAllBytes(
                            java.nio.file.Paths.get("/tmp/admin_file.txt")
                    );
                } catch (Exception e) {
                    System.out.println("Admin access fail, " + e.getMessage());
                }
                return null;
            });
        });


        // Leak the admin subject to the guest subject
        executor.submit(() -> {
            Subject.doAs(guestSubject, (PrivilegedAction<Void>) () -> {
                try {
                    System.out.println("Guest is running");
                    java.nio.file.Files.readAllBytes(
                            java.nio.file.Paths.get("/tmp/admin_file.txt")
                    );
                } catch (Exception e) {
                    System.out.println("Guest access fail, " + e.getMessage());
                }
                return null;
            });
        });
        executor.shutdown();
    }

    private static Subject createSubject(String username) {
        Subject subject = new Subject();
        subject.getPrincipals().add(new javax.security.auth.x500.X500Principal("CN=" + username));

        return subject;
    }
}
