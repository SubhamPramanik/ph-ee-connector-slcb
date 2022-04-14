package org.mifos.connector.slcb.dto;

import java.util.Date;

public class ReconciliationRequestDTO {

    public Date from;
    public Date to;
    public String accountNumber;

    public ReconciliationRequestDTO() {
    }

    public Date getFrom() {
        return from;
    }

    public void setFrom(Date from) {
        this.from = from;
    }

    public Date getTo() {
        return to;
    }

    public void setTo(Date to) {
        this.to = to;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }
}
