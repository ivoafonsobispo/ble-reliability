package main

import (
	"fmt"
	"time"

	"tinygo.org/x/bluetooth"
)

func initializeCurrentTimeCharacteristic() bluetooth.Characteristic {
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

	return counterMeasurement
}

func sendCurrentTimeContinuously(counterMeasurement bluetooth.Characteristic) {
	for {
		currentTime := time.Now()
		fmt.Printf("Sending current time: %s\n", currentTime.Format("2006-01-02 15:04:05"))

		currentTimePayload := encodeCurrentTime(currentTime)
		counterMeasurement.Write(currentTimePayload)

		time.Sleep(time.Second)
	}
}
