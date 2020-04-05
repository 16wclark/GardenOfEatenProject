package com.example.gardenofeatenproject;


import java.util.List;

/**
 * user class to keep track of registered user
 * and their data (profile info)
 */

public class User {

    private String email;
    private List<String> visitedPlaces[];

    public User() {
    }

    public List<String>[] getVisitedPlaces() {
        return visitedPlaces;
    }

    public void setVisitedPlaces(List<String>[] visitedPlaces) {
        this.visitedPlaces = visitedPlaces;
    }

    public User(String email, List<String> visitedPlaces[]) {
        this.visitedPlaces = visitedPlaces;
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }


}