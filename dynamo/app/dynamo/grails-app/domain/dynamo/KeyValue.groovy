package dynamo

class KeyValue {

    int hash;
    String key;
    String value;
    Map<String, Integer> vectorClock = new HashMap()

    static constraints = {
        key nullable: true;
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

    /**
     * @return -1, ak je záznam starší, 0 ak sa nedá porovnať, 1 ak je novší
     */
    private static int checkVC(Map vectorClock, Map other) {
        boolean newer = false, older = false;
        Set all = new LinkedHashSet();all.addAll(other.keySet());all.addAll(vectorClock.keySet())
        for(def o:all) {
            int cur = 0;
            int val = 0;
            if(vectorClock.containsKey(o)) {
                cur = vectorClock.get(o) as Integer
            }
            if(other.containsKey(o)) {
                val = other.get(o) as Integer
            }
            if(cur < val) {
                older = true;
            } else if(cur > val){
                newer = true;
            }
        }
        if(!newer) return -1;
        if(!older) return 1;
        return 0;
    }

    public static boolean saveImpl(String key, String value, int hash, Map vectorclock) {
        List<KeyValue> list = KeyValue.findAllByKey(key);
        boolean toSave = true;
        for(KeyValue k: list) {
            Integer status = checkVC(vectorclock, k.getVectorClock());
            if(status == 1) {//tento záznam je novší - zmaž starý
                k.delete(flush: true)
            }
            if(status == -1) {//tento záznam je starší
                toSave = false
            }
        }
        if(toSave) {
            KeyValue kv = new KeyValue()
            kv.setKey(key)
            kv.setValue(value);
            kv.setHash(hash)
            kv.vectorClock = vectorclock;
            kv.save(flush: true, failOnError: true);
        }
        return toSave;
    }
}
