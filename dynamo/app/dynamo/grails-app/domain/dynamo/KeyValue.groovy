package dynamo

class KeyValue {

    int hash;
    String key;
    String value;
    String vectorClock;

    static constraints = {
        key unique: true, nullable: true;
        vectorClock nullable: true, blank: true;
        value nullable: true;
    }

    static int calculateHash(String key) {
        if(key == null)
            return 0;
        if(isNumeric(key)) {
            return Integer.parseInt(key)%DynamoParams.maxClockNumber;
        }
        return key.hashCode()%DynamoParams.maxClockNumber;
    }

    public static boolean isNumeric(String inputData) {
        return inputData.matches("[-+]?\\d+(\\.\\d+)?");
    }
}
