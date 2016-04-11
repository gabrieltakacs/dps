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
        return key.hashCode()%DynamoParams.maxClockNumber;
    }
}
