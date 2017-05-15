package com.willblaschko.android.alexavoicelibrary;

/**
 * Created by nuwanda on 14/05/17.
 */

public class PayloadCard {
    public String getAlexaMessge() {
        return alexaMessge;
    }

    public void setAlexaMessge(String alexaMessge) {
        this.alexaMessge = alexaMessge;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    private String alexaMessge;
    private String type;
    private String intent;


}
