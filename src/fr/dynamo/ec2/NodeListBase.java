package fr.dynamo.ec2;

import com.amd.aparapi.device.OpenCLDevice;

import java.util.Set;

public interface NodeListBase {
    void addNode(DynamoInstance node);

    void removeNode(DynamoInstance node);

    Set<DynamoInstance> getNodes();

    Set<OpenCLDevice> getCloudDevices();

    Set<OpenCLDevice> getAllDevices();
}
