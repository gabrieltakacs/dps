package dynamo

class ClockNumber {
    private static Integer number = null;

    public static void setNumber(Integer number) {
        if(number == null) {
            this.number = number;
        }
    }

    public static Integer getNumber() {
        return number;
    }
}
