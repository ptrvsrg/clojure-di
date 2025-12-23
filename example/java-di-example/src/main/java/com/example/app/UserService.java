package com.example.app;

import com.example.app.database.Database;

import javax.inject.Inject;

public class UserService {

    private final Database database;

    @Inject
    public UserService(Database database) {
        this.database = database;
    }

    public String processUser(Long id) {
        String data = database.fetchUserData(id);
        return "Service processed: " + data;
    }
}
