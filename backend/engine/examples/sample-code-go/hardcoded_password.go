package main

import "fmt"

func connectToDB() {
    // ❌ Hardcoded password
    password := "s3cretP@ssw0rd!"
    _ = password
    fmt.Println("Connecting to database...")
}

func main() {
    // ❌ Hardcoded API key
    apiKey := "sk-12345-abcdef-67890"
    _ = apiKey
    connectToDB()
}
