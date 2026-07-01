def process_data(user_input):
    # ❌ Unsafe eval with user input
    result = eval(user_input)
    return result


def safe_operation():
    # ✅ Safe: literal eval
    import ast
    result = ast.literal_eval("{'key': 'value'}")
    return result


def also_unsafe():
    user_provided = "__import__('os').system('rm -rf /')"
    # ❌ Unsafe eval with variable
    result = eval(user_provided)
    return result


def main():
    print(process_data("1 + 1"))
    print(safe_operation())
    print(also_unsafe())
