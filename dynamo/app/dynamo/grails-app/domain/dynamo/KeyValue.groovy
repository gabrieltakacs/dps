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
        return key.hashCode()%DynamoParams.maxClockNumber;
    }
}
