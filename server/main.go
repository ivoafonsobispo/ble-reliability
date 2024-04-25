package main

import (
	"fmt"
	"log"

	"github.com/paypal/gatt"
	"github.com/paypal/gatt/examples/option"
	"github.com/paypal/gatt/examples/service"
)

func main() {
	d, err := gatt.NewDevice(option.DefaultServerOptions...)
	if err != nil {
		log.Fatalf("Failed to open device, err: %s", err)
	}

	// Register optional handlers.
	d.Handle(
		gatt.CentralConnected(func(c gatt.Central) { fmt.Println("Connect: ", c.ID()) }),
		gatt.CentralDisconnected(func(c gatt.Central) { fmt.Println("Disconnect: ", c.ID()) }),
	)

	// A mandatory handler for monitoring device state.
	onStateChanged := func(d gatt.Device, s gatt.State) {
		fmt.Printf("State: %s\n", s)
		switch s {
		case gatt.StatePoweredOn:
			// Setup GAP and GATT services for Linux implementation.
			d.AddService(service.NewGapService("BLE Server"))
			d.AddService(service.NewGattService())

			// Add a custom service to handle incoming data
			customService := service.NewGattService()
			d.AddService(customService)

			// Advertise device name and service's UUIDs.
			d.AdvertiseNameAndServices("BLE Server", []gatt.UUID{customService.UUID()})

		default:
		}
	}

	d.Init(onStateChanged)

	// Register a handler to handle incoming data
	d.Handle(gatt.CharValueWrite(func(r gatt.Request, data []byte) byte {
		// This function will be called when data is written to a characteristic on the server
		fmt.Printf("Received data from client: %s\n", string(data))
		return gatt.StatusSuccess // Return success status
	}))

	select {}
}
