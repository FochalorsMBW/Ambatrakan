import sys

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Replace hardcoded BluePrimaryDark with dynamic primary color
    content = content.replace('BluePrimaryDark', 'MaterialTheme.colorScheme.primary')
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

if __name__ == "__main__":
    process_file(sys.argv[1])
