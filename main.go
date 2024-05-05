package main

import (
	"fmt"
	"time"

	"tinygo.org/x/bluetooth"
)

const BleServerName = "BLE Counter"
const TimeFormat = "2006-01-02 15:04:05"

var adapter = bluetooth.DefaultAdapter

var savedPacket = make([]byte, 10)

const ConnectionCheckInterval = time.Second * 10

var connectionLost = false

func main() {
	must("enable BLE stack", adapter.Enable())
	adv := adapter.DefaultAdvertisement()
	must("config adv", adv.Configure(bluetooth.AdvertisementOptions{
		LocalName:    BleServerName,
		ServiceUUIDs: []bluetooth.UUID{bluetooth.ServiceUUIDCurrentTime},
	}))

	must("start adv", adv.Start())
	fmt.Println("Advertising...")
	address, _ := adapter.Address()
	fmt.Printf("%s | %s\n", BleServerName, address.MAC.String())

	time.Sleep(time.Second * 30)

	go sendCurrentTimeContinuously()
	//receiveAcknowledgments()

	// Simulate random loss of connection
	//go simulateConnectionLoss()

	// Monitor connection status
	//go monitorConnection()

	// Block indefinitely
	select {}
}

func sendCurrentTimeContinuously() {
	currentTime := time.Now()
	seq := 0

	// Add the service once
	var counterMeasurement bluetooth.Characteristic
	must("add service", adapter.AddService(&bluetooth.Service{
		UUID: bluetooth.ServiceUUIDCurrentTime,
		Characteristics: []bluetooth.CharacteristicConfig{
			{
				Handle: &counterMeasurement,
				UUID:   bluetooth.CharacteristicUUIDCurrentTime,
				Flags:  bluetooth.CharacteristicNotifyPermission,
			},
		},
	}))

	for {
		fmt.Printf("Sending current time: %s, Seq: %d\n", currentTime.Format(TimeFormat), seq)

		currentTimePayload := encodeCurrentTime(currentTime)
		err := sendDataPacket(counterMeasurement, currentTimePayload, seq)
		if err != nil {
			fmt.Println("Error sending data packet:", err)
		}

		seq++
		time.Sleep(time.Second)
		currentTime = time.Now()
	}
}

func simulateConnectionLoss() {
	for {
		// Simulate random loss of connection
		if randomLoss() {
			fmt.Println("Connection lost. Restarting server...")
			connectionLost = true
			adapter.StopScan()
		}
		time.Sleep(time.Second * 5) // Check connection status every 5 seconds
	}
}

func monitorConnection() {
	ticker := time.NewTicker(ConnectionCheckInterval)
	defer ticker.Stop()

	for {
		select {
		case <-ticker.C:
			// Check connection status
			if connectionLost {
				fmt.Println("Connection lost. Restarting server...")
				restartServer()
			}
		}
	}
}

func restartServer() {
	connectionLost = false
	main()
}

//func receiveAcknowledgments() {
//	for {
//		time.Sleep(time.Second * 5)
//		fmt.Println("Received acknowledgment for Seq:", randomSeq())
//	}
//}

func sendDataPacket(counterMeasurement bluetooth.Characteristic, payload []byte, seq int) error {
	// Simulate random packet loss
	if randomLoss() {
		copy(savedPacket, payload)
		return fmt.Errorf("packet loss for Seq: %d", seq)
	}

	if savedPacket[0] != 0 {
		_, err := counterMeasurement.Write(savedPacket)
		if err != nil {
			return err
		}
		savedPacket[0] = 0
		fmt.Println("Resending saved packet")
	}

	// Send data packet over Bluetooth
	_, err := counterMeasurement.Write(payload)
	return err
}

func must(action string, err error) {
	if err != nil {
		panic("failed to " + action + ": " + err.Error())
	}
}

func encodeCurrentTime(t time.Time) []byte {
	payload := make([]byte, 10)
	payload[0] = byte(t.Year() & 0xFF)
	payload[1] = byte(t.Year() >> 8)
	payload[2] = byte(t.Month())
	payload[3] = byte(t.Day())
	payload[4] = byte(t.Hour())
	payload[5] = byte(t.Minute())
	payload[6] = byte(t.Second())
	payload[7] = byte(t.Weekday())
	payload[8] = 0
	payload[9] = 0
	return payload
}

func randomLoss() bool {
	// Simulate random packet loss
	return time.Now().UnixNano()%5 == 0
}

func randomSeq() int {
	// Simulate random sequence number for acknowledgment
	return int(time.Now().UnixNano() % 10)
}
