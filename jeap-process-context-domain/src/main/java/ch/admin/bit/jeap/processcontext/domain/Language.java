package ch.admin.bit.jeap.processcontext.domain;

public enum Language {

    DE, FR, IT;

    public String languageId() {
        return name().toLowerCase();
    }

}
