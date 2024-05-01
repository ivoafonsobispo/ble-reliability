package main

import (
	"fmt"
	"time"

	"tinygo.org/x/bluetooth"
)

var adapter = bluetooth.DefaultAdapter

func main() {
	must("enable BLE stack", adapter.Enable())
	adv := adapter.DefaultAdvertisement()
	must("config adv", adv.Configure(bluetooth.AdvertisementOptions{
		LocalName:    "BLE Counter",
		ServiceUUIDs: []bluetooth.UUID{bluetooth.ServiceUUIDCurrentTime},
	}))

	must("start adv", adv.Start())
	println("advertising...")
	address, _ := adapter.Address()
	println("BLE Counter |", address.MAC.String())

	sendCurrentTimeContinuously()
}

func sendCurrentTimeContinuously() {
	currentTime := time.Now()
	currentTimePayload := encodeCurrentTime(currentTime)

	var counterMeasurement bluetooth.Characteristic
	must("add service", adapter.AddService(&bluetooth.Service{
		UUID: bluetooth.ServiceUUIDCurrentTime,
		Characteristics: []bluetooth.CharacteristicConfig{
			{
				Handle: &counterMeasurement,
				UUID:   bluetooth.CharacteristicUUIDCurrentTime,
				Value:  currentTimePayload,
				Flags:  bluetooth.CharacteristicNotifyPermission,
			},
		},
	}))

	for {
		currentTime := time.Now()
		fmt.Printf("Sending current time: %s\n", currentTime.Format("2006-01-02 15:04:05"))

		currentTimePayload := encodeCurrentTime(currentTime)
		counterMeasurement.Write(currentTimePayload)

		time.Sleep(time.Second)
	}
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
