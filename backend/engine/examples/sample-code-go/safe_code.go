package main

import "fmt"

func add(a, b int) int {
    return a + b
}

func greet(name string) string {
    return fmt.Sprintf("Hello, %s!", name)
}

func main() {
    result := add(3, 4)
    msg := greet("World")
    fmt.Printf("%d - %s\n", result, msg)
}
