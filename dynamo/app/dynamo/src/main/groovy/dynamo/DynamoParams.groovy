package dynamo

class DynamoParams {
    private static Integer myNumber = null;
    private static Integer nextNumber = null;
    public static final int maxClockNumber = 0x200;
    public static final int replicas = 3;
    public static final int readQuorum = 2;
    public static final int writeQuorum = 2;

    private DynamoParams() {
        throw new AssertionError();
    }

    public static void setMyNumber(Integer myNumber) {
        this.myNumber = myNumber;
    }

    public static Integer getMyNumber() {
        return myNumber;
    }

    public static Integer getNextNumber() {
        return nextNumber
    }

    public static void setNextNumber(Integer nextNumber) {
        this.nextNumber = nextNumber
    }
}
