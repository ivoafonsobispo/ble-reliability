package main

import (
	"tinygo.org/x/bluetooth"
)

var adapter = bluetooth.DefaultAdapter

func main() {
	var adv = configureAdvertisement()
	startAdvertising(adv)

	var counterMeasurement = initializeCurrentTimeCharacteristic()
	sendCurrentTimeContinuously(counterMeasurement)
}
