package main

import (
	"tinygo.org/x/bluetooth"
)

func configureAdvertisement() bluetooth.Advertisement {
	must("enable BLE stack", adapter.Enable())
	adv := adapter.DefaultAdvertisement()
	must("config adv", adv.Configure(bluetooth.AdvertisementOptions{
		LocalName:    "BLE Counter",
		ServiceUUIDs: []bluetooth.UUID{bluetooth.ServiceUUIDCurrentTime},
	}))
	return *adv
}

func startAdvertising(adv bluetooth.Advertisement) {
	must("start adv", adv.Start())
	println("advertising...")
	address, _ := adapter.Address()
	println("BLE Counter |", address.MAC.String())
}
