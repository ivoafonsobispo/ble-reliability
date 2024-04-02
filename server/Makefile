run: build
	@sudo ./bin/bluetooth-server

install:
	@go get ./...
	@go mod vendor
	@go mod tidy
	@go mod download

build:
	@go build -o bin/bluetooth-server