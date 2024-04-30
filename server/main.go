package main

import (
	"fmt"
	"time"

	"tinygo.org/x/bluetooth"
)

var adapter = bluetooth.DefaultAdapter

var counter uint8 = 1

func main() {
	must("enable BLE stack", adapter.Enable())
	adv := adapter.DefaultAdvertisement()
	must("config adv", adv.Configure(bluetooth.AdvertisementOptions{
		LocalName:    "BLE Server",
		ServiceUUIDs: []bluetooth.UUID{bluetooth.ServiceUUIDHeartRate},
		ManufacturerData: []bluetooth.ManufacturerDataElement{
			{CompanyID: 0xffff, Data: []byte{0x01, 0x02}},
		},
	}))
	must("start adv", adv.Start())

	println("advertising...")
	address, _ := adapter.Address()
	println("BLE Server /", address.MAC.String())

	var counterMeasurement bluetooth.Characteristic
	must("add service", adapter.AddService(&bluetooth.Service{
		UUID: bluetooth.ServiceUUIDHeartRate,
		Characteristics: []bluetooth.CharacteristicConfig{
			{
				Handle: &counterMeasurement,
				UUID:   bluetooth.CharacteristicUUIDHeartRateMeasurement,
				Value:  []byte{0, counter},
				Flags:  bluetooth.CharacteristicNotifyPermission,
			},
		},
	}))

	nextCounter := time.Now()
	for {
		nextCounter = nextCounter.Add(time.Minute / time.Duration(counter))
		currentTime := time.Now().Format("2006-01-02 15:04:05")
		fmt.Printf("counter: %d | %s\n", counter, currentTime)
		time.Sleep(time.Second)

		counter++
		counterMeasurement.Write([]byte{0, counter})
	}
}

func must(action string, err error) {
	if err != nil {
		panic("failed to " + action + ": " + err.Error())
	}
}
