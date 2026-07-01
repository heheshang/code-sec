def calculate(a, b):
    return a + b


def format_greeting(name):
    return f"Hello, {name}!"


def main():
    result = calculate(1, 2)
    msg = format_greeting("World")
    print(f"{result} - {msg}")
