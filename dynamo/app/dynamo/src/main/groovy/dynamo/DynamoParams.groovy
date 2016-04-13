package dynamo

class DynamoParams {
    public static final int maxClockNumber = 0x200;
    public static final int replicas = 3;
    public static final int readQuorum = 2;
    public static final int writeQuorum = 2;

    private static Integer myNumber = null;


    private DynamoParams() {
        throw new AssertionError();
    }

    public static void setMyNumber(Integer myNumber) {
        this.myNumber = myNumber;
    }

    public static Integer getMyNumber() {
        return myNumber;
    }

}
