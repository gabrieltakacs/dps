package dynamo

import org.apache.curator.x.discovery.ServiceInstance

class DynamoParams {
    public static final int maxClockNumber = 0x200;
    public static final int replicas = 3;
    public static final int readQuorum = 2;
    public static final int writeQuorum = 2;
    private static List<ServiceInstance> instanceList = null;

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

    public static void setInstanceList(List<ServiceInstance> instanceList) {
        this.instanceList = new ArrayList<ServiceInstance>(instanceList);
    }

    public static List<ServiceInstance> getInstanceList() {
        return instanceList;
    }
}
