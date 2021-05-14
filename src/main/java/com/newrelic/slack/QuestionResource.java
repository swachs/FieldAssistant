package com.newrelic.slack;

public class QuestionResource {
    private String resource;
    private String description;
    private String name;

    public QuestionResource(String resource, String name, String description) {
        this.resource = resource;
        this.description = description;
        this.name = name;
    }

    public QuestionResource(String resource) {
        this.resource = resource;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
