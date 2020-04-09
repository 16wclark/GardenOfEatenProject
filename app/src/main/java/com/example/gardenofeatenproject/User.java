package com.example.gardenofeatenproject;


import java.util.List;

/**
 * user class to keep track of registered user
 * and their data (profile info)
 */

public class User {

    private String name;
    private List<String> visitedPlaces;

    public User() {
    }

    public List<String> getVisitedPlaces() {
        return visitedPlaces;
    }

    public void setVisitedPlaces(List<String> visitedPlaces) {
        this.visitedPlaces = visitedPlaces;
    }

    public User(String name, List<String> visitedPlaces) {
        this.visitedPlaces = visitedPlaces;
        this.name = name;
    }

    public String getname() {
        return name;
    }

    public void setname(String email) {
        this.name = email;
    }


}