package com.bo0tzz.imagebot;

public class BotUser {
    private long id;
    private boolean canUse;
    private boolean sentRequest;
    private int uses;

    public BotUser(long id) {
        this.id = id;
    }

    public BotUser() {
    }

    public void resetUses() {
        uses = 0;
    }

    public void incrementUses() {
        uses++;
    }

    public long id() {
        return id;
    }

    public int uses() {
        return uses;
    }

    public boolean canUse() {
        return canUse;
    }

    public void setCanUse(boolean canUse) {
        this.canUse = canUse;
    }

    public boolean sentRequest() {
        return sentRequest;
    }

    public void setSentRequest(boolean sentRequest) {
        this.sentRequest = sentRequest;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BotUser botUser = (BotUser) o;

        return id == botUser.id;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }
}
