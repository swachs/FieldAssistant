package com.newrelic.slack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SMETopic {
    String topic;
    String wizardsChannel;
    List<String> smes = new ArrayList<String>();

    public String getWizardsChannel() {
        return wizardsChannel;
    }

    public void setWizardsChannel(String wizardsChannel) {
        this.wizardsChannel = wizardsChannel;
    }

    public SMETopic(String topic, String wizardsChannel, String smesList) {
        this.topic = topic;
        this.wizardsChannel = wizardsChannel;
        this.smes = Arrays.asList(smesList.split(",", -1));
    }

    public SMETopic(String topic, List<String> smes) {
        this.topic = topic;
        this.smes = smes;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public List<String> getSmes() {
        return smes;
    }

    public void setSmes(List<String> smes) {
        this.smes = smes;
    }
}
