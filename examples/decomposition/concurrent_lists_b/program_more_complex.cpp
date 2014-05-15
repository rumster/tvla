// A program inspired by the serial port device driver.

typedef struct _list {
	struct _list * next;
	void * data;
} * List;

typedef struct _DeviceObject {
	BOOL process;
	_DeviceObject * nextDevice;
	List readPackets;
	List writePackets;
	Lock readPacketsLock;
	Lock writePacketsLock;
} * DeviceObject;

typedef struct _Packet {
	_Packet * nextPacket;
	int type;
	// ... data fields
} * Packet;

DeviceObject * deviceObjs;
Lock deviceListLock;

void main() {
	deviceObjs = makeDummyDevice();
	while (true) {
		Packet * msg = getPacket();
		if (msg->type == ADD_DEVICE) {
			DeviceObject * newDevice = makeDevice(msg);


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

	start_thread(&acceptPackets, device);

	while (nondet()) {
		if (nondet()) {
			device->process = FALSE; // Signal producer to terminate
			while (!device->producerStopped) {} // Wait until it stops
		}

		// process read packets
 	    lock(device->readLock);
		  while (nondet()) {
		    packetToProcess = device->readPackets.next;
		    device->readPackets.next = device->readPackets.next->next;
			processReadPacket(packetToProcess);
		  }
		release(device->readLock);

		// process write packets
 	    lock(device->writeLock);
		  while (nondet()) {
		    packetToProcess = device->writePackets.next;
		    device->writePackets.next = device->writePackets.next->next;
			processWritePacket(packetToProcess);
		  }
		release(device->writeLock);
	}

	// Remove this device and terminate
	lock(deviceListLock);
	  // Loop over deviceObjs to find this device
	  currDevice = deviceObjs;
	  while (currDevice->next != device) {
	  	  currDevice = currDevice->next;
	  }
	  // Remove this device from the list
	  currDevice->next = currDevice->next->next;
	  free(device);
	release(deviceListLock);
}

void producePackets(DeviceObject * device) {
	while (device->process) {
		Packet * packet = getPacket();
		if (packet->type == readPacket) {
			lock(device->readLock);
			  packet->next = device->readPackets.next;
			  device->readPackets.next = packet;
			release(device->readLock);
		}
		else if (packet->type == writePacket) {
			lock(device->writeLock);
			  packet->next = device->writePackets.next;
			  device->writePackets.next = packet;
			release(device->writeLock);
		}
	}
	device->producerStopped;
}