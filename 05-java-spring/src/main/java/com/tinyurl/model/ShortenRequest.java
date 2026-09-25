package com.tinyurl.model;

public class ShortenRequest {
    private String url;
    private String custom_alias;

    public ShortenRequest() {}

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCustom_alias() {
        return custom_alias;
    }

    public void setCustom_alias(String custom_alias) {
        this.custom_alias = custom_alias;
    }
}
