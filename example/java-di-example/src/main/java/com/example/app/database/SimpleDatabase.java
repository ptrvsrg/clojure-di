package com.example.app.database;

import javax.inject.Inject;

public class SimpleDatabase implements Database {

    @Inject
    public SimpleDatabase() {
        System.out.println("SimpleDatabase created!");
    }

    @Override
    public String fetchUserData(Long id) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "Database processed: " + id;
    }
}
