package main

import (
    "fmt"
    "os/exec"
)

func runCommand(userInput string) {
    // ❌ Command injection: variable argument to exec.Command
    out, err := exec.Command("bash", "-c", userInput).Output()
    if err != nil {
        fmt.Printf("Error: %v\n", err)
    }
    fmt.Printf("Output: %s\n", out)
}

func runSafeCommand() {
    // ✅ Safe: only hardcoded arguments
    out, _ := exec.Command("ls", "-l", "/tmp").Output()
    fmt.Printf("Output: %s\n", out)
}

func main() {
    runCommand("echo hello; rm -rf /")
    runSafeCommand()
}
