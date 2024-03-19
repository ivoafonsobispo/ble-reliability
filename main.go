package main

import (
	"tinygo.org/x/bluetooth"
)

var adapter = bluetooth.DefaultAdapter

var AllowedDevices = map[string]bool{
	"2C:BE:EB:19:2B:F0": true,
}

func main() {
	// Enable BLE interface.
	must("enable BLE stack", adapter.Enable())

	// Start scanning.
	println("Scanning...")
	err := adapter.Scan(func(adapter *bluetooth.Adapter, device bluetooth.ScanResult) {
		if AllowedDevices[device.Address.String()] {
			println("Found your Android device:", device.Address.String(), device.RSSI, device.LocalName())

			// Stop scanning if device is found.
			adapter.StopScan()

			// Connect to the Android device.
			//go connectToDevice(device.Address)
		}
	})
	must("start scan", err)
}

func must(action string, err error) {
	if err != nil {
		panic("failed to " + action + ": " + err.Error())
	}
}
