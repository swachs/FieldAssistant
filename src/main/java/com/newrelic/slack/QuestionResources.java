package com.newrelic.slack;

import java.util.ArrayList;
import java.util.List;

public class QuestionResources {
    private String questionTopic;
    private List<QuestionResource> resources;

    public QuestionResources(String questionTopic) {
        this.questionTopic = questionTopic;
        resources = new ArrayList<>();
    }

    public void addResource(QuestionResource questionResource) {
        resources.add(questionResource);
    }

    public List<QuestionResource> getResources() {
        return resources;
    }
}
