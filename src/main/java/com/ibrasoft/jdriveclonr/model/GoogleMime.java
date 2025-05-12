package com.ibrasoft.jdriveclonr.model;

import lombok.Getter;

@Getter
public enum GoogleMime {
    DOCS("application/vnd.google-apps.document"),
    SHEETS("application/vnd.google-apps.spreadsheet"),
    SLIDES("application/vnd.google-apps.presentation"),
    DRAWINGS("application/vnd.google-apps.drawing"),
    JAMBOARD("application/vnd.google-apps.jam");

    private final String mimeType;

    GoogleMime(String mimeType) {
        this.mimeType = mimeType;
    }

    @Override
    public String toString() {
        return mimeType;
    }
}
