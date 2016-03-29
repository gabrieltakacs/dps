package dynamo

class ClockNumber {
    private Integer number = null;

    public void setNumber(Integer number) {
        if(number == null) {
            this.number = number;
        }
    }

    public Integer getNumber() {
        return number;
    }
}
