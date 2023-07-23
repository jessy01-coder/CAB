package com.sanj.cabme.models;

public class TransactionStatement {
    private String id, userId, statement, date, amount;

    public TransactionStatement() {
    }

    public TransactionStatement(String id, String userId, String statement, String date, String amount) {
        this.id = id;
        this.userId = userId;
        this.statement = statement;
        this.date = date;
        this.amount = amount;
    }

    public String getAmount() {
        return amount;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getStatement() {
        return statement;
    }

    public String getDate() {
        return date;
    }
}
