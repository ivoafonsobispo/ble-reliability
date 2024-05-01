package main

import "time"

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
