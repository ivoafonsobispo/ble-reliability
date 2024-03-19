package util

import (
	"fmt"
	"log"

	"tinygo.org/x/bluetooth"
)

func connectToDevice(deviceAddress bluetooth.Address) {
	println("Connecting to device:", deviceAddress.String())

	server, err := bluetooth.NewServer(bluetooth.UUID16(0x1234))
	if err != nil {
		log.Fatalf("Failed to create server: %v", err)
	}

	defer server.Close()

	// Start listening for incoming connections.
	err = server.Advertise("GolangBluetoothServer", nil)
	if err != nil {
		log.Fatalf("Failed to advertise: %v", err)
	}

	fmt.Println("Waiting for incoming connections...")

	// Accept incoming connections.
	conn, err := server.Accept()
	if err != nil {
		log.Fatalf("Failed to accept connection: %v", err)
	}

	fmt.Println("Accepted connection from:", conn.RemoteAddress())

	// Handle incoming data from the client.
	for {
		data, err := conn.Read()
		if err != nil {
			log.Fatalf("Failed to read data: %v", err)
		}
		fmt.Printf("Received data: %s\n", data)

		// Process data received from the client here.

		// Send response back to the client.
		response := "Hello from Golang server"
		_, err = conn.Write([]byte(response))
		if err != nil {
			log.Fatalf("Failed to write data: %v", err)
		}
	}
}
