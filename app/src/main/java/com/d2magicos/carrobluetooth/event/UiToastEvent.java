package com.d2magicos.carrobluetooth.event;

public class UiToastEvent {

    private String message;
    private Boolean longToast;
    private Boolean isWarning;

    public UiToastEvent(String message, Boolean longToast, Boolean isWarning){
        this.message = message;
        this.longToast = longToast;
        this.isWarning = isWarning;
    }

    public String getMessage(){ return this.message; }
    public void setMessage(String message){ this.message = message; }

    public Boolean getLongToast(){ return this.longToast; }
    public void setLongToast(Boolean longToast){ this.longToast = longToast; }

    public Boolean getIsWarning(){ return this.isWarning; }
    public void setIsWarning(boolean isWarning){
        this.isWarning = isWarning;
    }

}