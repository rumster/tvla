// A program inspired by the serial port device driver

typedef struct _DeviceObject {
    _DeviceObject * next;
    List readPackets;
    List writePackets;
    Lock readPacketsLock;
    Lock writePacketsLock;
    // In reality there are most lists here
} * DeviceObject;

typedef struct _Packet {
    _Packet * next;
    int type;
    // ... data fields
} * Packet;

// Global variables
DeviceObject * deviceObjs;
Lock deviceListLock;

void main() {
    deviceObjs = makeDummyDevice();
    while (true) {
        Packet * packet = getPacket();
		if (packet->type == ADD_DEVICE) {
            DeviceObject * newDevice = makeDevice(packet);
            start_thread(&consumePackets, newDevice);
        }
    }
}

void consumePackets(DeviceObject * device) {
    Packet * packetToProcess;

    // Add the device to the list
    lock(deviceListLock);
      newDevice->next = deviceObjs->next;
      deviceObjs->n = newDevice;
    release(deviceListLock);

    start_thread(&producePackets, device);

    while (nondet()) {
        // process read packets
        lock(device->readLock);
          while (nondet()) {
            packetToProcess = device->readPackets->next;
			if (packetToProcess == NULL)
				break;
            device->readPackets->next = device->readPackets->next->next;
            processReadPacket(packetToProcess);
          }
        release(device->readLock);

        // process write packets
        lock(device->writeLock);
          while (nondet()) {
            packetToProcess = device->writePackets->next;
			if (packetToProcess == NULL)
				break;
            device->writePackets->next = device->writePackets->next->next;
            processWritePacket(packetToProcess);
          }
        release(device->writeLock);
    }
}

void producePackets(DeviceObject * device) {
    while (nondet()) {
        Packet * packet = getPacket();
        if (packet->type == readPacket) {
            lock(device->readLock);
              packet->next = device->readPackets->next;
              device->readPackets.next = packet;
            release(device->readLock);
        }
        else if (packet->type == writePacket) {
            lock(device->writeLock);
              packet->next = device->writePackets->next;
              device->writePackets->next = packet;
            release(device->writeLock);
        }
    }
}
