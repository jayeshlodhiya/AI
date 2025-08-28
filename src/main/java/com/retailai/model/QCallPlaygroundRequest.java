package com.retailai.model;



import java.util.List;

public class QCallPlaygroundRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String assistantId;
    private List<String> phoneNumber; // e.g. ["+917350348860"]
    private String dialerId;          // optional

    // getters/setters

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAssistantId() { return assistantId; }
    public void setAssistantId(String assistantId) { this.assistantId = assistantId; }

    public List<String> getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(List<String> phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getDialerId() { return dialerId; }
    public void setDialerId(String dialerId) { this.dialerId = dialerId; }
}
